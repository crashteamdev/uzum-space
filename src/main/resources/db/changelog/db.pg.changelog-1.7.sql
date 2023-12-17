ALTER TABLE uzum_account_shop_item DROP CONSTRAINT fk_uzum_account_shop_item_strategy_id_uzum_account_shop_item;

DROP INDEX uzum_account_shop_item_shop_item_strategy_id_idx;

ALTER TABLE uzum_account_shop_item DROP COLUMN uzum_account_shop_item_strategy_id;

ALTER TABLE uzum_account_shop_item_strategy ADD uzum_account_shop_item_id uuid;

ALTER TABLE uzum_account_shop_item_strategy
    ADD CONSTRAINT fk_uzum_account_shop_item_id_uzum_account_shop_item_strategy_id
        FOREIGN KEY (uzum_account_shop_item_id) REFERENCES uzum_account_shop_item (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX shop_item_strategy_uzum_account_shop_item_id_idx
    ON uzum_account_shop_item_strategy (uzum_account_shop_item_id);