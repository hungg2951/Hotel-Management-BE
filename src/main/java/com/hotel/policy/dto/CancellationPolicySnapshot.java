package com.hotel.policy.dto;

import com.hotel.common.enums.CancellationPenaltyType;
import java.math.BigDecimal;

public record CancellationPolicySnapshot(
        Long policyId,
        String policyCode,
        String name,
        Integer freeCancelHours,
        CancellationPenaltyType penaltyType,
        BigDecimal penaltyValue
) {
}

