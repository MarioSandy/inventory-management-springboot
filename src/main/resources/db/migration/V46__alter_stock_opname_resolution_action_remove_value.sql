ALTER TYPE stock_opname_review_resolution_enum RENAME TO stock_opname_review_resolution_enum_old;

CREATE TYPE stock_opname_review_resolution_enum AS ENUM ('REJECT', 'RECONCILE_TO_ACTUAL', 'RESOLVE_RESERVATION_SHORTAGE');

ALTER TABLE stock_opname_review
ALTER COLUMN resolution_action TYPE stock_opname_review_resolution_enum
  USING resolution_action::text::stock_opname_review_resolution_enum;

DROP TYPE stock_opname_review_resolution_enum_old;