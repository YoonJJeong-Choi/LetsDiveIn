package com.swimshop.swim_mall.return_order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.swimshop.swim_mall.account.dto.AuthLoginResponseDto;
import com.swimshop.swim_mall.account.service.AuthService;
import com.swimshop.swim_mall.admin.repository.AdminRepository;
import com.swimshop.swim_mall.common.enums.AccountRole;
import com.swimshop.swim_mall.common.enums.ReturnReasonType;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.point.service.PointService;
import com.swimshop.swim_mall.product.service.ProductCustomerImageUrlResolver;
import com.swimshop.swim_mall.return_order.dto.ReturnAssistResponseDto;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;
import com.swimshop.swim_mall.return_order.repository.ReturnHistoryRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnImageRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnRepository;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class ReturnServiceReturnAssistTest {

    @Mock
    private ReturnRepository returnRepository;
    @Mock
    private ReturnHistoryRepository returnHistoryRepository;
    @Mock
    private com.swimshop.swim_mall.order.repository.OrderItemRepository orderItemRepository;
    @Mock
    private AuthService authService;
    @Mock
    private PartnerRepository partnerRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private PointService pointService;
    @Mock
    private CustomerActivityLogRepository customerActivityLogRepository;
    @Mock
    private UploadedFileRepository uploadedFileRepository;
    @Mock
    private ReturnImageRepository returnImageRepository;
    @Mock
    private ReturnImageAnalysisService returnImageAnalysisService;
    @Mock
    private ReturnCustomerHistoryInsightService returnCustomerHistoryInsightService;
    @Mock
    private ProductCustomerImageUrlResolver productCustomerImageUrlResolver;
    @Mock
    private HttpSession session;

    private ReturnService returnService;

    @BeforeEach
    void setUp() {
        returnService = new ReturnService(
                returnRepository,
                returnHistoryRepository,
                orderItemRepository,
                authService,
                partnerRepository,
                adminRepository,
                inventoryService,
                pointService,
                customerActivityLogRepository,
                uploadedFileRepository,
                returnImageRepository,
                returnImageAnalysisService,
                returnCustomerHistoryInsightService,
                productCustomerImageUrlResolver
        );
    }

    @Test
    void getAdminReturnAssist_shouldBlockCustomerAccess() {
        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.CUSTOMER, 101L, "c@swim.com", "customer"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> returnService.getAdminReturnAssist(session, 1L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(returnRepository, never()).findById(anyLong());
    }

    @Test
    void getAdminReturnAssist_shouldBuildRuleBasedAssistForDefect() {
        ReflectionTestUtils.setField(returnService, "returnAssistEnabled", true);

        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.DEFECT, "제품 파손, 사진 첨부");
        ReturnImageEntity returnImage = mockReturnImage("https://cdn.example.com/return/1.jpg");
        ReturnCustomerHistoryInsightService.CustomerHistoryInsight insight =
                new ReturnCustomerHistoryInsightService.CustomerHistoryInsight(
                        List.of("RECENT_RETURN_FREQUENCY"));
        ReturnImageAnalysisService.EvidenceResult evidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of("IMAGE_ATTACHED", "HAS_REASON_TEXT"),
                List.of("ADDITIONAL_IMAGE_RECOMMENDED"));

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));
        when(returnRepository.findByCustomerId(20L)).thenReturn(List.of(targetReturn));
        when(returnImageRepository.findByReturnEntityOrderByIdAsc(targetReturn)).thenReturn(List.of(returnImage));
        when(returnCustomerHistoryInsightService.summarize(targetReturn, List.of(targetReturn))).thenReturn(insight);
        when(returnImageAnalysisService.analyze(anyList(), any(ReturnReasonType.class), anyString())).thenReturn(evidence);

        ReturnAssistResponseDto response = returnService.getAdminReturnAssist(session, 1L);

        assertEquals("PARTIAL", response.getEvidenceStatus().getCode());
        assertEquals("MEDIUM", response.getReviewPriority().getCode());
        assertTrue(response.getSummary().contains("상품 불량·하자"));
        assertTrue(response.getSignals().getCustomerSignals().contains("최근 90일 반품 이력이 있습니다."));
        assertTrue(response.getEvidenceStatus().getDetail().contains("1장만 등록"));
        assertTrue(response.getCheckPoints().stream().anyMatch(item -> item.contains("추가 촬영")));
    }

    @Test
    void getAdminReturnAssist_shouldHandleChangeOfMindWithoutHistoryReferences() {
        ReflectionTestUtils.setField(returnService, "returnAssistEnabled", true);

        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.CHANGE_OF_MIND, "사이즈 미스");
        ReturnCustomerHistoryInsightService.CustomerHistoryInsight insight =
                new ReturnCustomerHistoryInsightService.CustomerHistoryInsight(List.of("NO_SPECIAL_HISTORY"));
        ReturnImageAnalysisService.EvidenceResult evidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of("HAS_REASON_TEXT"),
                List.of());

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));
        when(returnRepository.findByCustomerId(20L)).thenReturn(List.of(targetReturn));
        when(returnImageRepository.findByReturnEntityOrderByIdAsc(targetReturn)).thenReturn(List.of());
        when(returnCustomerHistoryInsightService.summarize(targetReturn, List.of(targetReturn))).thenReturn(insight);
        when(returnImageAnalysisService.analyze(anyList(), any(ReturnReasonType.class), anyString())).thenReturn(evidence);

        ReturnAssistResponseDto response = returnService.getAdminReturnAssist(session, 1L);

        assertEquals("NOT_REQUIRED", response.getEvidenceStatus().getCode());
        assertEquals("LOW", response.getReviewPriority().getCode());
        assertTrue(response.getSummary().contains("단순 변심"));
        assertTrue(response.getCheckPoints().stream().anyMatch(item -> item.contains("반품 가능 기간")));
        assertTrue(response.getSignals().getCustomerSignals().isEmpty());
    }

    @Test
    void getAdminReturnAssist_shouldReturnDisabledFallbackWhenFeatureOff() {
        ReflectionTestUtils.setField(returnService, "returnAssistEnabled", false);
        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.DEFECT, "파손");

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));

        ReturnAssistResponseDto response = returnService.getAdminReturnAssist(session, 1L);

        assertEquals("MANUAL", response.getReviewPriority().getCode());
        assertEquals("NOT_ANALYZED", response.getEvidenceStatus().getCode());
        assertTrue(response.getSummary().contains("비활성화"));
        verifyNoInteractions(returnCustomerHistoryInsightService, returnImageAnalysisService);
    }

    private ReturnEntity mockTargetReturn(Long returnNo, Long customerId, ReturnReasonType reasonType, String reason) {
        ReturnEntity targetReturn = mock(ReturnEntity.class);
        OrderItemEntity orderItem = mock(OrderItemEntity.class);
        OrderEntity order = mock(OrderEntity.class);
        CustomerEntity customer = mock(CustomerEntity.class);

        lenient().when(targetReturn.getReturnNo()).thenReturn(returnNo);
        lenient().when(targetReturn.getOrderItem()).thenReturn(orderItem);
        lenient().when(targetReturn.getReturnReasonType()).thenReturn(reasonType);
        lenient().when(targetReturn.getReturnReason()).thenReturn(reason);

        lenient().when(orderItem.getOrder()).thenReturn(order);
        lenient().when(order.getCustomer()).thenReturn(customer);
        lenient().when(customer.getCustomerId()).thenReturn(customerId);
        return targetReturn;
    }

    private ReturnImageEntity mockReturnImage(String imageUrl) {
        ReturnImageEntity returnImage = mock(ReturnImageEntity.class);
        when(returnImage.getImageUrl()).thenReturn(imageUrl);
        return returnImage;
    }
}
