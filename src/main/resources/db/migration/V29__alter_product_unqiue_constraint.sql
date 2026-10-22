ALTER TABLE product
    DROP CONSTRAINT product_unique;

ALTER TABLE product
    ADD CONSTRAINT product_unique UNIQUE(name, product_brand_id);