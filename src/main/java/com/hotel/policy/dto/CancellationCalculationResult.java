package com.hotel.policy.dto;

public record CancellationCalculationResult(
        boolean isFreeCancel,
        long cancellationFee,
        long totalDeposit,
        long refundAmount,
        long shortfallAmount,
        String policyCode,
        String policyName,
        String explanation
) {
}

