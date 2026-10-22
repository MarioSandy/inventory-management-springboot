ALTER TABLE data_bulk_import_log RENAME COLUMN total_chunk TO total_actual_chunk;
ALTER TABLE data_bulk_import_log RENAME COLUMN processed_chunk TO processed_actual_chunk;

ALTER TABLE data_bulk_import_log
    ADD COLUMN total_raw_chunk INT,
    ADD COLUMN max_chunk_size INT;