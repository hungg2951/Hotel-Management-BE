package com.hotel.report.dto;

import java.time.LocalDate;

public record RevenueTimelinePoint(
        String label,
        LocalDate date,
        long collectedRevenue,
        long recognizedRevenue,
        long roomRevenue,
        long serviceRevenue
) {
}

