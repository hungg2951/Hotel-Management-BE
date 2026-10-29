package com.hotel.policy.dto;

import com.hotel.common.enums.EarlyCheckoutPenaltyType;
import java.math.BigDecimal;

public record EarlyCheckoutPolicySnapshot(
        Long policyId,
        String policyCode,
        String name,
        EarlyCheckoutPenaltyType penaltyType,
        BigDecimal penaltyValue
) {
}

