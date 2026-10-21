-- Transactional outbox table for async notification events.
-- Rows are inserted in the same transaction as the task write so
-- the database is always the source of truth.  A relay service
-- (OutboxRelayService) reads undelivered rows and publishes them
-- to Kafka, then marks delivered = true.
--
-- event_id (UUID) is the idempotency key; the UNIQUE constraint
-- prevents re-insertion of the same logical event and lets consumers
-- safely deduplicate by event_id.

CREATE TABLE outbox_events (
    id          BIGSERIAL    PRIMARY KEY,
    event_id    UUID         NOT NULL UNIQUE,
    event_type  VARCHAR(64)  NOT NULL,
    payload     JSONB        NOT NULL,
    delivered   BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts    SMALLINT     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_undelivered ON outbox_events (delivered, created_at)
    WHERE delivered = FALSE;
