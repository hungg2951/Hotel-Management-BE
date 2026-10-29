package com.hotel.frontdesk.dto;

import jakarta.validation.constraints.NotNull;

public record CheckInRequest(
        @NotNull(message = "Mã phòng đặt (reservationRoomId) là bắt buộc")
        Long reservationRoomId,

        Long specificRoomId,

        String note
) {
}
