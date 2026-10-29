package com.hotel.booking.dto;

import com.hotel.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReservationDepositRequest(
        @NotNull(message = "Số tiền đặt cọc không được để trống")
        @Positive(message = "Số tiền đặt cọc phải lớn hơn 0")
        Long amount,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod method,

        @Size(max = 100, message = "Mã tham chiếu tối đa 100 ký tự")
        String referenceCode
) {
}
