package com.hotel.report.dto;

import java.time.LocalDate;
import java.util.List;

public record OccupancyResponse(
        String period,
        LocalDate startDate,
        LocalDate endDate,
        long totalRooms,
        long operationalRooms,
        long totalRoomNightsAvailable,
        long totalRoomNightsOccupied,
        long maintenanceRooms,
        long outOfOrderRooms,
        double occupancyRate,
        List<OccupancyTimelinePoint> timeline
) {
    public OccupancyResponse(
            LocalDate date,
            long totalRooms,
            long operationalRooms,
            long occupiedRooms,
            long maintenanceRooms,
            long outOfOrderRooms,
            double occupancyRate
    ) {
        this("DAY", date, date, totalRooms, operationalRooms, operationalRooms, occupiedRooms, maintenanceRooms, outOfOrderRooms, occupancyRate, List.of());
    }
}
