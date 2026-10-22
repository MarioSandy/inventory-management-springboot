ALTER TABLE user_balance_log RENAME COLUMN balance TO balance_change;
ALTER TABLE user_balance_log RENAME COLUMN reserved_balance TO reserved_balance_change;

ALTER TABLE user_balance_log
    ADD COLUMN balance_before NUMERIC DEFAULT 0 NOT NULL,
    ADD COLUMN balance_after NUMERIC DEFAULT 0 NOT NULL,
    ADD COLUMN reserved_balance_before NUMERIC DEFAULT 0 NOT NULL,
    ADD COLUMN reserved_balance_after NUMERIC DEFAULT 0 NOT NULL,
    ALTER COLUMN balance_change TYPE NUMERIC,
    ALTER COLUMN reserved_balance_change TYPE NUMERIC;