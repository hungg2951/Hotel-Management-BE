package com.hotel.policy.dto;

import com.hotel.common.enums.EarlyCheckoutPenaltyType;
import com.hotel.policy.entity.EarlyCheckoutPolicy;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EarlyCheckoutPolicyResponse(
        Long policyId,
        String policyCode,
        String name,
        String description,
        EarlyCheckoutPenaltyType penaltyType,
        BigDecimal penaltyValue,
        Boolean isDefault,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static EarlyCheckoutPolicyResponse from(EarlyCheckoutPolicy p) {
        return new EarlyCheckoutPolicyResponse(
                p.getPolicyId(),
                p.getPolicyCode(),
                p.getName(),
                p.getDescription(),
                p.getPenaltyType(),
                p.getPenaltyValue(),
                p.getIsDefault(),
                p.getIsActive(),
                p.getCreatedAt()
        );
    }
}

