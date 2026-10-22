ALTER TABLE product_brand
    ALTER COLUMN id TYPE UUID DEFAULT gen_random_uuid();