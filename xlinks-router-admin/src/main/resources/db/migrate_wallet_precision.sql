ALTER TABLE `customer_main_wallets`
    MODIFY COLUMN `total_balance` decimal(18,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `available_balance` decimal(18,6) NOT NULL DEFAULT '0.000000';

ALTER TABLE `customer_sub_wallets`
    MODIFY COLUMN `balance` decimal(18,6) NOT NULL DEFAULT '0.000000';

ALTER TABLE `customer_main_wallet_flows`
    MODIFY COLUMN `change_amount` decimal(18,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `total_balance_before` decimal(18,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `total_balance_after` decimal(18,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `available_balance_before` decimal(18,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `available_balance_after` decimal(18,6) NOT NULL DEFAULT '0.000000';

ALTER TABLE `customer_sub_wallet_flows`
    MODIFY COLUMN `change_amount` decimal(18,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `balance_before` decimal(18,6) NOT NULL DEFAULT '0.000000',
    MODIFY COLUMN `balance_after` decimal(18,6) NOT NULL DEFAULT '0.000000';
