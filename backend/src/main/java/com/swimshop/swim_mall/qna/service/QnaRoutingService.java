package com.swimshop.swim_mall.qna.service;

import org.springframework.stereotype.Service;

import com.swimshop.swim_mall.common.enums.InquiryCategory;
import com.swimshop.swim_mall.common.enums.QnaScope;
import com.swimshop.swim_mall.order.entity.OrderItemEntity;
import com.swimshop.swim_mall.partner.entity.PartnerEntity;

@Service
public class QnaRoutingService {

    public Long resolvePartnerId(OrderItemEntity orderItem) {
        if (orderItem == null) {
            return null;
        }
        if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null) {
            return orderItem.getOption().getPartner().getPartnerId();
        }
        if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
            return orderItem.getProduct().getPartner().getPartnerId();
        }
        return null;
    }

    public PartnerEntity resolvePartner(OrderItemEntity orderItem) {
        Long partnerId = resolvePartnerId(orderItem);
        if (partnerId == null) {
            return null;
        }
        if (orderItem.getOption() != null && orderItem.getOption().getPartner() != null
                && orderItem.getOption().getPartner().getPartnerId().equals(partnerId)) {
            return orderItem.getOption().getPartner();
        }
        if (orderItem.getProduct() != null && orderItem.getProduct().getPartner() != null) {
            return orderItem.getProduct().getPartner();
        }
        return null;
    }

    public Long resolvePartnerNoForCreate(
            InquiryCategory category,
            QnaScope scope,
            OrderItemEntity orderItem) {

        if (category.isAdminOnlyCategory()) {
            return null;
        }

        if (category == InquiryCategory.PRODUCT || category == InquiryCategory.DELIVERY) {
            return resolvePartnerId(orderItem);
        }

        if (category == InquiryCategory.RETURN_EXCHANGE) {
            if (scope == QnaScope.POLICY) {
                return null;
            }
            if (scope == QnaScope.ORDER_ITEM) {
                return resolvePartnerId(orderItem);
            }
        }

        return null;
    }
}
