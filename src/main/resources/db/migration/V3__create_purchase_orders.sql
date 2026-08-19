CREATE TABLE purchase_orders (
    id UUID PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    total NUMERIC(18, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_reference VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_purchase_orders_status CHECK (status IN ('PENDING', 'PAID')),
    CONSTRAINT ck_purchase_orders_total_non_negative CHECK (total >= 0),
    CONSTRAINT ck_purchase_orders_currency CHECK (currency = upper(currency)),
    CONSTRAINT ck_purchase_orders_payment_reference CHECK (
        payment_reference IS NULL OR btrim(payment_reference) <> ''
    )
);

CREATE TABLE purchase_order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(18, 2) NOT NULL,
    CONSTRAINT fk_purchase_order_items_order FOREIGN KEY (order_id)
        REFERENCES purchase_orders (id) ON DELETE CASCADE,
    CONSTRAINT uk_purchase_order_items_line UNIQUE (order_id, line_number),
    CONSTRAINT ck_purchase_order_items_product_name_not_blank CHECK (btrim(product_name) <> ''),
    CONSTRAINT ck_purchase_order_items_sku_not_blank CHECK (btrim(sku) <> ''),
    CONSTRAINT ck_purchase_order_items_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_purchase_order_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_purchase_order_items_line_total_non_negative CHECK (line_total >= 0)
);

CREATE INDEX idx_purchase_orders_created_at ON purchase_orders (created_at DESC);
CREATE INDEX idx_purchase_order_items_order_id ON purchase_order_items (order_id);
