# [MEDIUM] Hidden dropdown sheet tidak benar-benar disembunyikan

**File:** `src/main/java/com/example/inventory_api_springboot/service/ProductService.java` (`createHiddenDropdownExcelSheet`)
**Severity:** MEDIUM

## Problem
Method dinamai `createHiddenDropdownExcelSheet`, namun dua baris berikut masih berada dalam komentar (ditandai dua garis miring) di source code:
- `int sheetIndex = workbook.getSheetIndex(sheetName);`
- `workbook.setSheetVisibility(sheetIndex, SheetVisibility.VERY_HIDDEN);`

Efeknya, sheet `product_color`, `product_size`, `product_category`, `product_brand` muncul sebagai sheet biasa di workbook.

## Reasoning
- User yang membuka template akan melihat 4 sheet "internal" — membingungkan dan memperbesar ukuran file pada saat disimpan balik oleh user.
- `sheet.protectSheet(...)` juga masih dikomentari, sehingga data master pada sheet tersebut dapat diedit user. Ini dapat merusak validasi dropdown pada saat submit.

## Solution (high level)
- Re-enable pemanggilan `workbook.setSheetVisibility(sheetIndex, SheetVisibility.VERY_HIDDEN)` (impor `org.apache.poi.ss.usermodel.SheetVisibility`) dan (opsional) `sheet.protectSheet(...)`.
- Pindahkan perilaku ini ke test pembuatan Excel template agar tetap terjaga.

## Notes
- Dari seluruh file di folder `issues/`, hanya file ini yang memiliki baris dalam code block yang dimulai dengan karakter slash. File lain aman.
- Format markdown di atas sengaja menghindari code block dengan baris yang dimulai dengan karakter slash agar tidak disalahartikan sebagai path file oleh tooling.
- Jika tooling masih komplain pada file lain, kirimkan log errornya — akan disesuaikan formatnya.
