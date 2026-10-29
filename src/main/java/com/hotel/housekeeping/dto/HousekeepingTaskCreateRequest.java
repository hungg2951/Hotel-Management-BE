package com.hotel.housekeeping.dto;

import com.hotel.common.enums.HousekeepingTaskType;
import jakarta.validation.constraints.NotNull;

public record HousekeepingTaskCreateRequest(
        @NotNull(message = "Mã phòng không được để trống")
        Long roomId,

        HousekeepingTaskType taskType,

        Long assignedTo,

        String note
) {
}
