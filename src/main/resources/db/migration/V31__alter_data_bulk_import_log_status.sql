ALTER TYPE data_bulk_import_log_status_enum RENAME VALUE 'SUCCESS' TO 'FINISH';

ALTER TABLE data_bulk_import_log
  ALTER COLUMN status TYPE data_bulk_import_log_status_enum
  USING status::text::data_bulk_import_log_status_enum;