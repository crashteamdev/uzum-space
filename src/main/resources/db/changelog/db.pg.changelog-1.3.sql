--liquibase formatted sql
--changeset vitaxa:create-category-index
CREATE INDEX IF NOT EXISTS uzum_shop_item_category_id_index
    ON uzum_shop_item (category_id);

--changeset vitaxa:create-p_hash-index
CREATE INDEX IF NOT EXISTS uzum_shop_item_p_hash_fingerprint_index
    on uzum_shop_item (p_hash_fingerprint);

