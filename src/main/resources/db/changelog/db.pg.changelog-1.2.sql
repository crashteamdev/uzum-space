--liquibase formatted sql
--changeset vitaxa:recreate-competitor-fk
ALTER TABLE uzum_account_shop_item_competitor
    DROP CONSTRAINT fk_uzum_account_shop_item_competitor_uzum_account_shop_item;

ALTER TABLE uzum_account_shop_item_competitor
    ADD CONSTRAINT fk_uzum_account_shop_item_competitor_uzum_account_shop_item
        FOREIGN KEY (uzum_account_shop_item_id) REFERENCES uzum_account_shop_item
            ON DELETE CASCADE;

