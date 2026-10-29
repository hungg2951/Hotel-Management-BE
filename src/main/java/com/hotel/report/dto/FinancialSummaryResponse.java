package com.hotel.report.dto;

public record FinancialSummaryResponse(
        long todayRevenue,
        long monthRevenue,
        long advanceDepositHeld,
        long monthRefunded,
        long inHouseUnpaidFolios,
        long cancellationFeeCollected,
        double occupancyRate,
        long maintenanceRooms
) {
}

