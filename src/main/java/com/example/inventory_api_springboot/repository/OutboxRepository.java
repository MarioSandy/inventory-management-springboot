package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.Outbox;
import com.example.inventory_api_springboot.projection.OutboxPendingTriggerProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

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
            "       AND locked_at IS NULL " +
            "       AND retry_count <= 3 " +
            "   ORDER BY created_at ASC " +
            "   LIMIT 100 " +
            "   FOR UPDATE SKIP LOCKED " +
            ") " +
            "RETURNING id, event_type, entity_id, created_by, payload ",
        nativeQuery = true
    )
    public List<OutboxPendingTriggerProjection> getAndMarkedPendingOutbox();

    @Modifying
    @Transactional
    @Query(
        "UPDATE Outbox SET " +
        "   status = 'DISPATCHED', " +
        "   dispatchedAt = CURRENT_TIMESTAMP, " +
        "   lockedAt = NULL " +
        "WHERE id = :id "
    )
    public void updateStatusSuccessDispatch(@PathVariable("id") UUID id);

    @Modifying
    @Query(
        "UPDATE Outbox SET " +
        "   status = 'PENDING', " +
        "   dispatchedAt = CURRENT_TIMESTAMP, " +
        "   lockedAt = NULL, " +
        "   retryCount = retryCount + 1, " +
        "   error = :error " +
        "WHERE id = :id "
    )
    public void updateStatusForRetryDispatch(@PathVariable("id") UUID id, @PathVariable("error") String error);

    @Modifying
    @Query(
        "UPDATE Outbox SET " +
        "   status = 'DISPATCH_FAILED', " +
        "   dispatchedAt = CURRENT_TIMESTAMP, " +
        "   lockedAt = NULL, " +
        "   error = :error " +
        "WHERE id = :id "
    )
    public void updateStatusFailedDispatch(@PathVariable("id") UUID id, @PathVariable("error") String error);
}
