package com.hotel.room.dto;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.room.entity.Room;

public record RoomResponse(
        Long roomId,
        Long roomTypeId,
        String roomTypeName,
        String roomNumber,
        Integer floorNo,
        RoomStatus status,
        HousekeepingStatus housekeepingStatus,
        Boolean isActive,
        String note
) {
    public static RoomResponse fromEntity(Room room) {
        return new RoomResponse(
                room.getRoomId(),
                room.getRoomType() != null ? room.getRoomType().getRoomTypeId() : null,
                room.getRoomType() != null ? room.getRoomType().getName() : null,
                room.getRoomNumber(),
                room.getFloorNo(),
                room.getStatus(),
                room.getHousekeepingStatus(),
                room.getIsActive(),
                room.getNote()
        );
    }
}
