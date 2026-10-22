# [MAJOR] Klaim order `PENDING`: lock ada tapi tidak mengikat, dan `UPDATE` tanpa guard status

- **ID**: CRIT-04
- **Severity**: Major
- **Status**: DIREVISI (revisi 2). Klaim awal "tanpa lock" **dicabut** — `FOR UPDATE SKIP LOCKED`
  sudah ada. Masalah sebenarnya ada di transaksi & guard status.
- **File**: `src/main/java/com/example/inventory_api_springboot/repository/order/OrderNativeRepositoryImpl.java`

## Masalah
1. `getReadyPendingOrderIdsForProcess()` menjalankan **dua** statement terpisah
   (`SELECT ... FOR UPDATE SKIP LOCKED`, lalu `UPDATE ... SET status='PROCESSING'`) tetapi method
   ini tidak berada dalam transaksi (custom fragment tidak di-cover transaksi default Spring Data).
2. `UPDATE "order" SET status = 'PROCESSING' WHERE id IN :orderIds` tidak punya guard
   `AND status = 'PENDING'` dan row count-nya tidak diperiksa.
3. Bila `listOrderIds` kosong memang sudah di-return, tetapi tidak ada penanganan bila `UPDATE`
   hanya meng-update sebagian id (mis. sebagian sudah dibatalkan).

## Alasan (Reasoning)
`SELECT ... FOR UPDATE` hanya menahan lock selama transaksi aktif. Tanpa transaksi, lock langsung
lepas setelah statement selesai, sehingga dua worker bisa memperoleh id yang sama -> order
diproses dua kali (reservasi inventory ganda, `user_balance` terpotong ganda). Tanpa guard status,
order yang sudah `CANCELLED` bisa "dihidupkan" kembali menjadi `PROCESSING`.

## Solusi (High Level)
1. Tambahkan `@Transactional` pada method (atau klaim memakai satu statement
   `UPDATE ... WHERE id IN (SELECT ... FOR UPDATE SKIP LOCKED) RETURNING id`).
2. Tambahkan `AND status = 'PENDING'` pada `UPDATE` dan bandingkan jumlah row yang kembali.
3. Terapkan pola yang sama pada `getReadyProcessOrder` (saat ini sudah pakai CTE + lock).

## Dampak
Double processing order: stok berkurang ganda, saldo user terpotong ganda, dan status order
berubah tanpa kontrol state machine.
