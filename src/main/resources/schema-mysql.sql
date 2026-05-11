DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
                        order_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_date DATETIME NOT NULL,
                        customer_id BIGINT NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        amount DECIMAL(15,2) NOT NULL,
                        created_at DATETIME NOT NULL,
                        INDEX idx_orders_order_date (order_date),
                        INDEX idx_orders_customer_status (customer_id, status),
                        INDEX idx_orders_status (status)
);