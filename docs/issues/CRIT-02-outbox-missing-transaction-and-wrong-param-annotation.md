# [CRITICAL] Update status (`@Modifying`) tanpa `@Transactional` dan parameter memakai `@PathVariable`

- **ID**: CRIT-02
- **Severity**: Critical
- **Status**: CONFIRMED (revisi 2)
- **File**:
  - `src/main/java/com/example/inventory_api_springboot/repository/OutboxRepository.java`
  - `src/main/java/com/example/inventory_api_springboot/repository/DataBulkImportLogRepository.java`
  - terkait: `service/OutboxService.java`, `service/DataBulkImportLogService.java`

## Masalah
1. `OutboxRepository.updateStatusForRetryDispatch(...)` & `updateStatusFailedDispatch(...)` memakai
   `@Modifying` **tanpa** `@Transactional`. `getAndMarkedPendingOutbox()` (native
   `UPDATE ... RETURNING`) juga tanpa `@Transactional`.
2. `DataBulkImportLogRepository`: `incrementTotalRawAndActualChunk`,
   `incrementProcessedActualChunk`, `updateStatusError`, `updateStatusFinish` — semuanya
   `@Modifying` **tanpa** `@Transactional`.
3. Parameter binding memakai `@PathVariable("id")` / `@PathVariable("error")`, padahal yang benar
   untuk repository adalah `@Param`.
4. Tidak ada satu pun dari method di atas yang memeriksa jumlah row ter-update (`int` diabaikan
   oleh service).

## Bukti
`updateStatusSuccessDispatch` punya `@Transactional`, dua method saudaranya tidak.
`@PathVariable` jelas anotasi web (paket `org.springframework.web.bind.annotation`).

## Alasan (Reasoning)
- `@Modifying` (UPDATE/DELETE) wajib berjalan di dalam transaksi. Custom/complex query yang
  dipanggil dari service tanpa `@Transactional` akan gagal `TransactionRequiredException`
  ("Executing an update/delete query"), sehingga update status tidak pernah terjadi; atau
  berjalan autocommit per statement sehingga sebagian perubahan tersimpan, sebagian tidak.
- `@PathVariable` tidak dikenal mekanisme binding Spring Data; `:id` hanya terikat "kebetulan"
  bila nama parameter tersimpan di bytecode (`-parameters`). Tanpa itu -> `Named parameter not bound`.
- Row count yang diabaikan membuat kegagalan "0 row ter-update" (mis. karena guard status tidak
  terpenuhi) tidak pernah terdeteksi.

## Solusi (High Level)
1. Tambahkan `@Transactional` pada semua `@Modifying` (atau jadikan service sebagai batas transaksi).
2. Ganti `@PathVariable` -> `@Param` di seluruh repository.
3. Periksa return value `int` di service dan lempar error bila `!= 1`.

## Dampak
Retry mechanism outbox tidak berfungsi (event gagal tidak pernah di-retry/ditandai), status bulk
import tidak pernah berpindah ke `ERROR`/`FINISH`, dan monitoring import menjadi tidak akurat.
