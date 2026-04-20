ALTER TABLE orders
    ALTER COLUMN total_price     TYPE NUMERIC(15, 2),
    ALTER COLUMN payment_amount  TYPE NUMERIC(15, 2);
