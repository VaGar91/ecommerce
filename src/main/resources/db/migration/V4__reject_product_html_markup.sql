ALTER TABLE products
    ADD CONSTRAINT ck_products_plain_text
    CHECK (
        position('<' in name) = 0
        AND position('>' in name) = 0
        AND position('<' in sku) = 0
        AND position('>' in sku) = 0
        AND position('<' in description) = 0
        AND position('>' in description) = 0
        AND position('<' in category) = 0
        AND position('>' in category) = 0
    ) NOT VALID;
