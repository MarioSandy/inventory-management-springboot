DROP TABLE IF EXISTS user_detail;
DROP TABLE IF EXISTS user_session;
DROP TABLE IF EXISTS user_role;

DROP TYPE user_role_enum;

CREATE TYPE user_role_enum AS ENUM (
    'CUSTOMER',
    'USER_ADMIN',
    'WAREHOUSE_STAFF'
);

ALTER TABLE "user"
    ADD COLUMN address VARCHAR(255) NOT NULL,
    ADD COLUMN role user_role_enum NOT NULL;

CREATE TABLE user_detail (
    user_id UUID PRIMARY KEY,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone_number VARCHAR(15),
    address VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    CONSTRAINT fk_user_details_user FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE user_session (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id UUID,
    client_session_id UUID UNIQUE,
    client_name VARCHAR(50) NOT NULL,
    os_name VARCHAR(50) NOT NULL,
    ip_address VARCHAR(50) NOT NULL,
    token TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    CONSTRAINT fk_users FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);