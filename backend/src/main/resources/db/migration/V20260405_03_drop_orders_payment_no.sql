-- Drop legacy orders.payment_no (after migration and code switch)
-- 1) Drop FK and index referencing orders.payment_no if exist
-- The exact constraint/index names may differ across envs; adjust if needed.
ALTER TABLE `orders` DROP FOREIGN KEY IF EXISTS `fk_orders_payment_no`;
DROP INDEX IF EXISTS `ux_orders_payment_no` ON `orders`;

-- 2) Set to NULL prior to drop (optional safety; column will be dropped next)
UPDATE `orders` SET `payment_no` = NULL WHERE `payment_no` IS NOT NULL;

-- 3) Drop column
ALTER TABLE `orders` DROP COLUMN `payment_no`;

