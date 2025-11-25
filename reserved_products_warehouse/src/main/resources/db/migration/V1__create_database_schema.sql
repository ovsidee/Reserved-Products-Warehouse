CREATE TABLE inventory_item (
    sku VARCHAR(255) PRIMARY KEY,
    available INT NOT NULL,
    reserved INT NOT NULL,
    version BIGINT NOT NULL
);

CREATE TABLE item_reserved_event (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_inventory_item
        FOREIGN KEY(sku) REFERENCES inventory_item(sku)
);

CREATE INDEX idx_item_reserved_event_sku ON item_reserved_event(sku);