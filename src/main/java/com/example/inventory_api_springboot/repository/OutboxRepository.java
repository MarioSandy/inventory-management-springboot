package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.Outbox;
import com.example.inventory_api_springboot.enums.OutboxEventTypeEnum;
import com.example.inventory_api_springboot.projection.OutboxPendingTriggerProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    @Transactional
    @Query(
        value =
            "UPDATE outbox SET " +
            "   status = 'PROCESSING', " +
            "   locked_at = CURRENT_TIMESTAMP " +
            "WHERE id IN ( " +
            "   SELECT id " +
            "   FROM outbox " +
            "   WHERE status IN (" +
            "       'PENDING', " +
            "       'DISPATCH_FAILED' " +
            "   ) " +
            "       AND event_type NOT IN :listType " +
            "       AND locked_at IS NULL " +
            "       AND retry_count <= 3 " +
            "   ORDER BY created_at ASC " +
            "   LIMIT 100 " +
            "   FOR UPDATE SKIP LOCKED " +
            ") " +
            "RETURNING id, event_type, entity_id, created_by, payload ",
        nativeQuery = true
    )
    public List<OutboxPendingTriggerProjection> getAndMarkedPendingOutbox(
        @Param("listType") List<OutboxEventTypeEnum> eventTypes
    );

    @Transactional
    @Query(
        value =
            "UPDATE outbox SET " +
            "   status = 'PROCESSING', " +
            "   locked_at = CURRENT_TIMESTAMP " +
            "WHERE id IN ( " +
            "   SELECT id " +
            "   FROM outbox " +
            "   WHERE status IN (" +
            "       'PENDING', " +
            "       'DISPATCH_FAILED' " +
            "   ) " +
            "       AND event_type = 'ORDER_PROCESSING' " +
            "       AND locked_at IS NULL " +
            "       AND retry_count <= 3 " +
            "   ORDER BY created_at ASC " +
            "   LIMIT 100 " +
            "   FOR UPDATE " +
            ") " +
            "RETURNING id, event_type, entity_id, created_by, payload ",
            nativeQuery = true
    )
    public List<OutboxPendingTriggerProjection> getAndMarkedPendingOrderOutbox();

    @Modifying
    @Transactional
    @Query(
        "UPDATE Outbox SET " +
        "   status = 'DISPATCHED', " +
        "   dispatchedAt = CURRENT_TIMESTAMP, " +
        "   lockedAt = NULL " +
        "WHERE id = :id "
    )
    public int updateStatusSuccessDispatch(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query(
        "UPDATE Outbox SET " +
        "   status = 'PENDING', " +
        "   dispatchedAt = CURRENT_TIMESTAMP, " +
        "   lockedAt = NULL, " +
        "   retryCount = retryCount + 1, " +
        "   error = :error " +
        "WHERE id = :id "
    )
    public int updateStatusForRetryDispatch(@Param("id") UUID id, @Param("error") String error);

    @Modifying
    @Transactional
    @Query(
        "UPDATE Outbox SET " +
        "   status = 'DISPATCH_FAILED', " +
        "   dispatchedAt = CURRENT_TIMESTAMP, " +
        "   lockedAt = NULL, " +
        "   error = :error " +
        "WHERE id = :id "
    )
    public int updateStatusFailedDispatch(@Param("id") UUID id, @Param("error") String error);
}
