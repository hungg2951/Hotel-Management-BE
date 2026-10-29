package com.hotel.booking.dto;

import com.hotel.booking.entity.ReservationRoom;
import com.hotel.common.enums.ReservationRoomStatus;

public record ReservationRoomResponse(
        Long reservationRoomId,
        Long reservationId,
        Long roomTypeId,
        String roomTypeName,
        Long roomId,
        String roomNumber,
        Long pricePerNight,
        ReservationRoomStatus status
) {
    public static ReservationRoomResponse fromEntity(ReservationRoom room, String roomTypeName, String roomNumber) {
        return new ReservationRoomResponse(
                room.getReservationRoomId(),
                room.getReservation() != null ? room.getReservation().getReservationId() : null,
                room.getRoomTypeId(),
                roomTypeName,
                room.getRoomId(),
                roomNumber,
                room.getPricePerNight(),
                room.getStatus()
        );
    }
}
