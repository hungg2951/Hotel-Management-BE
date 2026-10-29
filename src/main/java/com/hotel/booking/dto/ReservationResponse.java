package com.hotel.booking.dto;

import com.hotel.booking.entity.Reservation;
import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.ReservationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long reservationId,
        String reservationCode,
        Long guestId,
        String guestName,
        String guestPhone,
        String guestIdNumber,
        BookingSource bookingSource,
        ReservationStatus status,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer nights,
        Integer adults,
        Integer children,
        Integer roomCount,
        Long totalAmount,
        Long depositAmount,
        String note,
        Long createdBy,
        LocalDateTime createdAt
) {
    public ReservationResponse(
            Long reservationId,
            String reservationCode,
            Long guestId,
            String guestName,
            String guestPhone,
            BookingSource bookingSource,
            ReservationStatus status,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer nights,
            Integer adults,
            Integer children,
            Integer roomCount,
            Long totalAmount,
            Long depositAmount,
            String note,
            Long createdBy,
            LocalDateTime createdAt
    ) {
        this(reservationId, reservationCode, guestId, guestName, guestPhone, null, bookingSource, status,
                checkInDate, checkOutDate, nights, adults, children, roomCount, totalAmount, depositAmount, note, createdBy, createdAt);
    }

    public static ReservationResponse fromEntity(
            Reservation res,
            String guestName,
            String guestPhone,
            String guestIdNumber,
            int nights,
            int roomCount
    ) {
        return new ReservationResponse(
                res.getReservationId(),
                res.getReservationCode(),
                res.getGuestId(),
                guestName,
                guestPhone,
                guestIdNumber,
                res.getBookingSource(),
                res.getStatus(),
                res.getCheckInDate(),
                res.getCheckOutDate(),
                nights,
                res.getAdults(),
                res.getChildren(),
                roomCount,
                res.getTotalAmount(),
                res.getDepositAmount(),
                res.getNote(),
                res.getCreatedBy(),
                res.getCreatedAt()
        );
    }

    public static ReservationResponse fromEntity(
            Reservation res,
            String guestName,
            String guestPhone,
            int nights,
            int roomCount
    ) {
        return fromEntity(res, guestName, guestPhone, null, nights, roomCount);
    }
}
