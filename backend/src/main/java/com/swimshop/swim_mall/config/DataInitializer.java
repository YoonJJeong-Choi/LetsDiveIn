package com.swimshop.swim_mall.config;

import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.swimshop.swim_mall.account.entity.AccountEntity;
import com.swimshop.swim_mall.account.repository.AccountRepository;
import com.swimshop.swim_mall.admin.entity.AdminEntity;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.brand.entity.BrandEntity;
import com.swimshop.swim_mall.brand.repository.BrandRepository;
import com.swimshop.swim_mall.common.enums.ActiveStatus;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.AdminStatus;
import com.swimshop.swim_mall.common.enums.PartnerStatus;
import com.swimshop.swim_mall.common.enums.ProductSubType;
import com.swimshop.swim_mall.common.enums.ProductType;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.option.repository.OptionRepository;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.product.entity.ProductEntity;
import com.swimshop.swim_mall.product.repository.ProductRepository;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.entity.CustomerGradeEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerRepository;
import com.swimshop.swim_mall.customer.reopository.CustomerGradeRepository;
import com.swimshop.swim_mall.inventory.entity.InventoryEntity;
import com.swimshop.swim_mall.inventory.repository.InventoryRepository;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.order.repository.OrderRepository;
import com.swimshop.swim_mall.order.repository.OrderItemRepository;
import com.swimshop.swim_mall.delivery.entity.DeliveryEntity;
import com.swimshop.swim_mall.delivery.repository.DeliveryRepository;
import com.swimshop.swim_mall.payment.PaymentEntity;
import com.swimshop.swim_mall.payment.PaymentRepository;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.return_order.repository.ReturnRepository;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.common.enums.CustomerGradeEnum;
import com.swimshop.swim_mall.settlement.entity.SettlementEntity;
import com.swimshop.swim_mall.settlement.entity.SettlementOrderItemEntity;
import com.swimshop.swim_mall.settlement.repository.SettlementRepository;
import com.swimshop.swim_mall.settlement.repository.SettlementOrderItemRepository;
import com.swimshop.swim_mall.size.entity.SizeEntity;
import com.swimshop.swim_mall.size.repository.SizeRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 애플리케이션 시작 시 더미 데이터를 삽입하는 초기화 클래스
 * 개발 환경에서만 실행: application.properties에 app.data.init.enabled=true 설정 필요
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data.init.enabled", havingValue = "true", matchIfMissing = false)
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PartnerRepository partnerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final OptionRepository optionRepository;
    private final DataSource dataSource;
    private final CustomerRepository customerRepository;
    private final CustomerGradeRepository customerGradeRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final PaymentRepository paymentRepository;
    private final ReturnRepository returnRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementOrderItemRepository settlementOrderItemRepository;
    private final BrandRepository brandRepository;
    private final SizeRepository sizeRepository;
    private final com.swimshop.swim_mall.customer.service.CustomerGradeService customerGradeService;
    private final com.swimshop.swim_mall.point.service.PointService pointService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("더미 데이터 초기화를 시작합니다...");
        
        if (brandRepository.count() == 0) {
            createMasterBrands();
        } else {
            log.info("브랜드 마스터 데이터가 이미 존재합니다. 건너뜁니다.");
        }

        if (sizeRepository.count() == 0) {
            createMasterSizes();
        } else {
            log.info("사이즈 마스터 데이터가 이미 존재합니다. 건너뜁니다.");
        }

        // Admin이 없으면 생성
        if (adminRepository.count() == 0) {
            createAdminAndPartners();
        } else {
            log.info("Admin과 Partner는 이미 존재합니다. 건너뜁니다.");
        }
        
               // 상품/옵션이 없으면 생성, 있으면 건너뜀
               if (productRepository.count() == 0) {
                   // 시퀀스 초기화 (상품이 없을 때만)
                   resetSequences();
                   createProductsAndOptions();
               } else {
                   log.info("상품과 옵션이 이미 존재합니다. 건너뜁니다.");
               }
        
        // 고객이 없으면 생성
        if (customerRepository.count() == 0) {
            createCustomers();
        } else {
            log.info("고객이 이미 존재합니다. 건너뜁니다.");
        }
        
        // 재고가 없으면 생성 (모든 옵션과 상품에 10개씩)
        if (inventoryRepository.count() == 0) {
            createInventories();
        } else {
            log.info("재고가 이미 존재합니다. 건너뜁니다.");
        }
        
        // 주문이 없으면 생성
        if (orderRepository.count() == 0) {
            createOrders();
        } else {
            log.info("주문이 이미 존재합니다. 건너뜁니다.");
        }
        
        // 정산이 없으면 생성
        if (settlementRepository.count() == 0) {
            createSettlements();
        } else {
            log.info("정산이 이미 존재합니다. 건너뜁니다.");
        }

        // 주문 기반으로 고객 등급 & 포인트 초기 계산
        recalculateGradesAndPointsForAllCustomers();
        
        log.info("더미 데이터 초기화 완료!");
    }
    
    /**
     * Admin과 Partner 생성
     */
    private void createAdminAndPartners() {

        LocalDateTime now = LocalDateTime.now();
        String defaultPassword = passwordEncoder.encode("admin123"); // 기본 비밀번호: admin123

        // 1. Admin 계정 생성
        log.info("Admin 계정 생성 중...");
        AccountEntity adminAccount = AccountEntity.builder()
                .email("admin@swim-mall.com")
                .password(defaultPassword)
                .role(AccountRole.ADMIN)
                .emailVerified(true)
                .status("ACTIVE")
                // createdAt은 @PrePersist에서 자동 설정됨
                .build();
        adminAccount = accountRepository.save(adminAccount);

        AdminEntity admin = AdminEntity.create(
                "시스템 관리자",
                AdminStatus.ACTIVE,
                now,
                adminAccount // Account를 생성 시 바로 전달
        );
        admin = adminRepository.saveAndFlush(admin); // flush()로 즉시 DB 반영하여 양방향 동기화 보장

        log.info("Admin 계정 생성 완료: admin@swim-mall.com / admin123");

        // 2. Partner 계정 생성 (2개)
        log.info("Partner 계정 생성 중...");

        // Partner 1
        String partner1Password = passwordEncoder.encode("partner123");
        AccountEntity partner1Account = AccountEntity.builder()
                .email("partner1@swim-mall.com")
                .password(partner1Password)
                .role(AccountRole.PARTNER)
                .emailVerified(true)
                .status("ACTIVE")
                // createdAt은 @PrePersist에서 자동 설정됨
                .build();
        partner1Account = accountRepository.save(partner1Account);

        PartnerEntity partner1 = PartnerEntity.create(
                "수영용품 전문샵",
                "010-1234-5678",
                "111-222-456789",
                "123-45-67890", // 사업자등록번호
                "SPEEDO",
                PartnerStatus.APPROVED,
                now,
                admin,
                partner1Account // Account를 생성 시 바로 전달
        );
        partner1 = partnerRepository.saveAndFlush(partner1); // flush()로 즉시 DB 반영하여 양방향 동기화 보장

        log.info("Partner 1 계정 생성 완료: partner1@swim-mall.com / partner123");

        // Partner 2
        String partner2Password = passwordEncoder.encode("partner123");
        AccountEntity partner2Account = AccountEntity.builder()
                .email("partner2@swim-mall.com")
                .password(partner2Password)
                .role(AccountRole.PARTNER)
                .emailVerified(true)
                .status("ACTIVE")
                // createdAt은 @PrePersist에서 자동 설정됨
                .build();
        partner2Account = accountRepository.save(partner2Account);

        PartnerEntity partner2 = PartnerEntity.create(
                "비치웨어 스토어",
                "010-9876-5432",
                "333-444-789012",
                "987-65-43210", // 사업자등록번호
                "ARENA",
                PartnerStatus.APPROVED,
                now,
                admin,
                partner2Account // Account를 생성 시 바로 전달
        );
        partner2 = partnerRepository.saveAndFlush(partner2); // flush()로 즉시 DB 반영하여 양방향 동기화 보장

        log.info("Partner 2 계정 생성 완료: partner2@swim-mall.com / partner123");
    }

    private void createMasterBrands() {
        log.info("브랜드 마스터 데이터 생성 중...");

        brandRepository.save(BrandEntity.builder()
                .code("SPEEDO")
                .canonicalName("Speedo")
                .displayName("SPEEDO")
                .slug("speedo")
                .country("UK")
                .isActive(true)
                .sortOrder(10)
                .build());

        brandRepository.save(BrandEntity.builder()
                .code("ARENA")
                .canonicalName("Arena")
                .displayName("ARENA")
                .slug("arena")
                .country("JP")
                .isActive(true)
                .sortOrder(20)
                .build());

        log.info("브랜드 마스터 데이터 생성 완료");
    }

    private void createMasterSizes() {
        log.info("사이즈 마스터 데이터 생성 중...");

        sizeRepository.save(SizeEntity.builder().code("S").label("S").isActive(true).sortOrder(10).build());
        sizeRepository.save(SizeEntity.builder().code("M").label("M").isActive(true).sortOrder(20).build());
        sizeRepository.save(SizeEntity.builder().code("L").label("L").isActive(true).sortOrder(30).build());
        sizeRepository.save(SizeEntity.builder().code("XL").label("XL").isActive(true).sortOrder(40).build());
        sizeRepository.save(SizeEntity.builder().code("FREE").label("FREE").isActive(true).sortOrder(50).build());

        log.info("사이즈 마스터 데이터 생성 완료");
    }
    
    /**
     * 상품 및 옵션 더미 데이터 생성
     */
    private void createProductsAndOptions() {
        log.info("상품 및 옵션 더미 데이터 생성 중...");
        
        // Partner 조회 (이미 생성되어 있어야 함)
        PartnerEntity partner1 = partnerRepository.findAll().stream()
                .filter(p -> p.getPartnerName().equals("수영용품 전문샵"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Partner 1을 찾을 수 없습니다. 먼저 Partner를 생성해주세요."));
        
        PartnerEntity partner2 = partnerRepository.findAll().stream()
                .filter(p -> p.getPartnerName().equals("비치웨어 스토어"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Partner 2를 찾을 수 없습니다. 먼저 Partner를 생성해주세요."));
        
        LocalDateTime now = LocalDateTime.now();
        
        // 상품 1: 수영모자 (Partner 1)
        ProductEntity product1 = ProductEntity.builder()
                .productName("실리콘 수영모자")
                .productType(ProductType.SWIM_CAP)
                .productSubType(ProductSubType.CAP_SILICONE)
                .productPrice("15000")
                .productDescription("편안하고 물에 강한 실리콘 수영모자입니다. 다양한 색상과 사이즈로 제공됩니다.")
                .productImageUrl("/images/products/womens/women-19.jpg")
                .productCreatedAt(now)
                .productActiveStatus(ActiveStatus.ACTIVE)
                .build();
        product1.setPartner(partner1); // Partner 설정
        product1 = productRepository.save(product1);
        
        // 상품 1의 옵션들 (Partner 1) - 색상별로 모든 사이즈
        // 블랙: S, M, L
        OptionEntity option1_1 = OptionEntity.builder()
                .product(product1)
                .partner(partner1)
                .color("블랙")
                .size("S")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option1_1);
        
        OptionEntity option1_2 = OptionEntity.builder()
                .product(product1)
                .partner(partner1)
                .color("블랙")
                .size("M")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option1_2);
        
        OptionEntity option1_3 = OptionEntity.builder()
                .product(product1)
                .partner(partner1)
                .color("블랙")
                .size("L")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option1_3);
        
        // 네이비: S, M, L
        OptionEntity option1_4 = OptionEntity.builder()
                .product(product1)
                .partner(partner1)
                .color("네이비")
                .size("S")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option1_4);
        
        OptionEntity option1_5 = OptionEntity.builder()
                .product(product1)
                .partner(partner1)
                .color("네이비")
                .size("M")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option1_5);
        
        OptionEntity option1_6 = OptionEntity.builder()
                .product(product1)
                .partner(partner1)
                .color("네이비")
                .size("L")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option1_6);
        
        log.info("상품 1 생성 완료: 실리콘 수영모자 (옵션 6개: 블랙-S/M/L, 네이비-S/M/L)");
        
        // 상품 2: 여성 원피스 수영복 (Partner 1)
        ProductEntity product2 = ProductEntity.builder()
                .productName("여성 원피스 수영복")
                .productType(ProductType.SWIMSUIT_WOMEN)
                .productSubType(ProductSubType.ONE_PIECE)
                .productPrice("89000")
                .productDescription("세련된 디자인의 여성 원피스 수영복입니다. 편안한 착용감과 스타일을 동시에 만족시킵니다.")
                .productImageUrl("/images/products/womens/women-20.jpg")
                .productCreatedAt(now)
                .productActiveStatus(ActiveStatus.ACTIVE)
                .build();
        product2.setPartner(partner1); // Partner 설정
        product2 = productRepository.save(product2);
        
        // 상품 2의 옵션들 (Partner 1) - 색상별로 모든 사이즈
        // 블랙: S, M, L
        OptionEntity option2_1 = OptionEntity.builder()
                .product(product2)
                .partner(partner1)
                .color("블랙")
                .size("S")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option2_1);
        
        OptionEntity option2_2 = OptionEntity.builder()
                .product(product2)
                .partner(partner1)
                .color("블랙")
                .size("M")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option2_2);
        
        OptionEntity option2_3 = OptionEntity.builder()
                .product(product2)
                .partner(partner1)
                .color("블랙")
                .size("L")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option2_3);
        
        // 네이비: S, M, L
        OptionEntity option2_4 = OptionEntity.builder()
                .product(product2)
                .partner(partner1)
                .color("네이비")
                .size("S")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option2_4);
        
        OptionEntity option2_5 = OptionEntity.builder()
                .product(product2)
                .partner(partner1)
                .color("네이비")
                .size("M")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option2_5);
        
        OptionEntity option2_6 = OptionEntity.builder()
                .product(product2)
                .partner(partner1)
                .color("네이비")
                .size("L")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option2_6);
        
        log.info("상품 2 생성 완료: 여성 원피스 수영복 (옵션 6개: 블랙-S/M/L, 네이비-S/M/L)");
        
        // 상품 3: 수영안경 (Partner 2)
        ProductEntity product3 = ProductEntity.builder()
                .productName("프리미엄 수영안경")
                .productType(ProductType.SWIM_GOGGLES)
                .productSubType(ProductSubType.NONE) // null 대신 NONE 사용
                .productPrice("35000")
                .productDescription("안개 방지 코팅이 적용된 고급 수영안경입니다. 물속에서도 선명한 시야를 제공합니다.")
                .productImageUrl("/images/products/womens/women-176.jpg")
                .productCreatedAt(now)
                .productActiveStatus(ActiveStatus.ACTIVE)
                .build();
        product3.setPartner(partner2); // Partner 설정
        product3 = productRepository.save(product3);
        
        // 상품 3의 옵션들 (Partner 2)
        OptionEntity option3_1 = OptionEntity.builder()
                .product(product3)
                .partner(partner2)
                .color("투명")
                .size("일반")
                .optionAddPrice(0L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option3_1);
        
        OptionEntity option3_2 = OptionEntity.builder()
                .product(product3)
                .partner(partner2)
                .color("미러")
                .size("일반")
                .optionAddPrice(5000L)
                .optionStatus(ActiveStatus.ACTIVE)
                .build();
        optionRepository.save(option3_2);
        
        log.info("상품 3 생성 완료: 프리미엄 수영안경 (옵션 2개)");
        
        // 상품 4: 단일 상품 (옵션 없음) - 수영용품 (Partner 1)
        ProductEntity product4 = ProductEntity.builder()
                .productName("수영용 수건")
                .productType(ProductType.SWIM_TOY)
                .productSubType(ProductSubType.NONE)
                .productPrice("15000")
                .productDescription("빠르게 마르는 수영용 수건입니다. 옵션 없이 바로 구매 가능한 단일 상품입니다.")
                .productImageUrl("/images/products/womens/women-1.jpg")
                .productCreatedAt(now)
                .productActiveStatus(ActiveStatus.ACTIVE)
                .build();
        product4.setPartner(partner1); // Partner 설정 (옵션이 없는 단일 상품도 파트너 설정 필요)
        product4 = productRepository.save(product4);
        
        // 상품 4는 옵션이 없음 (단일 상품)
        // 옵션 없이 장바구니에 추가 가능
        
        log.info("상품 4 생성 완료: 수영용 수건 (옵션 없음 - 단일 상품)");
        log.info("=== 상품 정보 ===");
        log.info("Partner 1 상품: 실리콘 수영모자 (옵션 6개: 블랙-S/M/L, 네이비-S/M/L), 여성 원피스 수영복 (옵션 6개: 블랙-S/M/L, 네이비-S/M/L), 수영용 수건 (옵션 없음)");
        log.info("Partner 2 상품: 프리미엄 수영안경 (옵션 2개)");
    }

    /**
     * PostgreSQL 시퀀스를 초기화합니다.
     * 상품이 없을 때만 호출하여 시퀀스를 1부터 시작하도록 설정합니다.
     * 
     * PostgreSQL에서 IDENTITY 컬럼을 사용하면 자동으로 시퀀스가 생성됩니다.
     * 테이블의 IDENTITY 컬럼에 연결된 시퀀스를 찾아서 초기화합니다.
     */
    private void resetSequences() {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement()) {
            
            log.info("시퀀스 초기화 시작...");
            
            // product 테이블의 시퀀스 찾기 및 초기화
            try (var rs = stmt.executeQuery(
                    "SELECT pg_get_serial_sequence('product', 'product_no') as seq_name")) {
                if (rs.next()) {
                    String seqName = rs.getString("seq_name");
                    if (seqName != null && !seqName.isEmpty()) {
                        stmt.execute("SELECT setval('" + seqName + "', 1, false)");
                        log.info("✅ product 시퀀스 초기화: {} -> 1", seqName);
                    } else {
                        log.warn("⚠️ product 시퀀스를 찾을 수 없습니다.");
                    }
                }
            }
            
            // option 테이블의 시퀀스 찾기
            try (var rs = stmt.executeQuery(
                    "SELECT pg_get_serial_sequence('option', 'option_no') as seq_name")) {
                if (rs.next()) {
                    String seqName = rs.getString("seq_name");
                    if (seqName != null && !seqName.isEmpty()) {
                        stmt.execute("SELECT setval('" + seqName + "', 1, false)");
                        log.info("✅ option 시퀀스 초기화: {} -> 1", seqName);
                    } else {
                        log.warn("⚠️ option 시퀀스를 찾을 수 없습니다.");
                    }
                }
            }
            
            log.info("시퀀스 초기화 완료");
        } catch (Exception e) {
            log.error("❌ 시퀀스 초기화 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 고객 더미 데이터 생성 (10명)
     */
    private void createCustomers() {
        log.info("고객 더미 데이터 생성 중...");
        
        // 등급이 없으면 먼저 생성
        if (customerGradeRepository.count() == 0) {
            log.info("등급이 없어 초기 등급을 생성합니다...");
            createDefaultGrades();
        }
        
        // 기본 등급 조회 (BEGINNER 등급)
        CustomerGradeEntity defaultGrade = customerGradeRepository
                .findByGradeName(CustomerGradeEnum.BEGINNER)
                .orElseGet(() -> {
                    // BEGINNER 등급이 없으면 가장 낮은 등급 조회
                    return customerGradeRepository
                            .findFirstByIsActiveTrueOrderByGradeLevelAsc()
                            .orElseThrow(() -> new IllegalStateException("활성화된 등급이 없습니다. 먼저 등급을 생성해주세요."));
                });
        
        LocalDateTime now = LocalDateTime.now();
        String defaultPassword = passwordEncoder.encode("customer123");
        
        String[] names = {"김철수", "이영희", "박민수", "최지영", "정대현", "한소희", "윤성호", "강미영", "임동욱", "오수진"};
        String[] emails = {"customer1@test.com", "customer2@test.com", "customer3@test.com", "customer4@test.com", 
                          "customer5@test.com", "customer6@test.com", "customer7@test.com", "customer8@test.com", 
                          "customer9@test.com", "customer10@test.com"};
        
        for (int i = 0; i < 10; i++) {
            // 각 고객마다 다른 생성일시 (최근 3개월 내)
            LocalDateTime customerCreateAt = now.minusDays(90 - (i * 9)); // 90일 전부터 9일 간격으로
            
            // Account 생성
            AccountEntity customerAccount = AccountEntity.builder()
                    .email(emails[i])
                    .password(defaultPassword)
                    .role(AccountRole.CUSTOMER)
                    .emailVerified(true)
                    .status("ACTIVE")
                    .build();
            customerAccount = accountRepository.save(customerAccount);
            
            // Customer 생성
            CustomerEntity customer = CustomerEntity.builder()
                    .customerName(names[i])
                    .customerEmail(emails[i])
                    .customerPassword(defaultPassword)
                    .customerBirth(LocalDate.of(1990 + i, 1 + i, 1 + i))
                    .customerCreateAt(customerCreateAt)
                    .emailChecked(true)
                    .customerGrade(defaultGrade)
                    .totalPurchaseAmount(0L)
                    .totalOrderCount(0)
                    .build();
            customer.setAccount(customerAccount);
            customerRepository.save(customer);
        }
        
        log.info("고객 10명 생성 완료 (비밀번호: customer123)");
    }
    
    /**
     * 기본 등급 생성 (5개 등급 고정)
     */
    private void createDefaultGrades() {
        log.info("기본 등급 생성 중...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // BEGINNER: 초보자 (등급 레벨 1)
        CustomerGradeEntity beginner = CustomerGradeEntity.builder()
                .gradeName(CustomerGradeEnum.BEGINNER)
                .gradeLevel(1)
                .minPurchaseAmount(0L)
                .minOrderCount(0)
                .discountRate(0.0)
                .pointAccumulationRate(1.0)
                .isActive(true)
                .createdAt(now)
                .build();
        customerGradeRepository.save(beginner);
        
        // SWIMMER: 수영인 (등급 레벨 2)
        CustomerGradeEntity swimmer = CustomerGradeEntity.builder()
                .gradeName(CustomerGradeEnum.SWIMMER)
                .gradeLevel(2)
                .minPurchaseAmount(100000L)
                .minOrderCount(3)
                .discountRate(2.0)
                .pointAccumulationRate(1.5)
                .isActive(true)
                .createdAt(now)
                .build();
        customerGradeRepository.save(swimmer);
        
        // PRO: 프로 (등급 레벨 3)
        CustomerGradeEntity pro = CustomerGradeEntity.builder()
                .gradeName(CustomerGradeEnum.PRO)
                .gradeLevel(3)
                .minPurchaseAmount(500000L)
                .minOrderCount(10)
                .discountRate(5.0)
                .pointAccumulationRate(2.0)
                .isActive(true)
                .createdAt(now)
                .build();
        customerGradeRepository.save(pro);
        
        // MASTER: 마스터 (등급 레벨 4)
        CustomerGradeEntity master = CustomerGradeEntity.builder()
                .gradeName(CustomerGradeEnum.MASTER)
                .gradeLevel(4)
                .minPurchaseAmount(1000000L)
                .minOrderCount(20)
                .discountRate(7.0)
                .pointAccumulationRate(2.5)
                .isActive(true)
                .createdAt(now)
                .build();
        customerGradeRepository.save(master);
        
        // LEGEND: 레전드 (등급 레벨 5)
        CustomerGradeEntity legend = CustomerGradeEntity.builder()
                .gradeName(CustomerGradeEnum.LEGEND)
                .gradeLevel(5)
                .minPurchaseAmount(2000000L)
                .minOrderCount(50)
                .discountRate(10.0)
                .pointAccumulationRate(3.0)
                .isActive(true)
                .createdAt(now)
                .build();
        customerGradeRepository.save(legend);
        
        log.info("기본 등급 5개 생성 완료: BEGINNER, SWIMMER, PRO, MASTER, LEGEND");
    }
    
    /**
     * 재고 더미 데이터 생성 (모든 옵션과 상품에 10개씩)
     */
    private void createInventories() {
        log.info("재고 더미 데이터 생성 중...");
        
        // 모든 옵션에 재고 10개씩
        List<OptionEntity> options = optionRepository.findAll();
        for (OptionEntity option : options) {
            InventoryEntity inventory = InventoryEntity.builder()
                    .inventoryStock(10)
                    .option(option)
                    .product(null)
                    .build();
            inventoryRepository.save(inventory);
        }
        
        // 옵션이 없는 상품에 재고 10개씩
        List<ProductEntity> products = productRepository.findAll();
        for (ProductEntity product : products) {
            // 옵션이 있는 상품은 제외
            boolean hasOptions = optionRepository.findByProduct_ProductNo(product.getProductNo()).size() > 0;
            if (!hasOptions) {
                InventoryEntity inventory = InventoryEntity.builder()
                        .inventoryStock(10)
                        .option(null)
                        .product(product)
                        .build();
                inventoryRepository.save(inventory);
            }
        }
        
        log.info("재고 생성 완료 (모든 옵션과 상품에 10개씩)");
    }
    
    /**
     * 주문 더미 데이터 생성
     * - 주문 완료(배송중)
     * - 주문 취소
     * - 구매확정
     * - 반품완료
     */
    private void createOrders() {
        log.info("주문 더미 데이터 생성 중...");
        
        List<CustomerEntity> customers = customerRepository.findAll();
        List<ProductEntity> products = productRepository.findAll();
        List<OptionEntity> options = optionRepository.findAll();
        
        if (customers.isEmpty() || products.isEmpty()) {
            log.warn("고객이나 상품이 없어 주문을 생성할 수 없습니다.");
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Random random = new Random();
        
        // 1. 주문 완료(배송중) - 3개
        for (int i = 0; i < 3; i++) {
            CustomerEntity customer = customers.get(random.nextInt(customers.size()));
            ProductEntity product = products.get(random.nextInt(products.size()));
            OptionEntity option = null;
            
            // 옵션이 있는 상품이면 옵션 선택
            List<OptionEntity> productOptions = optionRepository.findByProduct_ProductNo(product.getProductNo());
            if (!productOptions.isEmpty()) {
                option = productOptions.get(random.nextInt(productOptions.size()));
            }
            
            Long itemPrice = Long.parseLong(product.getProductPrice());
            int quantity = random.nextInt(3) + 1; // 1~3개
            Long itemTotalPrice = itemPrice * quantity;
            
            // 주문 생성
            OrderEntity order = OrderEntity.builder()
                    .orderTotalPrice(itemTotalPrice)
                    .orderCreatedAt(now.minusDays(5 + i))
                    .orderStatus(OrderStatus.ACTIVE)
                    .customer(customer)
                    .recipientName(customer.getCustomerName())
                    .recipientPhone("010-1234-5678")
                    .deliveryAddress("서울시 강남구 테헤란로 123")
                    .deliveryAddressDetail("456호")
                    .deliveryZipCode("06234")
                    .paymentMethod("CARD")
                    .build();
            order = orderRepository.save(order);
            
            // 결제 생성
            PaymentEntity payment = PaymentEntity.builder()
                    .paymentAmount(itemTotalPrice)
                    .paymentMethod("CARD")
                    .paymentCreatedAt(now.minusDays(5 + i))
                    .paidAt(now.minusDays(5 + i))
                    .paymentCancelYn(false)
                    .build();
            payment = paymentRepository.save(payment);
            order.setPayment(payment);
            orderRepository.save(order);
            
            // 주문 상품 생성
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .option(option)
                    .itemQuantity(quantity)
                    .itemPrice(itemPrice)
                    .itemTotalPrice(itemTotalPrice)
                    .isCancelled(false)
                    .confirmedAt(now.minusDays(4 + i))
                    .build();
            orderItem = orderItemRepository.save(orderItem);
            
            // 배송 생성 (배송중)
            DeliveryEntity delivery = DeliveryEntity.builder()
                    .orderItem(orderItem)
                    .deliveryStatus(DeliveryStatus.SHIPPED)
                    .deliveryStartDate(now.minusDays(3 + i))
                    .deliveryTrackingNumber("TRACK" + (1000 + i))
                    .deliveryCourier("CJ대한통운")
                    .build();
            deliveryRepository.save(delivery);
        }
        
        // 2. 주문 취소 - 2개
        for (int i = 0; i < 2; i++) {
            CustomerEntity customer = customers.get(random.nextInt(customers.size()));
            ProductEntity product = products.get(random.nextInt(products.size()));
            OptionEntity option = null;
            
            List<OptionEntity> productOptions = optionRepository.findByProduct_ProductNo(product.getProductNo());
            if (!productOptions.isEmpty()) {
                option = productOptions.get(random.nextInt(productOptions.size()));
            }
            
            Long itemPrice = Long.parseLong(product.getProductPrice());
            int quantity = random.nextInt(2) + 1;
            Long itemTotalPrice = itemPrice * quantity;
            
            OrderEntity order = OrderEntity.builder()
                    .orderTotalPrice(itemTotalPrice)
                    .orderCreatedAt(now.minusDays(10 + i))
                    .orderStatus(OrderStatus.CANCELLED)
                    .customer(customer)
                    .recipientName(customer.getCustomerName())
                    .recipientPhone("010-1234-5678")
                    .deliveryAddress("서울시 강남구 테헤란로 123")
                    .deliveryAddressDetail("456호")
                    .deliveryZipCode("06234")
                    .paymentMethod("CARD")
                    .build();
            order = orderRepository.save(order);
            
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .option(option)
                    .itemQuantity(quantity)
                    .itemPrice(itemPrice)
                    .itemTotalPrice(itemTotalPrice)
                    .isCancelled(true)
                    .build();
            orderItemRepository.save(orderItem);
        }
        
        // 3. 구매확정 - 4개
        for (int i = 0; i < 4; i++) {
            CustomerEntity customer = customers.get(random.nextInt(customers.size()));
            ProductEntity product = products.get(random.nextInt(products.size()));
            OptionEntity option = null;
            
            List<OptionEntity> productOptions = optionRepository.findByProduct_ProductNo(product.getProductNo());
            if (!productOptions.isEmpty()) {
                option = productOptions.get(random.nextInt(productOptions.size()));
            }
            
            Long itemPrice = Long.parseLong(product.getProductPrice());
            int quantity = random.nextInt(3) + 1;
            Long itemTotalPrice = itemPrice * quantity;
            
            OrderEntity order = OrderEntity.builder()
                    .orderTotalPrice(itemTotalPrice)
                    .orderCreatedAt(now.minusDays(20 + i))
                    .orderStatus(OrderStatus.ACTIVE)
                    .customer(customer)
                    .recipientName(customer.getCustomerName())
                    .recipientPhone("010-1234-5678")
                    .deliveryAddress("서울시 강남구 테헤란로 123")
                    .deliveryAddressDetail("456호")
                    .deliveryZipCode("06234")
                    .paymentMethod("CARD")
                    .build();
            order = orderRepository.save(order);
            
            PaymentEntity payment = PaymentEntity.builder()
                    .paymentAmount(itemTotalPrice)
                    .paymentMethod("CARD")
                    .paymentCreatedAt(now.minusDays(20 + i))
                    .paidAt(now.minusDays(20 + i))
                    .paymentCancelYn(false)
                    .build();
            payment = paymentRepository.save(payment);
            order.setPayment(payment);
            orderRepository.save(order);
            
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .option(option)
                    .itemQuantity(quantity)
                    .itemPrice(itemPrice)
                    .itemTotalPrice(itemTotalPrice)
                    .isCancelled(false)
                    .confirmedAt(now.minusDays(19 + i))
                    .completedAt(now.minusDays(12 + i)) // 구매 확정
                    .build();
            orderItem = orderItemRepository.save(orderItem);
            
            // 배송 생성 (배송 완료)
            DeliveryEntity delivery = DeliveryEntity.builder()
                    .orderItem(orderItem)
                    .deliveryStatus(DeliveryStatus.DELIVERED)
                    .deliveryStartDate(now.minusDays(18 + i))
                    .deliveryEndDate(now.minusDays(13 + i))
                    .deliveryTrackingNumber("TRACK" + (2000 + i))
                    .deliveryCourier("CJ대한통운")
                    .build();
            deliveryRepository.save(delivery);
        }
        
        // 4. 반품완료 - 2개
        for (int i = 0; i < 2; i++) {
            CustomerEntity customer = customers.get(random.nextInt(customers.size()));
            ProductEntity product = products.get(random.nextInt(products.size()));
            OptionEntity option = null;
            
            List<OptionEntity> productOptions = optionRepository.findByProduct_ProductNo(product.getProductNo());
            if (!productOptions.isEmpty()) {
                option = productOptions.get(random.nextInt(productOptions.size()));
            }
            
            Long itemPrice = Long.parseLong(product.getProductPrice());
            int quantity = random.nextInt(2) + 1;
            Long itemTotalPrice = itemPrice * quantity;
            
            OrderEntity order = OrderEntity.builder()
                    .orderTotalPrice(itemTotalPrice)
                    .orderCreatedAt(now.minusDays(30 + i))
                    .orderStatus(OrderStatus.ACTIVE)
                    .customer(customer)
                    .recipientName(customer.getCustomerName())
                    .recipientPhone("010-1234-5678")
                    .deliveryAddress("서울시 강남구 테헤란로 123")
                    .deliveryAddressDetail("456호")
                    .deliveryZipCode("06234")
                    .paymentMethod("CARD")
                    .build();
            order = orderRepository.save(order);
            
            PaymentEntity payment = PaymentEntity.builder()
                    .paymentAmount(itemTotalPrice)
                    .paymentMethod("CARD")
                    .paymentCreatedAt(now.minusDays(30 + i))
                    .paidAt(now.minusDays(30 + i))
                    .paymentCancelYn(false)
                    .build();
            payment = paymentRepository.save(payment);
            order.setPayment(payment);
            orderRepository.save(order);
            
            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .option(option)
                    .itemQuantity(quantity)
                    .itemPrice(itemPrice)
                    .itemTotalPrice(itemTotalPrice)
                    .isCancelled(false)
                    .confirmedAt(now.minusDays(29 + i))
                    .build();
            orderItem = orderItemRepository.save(orderItem);
            
            // 배송 생성 (배송 완료)
            DeliveryEntity delivery = DeliveryEntity.builder()
                    .orderItem(orderItem)
                    .deliveryStatus(DeliveryStatus.DELIVERED)
                    .deliveryStartDate(now.minusDays(28 + i))
                    .deliveryEndDate(now.minusDays(25 + i))
                    .deliveryTrackingNumber("TRACK" + (3000 + i))
                    .deliveryCourier("CJ대한통운")
                    .build();
            deliveryRepository.save(delivery);
            
            // 반품 생성 (환불완료)
            ReturnEntity returnEntity = ReturnEntity.builder()
                    .orderItem(orderItem)
                    .returnStatus(ReturnStatus.REFUNDED)
                    .returnRequestedAt(now.minusDays(24 + i))
                    .returnReason("상품 불량")
                    .returnAmount(itemTotalPrice)
                    .returnTrackingNumber("RETURN" + (1000 + i))
                    .returnCourier("CJ대한통운")
                    .build();
            returnRepository.save(returnEntity);
        }
        
        log.info("주문 더미 데이터 생성 완료:");
        log.info("  - 주문 완료(배송중): 3개");
        log.info("  - 주문 취소: 2개");
        log.info("  - 구매확정: 4개");
        log.info("  - 반품완료: 2개");
    }

    /**
     * 모든 고객에 대해 등급 & 포인트 재계산
     * - 구매확정된 주문상품(OrderItem.completedAt != null, isCancelled=false) 기준
     * - CustomerGradeService로 등급 재계산 후, PointService로 포인트 적립
     */
    private void recalculateGradesAndPointsForAllCustomers() {
        log.info("고객 등급 및 포인트 일괄 재계산 시작...");

        List<CustomerEntity> customers = customerRepository.findAll();

        for (CustomerEntity customer : customers) {
            Long customerId = customer.getCustomerId();
            try {
                // 1) 등급 재계산
                customerGradeService.updateCustomerGrade(customerId);

                // 2) 포인트 재적립: 구매확정된 주문상품 기준
                List<OrderItemEntity> completedItems = orderItemRepository.findAll().stream()
                        .filter(oi -> !oi.getIsCancelled())
                        .filter(oi -> oi.getCompletedAt() != null)
                        .filter(oi -> oi.getOrder().getCustomer().getCustomerId().equals(customerId))
                        .toList();

                CustomerEntity refreshed = customerRepository.findById(customerId).orElse(customer);

                for (OrderItemEntity item : completedItems) {
                    if (refreshed.getCustomerGrade() == null) {
                        continue;
                    }
                    Double rate = refreshed.getCustomerGrade().getPointAccumulationRate();
                    Long amount = item.getItemTotalPrice();
                    Long point = (long) (amount * rate / 100.0);
                    if (point > 0) {
                        pointService.accumulatePoint(refreshed, item, point);
                    }
                }
            } catch (Exception e) {
                log.warn("고객 등급/포인트 재계산 실패: customerId={}, message={}", customerId, e.getMessage());
            }
        }

        log.info("고객 등급 및 포인트 일괄 재계산 완료");
    }
    
    /**
     * 정산 더미 데이터 생성 (10개)
     * - 구매 확정된 OrderItem을 기반으로 생성
     * - 다양한 상태 (PENDING, COMPLETED, CANCELLED)
     * - 파트너별로 분산
     */
    private void createSettlements() {
        log.info("정산 더미 데이터 생성 중...");
        
        AdminEntity admin = adminRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Admin을 찾을 수 없습니다. 먼저 Admin을 생성해주세요."));
        
        List<PartnerEntity> partners = partnerRepository.findAll();
        if (partners.isEmpty()) {
            log.warn("파트너가 없어 정산을 생성할 수 없습니다.");
            return;
        }
        
        // 구매 확정된 OrderItem 조회 (completedAt이 있는 것)
        List<OrderItemEntity> completedOrderItems = orderItemRepository.findAll().stream()
                .filter(item -> item.getCompletedAt() != null && !item.getIsCancelled())
                .toList();
        
        if (completedOrderItems.isEmpty()) {
            log.warn("구매 확정된 주문 상품이 없어 정산을 생성할 수 없습니다.");
            return;
        }
        
        LocalDate now = LocalDate.now();
        Random random = new Random();
        double commissionRate = 0.1; // 수수료율 10%
        
        // 10개의 정산 생성
        for (int i = 0; i < 10; i++) {
            // 파트너 랜덤 선택
            PartnerEntity partner = partners.get(random.nextInt(partners.size()));
            
            // 해당 파트너의 구매 확정된 OrderItem 필터링
            List<OrderItemEntity> partnerOrderItems = completedOrderItems.stream()
                    .filter(item -> {
                        ProductEntity product = item.getProduct();
                        return product != null && product.getPartner() != null 
                                && product.getPartner().getPartnerId().equals(partner.getPartnerId());
                    })
                    .toList();
            
            if (partnerOrderItems.isEmpty()) {
                // 해당 파트너의 주문이 없으면 랜덤 OrderItem 사용 (더미 데이터이므로)
                partnerOrderItems = completedOrderItems.stream()
                        .limit(1)
                        .toList();
            }
            
            // 정산에 포함할 OrderItem 선택 (1~3개)
            int itemCount = Math.min(random.nextInt(3) + 1, partnerOrderItems.size());
            List<OrderItemEntity> selectedItems = partnerOrderItems.stream()
                    .limit(itemCount)
                    .toList();
            
            // 총 판매 금액 계산
            Long totalSalesAmount = selectedItems.stream()
                    .mapToLong(OrderItemEntity::getItemTotalPrice)
                    .sum();
            
            // 수수료 계산 (10%)
            Long commissionAmount = (long) (totalSalesAmount * commissionRate);
            
            // 정산 금액 = 총 판매 금액 - 수수료
            Long settlementAmount = totalSalesAmount - commissionAmount;
            
            // 정산 기간 설정 (과거 날짜)
            LocalDate periodStart = now.minusDays(30 + i * 3);
            LocalDate periodEnd = now.minusDays(1 + i * 3);
            LocalDate createdAt = now.minusDays(i * 2);
            
            // 정산 상태 랜덤 선택
            SettlementStatus status;
            LocalDate paidDate = null;
            int statusRandom = random.nextInt(10);
            if (statusRandom < 5) {
                status = SettlementStatus.COMPLETED; // 50% 완료
                paidDate = createdAt.plusDays(random.nextInt(5) + 1);
            } else if (statusRandom < 8) {
                status = SettlementStatus.PENDING; // 30% 대기
            } else {
                status = SettlementStatus.CANCELLED; // 20% 취소
            }
            
            // 정산 생성
            SettlementEntity settlement = SettlementEntity.create(
                    totalSalesAmount,
                    commissionAmount,
                    settlementAmount,
                    createdAt,
                    status,
                    periodStart,
                    periodEnd,
                    admin,
                    partner
            );
            
            if (paidDate != null) {
                settlement.updatePaidDate(paidDate);
            }
            
            settlement = settlementRepository.save(settlement);
            
            // 정산-주문 아이템 관계 생성
            for (OrderItemEntity orderItem : selectedItems) {
                SettlementOrderItemEntity settlementOrderItem = SettlementOrderItemEntity.create(
                        settlement,
                        orderItem
                );
                settlementOrderItemRepository.save(settlementOrderItem);
            }
        }
        
        log.info("정산 더미 데이터 생성 완료: 10개");
    }
}
