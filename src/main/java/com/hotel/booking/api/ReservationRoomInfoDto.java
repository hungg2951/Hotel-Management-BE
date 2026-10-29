package com.hotel.booking.api;

import com.hotel.common.enums.ReservationRoomStatus;

public record ReservationRoomInfoDto(
        Long reservationRoomId,
        Long reservationId,
        Long roomTypeId,
        Long roomId,
        Long pricePerNight,
        ReservationRoomStatus status
) {
}
