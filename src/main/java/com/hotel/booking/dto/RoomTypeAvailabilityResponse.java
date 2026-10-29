package com.hotel.booking.dto;

import java.math.BigDecimal;

public record RoomTypeAvailabilityResponse(
        Long roomTypeId,
        String name,
        String description,
        Integer maxAdults,
        Integer maxChildren,
        String bedType,
        BigDecimal areaSqm,
        Long basePrice,
        Integer nights,
        Long totalPrice,
        Integer totalRooms,
        Integer availableRoomsCount,
        boolean isAvailable
) {
}
