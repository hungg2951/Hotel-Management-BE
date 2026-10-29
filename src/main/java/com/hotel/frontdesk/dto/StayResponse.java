package com.hotel.frontdesk.dto;

import com.hotel.common.enums.StayStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StayResponse(
        Long stayId,
        Long reservationRoomId,
        Long reservationId,
        String reservationCode,
        Long guestId,
        String guestName,
        String guestPhone,
        Long roomId,
        String roomNumber,
        Integer floorNo,
        Long roomTypeId,
        String roomTypeName,
        LocalDateTime actualCheckIn,
        LocalDateTime actualCheckOut,
        LocalDate expectedCheckIn,
        LocalDate expectedCheckOut,
        StayStatus status,
        Long folioId,
        Long folioTotalAmount,
        Long folioPaidAmount,
        Long folioBalance,
        boolean isFolioSettled,
        Long checkedInBy,
        Long checkedOutBy
) {
}
