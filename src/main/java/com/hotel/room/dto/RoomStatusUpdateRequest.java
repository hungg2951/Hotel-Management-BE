package com.hotel.room.dto;

import com.hotel.common.enums.RoomStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomStatusUpdateRequest(
        @NotNull(message = "Trạng thái phòng không được để trống")
        RoomStatus status,

        @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
        String note
) {
}
