CREATE TYPE inventory_log_type_enum AS ENUM ('RESTOCK', 'DAMAGED', 'ORDER_RESERVED', 'ORDER_SHIPPED', 'ORDER_CANCELLED');

CREATE TABLE inventory_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id UUID,
    quantity int,
    reserved_quantity int,
    type inventory_log_type_enum,
    reference_id UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_products FOREIGN KEY (product_id) REFERENCES "product"(id),
    CONSTRAINT fk_users FOREIGN KEY (created_by) REFERENCES "user"(id)
);

ALTER TABLE inventory
    ADD CONSTRAINT quantity_non_negative CHECK (quantity >= 0),
    ADD CONSTRAINT reserved_quantity_non_negative CHECK (reserved_quantity >= 0)

