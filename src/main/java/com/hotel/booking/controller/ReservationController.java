package com.hotel.booking.controller;

import com.hotel.booking.dto.*;
import com.hotel.booking.service.BookingService;
import com.hotel.common.enums.ReservationStatus;
import com.hotel.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final BookingService bookingService;

    public ReservationController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> createReservation(
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        ReservationDetailResponse created = bookingService.createReservation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.reservationId())
                .toUri();

        return ResponseEntity.created(location)
                .body(ApiResponse.success(created, "Tạo đơn đặt phòng thành công"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String effectiveKeyword = (keyword != null && !keyword.isBlank()) ? keyword : code;
        Page<ReservationResponse> page = bookingService.getReservations(
                status, guestId, checkInDate, checkOutDate, effectiveKeyword, pageable
        );
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách đơn đặt phòng thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationById(@PathVariable Long id) {
        ReservationDetailResponse response = bookingService.getReservationDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết đơn đặt phòng thành công"));
    }

    @PostMapping("/{id}/deposits")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> recordDeposit(
            @PathVariable Long id,
            @Valid @RequestBody ReservationDepositRequest request
    ) {
        ReservationDetailResponse response = bookingService.recordDeposit(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Ghi nhận đặt cọc thành công"));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> confirmReservation(@PathVariable Long id) {
        ReservationDetailResponse response = bookingService.confirmReservation(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Xác nhận đơn đặt phòng thành công"));
    }

    @GetMapping("/{id}/cancellation-preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<CancellationPreviewResponse>> previewCancellation(@PathVariable Long id) {
        CancellationPreviewResponse response = bookingService.previewCancellation(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Xem trước tính phí hủy đơn thành công"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> cancelReservation(
            @PathVariable Long id,
            @RequestBody(required = false) ReservationCancelRequest request
    ) {
        ReservationDetailResponse response = bookingService.cancelReservation(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Hủy đơn đặt phòng thành công"));
    }

    @PostMapping("/{id}/no-show")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> markNoShow(@PathVariable Long id) {
        ReservationDetailResponse response = bookingService.markNoShow(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật vắng mặt (No-Show) thành công"));
    }
}
