ALTER TABLE order_detail
    DROP COLUMN product_id,
    ADD COLUMN product_variant_id UUID NOT NULL,
    ADD CONSTRAINT fk_product_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant(id);