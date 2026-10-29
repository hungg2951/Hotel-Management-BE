package com.hotel.room.api;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;

public record RoomInfoDto(
        Long roomId,
        Long roomTypeId,
        String roomNumber,
        Integer floorNo,
        RoomStatus status,
        HousekeepingStatus housekeepingStatus,
        Boolean isActive,
        String note
) {
}
