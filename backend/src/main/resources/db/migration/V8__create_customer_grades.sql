-- V8__create_customer_grades.sql
-- 고객 등급 초기 데이터 생성 (5개 등급 고정)

-- customer_grade 테이블의 grade_name 컬럼을 VARCHAR에서 ENUM으로 변경하기 전에
-- 기존 데이터가 있다면 삭제 (더미 데이터 초기화를 위해)
DELETE FROM customer_grade;

-- 5개 등급 생성 (고정)
-- BEGINNER: 초보자 (등급 레벨 1)
INSERT INTO customer_grade (grade_name, grade_level, min_purchase_amount, min_order_count, discount_rate, point_accumulation_rate, is_active, created_at)
VALUES ('BEGINNER', 1, 0, 0, 0.0, 1.0, true, NOW());

-- SWIMMER: 수영인 (등급 레벨 2)
INSERT INTO customer_grade (grade_name, grade_level, min_purchase_amount, min_order_count, discount_rate, point_accumulation_rate, is_active, created_at)
VALUES ('SWIMMER', 2, 100000, 3, 2.0, 1.5, true, NOW());

-- PRO: 프로 (등급 레벨 3)
INSERT INTO customer_grade (grade_name, grade_level, min_purchase_amount, min_order_count, discount_rate, point_accumulation_rate, is_active, created_at)
VALUES ('PRO', 3, 500000, 10, 5.0, 2.0, true, NOW());

-- MASTER: 마스터 (등급 레벨 4)
INSERT INTO customer_grade (grade_name, grade_level, min_purchase_amount, min_order_count, discount_rate, point_accumulation_rate, is_active, created_at)
VALUES ('MASTER', 4, 1000000, 20, 7.0, 2.5, true, NOW());

-- LEGEND: 레전드 (등급 레벨 5)
INSERT INTO customer_grade (grade_name, grade_level, min_purchase_amount, min_order_count, discount_rate, point_accumulation_rate, is_active, created_at)
VALUES ('LEGEND', 5, 2000000, 50, 10.0, 3.0, true, NOW());
