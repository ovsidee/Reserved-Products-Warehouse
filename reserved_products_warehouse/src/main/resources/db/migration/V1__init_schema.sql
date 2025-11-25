CREATE TABLE inventory_item (
    sku VARCHAR(255) PRIMARY KEY,
    available INTEGER NOT NULL CHECK (available >= 0),
    reserved INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE domain_events (
    id UUID PRIMARY KEY,
    sku VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Helps if we ever need to query for a specific product
CREATE INDEX idx_domain_events_sku ON domain_events(sku);