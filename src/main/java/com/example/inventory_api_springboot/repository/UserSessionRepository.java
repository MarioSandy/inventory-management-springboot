package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    Optional<UserSession> findByClientSessionIdAndDeletedAtIsNull(UUID clientSessionId);

    @Transactional
    @Modifying
    @Query(value =
            "UPDATE UserSession us " +
            "   SET deletedAt = :now " +
            "WHERE userId = :id " +
            "   AND deletedAt IS NULL "
    )
    int logoutAll(
            @PathVariable("id")  UUID userId,
            @PathVariable("now") LocalDateTime now
    );
}
