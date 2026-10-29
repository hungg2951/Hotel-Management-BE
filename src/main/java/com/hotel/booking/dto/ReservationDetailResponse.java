package com.hotel.booking.dto;

import com.hotel.billing.api.PaymentInfoDto;
import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.ReservationStatus;
import com.hotel.guest.api.GuestInfoDto;
import com.hotel.policy.dto.CancellationPolicySnapshot;
import com.hotel.policy.dto.EarlyCheckoutPolicySnapshot;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ReservationDetailResponse(
        Long reservationId,
        String reservationCode,
        GuestInfoDto guest,
        BookingSource bookingSource,
        ReservationStatus status,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer nights,
        Integer adults,
        Integer children,
        Long totalAmount,
        Long depositAmount,
        Long requiredDepositAmount,
        boolean isDepositMet,
        String note,
        Long createdBy,
        LocalDateTime createdAt,
        List<ReservationRoomResponse> rooms,
        List<PaymentInfoDto> payments,
        CancellationPolicySnapshot cancellationPolicy,
        EarlyCheckoutPolicySnapshot earlyCheckoutPolicy,
        Long cancellationFee,
        Long refundAmount,
        String overrideReason
) {
    public ReservationDetailResponse(
            Long reservationId,
            String reservationCode,
            GuestInfoDto guest,
            BookingSource bookingSource,
            ReservationStatus status,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer nights,
            Integer adults,
            Integer children,
            Long totalAmount,
            Long depositAmount,
            Long requiredDepositAmount,
            boolean isDepositMet,
            String note,
            Long createdBy,
            LocalDateTime createdAt,
            List<ReservationRoomResponse> rooms,
            List<PaymentInfoDto> payments
    ) {
        this(
                reservationId,
                reservationCode,
                guest,
                bookingSource,
                status,
                checkInDate,
                checkOutDate,
                nights,
                adults,
                children,
                totalAmount,
                depositAmount,
                requiredDepositAmount,
                isDepositMet,
                note,
                createdBy,
                createdAt,
                rooms,
                payments,
                null,
                null,
                null,
                null,
                null
        );
    }
}
