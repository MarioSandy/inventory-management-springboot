CREATE TYPE outbox_event_type_enum AS ENUM (
    'CREATE_PRODUCT_BATCH',
    'STOCK_OPNAME',
    'FILE_ORCHESTRA_PRODUCT_BATCH_CREATE',
    'FILE_ORCHESTRA_INVENTORY_STOCK_OPNAME'
);

CREATE TYPE outbox_status_enum AS ENUM(
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED'
);

CREATE TYPE data_bulk_import_log_status_enum AS ENUM ('PENDING', 'SUCCESS', 'ERROR');

CREATE TABLE outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_id UUID NOT NULL,
    entity VARCHAR(255) NOT NULL,
    event_type outbox_event_type_enum NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatched_at TIMESTAMP NULL,
    status outbox_status_enum NOT NULL,
    retry_count INT NOT NULL,
    error TEXT NULL
);

CREATE TABLE data_bulk_import_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_key VARCHAR(255) NOT NULL,
    status data_bulk_import_log_status_enum NOT NULL,
    total_chunk INT,
    processed_chunk INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL
);

CREATE TABLE data_bulk_import_log_error (
   data_bulk_import_log_id UUID NOT NULL,
   row_number INT NOT NULL UNIQUE,
   error TEXT NOT NULL,
   CONSTRAINT data_bulk_import_log_error_pkey PRIMARY KEY (data_bulk_import_log_id, row_number),
   CONSTRAINT fk_data_bulk_import_log FOREIGN KEY (data_bulk_import_log_id) REFERENCES data_bulk_import_log(id)
);