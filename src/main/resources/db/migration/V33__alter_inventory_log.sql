ALTER TABLE inventory_log RENAME COLUMN quantity TO quantity_change;
ALTER TABLE inventory_log RENAME COLUMN reserved_quantity TO reserved_quantity_change;

ALTER TABLE inventory_log
    ADD COLUMN quantity_before int NOT NULL,
    ADD COLUMN quantity_after int NOT NULL,
    ADD COLUMN reserved_quantity_before int NOT NULL,
    ADD COLUMN reserved_quantity_after int NOT NULL,
    DROP CONSTRAINT fk_stock_opname_review,
    DROP COLUMN stock_opname_review_id;

ALTER TYPE inventory_log_type_enum ADD VALUE 'STOCK_OPNAME';
ALTER TYPE inventory_log_type_enum ADD VALUE 'RESERVATION_RELEASE';
