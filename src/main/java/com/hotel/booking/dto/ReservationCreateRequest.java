package com.hotel.booking.dto;

import com.hotel.common.enums.BookingSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ReservationCreateRequest(
        @NotNull(message = "Mã khách hàng không được để trống")
        Long guestId,

        BookingSource bookingSource,

        @NotNull(message = "Ngày nhận phòng không được để trống")
        LocalDate checkInDate,

        @NotNull(message = "Ngày trả phòng không được để trống")
        LocalDate checkOutDate,

        @Min(value = 1, message = "Số người lớn tối thiểu là 1")
        Integer adults,

        @Min(value = 0, message = "Số trẻ em không được âm")
        Integer children,

        @NotEmpty(message = "Danh sách phòng đặt không được để trống")
        @Valid
        List<ReservationRoomItemRequest> rooms,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note,

        Long cancellationPolicyId,

        Long earlyCheckoutPolicyId
) {
    public ReservationCreateRequest(
            Long guestId,
            BookingSource bookingSource,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer adults,
            Integer children,
            List<ReservationRoomItemRequest> rooms,
            String note
    ) {
        this(guestId, bookingSource, checkInDate, checkOutDate, adults, children, rooms, note, null, null);
    }
}
