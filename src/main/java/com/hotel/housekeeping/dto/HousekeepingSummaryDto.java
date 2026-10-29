package com.hotel.housekeeping.dto;

public record HousekeepingSummaryDto(
        long totalTasks,
        long pendingTasks,
        long inProgressTasks,
        long doneTasks,
        long dirtyRooms,
        long cleanRooms,
        long cleaningRooms,
        long inspectedRooms
) {
}
