package com.hotel.housekeeping.dto;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.HousekeepingTaskStatus;
import com.hotel.common.enums.HousekeepingTaskType;

import java.time.LocalDateTime;

public record HousekeepingTaskResponse(
        Long taskId,
        Long roomId,
        String roomNumber,
        Integer floorNo,
        String roomTypeName,
        HousekeepingStatus roomHousekeepingStatus,
        Long assignedTo,
        String assignedEmployeeName,
        HousekeepingTaskType taskType,
        HousekeepingTaskStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
