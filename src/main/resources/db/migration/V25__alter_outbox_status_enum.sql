ALTER TABLE outbox
  ALTER COLUMN status TYPE outbox_status_enum
  USING status::text::outbox_status_enum;