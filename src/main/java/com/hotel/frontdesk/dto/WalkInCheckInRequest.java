package com.hotel.frontdesk.dto;

import com.hotel.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record WalkInCheckInRequest(
        @NotNull(message = "Mã khách hàng là bắt buộc")
        Long guestId,

        @NotNull(message = "Hạng phòng là bắt buộc")
        Long roomTypeId,

        Long roomId,

        @NotNull(message = "Ngày trả phòng là bắt buộc")
        LocalDate checkOutDate,

        Integer adults,

        Integer children,

        Long depositAmount,

        PaymentMethod depositMethod,

        String depositRef,

        String note
) {
}
