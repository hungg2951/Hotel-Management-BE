package com.hotel.housekeeping.dto;

import jakarta.validation.constraints.NotNull;

public record HousekeepingTaskAssignRequest(
        @NotNull(message = "Mã nhân viên phân công không được để trống")
        Long assignedTo
) {
}
