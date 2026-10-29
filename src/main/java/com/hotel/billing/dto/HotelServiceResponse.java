package com.hotel.billing.dto;

import com.hotel.billing.entity.HotelService;
import com.hotel.common.enums.ServiceCategory;

public record HotelServiceResponse(
        Long serviceId,
        String name,
        ServiceCategory category,
        String unit,
        Long unitPrice,
        Boolean isActive
) {
    public static HotelServiceResponse fromEntity(HotelService entity) {
        if (entity == null) {
            return null;
        }
        return new HotelServiceResponse(
                entity.getServiceId(),
                entity.getName(),
                entity.getCategory(),
                entity.getUnit(),
                entity.getUnitPrice(),
                entity.getIsActive()
        );
    }
}
