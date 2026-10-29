package com.hotel.frontdesk.dto;

import jakarta.validation.constraints.NotNull;

public record RoomChangeRequest(
        @NotNull(message = "Mã lượt lưu trú (stayId) là bắt buộc")
        Long stayId,

        @NotNull(message = "Mã phòng mới (newRoomId) là bắt buộc")
        Long newRoomId,

        String reason
) {
}
