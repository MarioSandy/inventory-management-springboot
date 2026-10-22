ALTER TABLE inventory
    DROP CONSTRAINT fk_product,
    DROP COLUMN product_id,
    ADD COLUMN product_variant_id UUID NOT NULL,
    ADD CONSTRAINT fk_product FOREIGN KEY (product_variant_id) REFERENCES product_variant(id);

ALTER TABLE inventory_log
    DROP CONSTRAINT fk_products,
    DROP COLUMN product_id,
    ADD COLUMN product_variant_id UUID NOT NULL,
    ADD CONSTRAINT fk_product FOREIGN KEY (product_variant_id) REFERENCES product_variant(id);