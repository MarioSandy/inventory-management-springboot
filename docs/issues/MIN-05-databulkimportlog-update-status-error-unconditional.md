# [MINOR] `DataBulkImportLogRepository`: tanpa `@Transactional`, literal enum di JPQL, tanpa guard status

- **ID**: MIN-05
- **Severity**: Minor (berpotensi naik karena efek transaksi)
- **Status**: CONFIRMED (revisi 2)
- **File**: `src/main/java/com/example/inventory_api_springboot/repository/DataBulkImportLogRepository.java`

## Masalah
1. Keempat method `@Modifying` tidak punya `@Transactional`.
2. `updateStatusError` (`SET status = 'ERROR'`) dan `updateStatusFinish` (`SET status = 'FINISH'`)
   menulis enum sebagai literal string di dalam JPQL.
3. `updateStatusError` tidak punya guard status (menimpa status apa pun, termasuk yang sudah `FINISH`).
4. `updateStatusFinish` mensyaratkan `processedActualChunk = totalActualChunk`; bila
   `incrementTotalRawAndActualChunk` datang setelahnya, status tidak pernah menjadi `FINISH`.
5. Return value `int` tidak diperiksa pemanggil.

## Alasan (Reasoning)
- Tanpa transaksi, `@Modifying` melempar `TransactionRequiredException` atau berjalan autocommit
  per statement sehingga counter chunk bisa tidak konsisten.
- Literal string untuk properti enum tidak tervalidasi compiler dan perilakunya bergantung versi
  Hibernate; rename pada `DataBulkImportLogStatusEnum` tidak akan terdeteksi.
- Tanpa guard, job yang sudah selesai bisa berubah menjadi `ERROR` hanya karena satu chunk
  terlambat gagal -> laporan import menyesatkan.

## Solusi (High Level)
1. Tambahkan `@Transactional` + bind status lewat parameter enum (`:status`).
2. Batasi transisi status (mis. hanya dari `PROCESSING`) dan periksa row count.
3. Pertimbangkan state machine import terpusat di service agar race `total` vs `processed` tertangani.

## Dampak
Status import tidak akurat, job bisa "selesai" tanpa pernah ditandai `FINISH`, dan monitoring
bulk import tidak dapat dipercaya.
