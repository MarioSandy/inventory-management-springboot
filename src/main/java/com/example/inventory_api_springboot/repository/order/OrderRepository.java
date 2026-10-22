package com.example.inventory_api_springboot.repository.order;

import com.example.inventory_api_springboot.entity.Order;
import com.example.inventory_api_springboot.enums.OrderStatusEnum;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderNativeRepository {

    /**
     * Mengambil order sekaligus mengunci barisnya (SELECT ... FOR UPDATE).
     *
     * <p>Khusus untuk alur MUTASI. Jangan dipakai untuk endpoint baca/display, karena lock
     * menambah kontensi, latensi, dan risiko deadlock.
     *
     * <p>Lock hanya mengikat bila method ini dipanggil di dalam transaksi aktif. Bila dipanggil
     * di luar transaksi, perilakunya bergantung driver (bisa error, bisa diabaikan).
     *
     * <p>Catatan: saat ini belum ada pemanggil. Method ini disediakan sebagai jalur eksplisit
     * untuk kebutuhan mutasi, menggantikan lock yang sebelumnya dipasang pada {@code findById}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "-2"
        )
    })
    Optional<Order> findWithLockById(UUID id);

    @Modifying
    @Query(
        "UPDATE Order " +
        "   SET status = :status " +
        "WHERE id = :id "
    )
    void updateOrderStatus(@Param("id") UUID id, @Param("status") OrderStatusEnum status);

    /**
     * Versi {@link #findWithLockById(UUID)} dengan filter status, dipakai untuk transisi state
     * ({@code PENDING -> CANCELLED}, {@code RESERVED -> COMPLETED}).
     *
     * <p>Query ini hanya menyentuh tabel {@code order} (tanpa join), sehingga aman dipasangi
     * {@code FOR UPDATE}. Detail order dimuat lazy di dalam transaksi pemanggil.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(
            name = "jakarta.persistence.lock.timeout",
            value = "-2"
        )
    })
    Optional<Order> findByIdAndStatus(UUID id, OrderStatusEnum status);

    /**
     * Mengambil order beserta detail-nya TANPA lock.
     *
     * <p>{@code @EntityGraph} pada relasi {@code @OneToMany} menghasilkan LEFT OUTER JOIN, dan
     * PostgreSQL menolak {@code FOR UPDATE} pada sisi nullable outer join
     * ("FOR UPDATE cannot be applied to the nullable side of an outer join"). Karena itu method
     * ini sengaja TIDAK memakai {@code @Lock}.
     *
     * <p>Bila butuh lock sekaligus detail, kunci parent lebih dulu lewat
     * {@link #findByIdAndStatus(UUID, OrderStatusEnum)}, lalu baca detail di dalam transaksi
     * yang sama.
     */
    @EntityGraph(attributePaths = { "listOrderDetails" })
    Optional<Order> findWithDetailsByIdAndStatus(UUID id, OrderStatusEnum status);
}
