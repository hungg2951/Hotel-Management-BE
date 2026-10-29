package com.hotel.housekeeping.service;

import com.hotel.common.enums.HousekeepingTaskType;
import com.hotel.housekeeping.api.HousekeepingApi;
import com.hotel.housekeeping.dto.HousekeepingTaskCreateRequest;
import org.springframework.stereotype.Service;

@Service
public class HousekeepingApiImpl implements HousekeepingApi {

    private final HousekeepingService housekeepingService;

    public HousekeepingApiImpl(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    @Override
    public void createCleaningTask(Long roomId, HousekeepingTaskType taskType, String note) {
        housekeepingService.createTask(new HousekeepingTaskCreateRequest(
                roomId,
                taskType != null ? taskType : HousekeepingTaskType.CLEANING,
                null,
                note
        ));
    }
}
