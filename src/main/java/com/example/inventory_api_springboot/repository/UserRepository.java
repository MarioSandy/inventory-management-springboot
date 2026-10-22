package com.example.inventory_api_springboot.repository;

import com.example.inventory_api_springboot.entity.User;
import com.example.inventory_api_springboot.enums.UserRoleEnum;
import com.example.inventory_api_springboot.projection.UserDetailProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<UserDetailProjection> getUserDetailById(UUID userId);

    @Query(
            value =
                "SELECT EXISTS (" +
                "   SELECT 1 " +
                "   FROM User u " +
                "   WHERE u.id = :id " +
                "       AND u.role IN :role " +
                ") "
    )
    boolean existsByUserIdAndRolesNative(
            @Param("id") UUID userId,
            @Param("role") UserRoleEnum role
    );

    @Transactional
    @Modifying
    @Query(
            value =
                    "UPDATE users " +
                    " SET deleted_at = CURRENT_TIMESTAMP " +
                    "WHERE id = :id " +
                    "   AND deleted_at IS NULL ",
            nativeQuery = true
    )
    int blockUser(@Param("id") UUID userId);
}
