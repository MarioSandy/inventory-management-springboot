ALTER TABLE product
    ADD CONSTRAINT product_unique UNIQUE(name, product_category_id, product_brand_id);

ALTER TABLE product_variant
    ADD CONSTRAINT product_variant_unique UNIQUE(id, color, "size");