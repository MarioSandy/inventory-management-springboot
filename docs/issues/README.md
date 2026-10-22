# Daftar Issue Hasil Code Review

> Dokumen ini dibuat otomatis dari review kode. **Tidak ada kode aplikasi yang diubah / difix.**
> Semua issue di folder ini hanya dokumentasi, siap di-push sebagai issue tracker.

## Revisi 2 (verifikasi terhadap file lengkap)

Sumber verifikasi: `ProductSize`, `ProductVariant`, `UserBalance`, `DataBulkImportLogRepository`,
`OutboxRepository`, `ProductBrandRepository`, `ProductSizeRepository`, `UserBalanceRepository`,
`UserRepository`, `InventoryRepository`, `OrderNativeRepositoryImpl`, `OrderRepository`, `NetworkUtil`.

- **CONFIRMED (4):** CRIT-01, CRIT-02, CRIT-06, MAJ-01, MAJ-02, MIN-01, MIN-02, MIN-03, MIN-05.
- **DIREVISI (3):** CRIT-03, CRIT-04, CRIT-05 — kesimpulan awal kurang tepat, arah masalah diganti.
- **DITARIK (1):** MIN-04 — tidak ditemukan bukti.
- **BARU (3):** CRIT-07, CRIT-08 (digabung di CRIT-03), MIN-06.

## Tabel Temuan

| ID | Severity | Status | File Terdampak | Ringkasan |
|----|----------|--------|----------------|-----------|
| CRIT-01 | Critical | Confirmed | `repository/UserRepository.java` | `blockUser` menulis ke tabel `users`, entity memetakan `"user"` |
| CRIT-02 | Critical | Confirmed | `repository/OutboxRepository.java`, `repository/DataBulkImportLogRepository.java` | `@Modifying` tanpa `@Transactional`; parameter pakai `@PathVariable` |
| CRIT-03 | Major | Direvisi | `repository/OutboxRepository.java` | Row `PROCESSING` yang crash tidak pernah dipulihkan; event `DISPATCH_FAILED` bisa di-retry tanpa batas |
| CRIT-04 | Major | Direvisi | `repository/order/OrderNativeRepositoryImpl.java` | Lock ada tapi tidak mengikat (tanpa transaksi); `UPDATE` tanpa guard `status` |
| CRIT-05 | Critical | Direvisi | `repository/UserBalanceRepository.java` | Nama kolom tidak konsisten: `ubl.balance` / `ubl.reserved_balance` vs `*_change` |
| CRIT-06 | Critical | Confirmed | `repository/ProductSizeRepository.java` | Derived query `findById` sementara PK entity adalah `size` |
| CRIT-07 | Critical | Baru | `repository/order/OrderNativeRepositoryImpl.java` | `cancelOrderDetail`: SQL invalid (3x) + join `od.id = o.id` |
| MAJ-01 | Major | Confirmed | `entity/UserBalance.java` | `reservedBalance` bisa negatif -> `balance` juga bisa negatif |
| MAJ-02 | Major | Confirmed | `repository/order/OrderRepository.java` | Override `findById` dengan `PESSIMISTIC_WRITE`; `@EntityGraph` + `FOR UPDATE` |
| MIN-01 | Minor | Confirmed | `util/NetworkUtil.java` | `X-Forwarded-For` dipercaya mentah (IP spoofing) |
| MIN-02 | Minor | Confirmed | `entity/ProductVariant.java` | `generateSku()` NPE & tanpa jaminan unik |
| MIN-03 | Minor | Confirmed | `repository/ProductBrandRepository.java` | `findById`/`restoreById` bertipe `String` padahal ID `UUID` |
| MIN-04 | - | Ditarik | - | Dugaan konkatenasi SQL tanpa spasi: tidak terbukti |
| MIN-05 | Minor | Confirmed | `repository/DataBulkImportLogRepository.java` | Tanpa `@Transactional`, literal enum di JPQL, tanpa guard status |
| MIN-06 | Minor | Baru | `repository/UserRepository.java` | `u.role IN :role` dengan parameter scalar + nama `...Native` menyesatkan |

## Format Tiap File

`Masalah` -> `Alasan (Reasoning)` -> `Solusi (High Level)` -> `Dampak` (+ `Bukti` bila ada).

Setiap file = 1 issue, bisa langsung di-copy ke GitHub/GitLab issue.
