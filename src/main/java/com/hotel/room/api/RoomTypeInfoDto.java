package com.hotel.room.api;

import java.math.BigDecimal;

public record RoomTypeInfoDto(
        Long roomTypeId,
        String name,
        String description,
        Integer maxAdults,
        Integer maxChildren,
        String bedType,
        BigDecimal areaSqm,
        Long basePrice,
        Boolean isActive
) {
}
