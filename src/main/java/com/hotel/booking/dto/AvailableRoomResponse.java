package com.hotel.booking.dto;

public record AvailableRoomResponse(
        Long roomId,
        String roomNumber,
        Integer floorNo,
        Long roomTypeId,
        boolean isAvailable
) {
}
