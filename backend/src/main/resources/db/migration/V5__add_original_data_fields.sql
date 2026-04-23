-- 상품 원본 데이터 필드 추가 (수정 취소를 위한 백업 필드)
ALTER TABLE product ADD COLUMN IF NOT EXISTS original_product_name VARCHAR(255);
ALTER TABLE product ADD COLUMN IF NOT EXISTS original_product_type VARCHAR(50);
ALTER TABLE product ADD COLUMN IF NOT EXISTS original_product_sub_type VARCHAR(50);
ALTER TABLE product ADD COLUMN IF NOT EXISTS original_product_price VARCHAR(20);
ALTER TABLE product ADD COLUMN IF NOT EXISTS original_product_description TEXT;
ALTER TABLE product ADD COLUMN IF NOT EXISTS original_product_image_url VARCHAR(500);

-- 옵션 원본 데이터 필드 추가 (수정 취소를 위한 백업 필드)
ALTER TABLE "option" ADD COLUMN IF NOT EXISTS original_color VARCHAR(100);
ALTER TABLE "option" ADD COLUMN IF NOT EXISTS original_size VARCHAR(50);
ALTER TABLE "option" ADD COLUMN IF NOT EXISTS original_option_add_price BIGINT;
