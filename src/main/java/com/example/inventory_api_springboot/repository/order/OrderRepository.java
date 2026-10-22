package com.example.inventory_api_springboot.repository.order;

import com.example.inventory_api_springboot.entity.Order;
import com.example.inventory_api_springboot.enums.OrderStatusEnum;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderNativeRepository {

    /**
     * Transisi status dengan guard status asal (compare-and-set di level database).
     *
     * <p>Karena {@code WHERE ... AND status = :expectedStatus}, transisi hanya terjadi bila status
     * baris saat ini benar-benar sama dengan {@code expectedStatus}. Kondisi balapan (mis. order
     * sudah dibatalkan oleh transaksi lain) otomatis menghasilkan 0 baris, bukan menimpa status.
     *
     * @return jumlah baris ter-update. Nilai selain 1 berarti guard tidak terpenuhi dan
     *         transisi TIDAK terjadi; caller wajib memperlakukannya sebagai kegagalan.
     *
     * <p>Propagation MANDATORY: method ini adalah UPDATE, sehingga wajib berjalan di dalam
     * transaksi aktif. Tanpa transaksi, perubahan bisa ter-commit sebagian (autocommit per
     * statement) dan kegagalan tidak bisa di-rollback bersama operasi lain.
     */
    @Modifying
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(
        "UPDATE Order " +
        "   SET status = :newStatus " +
        "WHERE id = :id " +
        "   AND status = :expectedStatus "
    )
    int updateOrderStatus(
        @Param("id") UUID id,
        @Param("newStatus") OrderStatusEnum newStatus,
        @Param("expectedStatus") OrderStatusEnum expectedStatus
    );

    /**
     * Mengambil order sekaligus mengunci barisnya ({@code SELECT ... FOR UPDATE}), dengan filter
     * status.
     *
     * <p>Lock sengaja TANPA {@code jakarta.persistence.lock.timeout = -2} (SKIP LOCKED) agar
     * transaksi yang bersaing MENUNGGU, bukan melompati baris. Setelah transaksi pemegang lock
     * selesai, PostgreSQL mengevaluasi ulang filter status pada versi baris terbaru, sehingga
     * transisi ganda (double cancel / double finish) tetap tidak mungkin terjadi: transisi kedua
     * akan mendapat 0 baris dan berujung 404.
     *
     * <p>Khusus alur MUTASI. Jangan dipakai untuk endpoint baca/display, karena lock menambah
     * kontensi dan latensi.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByIdAndStatus(UUID id, OrderStatusEnum status);
}
