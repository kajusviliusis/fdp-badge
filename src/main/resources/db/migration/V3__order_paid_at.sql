-- Sales are attributed to the moment of payment (used by the monthly bestseller ranking).
ALTER TABLE orders ADD COLUMN paid_at TIMESTAMP WITH TIME ZONE;

-- Orders paid before this column existed: the best available approximation is their creation time.
UPDATE orders SET paid_at = created_at WHERE status IN ('CONFIRMED', 'SHIPPED', 'DELIVERED');

CREATE INDEX idx_orders_paid_at ON orders (paid_at);
