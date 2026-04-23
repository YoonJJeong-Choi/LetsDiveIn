package com.swimshop.swim_mall.return_order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
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
import com.swimshop.swim_mall.common.enums.ReturnRiskTier;
import com.swimshop.swim_mall.common.error.BusinessException;
import com.swimshop.swim_mall.common.error.ErrorCode;
import com.swimshop.swim_mall.customer.entity.CustomerEntity;
import com.swimshop.swim_mall.customer.reopository.CustomerActivityLogRepository;
import com.swimshop.swim_mall.file.entity.UploadedFileEntity;
import com.swimshop.swim_mall.file.repository.UploadedFileRepository;
import com.swimshop.swim_mall.file.service.SignedFileUrlService;
import com.swimshop.swim_mall.inventory.service.InventoryService;
import com.swimshop.swim_mall.order.entity.OrderEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.partner.repository.PartnerRepository;
import com.swimshop.swim_mall.point.service.PointService;
import com.swimshop.swim_mall.return_order.dto.ReturnAiAssistResponseDto;
import com.swimshop.swim_mall.return_order.client.OpenAiReturnAssistVisionClient;
import com.swimshop.swim_mall.return_order.entity.ReturnEntity;
import com.swimshop.swim_mall.return_order.entity.ReturnImageEntity;
import com.swimshop.swim_mall.return_order.repository.ReturnHistoryRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnImageRepository;
import com.swimshop.swim_mall.return_order.repository.ReturnRepository;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class ReturnServiceAiAssistTest {

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
    private ReturnFraudService returnFraudService;
    @Mock
    private ReturnImageAnalysisService returnImageAnalysisService;
    @Mock
    private ChecklistGenerator checklistGenerator;
    @Mock
    private OpenAiReturnAssistVisionClient openAiReturnAssistVisionClient;
    @Mock
    private SignedFileUrlService signedFileUrlService;
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
                returnFraudService,
                returnImageAnalysisService,
                checklistGenerator,
                openAiReturnAssistVisionClient,
                signedFileUrlService
        );
        ReflectionTestUtils.setField(returnService, "appPublicBaseUrl", "https://public.example.com");
    }

    @Test
    void getAdminAiAssist_shouldBlockCustomerAccess() {
        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.CUSTOMER, 101L, "c@swim.com", "customer"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> returnService.getAdminAiAssist(session, 1L));

        assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        verify(returnRepository, never()).findById(anyLong());
    }

    @Test
    void getAdminAiAssist_shouldAllowAdminAndReturnOpenAiPayloadWhenSuccessful() {
        ReflectionTestUtils.setField(returnService, "aiFeatureEnabled", true);

        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.DEFECT, "제품 파손, 사진 첨부");
        ReturnImageEntity returnImage = mockPublicReturnImage(21L, "return-image", "img-21.jpg");
        ReturnFraudService.FraudResult fraud = new ReturnFraudService.FraudResult(
                40, ReturnRiskTier.MEDIUM, List.of("SAME_ADDRESS_REPEAT_HIGH", "RECENT_RETURN_FREQUENCY"));
        ReturnImageAnalysisService.EvidenceResult evidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of("IMAGE_ATTACHED", "HAS_REASON_TEXT"),
                List.of("ADDITIONAL_IMAGE_RECOMMENDED"));

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));
        when(returnRepository.findByCustomerId(20L)).thenReturn(List.of(targetReturn));
        when(returnImageRepository.findByReturnEntityOrderByIdAsc(targetReturn)).thenReturn(List.of(returnImage));
        when(returnFraudService.evaluate(targetReturn, List.of(targetReturn))).thenReturn(fraud);
        when(returnImageAnalysisService.analyze(anyList(), any(ReturnReasonType.class), anyString())).thenReturn(evidence);
        when(checklistGenerator.generate(fraud, evidence)).thenReturn(List.of("중위험 건으로 분류되어 증빙과 사유 일치 여부를 우선 확인하세요."));
        when(openAiReturnAssistVisionClient.requestVisionAssist(anyList(), any(ReturnReasonType.class), anyString()))
                .thenReturn(new OpenAiReturnAssistVisionClient.VisionAssistResult(
                        List.of("VISION_DAMAGE_CONFIRMED"),
                        List.of("MISSING_LABEL_CLOSEUP"),
                        List.of("라벨/시리얼 근접 사진을 추가 요청하고 손상 부위와 사유 일치 여부를 재검토하세요.")));

        ReturnAiAssistResponseDto response = returnService.getAdminAiAssist(session, 1L);

        assertTrue(response.isFeatureEnabled());
        assertEquals(40, response.getFraudScore());
        assertEquals("MEDIUM", response.getRiskLevel());
        assertEquals(List.of("SAME_ADDRESS_REPEAT_HIGH", "RECENT_RETURN_FREQUENCY"), response.getRiskFactors());
        assertEquals(List.of("VISION_DAMAGE_CONFIRMED"), response.getEvidenceTags());
        assertEquals(List.of("MISSING_LABEL_CLOSEUP"), response.getEvidenceGaps());
        assertEquals(1, response.getRecommendedActions().size());
        assertTrue(response.getRecommendedActions().get(0).contains("근접 사진"));
    }

    @Test
    void getAdminAiAssist_shouldFallbackToRuleBasedWhenOpenAiFails() {
        ReflectionTestUtils.setField(returnService, "aiFeatureEnabled", true);

        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.DEFECT, "제품 파손, 사진 첨부");
        ReturnImageEntity returnImage = mockPublicReturnImage(22L, "return-image", "img-22.jpg");
        ReturnFraudService.FraudResult fraud = new ReturnFraudService.FraudResult(
                40, ReturnRiskTier.MEDIUM, List.of("SAME_ADDRESS_REPEAT_HIGH", "RECENT_RETURN_FREQUENCY"));
        ReturnImageAnalysisService.EvidenceResult evidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of("IMAGE_ATTACHED", "HAS_REASON_TEXT"),
                List.of("ADDITIONAL_IMAGE_RECOMMENDED"));
        List<String> fallbackActions = List.of("중위험 건으로 분류되어 증빙과 사유 일치 여부를 우선 확인하세요.");

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));
        when(returnRepository.findByCustomerId(20L)).thenReturn(List.of(targetReturn));
        when(returnImageRepository.findByReturnEntityOrderByIdAsc(targetReturn)).thenReturn(List.of(returnImage));
        when(returnFraudService.evaluate(targetReturn, List.of(targetReturn))).thenReturn(fraud);
        when(returnImageAnalysisService.analyze(anyList(), any(ReturnReasonType.class), anyString())).thenReturn(evidence);
        when(checklistGenerator.generate(fraud, evidence)).thenReturn(fallbackActions);
        when(openAiReturnAssistVisionClient.requestVisionAssist(anyList(), any(ReturnReasonType.class), anyString()))
                .thenThrow(new IllegalStateException("openai unavailable"));

        ReturnAiAssistResponseDto response = returnService.getAdminAiAssist(session, 1L);

        assertTrue(response.isFeatureEnabled());
        assertEquals(List.of("IMAGE_ATTACHED", "HAS_REASON_TEXT"), response.getEvidenceTags());
        assertEquals(List.of("ADDITIONAL_IMAGE_RECOMMENDED"), response.getEvidenceGaps());
        assertEquals(fallbackActions, response.getRecommendedActions());
    }

    @Test
    void getAdminAiAssist_shouldFallbackToRuleBasedWhenOpenAiResultIsEmpty() {
        ReflectionTestUtils.setField(returnService, "aiFeatureEnabled", true);

        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.DEFECT, "제품 파손, 사진 첨부");
        ReturnImageEntity returnImage = mockPublicReturnImage(23L, "return-image", "img-23.jpg");
        ReturnFraudService.FraudResult fraud = new ReturnFraudService.FraudResult(
                40, ReturnRiskTier.MEDIUM, List.of("SAME_ADDRESS_REPEAT_HIGH", "RECENT_RETURN_FREQUENCY"));
        ReturnImageAnalysisService.EvidenceResult evidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of("IMAGE_ATTACHED", "HAS_REASON_TEXT"),
                List.of("ADDITIONAL_IMAGE_RECOMMENDED"));
        List<String> fallbackActions = List.of("중위험 건으로 분류되어 증빙과 사유 일치 여부를 우선 확인하세요.");

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));
        when(returnRepository.findByCustomerId(20L)).thenReturn(List.of(targetReturn));
        when(returnImageRepository.findByReturnEntityOrderByIdAsc(targetReturn)).thenReturn(List.of(returnImage));
        when(returnFraudService.evaluate(targetReturn, List.of(targetReturn))).thenReturn(fraud);
        when(returnImageAnalysisService.analyze(anyList(), any(ReturnReasonType.class), anyString())).thenReturn(evidence);
        when(checklistGenerator.generate(fraud, evidence)).thenReturn(fallbackActions);
        when(openAiReturnAssistVisionClient.requestVisionAssist(anyList(), any(ReturnReasonType.class), anyString()))
                .thenReturn(new OpenAiReturnAssistVisionClient.VisionAssistResult(
                        List.of("VISION_DAMAGE_CONFIRMED"),
                        List.of("MISSING_LABEL_CLOSEUP"),
                        List.of()));

        ReturnAiAssistResponseDto response = returnService.getAdminAiAssist(session, 1L);

        assertEquals(List.of("IMAGE_ATTACHED", "HAS_REASON_TEXT"), response.getEvidenceTags());
        assertEquals(List.of("ADDITIONAL_IMAGE_RECOMMENDED"), response.getEvidenceGaps());
        assertEquals(fallbackActions, response.getRecommendedActions());
    }

    @Test
    void getAdminAiAssist_shouldSkipOpenAiForLowRiskPolicy() {
        ReflectionTestUtils.setField(returnService, "aiFeatureEnabled", true);

        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.CHANGE_OF_MIND, "사이즈 미스");
        ReturnFraudService.FraudResult fraud = new ReturnFraudService.FraudResult(
                15, ReturnRiskTier.LOW, List.of("LOW_SIGNAL"));
        ReturnImageAnalysisService.EvidenceResult evidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of("HAS_REASON_TEXT"),
                List.of("NO_IMAGE_EVIDENCE"));
        List<String> fallbackActions = List.of("주문/배송/반품 이력을 수동으로 재확인하세요.");

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));
        when(returnRepository.findByCustomerId(20L)).thenReturn(List.of(targetReturn));
        when(returnImageRepository.findByReturnEntityOrderByIdAsc(targetReturn)).thenReturn(List.of());
        when(returnFraudService.evaluate(targetReturn, List.of(targetReturn))).thenReturn(fraud);
        when(returnImageAnalysisService.analyze(anyList(), any(ReturnReasonType.class), anyString())).thenReturn(evidence);
        when(checklistGenerator.generate(fraud, evidence)).thenReturn(fallbackActions);

        ReturnAiAssistResponseDto response = returnService.getAdminAiAssist(session, 1L);

        assertEquals(List.of("HAS_REASON_TEXT"), response.getEvidenceTags());
        assertEquals(List.of("NO_IMAGE_EVIDENCE"), response.getEvidenceGaps());
        assertEquals(fallbackActions, response.getRecommendedActions());
        verify(openAiReturnAssistVisionClient, never()).requestVisionAssist(anyList(), any(ReturnReasonType.class), anyString());
    }

    @Test
    void getAdminAiAssist_shouldFallbackWhenSignedUrlGenerationFails() {
        ReflectionTestUtils.setField(returnService, "aiFeatureEnabled", true);

        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.DEFECT, "제품 파손, 사진 첨부");
        ReturnFraudService.FraudResult fraud = new ReturnFraudService.FraudResult(
                40, ReturnRiskTier.MEDIUM, List.of("SAME_ADDRESS_REPEAT_HIGH"));
        ReturnImageAnalysisService.EvidenceResult evidence = new ReturnImageAnalysisService.EvidenceResult(
                List.of("IMAGE_ATTACHED"),
                List.of("ADDITIONAL_IMAGE_RECOMMENDED"));
        List<String> fallbackActions = List.of("주문/배송/반품 이력을 수동으로 재확인하세요.");
        ReturnImageEntity returnImage = mock(ReturnImageEntity.class);
        UploadedFileEntity uploadedFile = mock(UploadedFileEntity.class);

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));
        when(returnRepository.findByCustomerId(20L)).thenReturn(List.of(targetReturn));
        when(returnImageRepository.findByReturnEntityOrderByIdAsc(targetReturn)).thenReturn(List.of(returnImage));
        lenient().when(returnImage.getImageUrl()).thenReturn("http://localhost:8080/api/files/11/download");
        when(returnImage.getFile()).thenReturn(uploadedFile);
        when(uploadedFile.getIsPrivate()).thenReturn(true);
        when(uploadedFile.getFileId()).thenReturn(11L);
        when(returnFraudService.evaluate(targetReturn, List.of(targetReturn))).thenReturn(fraud);
        when(returnImageAnalysisService.analyze(anyList(), any(ReturnReasonType.class), anyString())).thenReturn(evidence);
        when(checklistGenerator.generate(fraud, evidence)).thenReturn(fallbackActions);
        when(signedFileUrlService.createSignedDownloadUrl(11L)).thenThrow(new IllegalStateException("signing failed"));

        ReturnAiAssistResponseDto response = returnService.getAdminAiAssist(session, 1L);

        assertEquals(List.of("IMAGE_ATTACHED"), response.getEvidenceTags());
        assertEquals(List.of("ADDITIONAL_IMAGE_RECOMMENDED"), response.getEvidenceGaps());
        assertEquals(fallbackActions, response.getRecommendedActions());
        verify(openAiReturnAssistVisionClient, never()).requestVisionAssist(anyList(), any(ReturnReasonType.class), anyString());
    }

    @Test
    void getAdminAiAssist_shouldReturnFeatureOffFallbackWhenDisabled() {
        ReflectionTestUtils.setField(returnService, "aiFeatureEnabled", false);
        ReturnEntity targetReturn = mockTargetReturn(1L, 20L, ReturnReasonType.DEFECT, "파손");

        when(authService.getCurrentUser(session))
                .thenReturn(AuthLoginResponseDto.of(AccountRole.ADMIN, 10L, "admin@swim.com", "admin"));
        when(returnRepository.findById(1L)).thenReturn(Optional.of(targetReturn));

        ReturnAiAssistResponseDto response = returnService.getAdminAiAssist(session, 1L);

        assertFalse(response.isFeatureEnabled());
        assertEquals("LOW", response.getRiskLevel());
        assertEquals(List.of("AI_FEATURE_DISABLED"), response.getRiskFactors());
        assertTrue(response.getRecommendedActions().get(0).contains("비활성화"));
        verifyNoInteractions(returnFraudService, returnImageAnalysisService, checklistGenerator, openAiReturnAssistVisionClient);
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

    private ReturnImageEntity mockPublicReturnImage(Long fileId, String category, String storedName) {
        ReturnImageEntity returnImage = mock(ReturnImageEntity.class);
        UploadedFileEntity uploadedFile = mock(UploadedFileEntity.class);
        when(returnImage.getImageUrl()).thenReturn("https://public.example.com/uploads/" + category + "/" + storedName);
        when(returnImage.getFile()).thenReturn(uploadedFile);
        when(uploadedFile.getIsPrivate()).thenReturn(false);
        when(uploadedFile.getCategory()).thenReturn(category);
        when(uploadedFile.getStoredName()).thenReturn(storedName);
        return returnImage;
    }
}

