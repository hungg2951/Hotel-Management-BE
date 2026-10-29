package com.hotel.billing.dto;

import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.PaymentStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod method,

        @NotNull(message = "Số tiền thanh toán không được để trống")
        @Min(value = 1, message = "Số tiền thanh toán phải lớn hơn 0")
        Long amount,

        PaymentStatus status,

        @Size(max = 100, message = "Mã tham chiếu không được vượt quá 100 ký tự")
        String referenceCode,

        Long reservationId
) {
}
