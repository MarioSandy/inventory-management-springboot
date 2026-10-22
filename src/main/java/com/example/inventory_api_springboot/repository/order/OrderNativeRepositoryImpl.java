package com.example.inventory_api_springboot.repository.order;

import com.example.inventory_api_springboot.dto.query.response.OrderReadyProcessDetailDto;
import com.example.inventory_api_springboot.dto.query.response.OrderReadyProcessDto;
import com.example.inventory_api_springboot.dto.response.OrderDetailCancelResponseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class OrderNativeRepositoryImpl implements OrderNativeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    public List<OrderDetailCancelResponseDto> cancelOrderDetail(
        UUID productVariantId,
        int shortage
    ) {
        String queryOrderLocked =
            "WITH order_item_counted AS (" +
            "   SELECT o.id, " +
            "          od.quantity, " +
            "          SUM(quantity) OVER (" +
            "               ORDER BY o.created_at DESC " +
            "           ) AS cumulative_reserved " +
            "   FROM order o " +
            "   JOIN order_detail od " +
            "   WHERE o.status = 'ORDER_RESERVED' " +
            "       AND od.product_variant_id = :productVariantId " +
            "   ORDER BY o.created_at DESC " +
            "), " +
            "selected AS (" +
            "   SELECT id " +
            "   FROM order_item_counted " +
            "   WHERE cumulative_reserved - quantity < :shortage " +
            ") " +
            "SELECT od.id," +
            "       o.user_id," +
            "       od.quantity, " +
            "       od.sub_total " +
            "FROM order o " +
            "JOIN order_detail od " +
            "   ON o.id = od.order_id " +
            "JOIN selected s " +
            "   ON od.id = s.id " +
            "FOR UPDATE od ";

        Query query = entityManager.createNativeQuery(queryOrderLocked);
        query.setParameter("productVariantId", productVariantId);
        query.setParameter("shortage", shortage);
        List<Object[]> returnedList = query.getResultList();

        List<OrderDetailCancelResponseDto> listOrderDetailCancel = new ArrayList<>();
        List<UUID> listOrderIds = new ArrayList<>();

        for (Object[] currentReturnedList : returnedList) {
            listOrderIds.add(
                (UUID) currentReturnedList[0]
            );

            listOrderDetailCancel.add(
                new OrderDetailCancelResponseDto(
                    (UUID) currentReturnedList[0],
                    (UUID) currentReturnedList[1],
                    (int) currentReturnedList[2],
                    (BigDecimal) currentReturnedList[3]
                )
            );
        }

        String queryUpdateStatus =
            "UPDATE order_detail " +
            "   SET cancelled = TRUE " +
            "WHERE id IN :ids ";

        query = entityManager.createNativeQuery(queryUpdateStatus);
        query.setParameter("ids", listOrderIds);
        query.executeUpdate();

        return listOrderDetailCancel;
    }

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
            "   FOR UPDATE SKIP LOCKED "+
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

    public List<UUID> getReadyPendingOrderIdsForProcess() {
        Query query = entityManager.createNativeQuery(
            "SELECT id " +
            "FROM \"order\" o " +
            "WHERE o.status = 'PENDING' " +
            "   AND NOT EXISTS ( " +
            "       SELECT 1 " +
            "       FROM inventory i " +
            "       JOIN order_detail od " +
            "           ON i.product_variant_id = od.product_variant_id " +
            "       WHERE o.id = od.order_id " +
            "           AND \"freeze\" = TRUE " +
            ") " +
            "ORDER BY o.created_at ASC " +
            "LIMIT 500 " +
            "FOR UPDATE SKIP LOCKED",
            UUID.class
        );

        List<UUID> listOrderIds = query.getResultList();

        if (listOrderIds.isEmpty())
            return listOrderIds;

        query = entityManager.createNativeQuery(
            "UPDATE \"order\" " +
            "   SET status = 'PROCESSING' " +
            "WHERE id IN :orderIds " +
            "RETURNING id ",
            UUID.class
        );

        query.setParameter("orderIds", listOrderIds);
        return query.getResultList();
    }
}
