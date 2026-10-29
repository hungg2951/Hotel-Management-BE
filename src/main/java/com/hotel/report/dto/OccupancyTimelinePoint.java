package com.hotel.report.dto;

import java.time.LocalDate;

public record OccupancyTimelinePoint(
        String label,
        LocalDate date,
        long occupiedRooms,
        double occupancyRate
) {
}

