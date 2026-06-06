-- V11: Multiple photos per order (up to 3) + payment records history

-- Photo URLs stored as JSON array text
ALTER TABLE orders ADD COLUMN IF NOT EXISTS photo_urls TEXT;

-- Payment records: detailed installment history per order
CREATE TABLE IF NOT EXISTS payment_records (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    amount        NUMERIC(15, 2) NOT NULL,
    payment_date  DATE        NOT NULL,
    payment_method VARCHAR(100),
    notes         TEXT,
    created_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_records_order_id ON payment_records(order_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.payment_records TO anon, authenticated;
