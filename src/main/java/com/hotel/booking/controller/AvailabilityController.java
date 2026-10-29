package com.hotel.booking.controller;

import com.hotel.booking.dto.AvailableRoomResponse;
import com.hotel.booking.dto.RoomTypeAvailabilityResponse;
import com.hotel.booking.service.BookingService;
import com.hotel.common.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/availability")
public class AvailabilityController {

    private final BookingService bookingService;

    public AvailabilityController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<RoomTypeAvailabilityResponse>>> searchAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false, defaultValue = "1") Integer adults,
            @RequestParam(required = false, defaultValue = "0") Integer children
    ) {
        List<RoomTypeAvailabilityResponse> result = bookingService.searchAvailability(checkIn, checkOut, adults, children);
        return ResponseEntity.ok(ApiResponse.success(result, "Tra cứu phòng trống thành công"));
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<List<AvailableRoomResponse>>> getRoomsAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long roomTypeId
    ) {
        List<AvailableRoomResponse> result = bookingService.getRoomsAvailability(checkIn, checkOut, roomTypeId);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách trạng thái phòng cụ thể thành công"));
    }
}
