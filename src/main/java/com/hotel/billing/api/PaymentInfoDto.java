package com.hotel.billing.api;

import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentInfoDto(
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
}
