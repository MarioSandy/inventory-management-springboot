ALTER TABLE outbox
    ADD COLUMN created_by UUID NOT NULL,
    ADD CONSTRAINT fk_user FOREIGN KEY (created_by) REFERENCES "user"(id);