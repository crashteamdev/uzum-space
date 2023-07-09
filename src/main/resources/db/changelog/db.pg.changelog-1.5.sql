CREATE TYPE strategy_type AS ENUM ('close_to_minimal','quantity_dependent');

CREATE TABLE strategy_option
(
    id                BIGSERIAL PRIMARY KEY,
    minimum_threshold BIGINT,
    maximum_threshold BIGINT
);

CREATE TABLE uzum_account_shop_item_strategy
(

    id                 BIGSERIAL PRIMARY KEY,
    strategy_type      strategy_type NOT NULL DEFAULT 'close_to_minimal',
    strategy_option_id BIGINT,

    CONSTRAINT fk_uzum_account_shop_item_strategy_strategy_option
        FOREIGN KEY (strategy_option_id) REFERENCES strategy_option (id) ON DELETE CASCADE
);

ALTER TABLE uzum_account_shop_item
    ADD uzum_account_shop_item_strategy_id BIGINT;

ALTER TABLE uzum_account_shop_item
    ADD CONSTRAINT fk_uzum_account_shop_item_strategy_id_uzum_account_shop_item
        FOREIGN KEY (uzum_account_shop_item_strategy_id) REFERENCES uzum_account_shop_item_strategy (id) ON DELETE CASCADE;

CREATE UNIQUE INDEX uzum_account_shop_item_shop_item_strategy_id_idx ON uzum_account_shop_item (uzum_account_shop_item_strategy_id);

ALTER TABLE strategy_option
    ADD step INT;

ALTER TYPE strategy_type ADD VALUE 'equal_price'
