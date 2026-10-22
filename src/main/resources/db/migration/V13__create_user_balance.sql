CREATE TYPE user_balance_log_type_enum AS ENUM ('DEPOSIT', 'WITHDRAW', 'ORDER_RESERVED', 'ORDER_SHIPPED', 'ORDER_CANCELLED');

CREATE TABLE user_balance (
    user_id UUID PRIMARY KEY,
    balance BIGINT NOT NULL,
    reserved_balance BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT balance_non_negative CHECK (balance >= 0),
    CONSTRAINT reserved_balance_non_negative CHECK (reserved_balance >= 0)
);

CREATE TABLE user_balance_log (
    user_id UUID PRIMARY KEY,
    balance BIGINT NOT NULL,
    reserved_balance BIGINT NOT NULL,
    type user_balance_log_type_enum,
    reference_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);

ALTER TABLE inventory_log
    ALTER COLUMN created_by SET NOT NULL;