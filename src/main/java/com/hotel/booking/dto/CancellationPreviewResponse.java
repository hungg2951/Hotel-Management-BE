package com.hotel.booking.dto;

public record CancellationPreviewResponse(
        Long reservationId,
        String reservationCode,
        Long totalAmount,
        Long totalDeposit,
        Long cancellationFee,
        Long refundAmount,
        Long shortfallAmount,
        boolean isFreeCancel,
        String policyCode,
        String policyName,
        String explanation
) {
}

