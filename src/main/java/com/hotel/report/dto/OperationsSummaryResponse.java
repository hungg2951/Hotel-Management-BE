package com.hotel.report.dto;

public record OperationsSummaryResponse(
        long todayArrivals,
        long todayDepartures,
        long activeStays,
        long todayCollectedRevenue,
        long inHouseUnpaidBalance,
        long pendingArrivals,
        long pendingDepartures,
        long readyRooms,
        long dirtyRooms
) {
}

