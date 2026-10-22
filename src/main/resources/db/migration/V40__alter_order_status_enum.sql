ALTER TYPE order_status_enum ADD VALUE 'RESERVED';
ALTER TYPE order_status_enum RENAME VALUE 'FAILED' TO 'CANCELLED';

ALTER TABLE "order"
  ALTER COLUMN status TYPE order_status_enum
  USING status::text::order_status_enum;