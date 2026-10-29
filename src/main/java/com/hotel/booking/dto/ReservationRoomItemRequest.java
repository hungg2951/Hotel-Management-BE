package com.hotel.booking.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationRoomItemRequest(
        @NotNull(message = "Mã hạng phòng không được để trống")
        Long roomTypeId,

        Long specificRoomId
) {
}
