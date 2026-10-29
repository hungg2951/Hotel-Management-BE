package com.hotel.booking.api;

import java.time.LocalDate;
import java.util.List;

public record WalkInReservationCommand(
        Long guestId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer adults,
        Integer children,
        List<WalkInRoomItemCommand> rooms,
        String note
) {
    public record WalkInRoomItemCommand(
            Long roomTypeId,
            Long roomId
    ) {
    }
}
