package com.hotel.policy.dto;

import com.hotel.common.enums.CancellationPenaltyType;
import com.hotel.policy.entity.CancellationPolicy;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancellationPolicyResponse(
        Long policyId,
        String policyCode,
        String name,
        String description,
        Integer freeCancelHours,
        CancellationPenaltyType penaltyType,
        BigDecimal penaltyValue,
        Boolean isDefault,
        Boolean isActive,
        LocalDateTime createdAt
) {
    public static CancellationPolicyResponse from(CancellationPolicy p) {
        return new CancellationPolicyResponse(
                p.getPolicyId(),
                p.getPolicyCode(),
                p.getName(),
                p.getDescription(),
                p.getFreeCancelHours(),
                p.getPenaltyType(),
                p.getPenaltyValue(),
                p.getIsDefault(),
                p.getIsActive(),
                p.getCreatedAt()
        );
    }
}

