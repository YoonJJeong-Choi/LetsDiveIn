package com.swimshop.swim_mall.qna.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.enums.QnaScope;
import com.swimshop.swim_mall.option.entity.OptionEntity;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;
import com.swimshop.swim_mall.product.entity.ProductEntity;

@ExtendWith(MockitoExtension.class)
class QnaRoutingServiceTest {

    @InjectMocks
    private QnaRoutingService qnaRoutingService;

    @Test
    void resolvePartnerId_prefersOptionPartner() {
        PartnerEntity optionPartner = partner(10L);
        OrderItemEntity item = orderItem(optionPartner, null);
        assertThat(qnaRoutingService.resolvePartnerId(item)).isEqualTo(10L);
    }

    @Test
    void resolvePartnerId_fallsBackToProductPartner() {
        PartnerEntity productPartner = partner(20L);
        OrderItemEntity item = orderItem(null, productPartner);
        assertThat(qnaRoutingService.resolvePartnerId(item)).isEqualTo(20L);
    }

    @Test
    void resolvePartnerNoForCreate_adminOnlyCategories() {
        assertThat(qnaRoutingService.resolvePartnerNoForCreate(InquiryCategory.MEMBER, null, null)).isNull();
        assertThat(qnaRoutingService.resolvePartnerNoForCreate(
                InquiryCategory.RETURN_EXCHANGE, QnaScope.POLICY, null)).isNull();
    }

    @Test
    void resolvePartnerNoForCreate_deliveryUsesOrderItem() {
        PartnerEntity partner = partner(5L);
        OrderItemEntity item = orderItem(partner, null);
        assertThat(qnaRoutingService.resolvePartnerNoForCreate(InquiryCategory.DELIVERY, null, item))
                .isEqualTo(5L);
    }

    private static PartnerEntity partner(Long id) {
        PartnerEntity p = mock(PartnerEntity.class);
        when(p.getPartnerId()).thenReturn(id);
        return p;
    }

    private static OrderItemEntity orderItem(PartnerEntity optionPartner, PartnerEntity productPartner) {
        OrderItemEntity item = mock(OrderItemEntity.class);
        if (optionPartner != null) {
            OptionEntity option = mock(OptionEntity.class);
            when(option.getPartner()).thenReturn(optionPartner);
            when(item.getOption()).thenReturn(option);
        }
        if (productPartner != null) {
            ProductEntity product = mock(ProductEntity.class);
            when(product.getPartner()).thenReturn(productPartner);
            when(item.getProduct()).thenReturn(product);
        }
        return item;
    }
}
