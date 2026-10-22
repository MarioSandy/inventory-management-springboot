# [CRITICAL] `UserBalanceRepository`: nama kolom tidak konsisten (`ubl.balance` / `ubl.reserved_balance`)

- **ID**: CRIT-05
- **Severity**: Critical
- **Status**: DIREVISI (revisi 2). Dugaan awal "update multi-row per `reference_id`" **tidak
  terbukti** (sudah ada filter `type = 'ORDER_RESERVED'` + `NOT EXISTS`). Masalah nyata: nama kolom.
- **File**: `src/main/java/com/example/inventory_api_springboot/repository/UserBalanceRepository.java`
- **Catatan**: nama file dipertahankan agar tautan issue lama tidak putus.

## Masalah
Dalam **query yang sama** dipakai dua penamaan kolom yang berbeda untuk `user_balance_log`:

`cancelOrder`
