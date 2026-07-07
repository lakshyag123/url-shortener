-- V1: Create url_mapping table
-- PostgreSQL best practices: use IDENTITY, timestamptz, explicit constraints, and indexes

CREATE TABLE IF NOT EXISTS url_mapping (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  original_url TEXT NOT NULL CHECK (char_length(original_url) <= 2048),
  short_code VARCHAR(64) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  click_count BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT url_mapping_short_code_unique UNIQUE (short_code),
  CONSTRAINT short_code_format CHECK (short_code ~ '^[A-Za-z0-9_-]{4,64}$')
);

-- index to speed up lookups by short_code (unique constraint already creates an index,
-- but explicit index included for clarity and to ensure index name across DBs)
CREATE UNIQUE INDEX IF NOT EXISTS idx_url_mapping_short_code ON url_mapping (short_code);

-- Consider partitioning click/event tables later if volume grows; click_count kept as
-- an aggregated authoritative field (can be periodically reconciled with event store).
