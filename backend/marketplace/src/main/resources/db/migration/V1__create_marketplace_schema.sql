CREATE TABLE sellers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_seller_status
        CHECK (status IN ('APPROVED', 'PENDING', 'REJECTED'))
);

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seller_listings (
    id BIGSERIAL PRIMARY KEY,

    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,

    price NUMERIC(12, 2) NOT NULL,
    stock_quantity INTEGER NOT NULL,
    minimum_order_quantity INTEGER NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_listing_seller
        FOREIGN KEY (seller_id)
        REFERENCES sellers(id),

    CONSTRAINT fk_listing_product
        FOREIGN KEY (product_id)
        REFERENCES products(id),

    CONSTRAINT uq_seller_product
        UNIQUE (seller_id, product_id),

    CONSTRAINT chk_listing_price
        CHECK (price > 0),

    CONSTRAINT chk_listing_stock
        CHECK (stock_quantity >= 0),

    CONSTRAINT chk_listing_moq
        CHECK (minimum_order_quantity > 0),

    CONSTRAINT chk_listing_status
        CHECK (status IN ('ACTIVE', 'STOPPED'))
);

CREATE INDEX idx_products_category
    ON products(category);

CREATE INDEX idx_products_name
    ON products(name);

CREATE INDEX idx_listings_product
    ON seller_listings(product_id);

CREATE INDEX idx_listings_seller
    ON seller_listings(seller_id);

CREATE INDEX idx_listings_product_status
    ON seller_listings(product_id, status);

CREATE INDEX idx_listings_seller_status
    ON seller_listings(seller_id, status);