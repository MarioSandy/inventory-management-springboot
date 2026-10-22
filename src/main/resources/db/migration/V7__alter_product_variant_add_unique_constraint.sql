ALTER TABLE product_variant
    ADD CONSTRAINT unique_product_variant UNIQUE(product_id, color, "size")