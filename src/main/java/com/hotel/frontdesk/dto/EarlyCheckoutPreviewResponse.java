package com.hotel.frontdesk.dto;

public record EarlyCheckoutPreviewResponse(
        Long stayId,
        Long originalNights,
        Long actualNights,
        long unusedNights,
        long pricePerNight,
        long unusedRoomAmount,
        long earlyCheckoutPenalty,
        long netAdjustment,
        String policyCode,
        String policyName,
        String explanation,
        long currentFolioBalance,
        long estimatedFinalBalance
) {
}

