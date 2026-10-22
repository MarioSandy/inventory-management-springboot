ALTER TABLE data_bulk_import_log
    ADD COLUMN created_by UUID NOT NULL,
    ADD CONSTRAINT fk_user FOREIGN KEY (created_by) REFERENCES "user"(id);