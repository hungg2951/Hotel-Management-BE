package com.hotel.report.dto;

import java.util.List;

public record HousekeeperPerformanceResponse(
        long totalTasks,
        long pendingTasks,
        long inProgressTasks,
        long doneTasks,
        long myAssignedTasks,
        long myDoneTasks,
        long myPendingTasks,
        long myInProgressTasks,
        List<PerformerStat> topPerformers
) {
    public record PerformerStat(
            Long employeeId,
            String employeeName,
            long completedTasks
    ) {
    }
}

