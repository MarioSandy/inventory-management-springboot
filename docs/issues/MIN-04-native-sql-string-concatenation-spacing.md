# [DITARIK] Dugaan konkatenasi SQL tanpa spasi

- **ID**: MIN-04
- **Severity**: -
- **Status**: DITARIK (revisi 2) — tidak ditemukan bukti
- **File**: `repository/inventory/InventoryRepository.java`,
  `repository/order/OrderNativeRepositoryImpl.java`, `repository/OutboxRepository.java`
- **Catatan**: nama file dipertahankan agar tautan issue lama tidak putus.

## Masalah
Dugaan awal: fragmentasi string native query berpotensi menghasilkan `... il.reference_idWHERE ...`.

## Hasil Verifikasi
Seluruh fragment pada file yang tersedia sudah benar: setiap fragment diakhiri spasi, atau
fragment berikutnya diawali spasi (contoh: `"   ON od.id = il.reference_id"` diikuti
`"   AND od.id = :orderDetailId "`). **Tidak ada bug spasi.**

## Saran (pencegahan, bukan bug)
Query native di file ini sudah sangat panjang dan sulit dibaca. Untuk perubahan berikutnya,
gunakan Java Text Block (`"""`) agar spasi/newline eksplisit, dan jalankan test berbasis
Testcontainers untuk query native.
