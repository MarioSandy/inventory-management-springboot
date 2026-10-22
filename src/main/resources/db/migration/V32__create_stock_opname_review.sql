CREATE TYPE stock_opname_review_resolution_enum AS ENUM (
    'RECONCILE_TO_ACTUAL',
    'REQUEST_RECOUNT',
    'REJECT',
    'RESOLVE_RESERVATION_SHORTAGE'
);

CREATE TABLE stock_opname_review (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL,
    product_variant_id UUID NOT NULL,
    system_qty INT NOT NULL,
    actual_qty INT NOT NULL,
    system_reserved_qty INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    root_cause TEXT DEFAULT NULL,
    investigation_note TEXT DEFAULT NULL,
    resolution_action stock_opname_review_resolution_enum  DEFAULT NULL,
    reviewed_by UUID DEFAULT NULL,
    reviewed_at TIMESTAMP DEFAULT NULL,
    CONSTRAINT source_unique UNIQUE(source_id, product_variant_id),
    CONSTRAINT fk_user FOREIGN KEY (reviewed_by) REFERENCES "user"(id),
    CONSTRAINT fk_product_variant FOREIGN KEY (product_variant_id) REFERENCES product_variant(id)
);

ALTER TABLE inventory_log
    ADD COLUMN stock_opname_review_id UUID NOT NULL,
    ADD CONSTRAINT fk_stock_opname_review FOREIGN KEY (stock_opname_review_id) REFERENCES stock_opname_review(id);