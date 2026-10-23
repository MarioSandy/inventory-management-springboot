package com.example.inventory_api_springboot.repository.inventory;

import com.example.inventory_api_springboot.dto.query.request.InventoryStockOpnameDto;
import com.example.inventory_api_springboot.dto.query.response.InventoryStockOpnameExcelDto;
import com.example.inventory_api_springboot.dto.queue.InventoryStockOpnameExcelPayloadDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InventoryNativeRepositoryImpl implements InventoryNativeRepository {
    @PersistenceContext
    private EntityManager entityManager;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<InventoryStockOpnameExcelDto> getInventoryForStockOpnameExcel(UUID cursor) {
        List<InventoryStockOpnameExcelDto> listReturnedData = new ArrayList<>();
        String queryString = "WHERE i.\"freeze\" IS NOT TRUE ";

        if (cursor != null) {
            queryString += "AND pv.id > :cursor ";
        }

        Query query = entityManager.createNativeQuery(
            "SELECT i.product_variant_id AS id, " +
            "       pv.sku, " +
            "       p.name, " +
            "       i.quantity, " +
            "       i.reserved_quantity, " +
            "       i.quantity - i.reserved_quantity " +
            "FROM inventory i " +
            "JOIN product_variant pv " +
            "   ON i.product_variant_id = pv.id " +
            "JOIN product p " +
            "   ON pv.product_id = p.id " +
            queryString +
            "ORDER BY pv.id ASC " +
            "LIMIT 2001 "
        );

        if (cursor != null) query.setParameter("cursor", cursor);

        List<Object[]> returnedList = query.getResultList();

        for (Object[] currentReturnedData : returnedList) {
            listReturnedData.add(
                new InventoryStockOpnameExcelDto(
                    (UUID) currentReturnedData[0],
                    (String) currentReturnedData[1],
                    (String) currentReturnedData[2],
                    (int) currentReturnedData[3],
                    (int) currentReturnedData[4],
                    (int) currentReturnedData[5]
                )
            );
        }

        return listReturnedData;
    }

    @Override
    @Transactional
    public int[][] inventoryStockOpname(
        List<InventoryStockOpnameDto> listInventoryStockOpname,
        UUID userId
    ) {
        String queryInventory =
            "UPDATE inventory " +
            "   SET quantity = ?, " +
            "       updated_at = CURRENT_TIMESTAMP " +
            "WHERE product_variant_id = ? " +
            "   AND \"freeze\" = TRUE ";

        int[][] resultInventory = jdbcTemplate.batchUpdate(
            queryInventory,
            listInventoryStockOpname,
            100,
            (ps, data) -> {
                ps.setInt(1, data.actualQuantity());
                ps.setObject(2, data.productVariantId());
            }
        );

        String queryInventoryLog =
            "INSERT INTO inventory_log (" +
            "   product_variant_id, " +
            "   quantity_before, " +
            "   quantity_after, " +
            "   quantity_change, " +
            "   reserved_quantity_before, " +
            "   reserved_quantity_after, " +
            "   reserved_quantity_change, " +
            "   type, " +
            "   created_at, " +
            "   created_by " +
            ") VALUES (" +
            "   ?, ?, ?, ?, ?, ?, 0, 'STOCK_OPNAME', CURRENT_TIMESTAMP, ?" +
            ") ";

        List<InventoryStockOpnameDto> listCopyInventoryStockOpname =
            new ArrayList<>(listInventoryStockOpname);

        for (int[] currentResultInventory: resultInventory) {
            for (int i = 0; i < currentResultInventory.length; i++) {
                if (currentResultInventory[i] == 0)
                    listCopyInventoryStockOpname.remove(i);
            }
        }

        jdbcTemplate.batchUpdate(
            queryInventoryLog,
            listCopyInventoryStockOpname,
            100,
            (ps, data) -> {
                ps.setObject(1, data.productVariantId());
                ps.setInt(2, data.systemQuantity());
                ps.setInt(3, data.actualQuantity());
                ps.setInt(
                    4,
                    data.actualQuantity() - data.systemQuantity()
                );
                ps.setInt(5, data.systemReservedQuantity());
                ps.setInt(6, data.systemReservedQuantity());
                ps.setObject(7, userId);
            }
        );

        return resultInventory;
    }

//    @Override
//    @Transactional
//    public void approveStockOpnameReview(
//        UUID stockOpnameReviewId,
//        int shortage,
//        List<InventoryLog> listInventoryLog
//    ) {
//        Query query = entityManager.createNativeQuery(
//            "UPDATE inventory i " +
//            "   SET quantity = s.actual_quantity, " +
//            "       reserved_quantity = reserved_quantity - :shortage " +
//            "FROM stock_opname_review s " +
//            "WHERE i.product_variant_id = s.product_variant_id " +
//            "   AND s.id = :stockOpnameReviewId "
//        );
//
//        query.setParameter("shortage", shortage);
//        query.setParameter("stockOpnameReviewId", stockOpnameReviewId);
//        query.executeUpdate();
//
//        entityManager.
//    }
}
