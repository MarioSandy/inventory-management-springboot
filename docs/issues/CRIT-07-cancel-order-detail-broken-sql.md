# [CRITICAL] `cancelOrderDetail`: SQL tidak valid (3 titik) + join `od.id = o.id`

- **ID**: CRIT-07
- **Severity**: Critical
- **Status**: BARU (revisi 2)
- **File**: `src/main/java/com/example/inventory_api_springboot/repository/order/OrderNativeRepositoryImpl.java`

## Masalah
1. `FROM order o` — `order` adalah reserved keyword PostgreSQL dan **tidak di-quote**, padahal di
   method lain di file yang sama dipakai `FROM "order" o`.
2. `JOIN order_detail od` tanpa klausa `ON` (langsung diikuti `WHERE`).
3. `FOR UPDATE od` — sintaks PostgreSQL yang benar adalah `FOR UPDATE OF od`.
4. CTE `order_item_counted` memilih **`o.id`** (id order) sebagai `id`, lalu `selected` di-join
   dengan `JOIN selected s ON od.id = s.id` -> membandingkan `order_detail.id` dengan `order.id`.
5. Bila hasil kosong, `WHERE id IN :ids` dieksekusi dengan list kosong (SQL `IN ()` tidak valid).
6. `SUM(quantity) OVER (ORDER BY o.created_at DESC)` tanpa tie-breaker -> urutan tidak deterministik.

## Alasan (Reasoning)
Poin 1-3 membuat query gagal di level parser (`SQLGrammarException`) sehingga fungsinya tidak
pernah berjalan. Bahkan bila sintaks dibetulkan, poin 4 membuat hasil selalu kosong (kecuali
kebetulan id order = id detail), sehingga `shortage` tidak pernah dikonversi menjadi pembatalan
order detail. Poin 5 membuat jalur "tidak ada yang perlu dibatalkan" justru melempar error.

## Solusi (High Level)
1. Pakai Java Text Block dan quote identifier reserved: `FROM "order" o`.
2. Lengkapi `ON` pada setiap join dan pakai `FOR UPDATE OF od`.
3. Ubah CTE agar memilih `od.id` (atau join ulang lewat `order_id`) supaya id yang dipakai konsisten.
4. Lewati eksekusi `UPDATE` bila list kosong.
5. Tambahkan `created_at, id` sebagai tie-breaker pada window function.

## Dampak
Auto-cancel order detail saat stok kurang tidak pernah berjalan (atau error 500), sehingga stok
dan order tidak pernah dikoreksi: pesanan tetap menahan alokasi yang tidak ada.
