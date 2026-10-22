# [MAJOR] `mutateBalances` tidak mencegah `reservedBalance` negatif (membuka jalan `balance` negatif)

- **ID**: MAJ-01
- **Severity**: Major
- **Status**: CONFIRMED (revisi 2)
- **File**: `src/main/java/com/example/inventory_api_springboot/entity/UserBalance.java`

## Masalah
Validasi hanya `availableBalance = newBalance - newReservedBalance >= 0`. Tidak ada validasi
`newReservedBalance >= 0` maupun `newBalance >= 0`, dan tidak ada null-check.

## Alasan (Reasoning)
Contoh rangkaian yang lolos validasi saat ini: mulai `balance = 10`, `reserved = -100`
(available = 110, lolos) -> `mutateBalances(-50, 0)` -> `balance = -40`, `reserved = -100`,
available = 60 (>= 0) -> **lolos**, padahal saldo user negatif. Artinya satu kali `reserved`
negatif (mis. reversal dipanggil dua kali) membuka celah saldo negatif permanen. Selain itu
`IllegalArgumentException` di entity akan menjadi HTTP 500, bukan 400.

## Solusi (High Level)
1. Tambahkan invariant eksplisit: `newBalance >= 0` dan `newReservedBalance >= 0`.
2. Tambahkan CHECK constraint di DB (`balance >= 0`, `reserved_balance >= 0`) sebagai pertahanan akhir.
3. Gunakan exception domain (bukan `IllegalArgumentException`) agar menjadi 400.
4. Rapikan pesan error (jangan menyusun format string dengan konkatenasi manual).

## Dampak
Potensi kebocoran saldo (uang virtual) dan inkonsistensi data keuangan yang sulit direkonsiliasi.
