ALTER TABLE product_variant
    ADD CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES product(id);