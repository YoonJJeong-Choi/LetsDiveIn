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
import com.swimshop.swim_mall.product.entity.ProductImageEntity;
import com.swimshop.swim_mall.product.repository.ProductImageRepository;
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
import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;
import com.swimshop.swim_mall.return_order.repository.ReturnRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnImageRepository;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import com.swimshop.swim_mall.common.enums.OrderStatus;
import com.swimshop.swim_mall.common.enums.DeliveryStatus;
import com.swimshop.swim_mall.common.enums.ReturnReasonType;
import com.swimshop.swim_mall.common.enums.ReturnStatus;
import com.swimshop.swim_mall.common.enums.SettlementStatus;
import com.swimshop.swim_mall.common.enums.CustomerGradeEnum;
import com.swimshop.swim_mall.settlement.entity.SettlementEntity;
import com.swimshop.swim_mall.settlement.entity.SettlementOrderItemEntity;
import com.swimshop.swim_mall.settlement.repository.SettlementRepository;
import com.swimshop.swim_mall.settlement.repository.SettlementOrderItemRepository;
import com.swimshop.swim_mall.size.entity.SizeEntity;
import com.swimshop.swim_mall.size.repository.SizeRepository;
import com.swimshop.swim_mall.color.entity.ColorEntity;
import com.swimshop.swim_mall.color.repository.ColorRepository;
import com.swimshop.swim_mall.event.entity.EventEntity;
import com.swimshop.swim_mall.event.repository.EventRepository;
import com.swimshop.swim_mall.review.entity.ReviewEntity;
import com.swimshop.swim_mall.review.repository.ReviewRepository;
import com.swimshop.swim_mall.common.enums.EventStatus;
import com.swimshop.swim_mall.common.enums.EventType;
import com.swimshop.swim_mall.common.enums.EventMode;
import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.enums.QnaAuthorType;
import com.swimshop.swim_mall.common.enums.QnaStatus;
import com.swimshop.swim_mall.faq.entity.FaqEntity;
import com.swimshop.swim_mall.faq.repository.FaqRepository;
import com.swimshop.swim_mall.qna.entity.QnaEntity;
import com.swimshop.swim_mall.qna.entity.QnaMessageEntity;
import com.swimshop.swim_mall.qna.repository.QnaMessageRepository;
import com.swimshop.swim_mall.qna.repository.QnaRepository;

import javax.sql.DataSource;
import java.util.UUID;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.swimshop.swim_mall.payment.enums.PaymentStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 애플리케이션 시작 시 더미 데이터를 삽입하는 초기화 클래스
 * 개발 환경에서만 실행: application.properties에 app.data.init.enabled=true 설정 필요
 * <p>
 * 목표 스펙: {@code backend/docs/reference/SEED_DEMO_SPEC.md}
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data.init.enabled", havingValue = "true", matchIfMissing = false)
public class DataInitializer implements CommandLineRunner {

    /** 라이브 데모·문서 공개용 메인 테스트 계정 (비밀번호는 README에 적지 않음) */
    private static final String DEMO_ADMIN_EMAIL = "testAdmin@example.com";
    private static final String DEMO_PARTNER1_EMAIL = "testPartner1@example.com";
    private static final String DEMO_PARTNER2_EMAIL = "testPartner2@example.com";
    private static final String DEMO_CUSTOMER1_EMAIL = "testCustomer1@example.com";
    private static final String DEMO_CUSTOMER2_EMAIL = "testCustomer2@example.com";
    private static final String SEED_MAIN_PASSWORD = "test123!";
    /** 리뷰 AI 분석 데모용 추가 시드 식별자 (기존 createReviews와 별도, 멱등 체크용) */
//    private static final String REVIEW_AI_SEED_MARKER = "【리뷰`AI시드】";
    private static final String REVIEW_AI_SEED_PRODUCT_NAME = "실키 핏 프로 실리콘 캡";

    private final AdminRepository adminRepository;
    private final PartnerRepository partnerRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
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
    private final ReturnImageRepository returnImageRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementOrderItemRepository settlementOrderItemRepository;
    private final BrandRepository brandRepository;
    private final SizeRepository sizeRepository;
    private final ColorRepository colorRepository;
    private final EventRepository eventRepository;
    private final ReviewRepository reviewRepository;
    private final FaqRepository faqRepository;
    private final QnaRepository qnaRepository;
    private final QnaMessageRepository qnaMessageRepository;
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

        if (colorRepository.count() == 0) {
            createMasterColors();
        } else {
            log.info("컬러 마스터 데이터가 이미 존재합니다. 건너뜁니다.");
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
        boolean ranOrderSeed = false;
        if (orderRepository.count() == 0) {
            createOrders();
            ranOrderSeed = true;
        } else {
            log.info("주문이 이미 존재합니다. 건너뜁니다.");
        }
        
        // 정산이 없으면 생성
        if (settlementRepository.count() == 0) {
            createSettlements();
        } else {
            log.info("정산이 이미 존재합니다. 건너뜁니다.");
        }

        if (eventRepository.count() == 0) {
            createEvents();
        } else {
            log.info("이벤트가 이미 존재합니다. 건너뜁니다.");
        }

        if (reviewRepository.count() == 0 && orderItemRepository.count() > 0) {
            createReviews();
        } else if (reviewRepository.count() > 0) {
            log.info("리뷰가 이미 존재합니다. 건너뜁니다.");
        }

        // 리뷰 AI 분석 데모: 동일 상품 구매확정 주문 3건 + 리뷰 3건 (기존 시드 유지, 부족 시에만 추가)
        ensureReviewAiAnalysisSeeds();

        // 이번 기동에서 주문 시드를 새로 넣은 경우에만 등급·포인트 일괄 반영 (매 기동 재계산 방지)
        if (ranOrderSeed) {
            recalculateGradesAndPointsForAllCustomers();
        }

        if (faqRepository.count() == 0 && adminRepository.count() > 0) {
            createFaqs();
        } else if (faqRepository.count() > 0) {
            log.info("FAQ가 이미 존재합니다. 건너뜁니다.");
        }

        if (qnaRepository.count() == 0 && customerRepository.count() > 0 && orderItemRepository.count() > 0) {
            createQnaSeeds();
        } else if (qnaRepository.count() > 0) {
            log.info("QnA가 이미 존재합니다. 건너뜁니다.");
        }
        
        log.info("더미 데이터 초기화 완료!");
    }
    
    /**
     * Admin과 Partner 생성
     */
    private void createAdminAndPartners() {

        LocalDateTime now = LocalDateTime.now();
        String mainPassword = passwordEncoder.encode(SEED_MAIN_PASSWORD);
        String internalPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        // 1. Admin 계정 생성
        log.info("Admin 계정 생성 중...");
        AccountEntity adminAccount = AccountEntity.builder()
                .email(DEMO_ADMIN_EMAIL)
                .password(mainPassword)
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

        log.info("Admin 계정 생성 완료: {}", DEMO_ADMIN_EMAIL);

        // 2. Partner 계정 생성 (2개)
        log.info("Partner 계정 생성 중...");

        // Partner 1 (메인 테스트)
        AccountEntity partner1Account = AccountEntity.builder()
                .email(DEMO_PARTNER1_EMAIL)
                .password(mainPassword)
                .role(AccountRole.PARTNER)
                .emailVerified(true)
                .status("ACTIVE")
                // createdAt은 @PrePersist에서 자동 설정됨
                .build();
        partner1Account = accountRepository.save(partner1Account);

        PartnerEntity partner1 = PartnerEntity.create(
                "스피도",
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

        log.info("Partner 1(메인 테스트) 계정 생성 완료: {}", DEMO_PARTNER1_EMAIL);

        // Partner 2 (보조, 비밀번호 비공개)
        AccountEntity partner2Account = AccountEntity.builder()
                .email(DEMO_PARTNER2_EMAIL)
                .password(internalPassword)
                .role(AccountRole.PARTNER)
                .emailVerified(true)
                .status("ACTIVE")
                // createdAt은 @PrePersist에서 자동 설정됨
                .build();
        partner2Account = accountRepository.save(partner2Account);

        PartnerEntity partner2 = PartnerEntity.create(
                "아레나",
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

        log.info("Partner 2(보조) 계정 생성 완료: {} (비밀번호는 시드 전용 랜덤)", DEMO_PARTNER2_EMAIL);
    }

    private void createMasterColors() {
        log.info("컬러 마스터 데이터 생성 중...");
        colorRepository.save(ColorEntity.builder().code("BLACK").label("블랙").hex("#000000").sortOrder(10).build());
        colorRepository.save(ColorEntity.builder().code("NAVY").label("네이비").hex("#001F3F").sortOrder(20).build());
        colorRepository.save(ColorEntity.builder().code("WHITE").label("화이트").hex("#FFFFFF").sortOrder(30).build());
        colorRepository.save(ColorEntity.builder().code("CLEAR").label("투명").hex("#E8E8E8").sortOrder(40).build());
        colorRepository.save(ColorEntity.builder().code("MIRROR").label("미러").hex("#C0C0C0").sortOrder(50).build());
        colorRepository.save(ColorEntity.builder().code("PINK").label("핑크").hex("#F7D0EB").sortOrder(60).build());
        colorRepository.save(ColorEntity.builder().code("RED").label("빨강").hex("#FF0000").sortOrder(70).build());
        colorRepository.save(ColorEntity.builder().code("GREY").label("회색").hex("#808080").sortOrder(80).build());
        colorRepository.save(ColorEntity.builder().code("BLUE").label("블루").hex("#123A63").sortOrder(90).build());
        colorRepository.save(ColorEntity.builder().code("GREEN").label("연두").hex("#A8D47A").sortOrder(90).build());
        log.info("컬러 마스터 데이터 생성 완료 (블랙, 네이비, 화이트, 투명, 미러, 핑크, 빨강, 회색, 블루, 연두)");
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
     * 상품 및 옵션 더미 데이터 생성 (20종, 타입 골고루, 옵션 없는 단일 상품 포함)
     */
    private void createProductsAndOptions() {
        log.info("상품 및 옵션 더미 데이터 생성 중...");

        PartnerEntity partner1 = partnerRepository.findAll().stream()
                .filter(p -> p.getPartnerName().equals("스피도"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Partner 1을 찾을 수 없습니다."));
        PartnerEntity partner2 = partnerRepository.findAll().stream()
                .filter(p -> p.getPartnerName().equals("아레나"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Partner 2를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        int optionCount = 0;
        int singleProductCount = 0;

        for (ProductSeedSpec spec : buildProductSeedCatalog()) {
            PartnerEntity partner = spec.usePartner1() ? partner1 : partner2;
            ProductEntity product = saveProduct(partner, spec, now);
            saveProductGallery(product, spec.imageUrl(), spec.galleryUrls());
            if (spec.optionVariants() == null || spec.optionVariants().length == 0) {
                singleProductCount++;
            } else {
                optionCount += saveOptionVariants(product, partner, spec.optionVariants());
            }
        }

        log.info("상품 시드 완료: 상품 26개, 옵션 {}개, 옵션 없는 단일 상품 {}개",
                optionCount, singleProductCount);
    }

    private List<ProductSeedSpec> buildProductSeedCatalog() {
        return List.of(
                // --- Partner 1: 옵션 있음 (6) ---
                new ProductSeedSpec(true, "SPEEDO", "실키 핏 프로 실리콘 캡", ProductType.SWIM_CAP, ProductSubType.CAP_SILICONE,
                        "15000",
                        """
                        물에 젖어도 쉽게 벗겨지지 않는 소프트 실리콘 소재의 수모입니다.
                        머리카락을 깔끔하게 정리해 주며, 반복 사용에도 늘어남이 적습니다.
                        실내·실외 수영은 물론 수업과 대회 연습용으로도 적합합니다.
                        사용 후에는 세척한 뒤 그늘에서 건조해 주세요.""",
                        "/images/products/cap01.png",
                        List.of(
                                "/images/products/cap01.png",
                                "/images/products/cap02.png"),
                        opts(
                                variants("블랙", "S", 0), variants("블랙", "M", 0),
                                variants("네이비", "S", 0), variants("네이비", "M", 0))),
                new ProductSeedSpec(true, "SPEEDO", "클린핏 컷아웃 스윔수트", ProductType.SWIMSUIT_WOMEN, ProductSubType.ONE_PIECE,
                        "89000",
                        """
                        몸의 라인을 자연스럽게 잡아 주는 여성용 원피스 수영복입니다.
                        어깨끈 조절이 가능해 체형에 맞게 착용할 수 있으며,
                        염소 처리된 물에서도 색 바램이 적은 원단을 사용했습니다.
                        수영 후 찬물에 가볍게 헹구고 통풍이 잘 되는 곳에서 말려 주세요.""",
                        "/images/products/woman01.png",
                        List.of(
                            "/images/products/woman01.png",
                            "/images/products/woman02.png",
                            "/images/products/woman03.png"
                        ),
                        opts(
                                variants("블랙", "M", 0), variants("블랙", "L", 0),
                                variants("회색", "M", 0), variants("회색", "L", 0))),
                new ProductSeedSpec(true, "SPEEDO", "뜨왈 쟈도르 크로스백 비키니", ProductType.SWIMSUIT_WOMEN, ProductSubType.BIKINI,
                        "79000",
                        """
                        상·하의 세트로 구성된 비키니 수영복입니다.
                        상체는 안정감 있는 크로스백 타입이며,
                        하의는 허리 라인이 편안한 미드 라이즈 디자인입니다.""",
                        "/images/products/woman04.png",
                        List.of(
                            "/images/products/woman04.png",
                            "/images/products/woman05.png"
                        ),
                        opts(
                                variants("핑크", "S", 0), variants("핑크", "M", 0))),
                new ProductSeedSpec(true, "SPEEDO", "파워플렉스 플래티넘 잠머 IX", ProductType.SWIMSUIT_MEN, ProductSubType.JAMMER,
                        "72000",
                        """
                        무릎 위까지 오는 남성용 잠머 수영복으로, 수영 시 수직 저항을 줄여 줍니다.
                        허리 밴딩에 실리콘 그립이 들어가 격한 킥 동작에도 흘러내림이 적습니다.
                        장시간 착용에도 피부 마찰이 적은 평면 봉제를 적용했습니다.""",
                        "/images/products/man01.png",
                        List.of(
                            "/images/products/man01.png",
                            "/images/products/man02.png",
                            "/images/products/man03.png",
                            "/images/products/man04.png"
                        ),
                        opts(
                                variants("블랙", "M", 0), variants("블랙", "L", 0),
                                variants("네이비", "M", 0), variants("네이비", "L", 0))),
                new ProductSeedSpec(true, "SPEEDO", "키즈 스플래시 프린트 원피스", ProductType.SWIMSUIT_KIDS, ProductSubType.ONE_PIECE,
                        "49000",
                        """
                        편안한 착용감의 키즈 원피스 수영복입니다.
                        귀여운 패턴과 안정감 있게 착용감이 돋보이며며,
                        간편한 지퍼 디자인으로 탈착이 쉽습니다.
                        피부에 닿는 안감은 부드러운 소재를 사용해 편안함을 높였습니다.""",
                        "/images/products/kid01.png",
                        List.of(
                            "/images/products/kid01.png",
                            "/images/products/kid03.png",
                            "/images/products/kid02.png",
                            "/images/products/kid04.png"
                        ),
                        opts(
                                variants("빨강", "S", 0), variants("빨강", "M", 0),
                                variants("화이트", "S", 0), variants("화이트", "M", 0))),
                new ProductSeedSpec(true, "SPEEDO", "라이트웨이트 코튼 메쉬 캡", ProductType.SWIM_CAP, ProductSubType.CAP_FABRIC,
                        "9000",
                        """
                        통기성이 좋은 폴리에스터 소재의 천 수모입니다.
                        머리카락 보호와 수영장 규정 준수에 적합하며,
                        실리콘 수모보다 착용감이 가볍습니다.
                        세탁망 사용을 권장합니다.""",
                        "/images/products/cap03.png",
                        List.of(
                            "/images/products/cap03.png",
                            "/images/products/cap04.png"
                        ),
                        opts(variants("블랙", "FREE", 0), variants("화이트", "FREE", 0))),
                // --- Partner 1: 옵션 없음 (5) ---
                new ProductSeedSpec(true, "SPEEDO", "초흡수 마이크로파이버 래핑 타월 80", ProductType.SWIM_TOY, ProductSubType.NONE,
                        "15900",
                        """
                        80×160cm 규격의 마이크로파이버 수건으로, 수영 후 몸을 빠르게 말려 줍니다.
                        흡수력이 좋고 가볍게 휴대할 수 있어 수영장·헬스장·여행용으로 많이 사용합니다.
                        표백제 세탁은 피해 주세요.""",
                        "/images/products/towel01.png",
                        List.of(
                            "/images/products/towel01.png"
                        ), new String[0][]),
                new ProductSeedSpec(true, "SPEEDO", "소프트 실 이어플러그 3P 세트", ProductType.ETC, ProductSubType.NONE,
                        "12900",
                        """
                        물 유입을 줄여 주는 소프트 실리콘 귀마개 3쌍 세트입니다.
                        S·M·L 사이즈가 함께 구성되어 가족 단위로 사용하기 좋습니다.
                        사용 후 흐르는 물에 헹구고 통풍이 잘 되는 곳에서 보관하세요.""",
                        "/images/products/earplugs01.png",
                        List.of(
                            "/images/products/earplugs01.png"
                        ), new String[0][]),
                new ProductSeedSpec(true, "SPEEDO", "아쿠아 퍼지 클렌징 젤 200ml", ProductType.ETC, ProductSubType.NONE,
                        "18900",
                        """
                        염소 잔류물과 염분을 부드럽게 씻어 내는 약산성 클렌징 젤입니다.
                        수영 후 피부 당김을 줄이도록 보습 성분을 배합했습니다.
                        눈 주위에 들어갔을 때는 즉시 흐르는 물로 씻어 내세요.""",
                        "/images/products/cosmetic03.png",
                        List.of(
                            "/images/products/cosmetic01.png",
                            "/images/products/cosmetic02.png",
                            "/images/products/cosmetic03.png"
                        ), new String[0][]),
                new ProductSeedSpec(true, "SPEEDO", "베이직 샤인핑 앵글백", ProductType.ETC, ProductSubType.NONE,
                        "11900",
                        """
                        수경·귀마개·수모를 한곳에 보관할 수 있는 통풍 메쉬 파우치입니다.
                        지퍼 잠금과 걸이 끈이 있어 가방 안에서도 정리가 쉽습니다.
                        젖은 장비를 넣은 뒤에는 완전히 말려 주세요.""",
                        "/images/products/pouch01.png",
                        List.of(
                            "/images/products/pouch01.png"
                        ), new String[0][]),
                new ProductSeedSpec(true, "SPEEDO", "노즈 가드 프로 클립", ProductType.ETC, ProductSubType.NONE,
                        "8900",
                        """
                        코로 들어오는 물을 막아 주는 수영용 노즈클립입니다.
                        부드러운 패드가 달려 있어 장시간 착용에도 압박감이 적습니다.
                        사용 전후에 흐르는 물로 세척해 주세요.""",
                        "/images/products/noseclip02.png",
                        List.of(
                        "/images/products/noseclip01.png",
                        "/images/products/noseclip02.png"
                        ), new String[0][]),
                // --- Partner 2: 옵션 있음 (6) ---
                new ProductSeedSpec(false, "ARENA", "코브라 트라이 안티포그 수경", ProductType.SWIM_GOGGLES, ProductSubType.NONE,
                        "35000",
                        """
                        안개 방지 코팅이 적용된 일반형 수경입니다.
                        3D 패드가 눈 주변을 부드럽게 감싸 물샘을 줄이고,
                        렌즈는 자외선 차단 처리가 되어 실내외 겸용으로 사용할 수 있습니다.""",
                        "/images/products/glasses01.png",
                        List.of(
                            "/images/products/glasses01.png",
                            "/images/products/glasses02.png",
                            "/images/products/glasses03.png"
                        ),
                        opts(variants("투명", "일반", 0), variants("미러", "일반", 5000))),
                new ProductSeedSpec(false, "ARENA", "스킨핏 UV 래쉬가드 V2", ProductType.SWIMSUIT_WOMEN, ProductSubType.RASH_GUARD,
                        "54000",
                        """
                        자외선 차단 기능이 있는 여성용 래쉬가드입니다.
                        해변에서의 보습과 수영 전후 체온 유지에 도움이 되며,
                        신축성 원단으로 팔 움직임이 편합니다.""",
                        "/images/products/woman06.png",
                        List.of(
                            "/images/products/woman06.png",
                            "/images/products/woman07.png",
                            "/images/products/woman08.png",
                            "/images/products/woman09.png"
                        ),
                        opts(
                                variants("블랙", "M", 0), variants("블랙", "L", 0),
                                variants("화이트", "M", 0), variants("화이트", "L", 0))),
                new ProductSeedSpec(false, "ARENA", "썸머 웨이브 에센셜 트렁크", ProductType.SWIMSUIT_MEN, ProductSubType.TRUNKS,
                        "58000",
                        """
                        일상적인 핏의 남성용 트렁크 수영복입니다.
                        허리 밴딩에 조절 끈이 있어 사이즈 조절이 가능하며,
                        물놀이와 가벼운 수영 연습에 적합합니다.""",
                        "/images/products/man05.png",
                        List.of(
                            "/images/products/man05.png",
                            "/images/products/man06.png",
                            "/images/products/man07.png"
                        ),
                        opts(
                                variants("핑크", "M", 0), variants("핑크", "L", 0),
                                variants("블랙", "M", 0), variants("블랙", "L", 0))),
                new ProductSeedSpec(false, "ARENA", "엘리트 맥스 숏핀 블랙 (오리발가방 증정)", ProductType.FINS, ProductSubType.FINS_SHORT,
                        "55000",
                        """
                        발목까지 오는 숏핀으로, 킥 연습 시 추진력을 높여 줍니다.
                        초·중급자 연습용으로 적당한 강성을 가지며,
                        실리콘 풋 포켓이 발에 밀착됩니다.""",
                        "/images/products/pin01.png",
                        List.of(
                            "/images/products/pin01.png"
                        ),
                        opts(variants("블랙", "S", 0), variants("블랙", "M", 0), variants("블랙", "L", 0))),
                new ProductSeedSpec(false, "ARENA", "드라이존 워터프루프 백팩 25L", ProductType.ETC, ProductSubType.NONE,
                        "45000",
                        """
                        25L 용량의 방수 백팩으로, 젖은 수영복과 수건을 분리해 넣을 수 있습니다.
                        가슴·허리 스트랩이 있어 이동 시 편합니다.""",
                        "/images/products/bag01.png",
                        List.of(
                            "/images/products/bag01.png",
                            "/images/products/bag02.png",
                            "/images/products/bag03.png"
                        ),
                        opts(variants("화이트", "FREE", 0), variants("네이비", "FREE", 0))),
                new ProductSeedSpec(false, "ARENA", "스윔 프로 롱핀 오픈워터", ProductType.FINS, ProductSubType.FINS_LONG,
                        "89000",
                        """
                        스노클링·오픈워터 훈련에 쓰이는 롱핀입니다.
                        긴 블레이드가 추진 효율을 높이며,
                        발목 고정력이 좋아 장시간 사용에도 피로를 분산합니다.""",
                        "/images/products/pin02.png",
                        List.of(
                            "/images/products/pin02.png",
                            "/images/products/pin03.png"
                        ),
                        opts(variants("회색", "S", 0), variants("회색", "M", 0))),
                // --- Partner 2: 옵션 없음 (3) ---
                new ProductSeedSpec(false, "ARENA", "비치 워크 아쿠아 슈즈", ProductType.ETC, ProductSubType.NONE,
                        "22900",
                        """
                        수영장·해변 바닥을 보호해 주는 아쿠아 슈즈입니다.
                        밑창이 미끄럼 방지 처리되어 있고 발을 안전하게 보호합니다.
                        신을 때 양맙 착용을 권장합니다.""",
                        "/images/products/shoes01.png",
                        List.of(
                           "/images/products/shoes01.png"
                        ), new String[0][]),
                new ProductSeedSpec(false, "ARENA", "스노클 스타터 풀페이스 세트", ProductType.SWIM_GOGGLES, ProductSubType.NONE,
                        "42000",
                        """
                        초보 스노클링용 마스크와 스노클이 함께 구성된 세트입니다.
                        180도 시야의 마스크와 물막이 밸브가 달린 스노클로 호흡이 편합니다.
                        사용 후에는 키슬링 세척을 권장합니다.""",
                        "/images/products/set01.png",
                        List.of(
                            "/images/products/set01.png"
                        ), new String[0][]),
                new ProductSeedSpec(false, "ARENA", "오버사이즈 샌드프루프 비치 타월", ProductType.ETC, ProductSubType.NONE,
                        "27900",
                        """
                        100×180cm 대형 비치 타월로, 수영 후 몸을 감거나 바닥에 펼쳐 사용할 수 있습니다.
                        코튼 혼방 소재로 피부에 부드럽고 흡수력이 좋습니다.""",
                        "/images/products/towel02.png", 
                        List.of(
                            "/images/products/towel02.png"
                        ), new String[0][]),

                new ProductSeedSpec(true, "SPEEDO", "엔듀런스 플러스 레이서백 원피스", ProductType.SWIMSUIT_WOMEN, ProductSubType.ONE_PIECE,
                        "95000",
                        """
                        장시간 수영에도 형태가 유지되는 엔듀런스 플러스 원단을 사용한 원피스입니다.
                        레이서백 디자인으로 어깨 움직임이 자유롭고, 염소에 강한 내구성을 자랑합니다.
                        수업·훈련·일반 수영 모두에 적합합니다.""",
                        "/images/products/woman10.png",
                        List.of(
                            "/images/products/woman10.png",
                            "/images/products/woman11.png"
                        ),
                        opts(
                                variants("블랙", "S", 0), variants("블랙", "M", 0), variants("블랙", "L", 0),
                                variants("네이비", "S", 0), variants("네이비", "M", 0), variants("네이비", "L", 0))),
                new ProductSeedSpec(true, "SPEEDO", "플로럴 블룸 모더스트 원피스", ProductType.SWIMSUIT_WOMEN, ProductSubType.ONE_PIECE,
                        "109000",
                        """
                        꽃무늬 프린트가 포인트인 여성용 모더스트 커버리지 원피스입니다.
                        높은 네크라인과 긴 기장으로 자외선 차단과 체형 커버를 동시에 제공합니다.
                        수영장뿐 아니라 워터파크·리조트에서도 활용도가 높습니다.""",
                        "/images/products/woman12.png",
                        List.of(
                            "/images/products/woman12.png",
                            "/images/products/woman13.png",
                            "/images/products/woman14.png",
                            "/images/products/woman15.png"
                        ),
                        opts(
                                variants("핑크", "M", 0), variants("핑크", "L", 0),
                                variants("블루", "M", 0), variants("블루", "L", 0))),
                new ProductSeedSpec(true, "SPEEDO", "이코 컨투어 V백 원피스", ProductType.SWIMSUIT_WOMEN, ProductSubType.ONE_PIECE,
                        "82000",
                        """
                        V자형 등판 라인이 세련된 여성용 원피스 수영복입니다.
                        친환경 재생 원단(EcoFiber)을 사용해 환경 부담을 줄였으며,
                        적당한 압축력으로 편안하면서도 깔끔한 실루엣을 만들어 줍니다.""",
                        "/images/products/woman16.png",
                        List.of(
                            "/images/products/woman16.png",
                            "/images/products/woman17.png",
                            "/images/products/woman18.png",
                            "/images/products/woman19.png"
                        ),
                        opts(
                                variants("블랙", "S", 0), variants("블랙", "M", 0),
                                variants("회색", "S", 0), variants("회색", "M", 0))),

                new ProductSeedSpec(false, "ARENA", "파워스킨 ST 넥스트 원피스", ProductType.SWIMSUIT_WOMEN, ProductSubType.ONE_PIECE,
                        "120000",
                        """
                        국제 수영 연맹(FINA) 승인을 받은 대회용 여성 원피스입니다.
                        근육 압축 기술이 적용되어 수중 저항을 줄이고,
                        고밀도 원단이 체형을 잡아 줍니다. 경기와 기록 단축을 목표로 하는 수영인에게 추천합니다.""",
                        "/images/products/woman20.png",
                        List.of(
                            "/images/products/woman20.png",
                            "/images/products/woman21.png",
                            "/images/products/woman22.png"
                        ),
                        opts(
                                variants("블랙", "S", 0), variants("블랙", "M", 0), variants("블랙", "L", 0))),
                new ProductSeedSpec(false, "ARENA", "스포티바 하이넥 원피스", ProductType.SWIMSUIT_WOMEN, ProductSubType.ONE_PIECE,
                        "68000",
                        """
                        스포티한 컬러 배색의 하이넥 원피스 수영복입니다.
                        목까지 올라오는 디자인으로 일광 차단 효과가 있으며,
                        뒷면 지퍼로 탈착이 간편합니다. 수영 강습·아쿠아로빅에 적합합니다.""",
                        "/images/products/woman23.png",
                        List.of(
                            "/images/products/woman23.png",
                            "/images/products/woman24.png",
                            "/images/products/woman25.png",
                            "/images/products/woman26.png"
                        ),
                        opts(
                                variants("레드", "M", 0), variants("레드", "L", 0),
                                variants("블랙", "M", 0), variants("블랙", "L", 0))),
                new ProductSeedSpec(false, "ARENA", "트로피컬 서프 홀터넥 비키니", ProductType.SWIMSUIT_WOMEN, ProductSubType.BIKINI,
                        "74000",
                        """
                        열대 식물 패턴의 홀터넥 비키니 세트입니다.
                        상의는 홀터넥과 밴드형 이중 고정으로 격한 움직임에도 안정적이며,
                        하의는 사이드 스트링 조절이 가능합니다. 비치·풀사이드에서 돋보이는 디자인입니다.""",
                        "/images/products/woman27.png",
                        List.of(
                            "/images/products/woman27.png",
                            "/images/products/woman28.png",
                            "/images/products/woman29.png",
                            "/images/products/woman30.png"
                        ),
                        opts(
                                variants("그린", "S", 0), variants("그린", "M", 0),
                                variants("블루", "S", 0), variants("블루", "M", 0)))
        );
    }

    private static String[][] variants(String color, String size, long addPrice) {
        return new String[][] {{color, size, String.valueOf(addPrice)}};
    }

    @SafeVarargs
    private static String[][] opts(String[][]... parts) {
        return Arrays.stream(parts).flatMap(Arrays::stream).toArray(String[][]::new);
    }

    private record ProductSeedSpec(
            boolean usePartner1,
            String brandName,
            String name,
            ProductType type,
            ProductSubType subType,
            String price,
            String description,
            String imageUrl,
            List<String> galleryUrls,
            String[][] optionVariants,
            String materialInfo,
            String originCountry,
            String manufactureCountry,
            String careInstructions,
            String sizeGuideJson
    ) {
        ProductSeedSpec(boolean usePartner1, String brandName, String name,
                        ProductType type, ProductSubType subType,
                        String price, String description, String imageUrl,
                        List<String> galleryUrls, String[][] optionVariants) {
            this(usePartner1, brandName, name, type, subType, price,
                 description, imageUrl, galleryUrls, optionVariants,
                 null, null, null, null, null);
        }
    }

    private record EventSeedSpec(
            String title,
            String content,
            EventStatus status,
            LocalDateTime customerExposeAt,
            LocalDateTime customerEventStartAt,
            LocalDateTime customerEventEndAt,
            String thumbnailUrl
    ) {}

    /** 상세 갤러리(product_image). galleryUrls 비어 있으면 대표 URL만 쓰고 행은 넣지 않음. */
    private void saveProductGallery(ProductEntity product, String primaryImageUrl, List<String> galleryUrls) {
        if (galleryUrls == null || galleryUrls.isEmpty()) {
            return;
        }
        String primary = primaryImageUrl != null ? primaryImageUrl.trim() : "";
        boolean hasPrimaryInList = galleryUrls.stream()
                .anyMatch(u -> u != null && !u.isBlank() && u.trim().equals(primary));
        int order = 0;
        for (String raw : galleryUrls) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String url = raw.trim();
            boolean isPrimary = url.equals(primary) || (!hasPrimaryInList && order == 0);
            productImageRepository.save(ProductImageEntity.create(
                    product, url, null, order++, isPrimary));
        }
    }

    private int saveOptionVariants(ProductEntity product, PartnerEntity partner, String[][] variants) {
        int count = 0;
        for (String[] row : variants) {
            long addPrice = Long.parseLong(row[2]);
            optionRepository.save(OptionEntity.builder()
                    .product(product)
                    .partner(partner)
                    .color(row[0])
                    .size(row[1])
                    .optionAddPrice(addPrice)
                    .optionStatus(ActiveStatus.ACTIVE)
                    .build());
            count++;
        }
        return count;
    }

    private ProductEntity saveProduct(
            PartnerEntity partner,
            ProductSeedSpec spec,
            LocalDateTime createdAt
    ) {
        String material = spec.materialInfo() != null ? spec.materialInfo() : defaultMaterial(spec.type(), spec.subType());
        String origin = spec.originCountry() != null ? spec.originCountry() : "대한민국";
        String manufacture = spec.manufactureCountry() != null ? spec.manufactureCountry() : defaultManufactureCountry(spec.type());
        String care = spec.careInstructions() != null ? spec.careInstructions() : defaultCareInstructions(spec.type());
        String sizeJson = spec.sizeGuideJson() != null ? spec.sizeGuideJson() : defaultSizeGuideJson(spec.type(), spec.subType());

        long seq = productRepository.count() + 1;
        String sku = generateSku(spec.brandName(), spec.type(), seq);

        ProductEntity product = ProductEntity.builder()
                .productName(spec.name())
                .brandName(spec.brandName())
                .productType(spec.type())
                .productSubType(spec.subType())
                .productPrice(spec.price())
                .productDescription(spec.description())
                .productImageUrl(spec.imageUrl())
                .productCreatedAt(createdAt)
                .sku(sku)
                .materialInfo(material)
                .originCountry(origin)
                .manufactureCountry(manufacture)
                .careInstructions(care)
                .sizeGuideJson(sizeJson)
                .productActiveStatus(ActiveStatus.ACTIVE)
                .build();
        product.setPartner(partner);
        return productRepository.save(product);
    }

    private String generateSku(String brand, ProductType type, long seq) {
        String brandCode = switch (brand) {
            case "SPEEDO" -> "SPD";
            case "ARENA" -> "ARN";
            default -> "ETC";
        };
        String typeCode = switch (type) {
            case SWIMSUIT_WOMEN -> "SW";
            case SWIMSUIT_MEN -> "SM";
            case SWIMSUIT_KIDS -> "SK";
            case SWIM_CAP -> "CP";
            case SWIM_GOGGLES -> "GG";
            case FINS -> "FN";
            case SWIM_TOY -> "TY";
            case ETC -> "ET";
        };
        return String.format("%s-%s-%04d", brandCode, typeCode, seq);
    }

    private String defaultMaterial(ProductType type, ProductSubType sub) {
        return switch (type) {
            case SWIMSUIT_WOMEN, SWIMSUIT_MEN, SWIMSUIT_KIDS ->
                sub == ProductSubType.RASH_GUARD ? "폴리에스터 85%, 스판덱스 15%" : "폴리에스터 80%, 스판덱스 20%";
            case SWIM_CAP ->
                sub == ProductSubType.CAP_SILICONE ? "실리콘 100%" : "폴리에스터 100%";
            case SWIM_GOGGLES -> "폴리카보네이트 렌즈, 실리콘 패드, TPR 프레임";
            case FINS -> "TPR 블레이드, 실리콘 풋포켓";
            case SWIM_TOY -> "극세사 마이크로파이버 100%";
            case ETC -> "폴리에스터";
        };
    }

    private String defaultManufactureCountry(ProductType type) {
        return switch (type) {
            case SWIMSUIT_WOMEN, SWIMSUIT_MEN, SWIMSUIT_KIDS -> "한국";
            case SWIM_CAP, SWIM_GOGGLES, FINS -> "한국";
            case SWIM_TOY, ETC -> "한국";
        };
    }

    private String defaultCareInstructions(ProductType type) {
        return switch (type) {
            case SWIMSUIT_WOMEN, SWIMSUIT_MEN, SWIMSUIT_KIDS ->
                "찬물 손세탁 권장, 표백제 사용 금지, 직사광선 건조 금지, 탈수기 사용 금지";
            case SWIM_CAP ->
                "사용 후 맑은 물에 헹구어 그늘에서 자연 건조, 파우더 도포 후 보관 권장";
            case SWIM_GOGGLES ->
                "사용 후 흐르는 물에 헹구어 건조, 렌즈 안쪽 문지르지 않기 (안개 방지 코팅 보호)";
            case FINS ->
                "사용 후 흐르는 물에 세척, 직사광선을 피해 보관";
            case SWIM_TOY, ETC ->
                "중성 세제 사용, 세탁망 권장, 그늘에서 건조";
        };
    }

    private String defaultSizeGuideJson(ProductType type, ProductSubType sub) {
        if (sub == ProductSubType.ONE_PIECE || sub == ProductSubType.MONOKINI) {
            return """
                {"templateKey":"SWIMSUIT_WOMEN_ONEPIECE","rows":[
                  {"sizeLabel":"S","chestCm":"78-82","waistCm":"60-64","hipCm":"86-90","torsoCm":"155-160"},
                  {"sizeLabel":"M","chestCm":"82-86","waistCm":"64-68","hipCm":"90-94","torsoCm":"160-165"},
                  {"sizeLabel":"L","chestCm":"86-90","waistCm":"68-72","hipCm":"94-98","torsoCm":"165-170"}
                ]}""";
        }
        if (sub == ProductSubType.BIKINI) {
            return """
                {"templateKey":"SWIMSUIT_WOMEN_BIKINI","rows":[
                  {"sizeLabel":"S","chestCm":"78-82","waistCm":"60-64","hipCm":"86-90"},
                  {"sizeLabel":"M","chestCm":"82-86","waistCm":"64-68","hipCm":"90-94"},
                  {"sizeLabel":"L","chestCm":"86-90","waistCm":"68-72","hipCm":"94-98"}
                ]}""";
        }
        if (sub == ProductSubType.JAMMER || sub == ProductSubType.BRIEF || sub == ProductSubType.TRUNKS) {
            return """
                {"templateKey":"SWIMSUIT_MEN_PANTS","rows":[
                  {"sizeLabel":"M","waistCm":"76-80","hipCm":"90-94"},
                  {"sizeLabel":"L","waistCm":"80-84","hipCm":"94-98"},
                  {"sizeLabel":"XL","waistCm":"84-88","hipCm":"98-102"}
                ]}""";
        }
        if (sub == ProductSubType.RASH_GUARD) {
            return """
                {"type":"SWIMSUIT","rows":[
                  {"sizeLabel":"M","chestCm":"82-86","waistCm":"64-68","hipCm":"90-94","torsoCm":"160-165"},
                  {"sizeLabel":"L","chestCm":"86-90","waistCm":"68-72","hipCm":"94-98","torsoCm":"165-170"}
                ]}""";
        }
        if (sub == ProductSubType.FINS_SHORT || sub == ProductSubType.FINS_LONG) {
            return """
                {"type":"FINS","rows":[
                  {"sizeLabel":"S (230-240)","footLengthCm":"23.0-24.0"},
                  {"sizeLabel":"M (250-260)","footLengthCm":"25.0-26.0"},
                  {"sizeLabel":"L (270-280)","footLengthCm":"27.0-28.0"}
                ]}""";
        }
        return null;
    }

    private void saveColorSizeOptions(
            ProductEntity product,
            PartnerEntity partner,
            String[] colors,
            String[] sizes,
            long addPrice
    ) {
        for (String color : colors) {
            for (String size : sizes) {
                optionRepository.save(OptionEntity.builder()
                        .product(product)
                        .partner(partner)
                        .color(color)
                        .size(size)
                        .optionAddPrice(addPrice)
                        .optionStatus(ActiveStatus.ACTIVE)
                        .build());
            }
        }
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
     * 고객 더미 데이터 생성 (2명: 메인 테스트 1 + 보조 1)
     */
    private void createCustomers() {
        log.info("고객 더미 데이터 생성 중...");

        if (customerGradeRepository.count() == 0) {
            log.info("등급이 없어 초기 등급을 생성합니다...");
            createDefaultGrades();
        }

        CustomerGradeEntity defaultGrade = customerGradeRepository
                .findByGradeName(CustomerGradeEnum.BEGINNER)
                .orElseGet(() -> customerGradeRepository
                        .findFirstByIsActiveTrueOrderByGradeLevelAsc()
                        .orElseThrow(() -> new IllegalStateException("활성화된 등급이 없습니다.")));

        LocalDateTime now = LocalDateTime.now();
        String mainPassword = passwordEncoder.encode(SEED_MAIN_PASSWORD);
        String internalPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        saveCustomer(DEMO_CUSTOMER1_EMAIL, "김민지", mainPassword, mainPassword,
                LocalDate.of(1992, 3, 15), now.minusDays(60), defaultGrade);
        saveCustomer(DEMO_CUSTOMER2_EMAIL, "박수현", internalPassword, internalPassword,
                LocalDate.of(1995, 7, 20), now.minusDays(30), defaultGrade);

        log.info("고객 2명 생성 완료 (메인 테스트: {})", DEMO_CUSTOMER1_EMAIL);
    }

    private void saveCustomer(
            String email,
            String name,
            String accountPasswordHash,
            String customerPasswordHash,
            LocalDate birth,
            LocalDateTime createdAt,
            CustomerGradeEntity grade
    ) {
        AccountEntity account = accountRepository.save(AccountEntity.builder()
                .email(email)
                .password(accountPasswordHash)
                .role(AccountRole.CUSTOMER)
                .emailVerified(true)
                .status("ACTIVE")
                .build());

        CustomerEntity customer = CustomerEntity.builder()
                .customerName(name)
                .customerEmail(email)
                .customerPassword(customerPasswordHash)
                .customerBirth(birth)
                .customerCreateAt(createdAt)
                .emailChecked(true)
                .customerGrade(grade)
                .totalPurchaseAmount(0L)
                .totalOrderCount(0)
                .build();
        customer.setAccount(account);
        customerRepository.save(customer);
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
    
    private OptionEntity pickOptionForProduct(ProductEntity product, int seed) {
        List<OptionEntity> productOptions = optionRepository.findByProduct_ProductNo(product.getProductNo());
        if (productOptions.isEmpty()) {
            return null;
        }
        productOptions.sort(Comparator.comparing(OptionEntity::getOptionNo));
        return productOptions.get(Math.floorMod(seed, productOptions.size()));
    }

    /**
     * 결제 완료 상태로 주문에 결제를 연결합니다(Payment FK + PaymentStatus.PAID).
     */
    private void linkPaidCardPayment(OrderEntity order, LocalDateTime paidAt, long amount) {
        linkPaidCardPayment(order, paidAt, amount, "SEED-PAY-" + order.getOrderNo());
    }

    private void linkPaidCardPayment(OrderEntity order, LocalDateTime paidAt, long amount, String paymentKey) {
        PaymentEntity payment = PaymentEntity.builder()
                .paymentAmount(amount)
                .paymentMethod("CARD")
                .paymentCreatedAt(paidAt)
                .paidAt(paidAt)
                .paymentCancelYn(false)
                .provider("seed-toss")
                .paymentKey(paymentKey)
                .pgOrderId("SEED-PG-" + order.getOrderNo())
                .status(PaymentStatus.PAID)
                .build();
        PaymentEntity saved = paymentRepository.save(payment);
        saved.setOrder(order);
        paymentRepository.save(saved);
        order.setPayment(saved);
        orderRepository.save(order);
    }

    private PartnerEntity resolvePartnerForOrderItem(OrderItemEntity item) {
        if (item.getProduct() != null && item.getProduct().getPartner() != null) {
            return item.getProduct().getPartner();
        }
        if (item.getOption() != null && item.getOption().getPartner() != null) {
            return item.getOption().getPartner();
        }
        throw new IllegalStateException("주문 상품에 파트너를 결정할 수 없습니다.");
    }

    private boolean hasRefundedReturn(Long orderItemNo) {
        return returnRepository.findByOrderItem_OrderItemNo(orderItemNo)
                .map(r -> r.getReturnStatus() == ReturnStatus.REFUNDED)
                .orElse(false);
    }

    /**
     * 주문 더미 데이터 생성
     * - 주문 완료(배송중)
     * - 주문 취소
     * - 구매확정
     * - 반품완료
     * <p>
     * 고객·상품·옵션·수량은 인덱스 기반으로 고정되어 동일 시드 그래프를 만듭니다.
     */
    private void createOrders() {
        log.info("주문 더미 데이터 생성 중...");

        List<CustomerEntity> customers = customerRepository.findAll().stream()
                .sorted(Comparator.comparing(CustomerEntity::getCustomerId))
                .collect(Collectors.toList());
        List<ProductEntity> products = productRepository.findAll().stream()
                .sorted(Comparator.comparing(ProductEntity::getProductNo))
                .collect(Collectors.toList());

        if (customers.isEmpty() || products.isEmpty()) {
            log.warn("고객이나 상품이 없어 주문을 생성할 수 없습니다.");
            return;
        }

        int nCust = customers.size();
        int nProd = products.size();

        LocalDateTime now = LocalDateTime.now();

        // 1. 주문 완료(배송중) - 3개 (결제 완료 후 진행중)
        for (int i = 0; i < 3; i++) {
            CustomerEntity customer = customers.get(i % nCust);
            ProductEntity product = products.get((i * 2) % nProd);
            OptionEntity option = pickOptionForProduct(product, i);

            Long itemPrice = Long.parseLong(product.getProductPrice());
            int quantity = (i % 3) + 1;
            Long itemTotalPrice = itemPrice * quantity;

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
            linkPaidCardPayment(order, now.minusDays(5 + i), itemTotalPrice);

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

            DeliveryEntity delivery = DeliveryEntity.builder()
                    .orderItem(orderItem)
                    .deliveryStatus(DeliveryStatus.SHIPPED)
                    .deliveryStartDate(now.minusDays(3 + i))
                    .deliveryTrackingNumber("TRACK" + (1000 + i))
                    .deliveryCourier("CJ대한통운")
                    .build();
            deliveryRepository.save(delivery);
        }

        // 2. 주문 취소 - 2개 (결제 없음)
        for (int i = 0; i < 2; i++) {
            CustomerEntity customer = customers.get((i + 1) % nCust);
            ProductEntity product = products.get((i * 3 + 1) % nProd);
            OptionEntity option = pickOptionForProduct(product, i + 10);

            Long itemPrice = Long.parseLong(product.getProductPrice());
            int quantity = (i % 2) + 1;
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

        // 3. 구매확정 — 정산 시드용으로 충분한 건수(동일 코드·동일 DB면 동일 선택)
        final int completedOrderCount = 28;
        for (int i = 0; i < completedOrderCount; i++) {
            CustomerEntity customer = customers.get((i + 2) % nCust);
            ProductEntity product = products.get((i * 5 + 2) % nProd);
            OptionEntity option = pickOptionForProduct(product, i + 20);

            Long itemPrice = Long.parseLong(product.getProductPrice());
            int quantity = (i % 3) + 1;
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
            linkPaidCardPayment(order, now.minusDays(20 + i), itemTotalPrice);

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .product(product)
                    .option(option)
                    .itemQuantity(quantity)
                    .itemPrice(itemPrice)
                    .itemTotalPrice(itemTotalPrice)
                    .isCancelled(false)
                    .confirmedAt(now.minusDays(19 + i))
                    .completedAt(now.minusDays(12 + i))
                    .build();
            orderItem = orderItemRepository.save(orderItem);

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

        // 4. 반품 신청 가능 (고객1) — UI에서 직접 신청용 1건 (Return row 없음)
        CustomerEntity mainCustomer = customers.stream()
                .filter(c -> DEMO_CUSTOMER1_EMAIL.equals(c.getCustomerEmail()))
                .findFirst()
                .orElse(customers.get(0));
        CustomerEntity secondCustomer = customers.stream()
                .filter(c -> DEMO_CUSTOMER2_EMAIL.equals(c.getCustomerEmail()))
                .findFirst()
                .orElse(customers.get(Math.min(1, nCust - 1)));

        ProductEntity uiReturnProduct = products.get((1 * 4 + 1) % nProd);
        createReturnEligibleOrder(mainCustomer, uiReturnProduct, pickOptionForProduct(uiReturnProduct, 60),
                now, 4, 3, 2, "TRACK-RETURN-ELIG-UI");

        // 4b. 반품 검토 보조 데모용 REQUESTED (사유·증빙 이미지 다양)
        createReturnAiReviewSeeds(mainCustomer, secondCustomer, products, now, nProd);

        // 5. 반품 완료 1건 (관리자 완료 목록용)
        ProductEntity refundedProduct = products.get((7 * 4 + 4) % nProd);
        OptionEntity refundedOption = pickOptionForProduct(refundedProduct, 50);
        Long refundedItemPrice = Long.parseLong(refundedProduct.getProductPrice());
        Long refundedItemTotal = refundedItemPrice;

        OrderEntity refundedOrder = OrderEntity.builder()
                .orderTotalPrice(refundedItemTotal)
                .orderCreatedAt(now.minusDays(30))
                .orderStatus(OrderStatus.ACTIVE)
                .customer(secondCustomer)
                .recipientName(secondCustomer.getCustomerName())
                .recipientPhone("010-1234-5678")
                .deliveryAddress("서울시 강남구 테헤란로 123")
                .deliveryAddressDetail("456호")
                .deliveryZipCode("06234")
                .paymentMethod("CARD")
                .build();
        refundedOrder = orderRepository.save(refundedOrder);
        linkPaidCardPayment(refundedOrder, now.minusDays(30), refundedItemTotal);

        OrderItemEntity refundedItem = OrderItemEntity.builder()
                .order(refundedOrder)
                .product(refundedProduct)
                .option(refundedOption)
                .itemQuantity(1)
                .itemPrice(refundedItemPrice)
                .itemTotalPrice(refundedItemTotal)
                .isCancelled(false)
                .confirmedAt(now.minusDays(29))
                .completedAt(now.minusDays(22))
                .build();
        refundedItem = orderItemRepository.save(refundedItem);

        deliveryRepository.save(DeliveryEntity.builder()
                .orderItem(refundedItem)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .deliveryStartDate(now.minusDays(28))
                .deliveryEndDate(now.minusDays(25))
                .deliveryTrackingNumber("TRACK3000")
                .deliveryCourier("CJ대한통운")
                .build());

        ReturnEntity refundedReturn = returnRepository.save(ReturnEntity.builder()
                .orderItem(refundedItem)
                .returnStatus(ReturnStatus.REFUNDED)
                .returnRequestedAt(now.minusDays(21))
                .returnReasonType(ReturnReasonType.DEFECT)
                .returnReason("수경 렌즈에 미세 스크래치가 있어 환불 처리했습니다.")
                .returnAmount(refundedItemTotal)
                .returnTrackingNumber("RETURN1000")
                .returnCourier("CJ대한통운")
                .build());
        attachReturnImages(refundedReturn, 2, secondCustomer.getCustomerId());

        log.info("주문 더미 데이터 생성 완료:");
        log.info("  - 주문 완료(배송중): 3개");
        log.info("  - 주문 취소: 2개");
        log.info("  - 구매확정: {}개", completedOrderCount);
        log.info("  - 반품 신청 가능(고객1, UI용): 1개");
        log.info("  - 반품 검토 보조 데모용(REQUESTED): 3개");
        log.info("  - 반품 완료(REFUNDED): 1개");
    }

    /** 배송 완료·구매확정 전·7일 이내 — 고객 반품 신청 UI 테스트용 주문 1건 */
    private OrderItemEntity createReturnEligibleOrder(
            CustomerEntity customer,
            ProductEntity product,
            OptionEntity option,
            LocalDateTime now,
            int orderDaysAgo,
            int confirmedDaysAgo,
            int deliveryEndDaysAgo,
            String trackingNo) {
        Long itemPrice = Long.parseLong(product.getProductPrice());
        Long itemTotalPrice = itemPrice;
        LocalDateTime orderAt = now.minusDays(orderDaysAgo);

        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .orderTotalPrice(itemTotalPrice)
                .orderCreatedAt(orderAt)
                .orderStatus(OrderStatus.ACTIVE)
                .customer(customer)
                .recipientName(customer.getCustomerName())
                .recipientPhone("010-1234-5678")
                .deliveryAddress("서울시 강남구 테헤란로 123")
                .deliveryAddressDetail("456호")
                .deliveryZipCode("06234")
                .paymentMethod("CARD")
                .build());
        linkPaidCardPayment(order, orderAt, itemTotalPrice);

        OrderItemEntity orderItem = orderItemRepository.save(OrderItemEntity.builder()
                .order(order)
                .product(product)
                .option(option)
                .itemQuantity(1)
                .itemPrice(itemPrice)
                .itemTotalPrice(itemTotalPrice)
                .isCancelled(false)
                .confirmedAt(now.minusDays(confirmedDaysAgo))
                .build());

        deliveryRepository.save(DeliveryEntity.builder()
                .orderItem(orderItem)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .deliveryStartDate(now.minusDays(confirmedDaysAgo))
                .deliveryEndDate(now.minusDays(deliveryEndDaysAgo))
                .deliveryTrackingNumber(trackingNo)
                .deliveryCourier("CJ대한통운")
                .build());
        return orderItem;
    }

    /** 관리자 반품 검토 보조(return-assist) 데모: REQUESTED + 사유 타입·본문·이미지 수 다양 */
    private void createReturnAiReviewSeeds(
            CustomerEntity customer1,
            CustomerEntity customer2,
            List<ProductEntity> products,
            LocalDateTime now,
            int nProd) {
        ProductEntity p1 = products.get((2 * 4 + 1) % nProd);
        OrderItemEntity defectItem = createReturnEligibleOrder(
                customer1, p1, pickOptionForProduct(p1, 61), now, 5, 4, 3, "TRACK-AI-DEFECT");
        saveReturnSeed(defectItem, ReturnStatus.REQUESTED, ReturnReasonType.DEFECT,
                """
                        수경 외부 렌즈에 미세 스크래치가 보입니다. 개봉 후 첫 사용인데 시야가 흐려져 교환·환불 요청드립니다.
                        사진으로 파손 부위를 첨부했으며, 포장 상태는 정상이었습니다.
                        """.stripIndent(),
                now.minusDays(1), 2, customer1.getCustomerId());

        ProductEntity p2 = products.get((3 * 4 + 2) % nProd);
        OrderItemEntity wrongItem = createReturnEligibleOrder(
                customer1, p2, pickOptionForProduct(p2, 62), now, 6, 5, 4, "TRACK-AI-WRONG");
        saveReturnSeed(wrongItem, ReturnStatus.REQUESTED, ReturnReasonType.WRONG_ITEM,
                """
                        주문한 옵션과 다른 색상이 배송되었습니다. 라벨과 실물 색상이 모두 다릅니다.
                        오배송 확인용 사진을 첨부했습니다.
                        """.stripIndent(),
                now.minusDays(2), 1, customer1.getCustomerId());

        ProductEntity p3 = products.get((4 * 4 + 3) % nProd);
        OrderItemEntity mindChangeItem = createReturnEligibleOrder(
                customer2, p3, pickOptionForProduct(p3, 63), now, 3, 2, 1, "TRACK-AI-MIND");
        saveReturnSeed(mindChangeItem, ReturnStatus.REQUESTED, ReturnReasonType.CHANGE_OF_MIND,
                """
                        단순 변심으로 반품 신청합니다. 상품은 개봉만 했고 사용하지 않았으며
                        태그와 구성품은 그대로 보관 중입니다.
                        """.stripIndent(),
                now.minusDays(1), 0, customer2.getCustomerId());
    }

    private void saveReturnSeed(
            OrderItemEntity orderItem,
            ReturnStatus status,
            ReturnReasonType reasonType,
            String reason,
            LocalDateTime requestedAt,
            int imageCount,
            Long uploaderCustomerId) {
        Long returnAmount = orderItem.getItemTotalPrice();
        ReturnEntity saved = returnRepository.save(ReturnEntity.builder()
                .orderItem(orderItem)
                .returnStatus(status)
                .returnRequestedAt(requestedAt)
                .returnReasonType(reasonType)
                .returnReason(reason)
                .returnAmount(returnAmount)
                .build());

        attachReturnImages(saved, imageCount, uploaderCustomerId);
    }

    private void attachReturnImages(ReturnEntity returnEntity, int imageCount, Long uploaderCustomerId) {
        for (int i = 0; i < imageCount; i++) {
            String storedName = "seed-return-" + returnEntity.getReturnNo() + "-" + i + ".png";
            UploadedFileEntity file = uploadedFileRepository.save(UploadedFileEntity.builder()
                    .category("return-image")
                    .originalName("return-proof-" + i + ".png")
                    .storedName(storedName)
                    .contentType("image/png")
                    .size(2048L)
                    .storagePath("seed/return/" + storedName)
                    .isPrivate(true)
                    .uploadedByRole(AccountRole.CUSTOMER)
                    .uploadedBySubjectId(uploaderCustomerId)
                    .build());
            String imageUrl = "http://localhost:8080/api/files/" + file.getFileId() + "/download";
            returnImageRepository.save(ReturnImageEntity.builder()
                    .returnEntity(returnEntity)
                    .file(file)
                    .imageUrl(imageUrl)
                    .build());
        }
    }

    private void saveEventSeed(AdminEntity admin, EventSeedSpec spec) {
        eventRepository.save(EventEntity.builder()
                .eventTitle(spec.title())
                .eventContent(spec.content())
                .eventStatus(spec.status())
                .customerExposeAt(spec.customerExposeAt())
                .customerEventStartAt(spec.customerEventStartAt())
                .customerEventEndAt(spec.customerEventEndAt())
                .partnerApplyEnabled(false)
                .thumbnailUrl(spec.thumbnailUrl())
                .eventType(EventType.NOTICE)
                .eventMode(EventMode.ADMIN_ONLY)
                .admin(admin)
                .build());
    }

    private void createEvents() {
        log.info("이벤트 더미 데이터 생성 중...");
        AdminEntity admin = adminRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Admin이 없습니다."));
        LocalDateTime now = LocalDateTime.now();

        List<EventSeedSpec> seeds = List.of(
                // 종료 3 — 종료일을 다르게 두어 목록(최근 종료순) 확인용
                new EventSeedSpec(
                        "🤍수영러 첫 만남 혜택🤍",
                        """
                                Lets Dive In 오픈 기념으로 전 상품 할인 혜택을 준비했습니다. 🍾
                                오픈 기간 한정으로 전 카테고리 특별 할인가가 진행되니,
                                매일 사용하는 데일리 수영용품도 부담 없이 쇼핑해보세요.
                                일부 인기 상품은 한정 수량 특가로 조기 품절될 수 있습니다.
                                수영러들의 새로운 쇼핑 공간, 지금 함께 입수해보세요. 🏊
                                """.stripIndent(),
                        EventStatus.ENDED,
                        now.minusDays(125),
                        now.minusDays(120),
                        now.minusDays(18),
                        "/images/event/event01.png"),
                new EventSeedSpec(
                        "여름을 담은 스윔샷 EVENT📸",
                        """
                                여름을 담은 스윔샷 EVENT

                                가장 마음에 들었던 여름을 공유해보세요.
                                반짝이는 물빛, 여행지의 분위기, 수영 후의 여유로운 순간까지
                                당신만의 여름 무드가 담긴 사진이라면 누구나 참여 가능합니다.

                                [ 참여 방법 ]
                                • 여름 감성이 담긴 사진 업로드
                                • 필수 해시태그와 함께 인스타그램 게시
                                • 공식 계정 태그 시 참여 완료

                                [ 사진 예시 ]
                                수영장 · 바다 · 여행 · 물빛 · 선베드 · 수영용품 · 여름 아이템 · 무드샷 등 자유롭게 참여 가능

                                [ 필수 해시태그 ]
                                #SUMMERSWIM #LetsDiveIn

                                [ 이벤트 혜택 ]
                                베스트 스윔상 : 백화점 상품권 증정
                                썸머 무드상 : 스윔 액세서리 증정

                                올여름 가장 빛나는 당신의 순간을 공유해주세요.🫰
                                """.stripIndent(),
                        EventStatus.ENDED,
                        now.minusDays(95),
                        now.minusDays(90),
                        now.minusDays(42),
                        "/images/event/event02.png"),
                new EventSeedSpec(
                        "SUMMER PLAYLIST SHARE🎵🎵",
                        """
                                수영할 때 가장 자주 듣는 노래를 공유해주세요.
                                잔잔한 물빛과 어울리는 플레이리스트부터
                                에너지 넘치는 스윔 음악까지,
                                당신의 여름 무드를 기다리고 있습니다.

                                참여 방법
                                수영할 때 듣는 노래를 캡처해서
                                공식 계정 태그 후, 인스타그램 스토리에 업로드하면
                                참여 완료❕❕

                                #SWIMMINGPLAYLIST #LetsDiveIn

                                참여자를 추첨하여 키링을 드립니다.
                                많은 참여 부탁드립니🎶
                                """.stripIndent(),
                        EventStatus.ENDED,
                        now.minusDays(75),
                        now.minusDays(70),
                        now.minusDays(65),
                        "/images/event/event03.png"),
                new EventSeedSpec(
                        "✨인기 스윔웨어 컬렉션",
                        """
                                이번 시즌 가장 사랑받는 인기 수영복을 특별한 가격으로 만나보세요.✨
                                미니멀한 디자인부터 감각적인 컬러 라인까지, 다양한 스타일을 최대 50% 할인된 가격으로 준비했습니다.
                                데일리 수영, 호텔 수영장, 바캉스룩까지 자연스럽게 어울리는 아이템들로 구성되어 있으며, 편안한 착용감과 세련된 실루엣을 동시에 느낄 수 있습니다.
                                한정 수량으로 진행되는 시즌 이벤트인 만큼 조기 품절될 수 있으며, 온라인 단독 혜택으로 더욱 합리적인 가격에 구매 가능합니다.
                                올여름 가장 분위기 있는 스윔웨어가 기다리고 있습니다.
                                """.stripIndent(),
                        EventStatus.PUBLISHED,
                        now.minusDays(1),
                        now.minusDays(1),
                        now.plusDays(365),
                        "/images/event/event04.png"),
                new EventSeedSpec(
                        "물 만난 세일",
                        """
                                수영 필수템만 모은 특별 할인전이 시작됐습니다. 🌊
                                수경부터 타월, 방수백까지 인기 수영용품을 한자리에서 만나보세요.
                                수영러들이 가장 많이 찾는 베스트 아이템과 매일 사용하는 데일리 수영템을 특별 혜택가로 만나보세요.
                                일부 인기 상품은 한정 수량 특가로 오픈되니 놓치지 마세요. 🥰
                                """.stripIndent(),
                        EventStatus.PRIVATE,
                        now.plusDays(14),
                        now.plusDays(14),
                        now.plusDays(45),
                        "/images/event/event05.png"));

        seeds.forEach(spec -> saveEventSeed(admin, spec));

        log.info("이벤트 생성 완료: 종료 3, 진행 1, 예정 1 (총 {}건)", seeds.size());
    }

    private void createReviews() {
        log.info("리뷰 더미 데이터 생성 중...");
        List<CustomerEntity> customers = customerRepository.findAll().stream()
                .sorted(Comparator.comparing(CustomerEntity::getCustomerId))
                .toList();

        List<OrderItemEntity> reviewable = orderItemRepository.findAll().stream()
                .filter(oi -> oi.getCompletedAt() != null && !Boolean.TRUE.equals(oi.getIsCancelled()))
                .filter(oi -> !hasRefundedReturn(oi.getOrderItemNo()))
                .filter(oi -> reviewRepository.findByOrderItem_OrderItemNo(oi.getOrderItemNo()).isEmpty())
                .sorted(Comparator.comparing(OrderItemEntity::getOrderItemNo))
                .toList();

        if (reviewable.isEmpty() || customers.isEmpty()) {
            log.warn("리뷰 시드 대상 주문 상품이 없습니다.");
            return;
        }

        Map<ProductType, String[]> reviewsByType = Map.of(
                ProductType.SWIMSUIT_WOMEN, new String[]{
                        "수영장에서 두 번 입어봤는데 전체적으로 만족합니다. 몸에 잘 붙지 않고, 사이즈도 평소와 동일하게 맞았어요. 세탁 후에도 형태가 크게 늘어나지 않았습니다.",
                        "사진보다 색감이 조금 더 차분한 느낌이에요. 실물이 더 예쁩니다. 어깨 끈 조절이 잘 되고, 염소에 의한 색빠짐도 아직까지는 없네요.",
                        "몸 라인이 자연스럽게 잡혀서 마음에 들어요. 수영 수업 갈 때마다 입는데, 원단이 부드럽고 건조도 빨라서 관리가 편합니다."},
                ProductType.SWIMSUIT_MEN, new String[]{
                        "허리 밴딩이 실리콘 그립이라 킥 연습할 때 흘러내리지 않아서 좋습니다. 사이즈는 평소 바지 사이즈로 선택하면 딱 맞아요.",
                        "무릎 위 기장이라 수영 시 저항이 적고, 착용감도 편합니다. 봉제선이 평면이라 피부 쓸림도 없었어요."},
                ProductType.SWIMSUIT_KIDS, new String[]{
                        "아이가 수영 학원 다니면서 쓰는데 디자인도 마음에 들고 내구성도 좋아 보여요. 세탁 여러 번 했는데 로고나 프린트가 벗겨지지 않았습니다.",
                        "지퍼 디자인이라 아이 혼자 입고 벗기 편해요. 안감이 부드러워서 피부 트러블도 없었습니다."},
                ProductType.SWIM_CAP, new String[]{
                        "머리카락이 많은 편인데 깔끔하게 정리되고, 물에 젖어도 벗겨지지 않아서 좋아요. 실리콘 소재라 착용감이 편합니다.",
                        "천 수모는 가벼워서 수업용으로 딱이에요. 세탁망에 넣고 돌려도 형태가 유지됩니다."},
                ProductType.SWIM_GOGGLES, new String[]{
                        "처음 수경이라 걱정했는데 안개가 잘 안 끼고 시야가 넓어서 편했어요. 고무 밴드 탄력도 적당하고, 장시간 착용해도 눌림이 심하지 않았습니다.",
                        "미러 렌즈 선택했는데 실외에서 눈이 덜 부시고 좋습니다. 케이스까지 같이 와서 보관하기 좋네요."},
                ProductType.FINS, new String[]{
                        "킥 연습 시 추진력이 확실히 느껴져요. 발에 밀착되지만 조이지 않아서 장시간 착용해도 괜찮았습니다.",
                        "숏핀이라 가방에 넣기 편하고, 발목 부담도 적어서 초보자에게 추천합니다."},
                ProductType.SWIM_TOY, new String[]{
                        "타월이 생각보다 두껍고 흡수력이 좋아요. 수영 후 물기가 금방 빠집니다. 가격 대비 만족도가 높아서 추가 구매 예정입니다."},
                ProductType.ETC, new String[]{
                        "데일리로 쓰기 좋은 소품이에요. 가방에 넣고 다니기에 부피가 작고 가벼워요. 마감도 깔끔해서 오래 쓸 수 있을 것 같습니다.",
                        "전반적으로 가성비 좋은 상품입니다. 상세 설명과 실제 제품이 잘 맞았고, 배송도 빠르게 받았습니다. 재구매 의사 있습니다."}
        );
        String fallbackReview = "전반적으로 만족합니다. 상세 페이지 설명과 실물이 잘 맞았고, 포장과 배송도 깔끔했습니다.";

        Map<ProductType, Integer> typeContentIdx = new java.util.HashMap<>();
        int created = 0;
        for (CustomerEntity customer : customers) {
            List<OrderItemEntity> mine = reviewable.stream()
                    .filter(oi -> oi.getOrder().getCustomer().getCustomerId().equals(customer.getCustomerId()))
                    .limit(3)
                    .toList();
            for (OrderItemEntity item : mine) {
                ProductType pType = item.getProduct().getProductType();
                String[] pool = reviewsByType.getOrDefault(pType, new String[]{fallbackReview});
                int idx = typeContentIdx.getOrDefault(pType, 0);
                String content = pool[idx % pool.length];
                typeContentIdx.put(pType, idx + 1);

                reviewRepository.save(ReviewEntity.builder()
                        .orderItem(item)
                        .customer(customer)
                        .product(item.getProduct())
                        .reviewContent(content)
                        .reviewRating(4 + (created % 2))
                        .reviewCreatedAt(item.getCompletedAt().plusDays(1))
                        .build());
                created++;
            }
        }
        log.info("리뷰 {}건 생성 완료 (텍스트만, 이미지 없음)", created);
    }

    /**
     * 파트너1(스피도) 「실키 핏 프로 실리콘 캡」에 구매확정 주문 3건·리뷰 3건을 추가합니다.
     * 기존 createOrders/createReviews 결과는 유지하며, 마커 본문 리뷰가 3건 미만일 때만 실행됩니다.
     */
    private void ensureReviewAiAnalysisSeeds() {
        ProductEntity product = productRepository.findAll().stream()
                .filter(p -> REVIEW_AI_SEED_PRODUCT_NAME.equals(p.getProductName()))
                .findFirst()
                .orElse(null);
        if (product == null) {
            log.warn("리뷰 AI 분석 시드 대상 상품을 찾을 수 없습니다: {}", REVIEW_AI_SEED_PRODUCT_NAME);
            return;
        }

        long existingSeedReviews = reviewRepository.findByProductNo(product.getProductNo()).stream()
                .filter(this::isReviewAiSeed)
                .count();
        if (existingSeedReviews >= 3) {
            log.info("리뷰 AI 분석 시드가 이미 있습니다. 건너뜁니다. (상품 #{})", product.getProductNo());
            return;
        }

        CustomerEntity customer1 = customerRepository.findAll().stream()
                .filter(c -> DEMO_CUSTOMER1_EMAIL.equals(c.getCustomerEmail()))
                .findFirst()
                .orElse(null);
        CustomerEntity customer2 = customerRepository.findAll().stream()
                .filter(c -> DEMO_CUSTOMER2_EMAIL.equals(c.getCustomerEmail()))
                .findFirst()
                .orElse(null);
        if (customer1 == null || customer2 == null) {
            log.warn("리뷰 AI 분석 시드: 데모 고객 계정이 없어 건너뜁니다.");
            return;
        }

        OptionEntity option = pickOptionForProduct(product, 99);
        LocalDateTime now = LocalDateTime.now();
        CustomerEntity[] buyers = {customer1, customer2, customer1};
        int[] ratings = {5, 3, 4};
        String[] bodies = {
                "머리카락이 많은 편인데도 잘 맞고, 킥 연습할 때 벗겨지지 않아요. 실리콘 소재라 착용감이 부드럽고 세척 후에도 형태가 잘 유지됩니다.",
                "처음엔 조금 꽉 느껴졌지만 몇 번 쓰니 늘어나서 괜찮아졌어요. 색상은 사진과 비슷하고, 가격 대비 품질은 만족합니다.",
                "수영장에서 매일 쓰는데 내구성이 좋아 보여요. 물에 젖어도 미끄러지지 않고, 머리를 단정하게 묶어 주는 느낌이 좋습니다."
        };

        int created = 0;
        for (int i = 0; i < 3; i++) {
            OrderItemEntity item = createReviewAiAnalysisCompletedOrder(
                    buyers[i], product, option, now, i + 1);
            reviewRepository.save(ReviewEntity.builder()
                    .orderItem(item)
                    .customer(buyers[i])
                    .product(product)
                    .reviewContent(bodies[i])
                    .reviewRating(ratings[i])
                    .reviewCreatedAt(item.getCompletedAt().plusDays(1))
                    .build());
            created++;
        }
        log.info("리뷰 AI 분석 시드 추가: 상품 '{}' (#{}) 구매확정 주문·리뷰 {}건",
                product.getProductName(), product.getProductNo(), created);
    }

    /** 리뷰 AI 분석 데모 시드로 만든 리뷰인지 판별 (결제 키 기준) */
    private boolean isReviewAiSeed(ReviewEntity review) {
        if (review.getOrderItem() == null) return false;
        OrderEntity order = review.getOrderItem().getOrder();
        if (order == null || order.getPayment() == null) return false;
        String key = order.getPayment().getPaymentKey();
        return key != null && key.startsWith("SEED-REVIEW-AI-");
    }

    /** 리뷰 AI 분석 데모용 구매확정 주문 1건 (배송 완료 포함) */
    private OrderItemEntity createReviewAiAnalysisCompletedOrder(
            CustomerEntity customer,
            ProductEntity product,
            OptionEntity option,
            LocalDateTime now,
            int slot) {
        Long itemPrice = Long.parseLong(product.getProductPrice());
        Long itemTotalPrice = itemPrice;
        int orderDaysAgo = 40 + slot;
        int confirmedDaysAgo = 39 + slot;
        int completedDaysAgo = 35 + slot;
        LocalDateTime orderAt = now.minusDays(orderDaysAgo);

        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .orderTotalPrice(itemTotalPrice)
                .orderCreatedAt(orderAt)
                .orderStatus(OrderStatus.ACTIVE)
                .customer(customer)
                .recipientName(customer.getCustomerName())
                .recipientPhone("010-1234-5678")
                .deliveryAddress("서울시 강남구 테헤란로 123")
                .deliveryAddressDetail("456호")
                .deliveryZipCode("06234")
                .paymentMethod("CARD")
                .build());
        linkPaidCardPayment(order, orderAt, itemTotalPrice, "SEED-REVIEW-AI-" + slot);

        OrderItemEntity orderItem = orderItemRepository.save(OrderItemEntity.builder()
                .order(order)
                .product(product)
                .option(option)
                .itemQuantity(1)
                .itemPrice(itemPrice)
                .itemTotalPrice(itemTotalPrice)
                .isCancelled(false)
                .confirmedAt(now.minusDays(confirmedDaysAgo))
                .completedAt(now.minusDays(completedDaysAgo))
                .build());

        deliveryRepository.save(DeliveryEntity.builder()
                .orderItem(orderItem)
                .deliveryStatus(DeliveryStatus.DELIVERED)
                .deliveryStartDate(now.minusDays(confirmedDaysAgo))
                .deliveryEndDate(now.minusDays(completedDaysAgo + 1))
                .deliveryTrackingNumber("TRACK-REVIEW-AI-" + slot)
                .deliveryCourier("CJ대한통운")
                .build());
        return orderItem;
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
     * 정산 더미 데이터 생성 (최대 10건)
     * - 구매 확정·비반품·비취소 정산 미연결 라인만 사용 (동일 라인 중복 배치 없음)
     * - 정산 기간 = 포함 주문상품의 주문일(OrderEntity.orderCreatedAt) min·max
     * - 상태는 인덱스 기반(COMPLETED / PENDING / CANCELLED 순환)
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

        List<OrderItemEntity> eligible = orderItemRepository.findAll().stream()
                .filter(item -> item.getCompletedAt() != null && !Boolean.TRUE.equals(item.getIsCancelled()))
                .filter(item -> !hasRefundedReturn(item.getOrderItemNo()))
                .filter(item -> !settlementOrderItemRepository.existsLinkToNonCancelledSettlement(
                        item.getOrderItemNo(), SettlementStatus.CANCELLED))
                .sorted(Comparator.comparing(OrderItemEntity::getOrderItemNo))
                .collect(Collectors.toCollection(ArrayList::new));

        if (eligible.isEmpty()) {
            log.warn("구매 확정된 주문 상품이 없어 정산을 생성할 수 없습니다.");
            return;
        }

        double commissionRate = 0.1;
        int idx = 0;
        int created = 0;

        for (int si = 0; si < 10 && idx < eligible.size(); si++) {
            OrderItemEntity anchor = eligible.get(idx);
            PartnerEntity partner = resolvePartnerForOrderItem(anchor);
            int desiredSize = 1 + (si % 3);

            List<OrderItemEntity> batch = new ArrayList<>();
            batch.add(anchor);
            idx++;

            while (batch.size() < desiredSize && idx < eligible.size()) {
                OrderItemEntity next = eligible.get(idx);
                if (!resolvePartnerForOrderItem(next).getPartnerId().equals(partner.getPartnerId())) {
                    break;
                }
                batch.add(next);
                idx++;
            }

            long totalSalesAmount = batch.stream().mapToLong(OrderItemEntity::getItemTotalPrice).sum();
            long commissionAmount = (long) (totalSalesAmount * commissionRate);
            long settlementAmount = totalSalesAmount - commissionAmount;

            List<LocalDate> orderDates = batch.stream()
                    .map(oi -> oi.getOrder().getOrderCreatedAt().toLocalDate())
                    .sorted()
                    .toList();
            LocalDate periodStart = orderDates.get(0);
            LocalDate periodEnd = orderDates.get(orderDates.size() - 1);

            LocalDate createdAt = periodEnd.plusDays(1 + si);

            SettlementStatus status;
            LocalDate paidDate = null;
            switch (si % 3) {
                case 0 -> {
                    status = SettlementStatus.COMPLETED;
                    paidDate = createdAt.plusDays(1 + (si % 4));
                }
                case 1 -> status = SettlementStatus.PENDING;
                default -> status = SettlementStatus.CANCELLED;
            }

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

            for (OrderItemEntity orderItem : batch) {
                SettlementOrderItemEntity settlementOrderItem = SettlementOrderItemEntity.create(
                        settlement,
                        orderItem
                );
                settlementOrderItemRepository.save(settlementOrderItem);
            }
            created++;
        }

        log.info("정산 더미 데이터 생성 완료: {}건", created);
    }

    private void createFaqs() {
        log.info("FAQ 더미 데이터 생성 중...");
        AdminEntity admin = adminRepository.findAll().stream().findFirst().orElse(null);
        if (admin == null) {
            log.warn("관리자가 없어 FAQ 시드를 건너뜁니다.");
            return;
        }

        saveFaq(admin, InquiryCategory.ORDER_PAYMENT,
                "주문 후 결제 취소는 어떻게 하나요?",
                "결제 완료 전에는 마이페이지에서 주문 취소가 가능합니다. 결제 완료 후에는 배송 준비 전까지 취소 요청이 가능하며, 이후에는 반품 절차를 이용해 주세요.");
        saveFaq(admin, InquiryCategory.DELIVERY,
                "배송은 보통 며칠 걸리나요?",
                "결제 완료 후 영업일 기준 2~5일 내 출고되며, 택배사 사정에 따라 도착일은 달라질 수 있습니다.");
        saveFaq(admin, InquiryCategory.RETURN_EXCHANGE,
                "반품 신청은 어디서 하나요?",
                "마이페이지 > 주문 내역 > 해당 상품에서 반품 신청을 선택해 주세요. 상품별로 판매자가 다를 수 있습니다.");
        saveFaq(admin, InquiryCategory.MEMBER,
                "회원가입 없이 주문할 수 있나요?",
                "현재 서비스는 회원 가입 후 주문·배송 조회·반품 신청이 가능합니다.");
        saveFaq(admin, InquiryCategory.PRODUCT,
                "옵션이 있는 상품은 어떻게 구매하나요?",
                "상품 상세에서 색상·사이즈 등 옵션을 선택한 뒤 장바구니에 담아 주세요.");
        saveFaq(admin, InquiryCategory.POINT,
                "포인트는 언제 적립되나요?",
                "구매 확정(수령 확인) 후 등급별 적립률에 따라 포인트가 적립됩니다. 이벤트 포인트는 이벤트 안내를 참고해 주세요.");
        saveFaq(admin, InquiryCategory.ETC,
                "고객센터 운영 시간은 어떻게 되나요?",
                "1:1 QnA는 접수 후 영업일 기준 순차 답변드립니다. FAQ에서 자주 묻는 내용을 먼저 확인해 주세요.");

        log.info("FAQ 더미 데이터 생성 완료: {}건", faqRepository.count());
    }

    private void saveFaq(AdminEntity admin, InquiryCategory category, String question, String answer) {
        faqRepository.save(FaqEntity.builder()
                .faqQuestion(question)
                .faqAnswer(answer)
                .faqCategory(category.name())
                .admin(admin)
                .build());
    }

    private void createQnaSeeds() {
        log.info("QnA 더미 데이터 생성 중...");
        LocalDateTime now = LocalDateTime.now();

        CustomerEntity customer = customerRepository.findAll().stream()
                .filter(c -> DEMO_CUSTOMER1_EMAIL.equals(c.getCustomerEmail()))
                .findFirst()
                .orElse(customerRepository.findAll().stream().findFirst().orElse(null));
        if (customer == null) {
            log.warn("고객이 없어 QnA 시드를 건너뜁니다.");
            return;
        }

        AdminEntity admin = adminRepository.findAll().stream().findFirst().orElse(null);
        PartnerEntity partner = partnerRepository.findAll().stream()
                .filter(p -> p.getAccount() != null && DEMO_PARTNER1_EMAIL.equals(p.getAccount().getEmail()))
                .findFirst()
                .orElse(partnerRepository.findAll().stream().findFirst().orElse(null));

        OrderItemEntity deliveryItem = orderItemRepository.findAll().stream()
                .filter(oi -> oi.getOrder() != null
                        && oi.getOrder().getCustomer() != null
                        && customer.getCustomerId().equals(oi.getOrder().getCustomer().getCustomerId())
                        && !Boolean.TRUE.equals(oi.getIsCancelled()))
                .findFirst()
                .orElse(orderItemRepository.findAll().stream().findFirst().orElse(null));

        // 1) 플랫폼 — 답변 대기
        QnaEntity platformPending = qnaRepository.save(QnaEntity.builder()
                .customer(customer)
                .category(InquiryCategory.MEMBER)
                .status(QnaStatus.PENDING)
                .title("비밀번호 변경 방법 문의")
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(2))
                .build());
        qnaMessageRepository.save(QnaMessageEntity.builder()
                .qna(platformPending)
                .authorType(QnaAuthorType.CUSTOMER)
                .body("비밀번호를 변경하고 싶은데 어디서 할 수 있나요?")
                .createdAt(now.minusDays(2))
                .build());

        if (deliveryItem != null && partner != null) {
            PartnerEntity routedPartner = resolvePartnerForOrderItem(deliveryItem);

            // 2) 파트너 — 배송 답변 대기
            QnaEntity partnerPending = qnaRepository.save(QnaEntity.builder()
                    .customer(customer)
                    .category(InquiryCategory.DELIVERY)
                    .status(QnaStatus.PENDING)
                    .title("배송 출고 일정 문의")
                    .order(deliveryItem.getOrder())
                    .orderItem(deliveryItem)
                    .partner(routedPartner)
                    .createdAt(now.minusDays(1))
                    .updatedAt(now.minusDays(1))
                    .build());
            qnaMessageRepository.save(QnaMessageEntity.builder()
                    .qna(partnerPending)
                    .authorType(QnaAuthorType.CUSTOMER)
                    .body("주문한 상품이 아직 출고되지 않았습니다. 확인 부탁드립니다.")
                    .createdAt(now.minusDays(1))
                    .build());

            // 3) 파트너 — 답변 완료
            QnaEntity partnerAnswered = qnaRepository.save(QnaEntity.builder()
                    .customer(customer)
                    .category(InquiryCategory.PRODUCT)
                    .status(QnaStatus.ANSWERED)
                    .title("상품 사이즈 문의")
                    .order(deliveryItem.getOrder())
                    .orderItem(deliveryItem)
                    .partner(routedPartner)
                    .createdAt(now.minusDays(3))
                    .updatedAt(now.minusHours(5))
                    .answeredAt(now.minusHours(5))
                    .build());
            qnaMessageRepository.save(QnaMessageEntity.builder()
                    .qna(partnerAnswered)
                    .authorType(QnaAuthorType.CUSTOMER)
                    .body("실측 사이즈표를 보내주실 수 있나요?")
                    .createdAt(now.minusDays(3))
                    .build());
            if (admin != null) {
                qnaMessageRepository.save(QnaMessageEntity.builder()
                        .qna(partnerAnswered)
                        .authorType(QnaAuthorType.PARTNER)
                        .authorPartner(routedPartner)
                        .body("안녕하세요. 상품 상세 페이지 하단에 사이즈 가이드가 있습니다. 추가 문의는 새 QnA로 남겨 주세요.")
                        .createdAt(now.minusHours(5))
                        .build());
            }
        }

        log.info("QnA 더미 데이터 생성 완료: {}건", qnaRepository.count());
    }
}
