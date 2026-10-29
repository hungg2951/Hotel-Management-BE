package com.hotel.billing.dto;

import com.hotel.billing.entity.Payment;
import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponse(
        Long paymentId,
        Long folioId,
        Long reservationId,
        PaymentMethod method,
        Long amount,
        PaymentStatus status,
        String referenceCode,
        LocalDateTime paidAt,
        Long receivedBy
) {
    public static PaymentResponse fromEntity(Payment entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentResponse(
                entity.getPaymentId(),
                entity.getFolio() != null ? entity.getFolio().getFolioId() : null,
                entity.getReservationId(),
                entity.getMethod(),
                entity.getAmount(),
                entity.getStatus(),
                entity.getReferenceCode(),
                entity.getPaidAt(),
                entity.getReceivedBy()
        );
    }
}
