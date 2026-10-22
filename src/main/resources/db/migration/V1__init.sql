CREATE TYPE user_role_enum AS ENUM ('ADMIN', 'CUSTOMER');
CREATE TYPE order_status_enum AS ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');
CREATE TYPE idempotency_key_enum AS ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED');

CREATE TABLE "user" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
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

CREATE TABLE user_role (
    user_id UUID,
    role user_role_enum DEFAULT 'CUSTOMER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    CONSTRAINT user_role_pkey PRIMARY KEY (user_id, role),
    CONSTRAINT fk_users FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE TABLE product_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(150),
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE product_color (
    id VARCHAR(5) PRIMARY KEY,
    color VARCHAR(20),
    hex_code CHAR(6),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE product_size (
    size VARCHAR(4) PRIMARY KEY,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE product_brand (
    id CHAR(4) PRIMARY KEY,
    name VARCHAR(150),
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE DEFAULT NULL
);

CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    product_category_id UUID NOT NULL,
    product_brand_id CHAR(4) NOT NULL,
    base_price NUMERIC(10, 2) NOT NULL,
    version INT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_product_category FOREIGN KEY (product_category_id) REFERENCES product_category(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_brand FOREIGN KEY (product_brand_id) REFERENCES product_brand(id) ON DELETE CASCADE
);

CREATE TABLE product_variant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID,
    sku VARCHAR(18) UNIQUE NOT NULL, -- [BRAND_ID]-[5 NUMBER LENGTH eg. 00001]-[COLOR]-[SIZE]
    color VARCHAR(20) NOT NULL,
    size VARCHAR(4) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_product_color FOREIGN KEY (color) REFERENCES product_color(id),
    CONSTRAINT fk_product_size FOREIGN KEY (size) REFERENCES product_size(size)
);

CREATE TABLE inventory (
    product_id UUID PRIMARY KEY,
    quantity INT,
    reserved_quantity INT,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE "order" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    status order_status_enum DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user"(id)
);

CREATE TABLE order_detail (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    sub_total NUMERIC(10, 2) NOT NULL,
    CONSTRAINT fk_order FOREIGN KEY (order_id) REFERENCES "order"(id),
    CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE TABLE idempotency_key (
    id BIGINT,
    user_id UUID NOT NULL,
    key UUID NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    status idempotency_key_enum DEFAULT 'PENDING',
    response_status INT,
    response_body VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES "user"(id),
    CONSTRAINT unique_user_key UNIQUE(user_id, key)
);