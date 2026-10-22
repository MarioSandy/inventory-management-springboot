ALTER TYPE user_balance_log_type_enum ADD VALUE 'REFUND_STOCK_SHORTAGE';

ALTER TABLE user_balance_log
  ALTER COLUMN type TYPE user_balance_log_type_enum
  USING type::text::user_balance_log_type_enum;