-- Step 1: Add nullable order_id to payment (zero-downtime)
ALTER TABLE `payment`
  ADD COLUMN `order_id` BIGINT NULL AFTER `paymentNo`;

-- Step 2: Backfill order_id using existing orders.payment_no mapping
UPDATE `payment` p
JOIN `orders` o ON o.`payment_no` = p.`paymentNo`
SET p.`order_id` = o.`order_no`
WHERE o.`payment_no` IS NOT NULL
  AND p.`order_id` IS NULL;

-- Step 3: Add UNIQUE constraint (allow NULLs; uniqueness enforced when not null)
CREATE UNIQUE INDEX `ux_payment_order_id` ON `payment`(`order_id`);

-- Step 4: Add FK to orders (nullable FK is allowed)
ALTER TABLE `payment`
  ADD CONSTRAINT `fk_payment_order`
  FOREIGN KEY (`order_id`) REFERENCES `orders`(`order_no`);

