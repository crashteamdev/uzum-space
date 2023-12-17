ALTER TABLE uzum_account_shop_item_strategy DROP CONSTRAINT fk_uzum_account_shop_item_strategy_strategy_option;

ALTER TABLE uzum_account_shop_item_strategy DROP COLUMN strategy_option_id;

ALTER TABLE strategy_option ADD uzum_account_shop_item_strategy_id BIGSERIAL;

ALTER TABLE strategy_option
    ADD CONSTRAINT fk_strategy_option_uzum_account_shop_item_strategy_id
        FOREIGN KEY (uzum_account_shop_item_strategy_id) REFERENCES uzum_account_shop_item_strategy (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uzum_account_shop_item_strategy_id_idx
    ON strategy_option (uzum_account_shop_item_strategy_id);