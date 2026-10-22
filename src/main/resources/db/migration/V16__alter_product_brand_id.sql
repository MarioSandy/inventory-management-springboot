ALTER TABLE product
    DROP CONSTRAINT fk_product_brand,
    ALTER COLUMN product_brand_id TYPE UUID USING NULL;

ALTER TABLE product_brand
    ALTER COLUMN id TYPE UUID USING NULL;

ALTER TABLE product
    ADD CONSTRAINT fk_product_brand FOREIGN KEY (product_brand_id) REFERENCES product_brand(id);