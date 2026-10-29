package com.hotel.billing.dto;

import com.hotel.billing.entity.FolioItem;
import com.hotel.common.enums.FolioItemType;

import java.time.LocalDateTime;

public record FolioItemResponse(
        Long itemId,
        Long folioId,
        FolioItemType itemType,
        Long serviceId,
        String serviceName,
        String description,
        Integer quantity,
        Long unitPrice,
        Long amount,
        LocalDateTime postedAt,
        Long postedBy
) {
    public static FolioItemResponse fromEntity(FolioItem entity) {
        if (entity == null) {
            return null;
        }
        return new FolioItemResponse(
                entity.getItemId(),
                entity.getFolio() != null ? entity.getFolio().getFolioId() : null,
                entity.getItemType(),
                entity.getService() != null ? entity.getService().getServiceId() : null,
                entity.getService() != null ? entity.getService().getName() : null,
                entity.getDescription(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getAmount(),
                entity.getPostedAt(),
                entity.getPostedBy()
        );
    }
}
