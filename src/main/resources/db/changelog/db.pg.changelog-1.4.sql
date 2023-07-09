--liquibase formatted sql
--changeset vitaxa:uzum-account-sku-title
ALTER TABLE uzum_account_shop ADD sku_title CHARACTER VARYING;
