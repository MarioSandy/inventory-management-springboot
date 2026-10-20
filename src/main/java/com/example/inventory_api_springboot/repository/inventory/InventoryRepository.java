package com.example.inventory_api_springboot.repository.inventory;

import com.example.inventory_api_springboot.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID>, InventoryNativeRepository {

    @Transactional
    @Modifying
    @Query(
        value =
            "UPDATE inventory i " +
            "   SET reserved_quantity = i.reserved_quantity - il.reserved_quantity " +
            "FROM inventory_log il " +
            "WHERE i.product_variant_id = il.product_variant_id " +
            "   AND il.reference_id = :referenceId " +
            "   AND il.type = 'ORDER_RESERVED' " +
            "   AND i.reserved_quantity - il.reserved_quantity >= 0 " +
            "   AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM inventory_log il2 " +
            "       WHERE il2.reference_id = il.reference_id " +
            "           AND il2.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            "   ) ",
        nativeQuery = true
    )
    int cancelOrder(@Param("referenceId") UUID referenceId);

    @Transactional
    @Modifying
    @Query(
        value =
            "UPDATE inventory i " +
            "   SET quantity = i.quantity - il.reserved_quantity, " +
            "       reserved_quantity = i.reserved_quantity - il.reserved_quantity " +
            "FROM inventory_log il " +
            "WHERE i.product_variant_id = il.product_variant_id " +
            "   AND il.reference_id = :referenceId " +
            "   AND il.type = 'ORDER_RESERVED' " +
            "   AND i.quantity - il.reserved_quantity >= 0 " +
            "   AND i.reserved_quantity - il.reserved_quantity >= 0 " +
            "   AND NOT EXISTS (" +
            "       SELECT 1 " +
            "       FROM inventory_log il2 " +
            "       WHERE il2.reference_id = il.reference_id " +
            "           AND il2.type IN ('ORDER_CANCELLED', 'ORDER_SHIPPED') " +
            "   ) ",
        nativeQuery = true
    )
    int finishOrder(@Param("referenceId") UUID referenceId);

    @Transactional
    @Modifying
    @Query(
        "UPDATE Inventory " +
        "   SET freeze = TRUE " +
        "WHERE productVariantId IN :ids "
    )
    int freezeInventories(@Param("ids") Set<UUID> id);

    @Transactional
    @Modifying
    @Query(
        "UPDATE Inventory " +
        "   SET freeze = FALSE " +
        "WHERE productVariantId IN :ids "
    )
    int unfreezeInventories(@Param("ids") Set<UUID> id);

    @Transactional
    List<Inventory> findAllByProductVariantIdInAndFreeze(Collection<UUID> listIds, boolean freeze);

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findForUpdateByProductVariantId(UUID productVariantId);

    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findForUpdateByProductVariantIdAndFreeze(
        UUID productVariantId,
        boolean freeze
    );
}
