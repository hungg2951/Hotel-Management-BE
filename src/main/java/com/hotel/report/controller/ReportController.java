package com.hotel.report.controller;

import com.hotel.common.exception.BusinessException;
import com.hotel.common.response.ApiResponse;
import com.hotel.common.security.CurrentUserApi;
import com.hotel.common.security.CustomUserDetails;
import com.hotel.report.dto.*;
import com.hotel.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/rooms-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<RoomsSummaryResponse>> getRoomsSummary() {
        RoomsSummaryResponse response = reportService.getRoomsSummary();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thống kê tình trạng phòng thành công"));
    }

    @GetMapping("/occupancy")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<OccupancyResponse>> getOccupancy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAY") String period
    ) {
        LocalDate start = startDate != null ? startDate : date;
        LocalDate end = endDate != null ? endDate : start;
        OccupancyResponse response = reportService.getOccupancyPeriod(period, start, end);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy báo cáo công suất phòng thành công"));
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<RevenueResponse>> getRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAY") String period
    ) {
        // Kiểm tra phân quyền: Lễ tân chỉ được xem doanh thu ngày/ca trực
        CustomUserDetails user = CurrentUserApi.getCurrentUser().orElse(null);
        if (user != null) {
            boolean isOnlyReceptionist = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_RECEPTIONIST"))
                    && user.getAuthorities().stream()
                    .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER") || a.getAuthority().equals("ROLE_ACCOUNTANT"));

            if (isOnlyReceptionist && period != null && !period.equalsIgnoreCase("DAY")) {
                throw BusinessException.forbidden("Lễ tân chỉ có quyền xem thống kê doanh thu theo ngày/ca trực");
            }
        }

        LocalDate start = startDate != null ? startDate : date;
        LocalDate end = endDate != null ? endDate : start;
        RevenueResponse response = reportService.getRevenuePeriod(period, start, end);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy báo cáo doanh thu thành công"));
    }

    @GetMapping("/operations-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<OperationsSummaryResponse>> getOperationsSummary() {
        OperationsSummaryResponse response = reportService.getOperationsSummary();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thống kê vận hành thành công"));
    }

    @GetMapping("/housekeeper-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<HousekeeperPerformanceResponse>> getHousekeeperPerformance() {
        HousekeeperPerformanceResponse response = reportService.getHousekeeperPerformance();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thống kê hiệu suất buồng phòng thành công"));
    }

    @GetMapping("/financial-summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FinancialSummaryResponse>> getFinancialSummary() {
        FinancialSummaryResponse response = reportService.getFinancialSummary();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thống kê tài chính thành công"));
    }
}
