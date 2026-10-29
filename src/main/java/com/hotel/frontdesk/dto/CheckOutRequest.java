package com.hotel.frontdesk.dto;

import com.hotel.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckOutRequest(
        @NotNull(message = "Mã lượt lưu trú (stayId) là bắt buộc")
        Long stayId,

        Boolean isEarlyCheckOut,

        Boolean autoSettle,

        PaymentMethod settleMethod,

        String settleRef,

        String note,

        Long customPenalty,

        @Size(max = 500, message = "Lý do can thiệp ngoại lệ tối đa 500 ký tự")
        String overrideReason
) {
    public CheckOutRequest(
            Long stayId,
            Boolean isEarlyCheckOut,
            Boolean autoSettle,
            PaymentMethod settleMethod,
            String settleRef,
            String note
    ) {
        this(stayId, isEarlyCheckOut, autoSettle, settleMethod, settleRef, note, null, null);
    }
}
