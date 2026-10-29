package com.hotel.report.dto;

import java.time.LocalDate;
import java.util.List;

public record RevenueResponse(
        String period,
        LocalDate startDate,
        LocalDate endDate,
        long collectedRevenue,
        long recognizedRevenue,
        long roomRevenue,
        long serviceRevenue,
        long discountTotal,
        List<RevenueTimelinePoint> timeline
) {
    public RevenueResponse(
            LocalDate date,
            long collectedRevenue,
            long recognizedRevenue,
            long roomRevenue,
            long serviceRevenue,
            long discountTotal
    ) {
        this("DAY", date, date, collectedRevenue, recognizedRevenue, roomRevenue, serviceRevenue, discountTotal, List.of());
    }
}
