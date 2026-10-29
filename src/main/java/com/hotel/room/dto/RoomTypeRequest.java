package com.hotel.room.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomTypeRequest(
        @NotBlank(message = "Tên hạng phòng không được để trống")
        @Size(max = 100, message = "Tên hạng phòng không được vượt quá 100 ký tự")
        String name,

        @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
        String description,

        @NotNull(message = "Số người lớn tối đa không được để trống")
        @Min(value = 1, message = "Số người lớn tối đa phải từ 1 trở lên")
        Integer maxAdults,

        @NotNull(message = "Số trẻ em tối đa không được để trống")
        @Min(value = 0, message = "Số trẻ em tối đa không được nhỏ hơn 0")
        Integer maxChildren,

        @Size(max = 255, message = "Loại giường không được vượt quá 255 ký tự")
        String bedType,

        @DecimalMin(value = "0.0", message = "Diện tích phòng không được nhỏ hơn 0")
        BigDecimal areaSqm,

        @NotNull(message = "Giá cơ bản không được để trống")
        @Min(value = 0, message = "Giá cơ bản không được nhỏ hơn 0")
        Long basePrice,

        Boolean isActive
) {
}
