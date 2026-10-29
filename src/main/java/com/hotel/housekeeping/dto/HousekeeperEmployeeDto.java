package com.hotel.housekeeping.dto;

public record HousekeeperEmployeeDto(
        Long employeeId,
        String fullName,
        String phone,
        String position
) {
}
