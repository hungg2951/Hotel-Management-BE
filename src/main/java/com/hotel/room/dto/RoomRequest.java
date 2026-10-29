package com.hotel.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomRequest(
        @NotNull(message = "Mã hạng phòng không được để trống")
        Long roomTypeId,

        @NotBlank(message = "Số phòng không được để trống")
        @Size(max = 10, message = "Số phòng không được vượt quá 10 ký tự")
        String roomNumber,

        Integer floorNo,

        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
        String note,

        Boolean isActive
) {
}
