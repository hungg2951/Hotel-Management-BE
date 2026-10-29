package com.hotel.billing.dto;

import com.hotel.common.enums.FolioItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FolioItemRequest(
        @NotNull(message = "Loại khoản phí không được để trống")
        FolioItemType itemType,

        Long serviceId,

        @Size(max = 300, message = "Mô tả không được vượt quá 300 ký tự")
        String description,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
        Integer quantity,

        @Min(value = 0, message = "Đơn giá không được âm")
        Long unitPrice
) {
}
