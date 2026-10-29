package com.hotel.booking.api;

import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.ReservationStatus;

import java.time.LocalDate;

public record ReservationInfoDto(
        Long reservationId,
        String reservationCode,
        Long guestId,
        BookingSource bookingSource,
        ReservationStatus status,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer adults,
        Integer children,
        Long totalAmount,
        Long depositAmount,
        String cancellationPolicySnapshot,
        String earlyCheckoutPolicySnapshot,
        Long cancellationPolicyId,
        Long earlyCheckoutPolicyId
) {
    public ReservationInfoDto(
            Long reservationId,
            String reservationCode,
            Long guestId,
            BookingSource bookingSource,
            ReservationStatus status,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            Long totalAmount,
            Long depositAmount
    ) {
        this(
                reservationId,
                reservationCode,
                guestId,
                bookingSource,
                status,
                checkInDate,
                checkOutDate,
                adults,
                children,
                totalAmount,
                depositAmount,
                null,
                null,
                null,
                null
        );
    }

    public ReservationInfoDto(
            Long reservationId,
            String reservationCode,
            Long guestId,
            BookingSource bookingSource,
            ReservationStatus status,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            Long totalAmount,
            Long depositAmount,
            String cancellationPolicySnapshot,
            String earlyCheckoutPolicySnapshot
    ) {
        this(
                reservationId,
                reservationCode,
                guestId,
                bookingSource,
                status,
                checkInDate,
                checkOutDate,
                adults,
                children,
                totalAmount,
                depositAmount,
                cancellationPolicySnapshot,
                earlyCheckoutPolicySnapshot,
                null,
                null
        );
    }
}
