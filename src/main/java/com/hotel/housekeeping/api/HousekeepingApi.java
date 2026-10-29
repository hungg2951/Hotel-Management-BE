package com.hotel.housekeeping.api;

import com.hotel.common.enums.HousekeepingTaskType;

public interface HousekeepingApi {
    void createCleaningTask(Long roomId, HousekeepingTaskType taskType, String note);
}
