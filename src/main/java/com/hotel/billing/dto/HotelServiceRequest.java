package com.hotel.billing.dto;

import com.hotel.common.enums.ServiceCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HotelServiceRequest(
        @NotBlank(message = "Tên dịch vụ không được để trống")
        @Size(max = 150, message = "Tên dịch vụ không được vượt quá 150 ký tự")
        String name,

        @NotNull(message = "Danh mục dịch vụ không được để trống")
        ServiceCategory category,

        @NotBlank(message = "Đơn vị tính không được để trống")
        @Size(max = 20, message = "Đơn vị tính không được vượt quá 20 ký tự")
        String unit,

        @NotNull(message = "Đơn giá không được để trống")
        @Min(value = 0, message = "Đơn giá không được âm")
        Long unitPrice,

        Boolean isActive
) {
}
