ALTER TABLE order_detail
    DROP COLUMN "freeze";

ALTER TABLE inventory
    ADD COLUMN "freeze" BOOLEAN;