-- 재고 관리에 상품 단위 지원 추가
-- 옵션이 없는 상품도 재고 관리 가능하도록 변경

-- 1. inventory 테이블에 product_no 컬럼 추가 (nullable)
ALTER TABLE inventory ADD COLUMN IF NOT EXISTS product_no BIGINT;

-- 2. product_no에 대한 외래키 추가
ALTER TABLE inventory 
ADD CONSTRAINT fk_inventory_product 
FOREIGN KEY (product_no) REFERENCES product(product_no);

-- 3. option_no를 nullable로 변경 (기존 데이터는 그대로 유지)
-- 먼저 외래키 제약조건 제거
ALTER TABLE inventory DROP CONSTRAINT IF EXISTS inventory_option_no_fkey;
ALTER TABLE inventory DROP CONSTRAINT IF EXISTS fk_inventory_option;

-- option_no 컬럼의 NOT NULL 제약조건 제거 (nullable로 변경)
ALTER TABLE inventory ALTER COLUMN option_no DROP NOT NULL;

-- option_no 외래키 재생성 (nullable 허용)
ALTER TABLE inventory 
ADD CONSTRAINT fk_inventory_option 
FOREIGN KEY (option_no) REFERENCES "option"(option_no);

-- 4. CHECK 제약조건 추가: option_no와 product_no 중 하나는 반드시 존재해야 함
ALTER TABLE inventory DROP CONSTRAINT IF EXISTS inventory_option_or_product_check;
ALTER TABLE inventory 
ADD CONSTRAINT inventory_option_or_product_check 
CHECK (
    (option_no IS NOT NULL AND product_no IS NULL) OR 
    (option_no IS NULL AND product_no IS NOT NULL)
);
