# [MAJOR] Override `findById` dengan `PESSIMISTIC_WRITE`; `@EntityGraph` + `FOR UPDATE`

- **ID**: MAJ-02
- **Severity**: Major
- **Status**: CONFIRMED (revisi 2)
- **File**: `src/main/java/com/example/inventory_api_springboot/repository/order/OrderRepository.java`

## Masalah
1. `@Lock(PESSIMISTIC_WRITE) Optional<Order> findById(UUID id);` meng-override CRUD standar,
   sehingga **semua** pemanggil (termasuk untuk baca/display) ikut mengambil row lock.
2. `@Lock(PESSIMISTIC_WRITE)` + `@EntityGraph(attributePaths = {"listOrderDetails"})` pada
   `findWithDetailsByIdAndStatus`.

## Alasan (Reasoning)
- Menaruh lock di `findById` meningkatkan kontensi dan latensi endpoint read, serta mempermudah
  deadlock. Lock hanya efektif di dalam transaksi; `SimpleJpaRepository.findById` berjalan pada
  transaksi `readOnly` default, sehingga pada sebagian driver `SELECT ... FOR UPDATE` di transaksi
  read-only bisa error atau diabaikan (perilaku tidak konsisten).
- `@EntityGraph` pada `@OneToMany` menghasilkan `LEFT OUTER JOIN`. PostgreSQL menolak locking pada
  sisi nullable outer join: `FOR UPDATE cannot be applied to the nullable side of an outer join`.
  Jadi method `findWithDetailsByIdAndStatus` berpotensi gagal pada runtime.

## Solusi (High Level)
1. Hapus `@Lock` dari `findById`; buat method eksplisit `findWithLockById` untuk kebutuhan mutasi.
2. Untuk fetch + lock, ambil parent dengan lock lebih dulu, lalu load detail pada statement
   terpisah (atau pakai `@Lock` tanpa outer join, mis. `INNER JOIN`/`JOIN FETCH` yang aman).
3. Terapkan `@QueryHint` lock timeout secara konsisten dan pastikan pemanggilan selalu di dalam transaksi.

## Dampak
Penurunan performa endpoint read, deadlock/timeout sporadis, dan kemungkinan error pada endpoint
yang memakai `findWithDetailsByIdAndStatus`.
