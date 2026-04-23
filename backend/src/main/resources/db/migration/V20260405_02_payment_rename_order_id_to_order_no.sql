-- 1) 새 컬럼 추가
ALTER TABLE `payment`
  ADD COLUMN `order_no` BIGINT NULL AFTER `order_id`;

-- 2) 데이터 백필: order_id -> order_no
UPDATE `payment`
SET `order_no` = `order_id`
WHERE `order_id` IS NOT NULL AND `order_no` IS NULL;

-- 3) 기존 제약/인덱스 제거 (이름이 다를 수 있어 IF EXISTS 사용이 불가한 DB도 있으므로 존재 시 수동 제거 필요)
-- 주: 아래 DROP INDEX/CONSTRAINT 이름은 이전 마이그레이션에서 생성한 이름과 일치해야 합니다.
DROP INDEX `ux_payment_order_id` ON `payment`;
ALTER TABLE `payment` DROP FOREIGN KEY `fk_payment_order`;

-- 4) order_id 컬럼 삭제
ALTER TABLE `payment` DROP COLUMN `order_id`;

-- 5) 새로운 제약/인덱스 생성
CREATE UNIQUE INDEX `ux_payment_order_no` ON `payment`(`order_no`);
ALTER TABLE `payment`
  ADD CONSTRAINT `fk_payment_order_no`
  FOREIGN KEY (`order_no`) REFERENCES `orders`(`order_no`);

