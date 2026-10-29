package com.hotel.housekeeping.controller;

import com.hotel.common.enums.HousekeepingTaskStatus;
import com.hotel.common.enums.HousekeepingTaskType;
import com.hotel.common.response.ApiResponse;
import com.hotel.housekeeping.dto.*;
import com.hotel.housekeeping.service.HousekeepingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/housekeeping-tasks")
public class HousekeepingController {

    private final HousekeepingService housekeepingService;

    public HousekeepingController(HousekeepingService housekeepingService) {
        this.housekeepingService = housekeepingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> createTask(
            @Valid @RequestBody HousekeepingTaskCreateRequest request
    ) {
        HousekeepingTaskResponse response = housekeepingService.createTask(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.taskId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Tạo công việc dọn phòng thành công"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<Page<HousekeepingTaskResponse>>> getTasks(
            @RequestParam(required = false) HousekeepingTaskStatus status,
            @RequestParam(required = false) HousekeepingTaskType taskType,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long assignedTo,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<HousekeepingTaskResponse> page = housekeepingService.getTasks(status, taskType, roomId, assignedTo, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách công việc buồng phòng thành công"));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<HousekeepingSummaryDto>> getSummary() {
        HousekeepingSummaryDto summary = housekeepingService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Lấy thống kê buồng phòng thành công"));
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<List<HousekeeperEmployeeDto>>> getEmployees() {
        List<HousekeeperEmployeeDto> employees = housekeepingService.getEmployees();
        return ResponseEntity.ok(ApiResponse.success(employees, "Lấy danh sách nhân viên thành công"));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> assignTask(
            @PathVariable Long id,
            @Valid @RequestBody HousekeepingTaskAssignRequest request
    ) {
        HousekeepingTaskResponse response = housekeepingService.assignTask(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Phân công công việc dọn phòng thành công"));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> startTask(@PathVariable Long id) {
        HousekeepingTaskResponse response = housekeepingService.startTask(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Bắt đầu thực hiện công việc dọn phòng"));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<HousekeepingTaskResponse>> completeTask(@PathVariable Long id) {
        HousekeepingTaskResponse response = housekeepingService.completeTask(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã hoàn thành công việc dọn phòng"));
    }
}
