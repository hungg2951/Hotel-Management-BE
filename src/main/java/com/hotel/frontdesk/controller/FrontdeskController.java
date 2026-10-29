package com.hotel.frontdesk.controller;

import com.hotel.booking.api.ReservationInfoDto;
import com.hotel.common.enums.StayStatus;
import com.hotel.common.response.ApiResponse;
import com.hotel.frontdesk.dto.*;
import com.hotel.frontdesk.service.FrontdeskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/frontdesk")
public class FrontdeskController {

    private final FrontdeskService frontdeskService;

    public FrontdeskController(FrontdeskService frontdeskService) {
        this.frontdeskService = frontdeskService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<StayResponse>> checkIn(@Valid @RequestBody CheckInRequest request) {
        StayResponse response = frontdeskService.checkIn(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thủ tục nhận phòng (Check-in) thành công"));
    }

    @GetMapping("/stays/{id}/early-checkout-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<com.hotel.frontdesk.dto.EarlyCheckoutPreviewResponse>> previewEarlyCheckout(@PathVariable Long id) {
        com.hotel.frontdesk.dto.EarlyCheckoutPreviewResponse response = frontdeskService.previewEarlyCheckout(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem trước tính toán trả phòng sớm thành công"));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<StayResponse>> checkOut(@Valid @RequestBody CheckOutRequest request) {
        StayResponse response = frontdeskService.checkOut(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thủ tục trả phòng (Check-out) thành công"));
    }

    @PostMapping("/change-room")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<StayResponse>> changeRoom(@Valid @RequestBody RoomChangeRequest request) {
        StayResponse response = frontdeskService.changeRoom(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đổi phòng thành công"));
    }

    @PostMapping("/walk-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<StayResponse>> walkIn(@Valid @RequestBody WalkInCheckInRequest request) {
        StayResponse response = frontdeskService.walkIn(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Khách vãng lai nhận phòng (Walk-in) thành công"));
    }

    @GetMapping("/stays")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<Page<StayResponse>>> getStays(
            @RequestParam(required = false) StayStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "actualCheckIn", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<StayResponse> page = frontdeskService.getStays(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách lượt lưu trú thành công"));
    }

    @GetMapping("/stays/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<StayResponse>> getStayDetail(@PathVariable Long id) {
        StayResponse response = frontdeskService.getStayDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết lượt lưu trú thành công"));
    }

    @GetMapping("/expected-arrivals")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<ReservationInfoDto>>> getExpectedArrivals(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<ReservationInfoDto> list = frontdeskService.getExpectedArrivals(date);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách dự kiến nhận phòng hôm nay thành công"));
    }

    @GetMapping("/expected-departures")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<StayResponse>>> getExpectedDepartures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<StayResponse> list = frontdeskService.getExpectedDepartures(date);
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách dự kiến trả phòng hôm nay thành công"));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<FrontdeskSummaryDto>> getSummary() {
        FrontdeskSummaryDto summary = frontdeskService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Lấy số liệu tổng quan tiền sảnh thành công"));
    }
}
