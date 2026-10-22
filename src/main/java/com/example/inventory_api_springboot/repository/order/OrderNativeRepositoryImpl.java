package com.example.inventory_api_springboot.repository.order;

import com.example.inventory_api_springboot.dto.query.response.OrderReadyProcessDetailDto;
import com.example.inventory_api_springboot.dto.query.response.OrderReadyProcessDto;
import com.example.inventory_api_springboot.dto.response.OrderDetailCancelResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class OrderNativeRepositoryImpl implements OrderNativeRepository {
    private static final String ORDER_DETAIL_CANCEL_UNCHANGED_MESSAGE =
        "Cancelling order detail affected %d row(s) but %d order detail id(s) were locked. " +
        "The locked rows are expected to be updated without exception.";

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    /**
     * Memilih order detail dari order berstatus ORDER_RESERVED untuk variant tertentu yang harus
     * dibatalkan karena stok kurang, lalu menandainya cancelled = TRUE.
     *
     * <p>Urutan pembatalan: order TERBARU dibatalkan lebih dulu, sehingga order terlama tetap
     * mempertahankan alokasinya (fairness untuk order yang lebih awal).
     *
     * <p>Catatan implementasi:
     * <ul>
     *   <li>Window function diletakkan di dalam CTE karena PostgreSQL menolak
     *       {@code FOR UPDATE} pada level query yang memakai window function.</li>
     *   <li>{@code order} adalah reserved keyword sehingga wajib di-quote: {@code "order"}.</li>
     *   <li>CTE memilih {@code od.id} (bukan {@code o.id}) agar join ke {@code selected}
     *       membandingkan kolom yang setara.</li>
     *   <li>Tie-breaker {@code o.id, od.id} dipakai supaya urutan window deterministik saat
     *       {@code created_at} kembar.</li>
     *   <li>{@code od.cancelled = FALSE} menyaring order detail yang sudah pernah dibatalkan,
     *       sehingga unit tersebut TIDAK ikut dihitung dalam {@code cumulative_reserved}.
     *       Tanpa filter ini, reservasi yang sudah dirilis tetap "memakan" kuota shortage dan
     *       jumlah yang dibatalkan bisa kurang dari yang seharusnya.</li>
     *   <li>{@code @Transactional} diperlukan agar lock {@code FOR UPDATE OF od} tetap mengikat
     *       sampai statement {@code UPDATE} berikutnya selesai.</li>
     * </ul>
     */
    @Transactional
    public List<OrderDetailCancelResponseDto> cancelOrderDetail(
        UUID productVariantId,
        int shortage
    ) {
        // Window function ada di dalam CTE supaya level terluar bebas dari window function
        // dan boleh memakai FOR UPDATE.
        Query selectQuery = entityManager.createNativeQuery(
            """
            WITH order_item_counted AS (
                SELECT od.id,
                       od.quantity,
                       SUM(od.quantity) OVER (
                           ORDER BY o.created_at DESC, o.id DESC, od.id DESC
                       ) AS cumulative_reserved
                FROM "order" o
                JOIN order_detail od
                    ON od.order_id = o.id
                WHERE o.status = 'ORDER_RESERVED'
                    AND od.product_variant_id = :productVariantId
                    AND od.cancelled = FALSE
            ), selected AS (
                SELECT id
                FROM order_item_counted
                WHERE cumulative_reserved - quantity < :shortage
            )
            SELECT od.id,
                   o.user_id,
                   od.quantity,
                   od.sub_total
            FROM "order" o
            JOIN order_detail od
                ON od.order_id = o.id
            JOIN selected s
                ON od.id = s.id
            FOR UPDATE OF od
            """
        );

        selectQuery.setParameter("productVariantId", productVariantId);
        selectQuery.setParameter("shortage", shortage);

        List<Object[]> returnedList = selectQuery.getResultList();

        List<OrderDetailCancelResponseDto> listOrderDetailCancel = new ArrayList<>(returnedList.size());
        List<UUID> listOrderDetailIds = new ArrayList<>(returnedList.size());

        for (Object[] currentReturnedList : returnedList) {
            UUID currentOrderDetailId = (UUID) currentReturnedList[0];

            listOrderDetailIds.add(currentOrderDetailId);

            listOrderDetailCancel.add(
                new OrderDetailCancelResponseDto(
                    currentOrderDetailId,
                    (UUID) currentReturnedList[1],
                    (int) currentReturnedList[2],
                    (BigDecimal) currentReturnedList[3]
                )
            );
        }

        if (listOrderDetailIds.isEmpty()) {
            return listOrderDetailCancel;
        }

        Query updateQuery = entityManager.createNativeQuery(
            """
            UPDATE order_detail
               SET cancelled = TRUE
            WHERE id IN :ids
            """
        );

        updateQuery.setParameter("ids", listOrderDetailIds);
        int updatedRows = updateQuery.executeUpdate();

        if (updatedRows != listOrderDetailIds.size()) {
            throw new IllegalStateException(
                String.format(
                    ORDER_DETAIL_CANCEL_UNCHANGED_MESSAGE,
                    updatedRows,
                    listOrderDetailIds.size()
                )
            );
        }

        return listOrderDetailCancel;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<OrderReadyProcessDto> getReadyProcessOrder(UUID id) {
        Query query = entityManager.createNativeQuery(
            "WITH locked AS (" +
            "   SELECT o.id, " +
            "          o.user_id " +
            "   FROM \"order\" o " +
            "   WHERE o.id = :id " +
            "       AND o.status = 'PROCESSING' " +
            "       AND NOT EXISTS ( " +
            "           SELECT 1 " +
            "           FROM order_detail od " +
            "           JOIN inventory i " +
            "               ON od.product_variant_id = i.product_variant_id " +
            "           WHERE od.order_id = o.id " +
            "               AND i.\"freeze\" = TRUE " +
            "       ) " +
            "   ORDER BY o.created_at ASC " +
            "   LIMIT 500 " +
            "   FOR UPDATE "+
            ") " +
            "SELECT  l.id, " +
            "        l.user_id, " +
            "        JSONB_AGG ( " +
            "           JSONB_BUILD_OBJECT ( " +
            "               'id', od.id, " +
            "               'product_variant_id', od.product_variant_id, " +
            "               'quantity', od.quantity, " +
            "               'sub_total', od.sub_total " +
            "           ) " +
            "        )::TEXT AS listOrderDetail  " +
            "FROM locked l " +
            "JOIN order_detail od " +
            "   ON l.id = od.order_id " +
            "GROUP BY l.id, l.user_id "
        );

        query.setParameter("id", id);
        List<Object[]> returnedList = query.getResultList();

        if (returnedList.isEmpty()) return Optional.empty();
        Object[] firstReturnedList = returnedList.get(0);

        return Optional.ofNullable(
            new OrderReadyProcessDto(
                (UUID) firstReturnedList[0],
                (UUID) firstReturnedList[1],
                objectMapper.readValue(
                    (String) firstReturnedList[2],
                    new TypeReference<List<OrderReadyProcessDetailDto>>() {}
                )
            )
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> getReadyPendingOrderIdsForProcess() {
        Query query = entityManager.createNativeQuery(
            "WITH locked AS ( " +
            "   SELECT o.id " +
            "   FROM \"order\" o " +
            "   WHERE o.status = 'PENDING' " +
            "       AND NOT EXISTS ( " +
            "           SELECT 1 " +
            "           FROM inventory i " +
            "           JOIN order_detail od " +
            "               ON i.product_variant_id = od.product_variant_id " +
            "           WHERE o.id = od.order_id " +
            "               AND i.\"freeze\" = TRUE " +
            "       ) " +
            "   ORDER BY o.created_at ASC, o.id ASC " +
            "   LIMIT 500 " +
            "   FOR UPDATE " +
            ") " +
            "UPDATE \"order\" o " +
            "   SET status = 'PROCESSING' " +
            "FROM locked l " +
            "WHERE o.id = l.id " +
            "   AND o.status = 'PENDING' " +
            "RETURNING o.id ",
            UUID.class
        );

        return query.getResultList();
    }
}
