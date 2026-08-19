CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_products_name_search
    ON products USING GIN (lower(name) gin_trgm_ops);

CREATE INDEX idx_products_sku_search
    ON products USING GIN (lower(sku) gin_trgm_ops);

CREATE INDEX idx_products_description_search
    ON products USING GIN (lower(description) gin_trgm_ops);

CREATE INDEX idx_products_category_search
    ON products USING GIN (lower(category) gin_trgm_ops);

CREATE INDEX idx_products_category_filter
    ON products (lower(category));
