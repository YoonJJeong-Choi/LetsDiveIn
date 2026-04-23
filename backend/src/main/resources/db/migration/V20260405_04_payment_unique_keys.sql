-- paymentKey, pgOrderId에 고유 제약(Nullable 허용: 여러 NULL은 허용됨)
CREATE UNIQUE INDEX `ux_payment_payment_key` ON `payment`(`paymentKey`);
CREATE UNIQUE INDEX `ux_payment_pg_order_id` ON `payment`(`pgOrderId`);

