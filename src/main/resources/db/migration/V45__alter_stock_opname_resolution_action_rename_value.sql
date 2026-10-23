ALTER TYPE stock_opname_review_resolution_enum RENAME VALUE 'REQUEST_RECOUNT' TO 'CORRECT_INVENTORY_MOVEMENT';

ALTER TABLE stock_opname_review
ALTER COLUMN resolution_action TYPE stock_opname_review_resolution_enum
  USING resolution_action::text::stock_opname_review_resolution_enum;