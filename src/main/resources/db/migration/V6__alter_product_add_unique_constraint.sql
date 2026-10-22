ALTER TABLE product
    ADD CONSTRAINT unique_product UNIQUE(product_brand_id, product_category_id, name)