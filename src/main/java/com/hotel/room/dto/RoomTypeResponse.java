package com.hotel.room.dto;

import com.hotel.room.entity.RoomType;
import java.math.BigDecimal;

public record RoomTypeResponse(
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
    public static RoomTypeResponse fromEntity(RoomType entity) {
        return new RoomTypeResponse(
                entity.getRoomTypeId(),
                entity.getName(),
                entity.getDescription(),
                entity.getMaxAdults(),
                entity.getMaxChildren(),
                entity.getBedType(),
                entity.getAreaSqm(),
                entity.getBasePrice(),
                entity.getIsActive()
        );
    }
}
