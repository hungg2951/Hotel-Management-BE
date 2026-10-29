package com.hotel.booking.dto;

import jakarta.validation.constraints.Size;

public record ReservationCancelRequest(
        @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự")
        String reason,

        Long customFee,

        @Size(max = 500, message = "Lý do can thiệp ngoại lệ tối đa 500 ký tự")
        String overrideReason
) {
    public ReservationCancelRequest(String reason) {
        this(reason, null, null);
    }
}
