CREATE TABLE IF NOT EXISTS products (
    id BIGINT PRIMARY KEY CHECK (id > 0),
    name VARCHAR(200) NOT NULL,
    price NUMERIC(14, 2) NOT NULL CHECK (price >= 0)
);

INSERT INTO products (id, name, price) VALUES
    (1, 'Mechanical keyboard', 1250000.00),
    (2, 'Wireless mouse', 450000.00),
    (3, 'USB-C hub', 790000.00)
ON CONFLICT (id) DO NOTHING;
