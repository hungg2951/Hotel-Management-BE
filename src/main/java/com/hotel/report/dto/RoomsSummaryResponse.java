package com.hotel.report.dto;

public record RoomsSummaryResponse(
        long totalRooms,
        long availableRooms,
        long readyRooms,
        long occupiedRooms,
        long maintenanceRooms,
        long outOfOrderRooms,
        long cleanRooms,
        long dirtyRooms,
        long cleaningRooms,
        long inspectedRooms
) {
    public RoomsSummaryResponse(
            long totalRooms,
            long availableRooms,
            long occupiedRooms,
            long maintenanceRooms,
            long outOfOrderRooms,
            long cleanRooms,
            long dirtyRooms,
            long cleaningRooms,
            long inspectedRooms
    ) {
        this(totalRooms, availableRooms, availableRooms, occupiedRooms, maintenanceRooms, outOfOrderRooms, cleanRooms, dirtyRooms, cleaningRooms, inspectedRooms);
    }
}
