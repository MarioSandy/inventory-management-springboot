# [MAJOR] Outbox: row `PROCESSING` tidak pernah dipulihkan & `DISPATCH_FAILED` bisa di-retry tanpa batas

- **ID**: CRIT-03
- **Severity**: Major
- **Status**: DIREVISI (revisi 2). Klaim awal "tanpa `FOR UPDATE SKIP LOCKED`" **dicabut** —
  query memang sudah memakai `FOR UPDATE SKIP LOCKED` di dalam sub-query.
- **File**: `src/main/java/com/example/inventory_api_springboot/repository/OutboxRepository.java`
- **Catatan**: nama file dipertahankan agar tautan issue lama tidak putus.

## Masalah
1. Klaim outbox mensyaratkan `locked_at IS NULL` dan `status IN ('PENDING','DISPATCH_FAILED')`.
   Bila proses mati setelah `UPDATE ... SET status='PROCESSING', locked_at=NOW()`, row tersebut
   **tidak akan pernah** diambil lagi (status `PROCESSING` tidak masuk filter, `locked_at` tidak null).
2. `updateStatusFailedDispatch` mengubah status ke `DISPATCH_FAILED` **tanpa** menaikkan
   `retry_count`, sedangkan filter klaim membatasi `retry_count <= 3`. Event yang selalu gagal
   dapat diklaim ulang terus-menerus (loop).
3. `updateStatusForRetryDispatch` juga mengisi `dispatchedAt`, padahal event belum dispatched
   (semantik timestamp jadi menyesatkan).

## Alasan (Reasoning)
Pola outbox butuh dua mekanisme: (a) **recovery** untuk row yang menggantung akibat crash, dan
(b) **batas percobaan** yang benar-benar bertambah pada jalur gagal. Tanpa (a), satu crash
membuat event hilang permanen (data antar-service tidak sinkron). Tanpa (b) pada jalur
`DISPATCH_FAILED`, scheduler akan terus mengulang event yang memang tidak mungkin sukses
(CPU/queue penuh, log banjir).

## Solusi (High Level)
1. Tambahkan recovery: klaim juga row `PROCESSING` yang `locked_at < NOW() - interval 'x menit'`.
2. Pastikan `retry_count` bertambah pada **semua** jalur gagal, dan gunakan satu guard batas.
3. Pisahkan kolom `dispatched_at` (sukses) dari `last_attempt_at` (percobaan).

## Dampak
Event hilang permanen (bisnis tidak pernah menerima event) atau infinite retry loop yang
membebani DB dan RabbitMQ.
