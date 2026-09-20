INSERT INTO sellers (id, name, status) VALUES
(1, 'Chennai BuildMart', 'APPROVED'),
(2, 'Sri Lakshmi Traders', 'APPROVED'),
(3, 'Metro Materials', 'PENDING'),
(4, 'South India Supplies', 'REJECTED');

INSERT INTO products (id, name, description, category, unit) VALUES
(1, 'OPC 53 Grade Cement', 'High strength ordinary portland cement for structural work', 'Cement', 'Bag'),
(2, 'TMT Steel Rod 12mm', 'Fe500D TMT bars for reinforcement', 'Steel', 'Tonne'),
(3, 'Red Clay Bricks', 'First class burnt clay bricks', 'Bricks', 'Piece'),
(4, 'PVC Pipe 4 inch', 'High pressure PVC pipes for drainage', 'Pipes', 'Meter'),
(5, 'M-Sand', 'Manufactured sand for plastering and concrete', 'Sand', 'CubicFt');

INSERT INTO seller_listings (seller_id, product_id, price, stock_quantity, minimum_order_quantity, status) VALUES
(1, 1, 450.00, 500, 10, 'ACTIVE'),
(2, 1, 435.00, 200, 50, 'ACTIVE');

INSERT INTO seller_listings (seller_id, product_id, price, stock_quantity, minimum_order_quantity, status) VALUES
(1, 2, 65000.00, 20, 1, 'ACTIVE'),
(2, 2, 67000.00, 15, 1, 'ACTIVE');

INSERT INTO seller_listings (seller_id, product_id, price, stock_quantity, minimum_order_quantity, status) VALUES
(1, 3, 8.50, 10000, 100, 'ACTIVE');

INSERT INTO seller_listings (seller_id, product_id, price, stock_quantity, minimum_order_quantity, status) VALUES
(2, 4, 120.00, 100, 5, 'STOPPED');

INSERT INTO seller_listings (seller_id, product_id, price, stock_quantity, minimum_order_quantity, status) VALUES
(1, 5, 65.00, 0, 100, 'ACTIVE');

INSERT INTO seller_listings (seller_id, product_id, price, stock_quantity, minimum_order_quantity, status) VALUES
(3, 1, 410.00, 1000, 20, 'ACTIVE');

INSERT INTO seller_listings (seller_id, product_id, price, stock_quantity, minimum_order_quantity, status) VALUES
(4, 2, 60000.00, 50, 1, 'ACTIVE');
