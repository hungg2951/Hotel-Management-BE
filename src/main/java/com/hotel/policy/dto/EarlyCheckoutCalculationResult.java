package com.hotel.policy.dto;

public record EarlyCheckoutCalculationResult(
        long originalNights,
        long actualNights,
        long unusedNights,
        long pricePerNight,
        long unusedRoomAmount,
        long earlyCheckoutPenalty,
        long netAdjustment,
        String policyCode,
        String policyName,
        String explanation
) {
}

