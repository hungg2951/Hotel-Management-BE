package com.hotel.billing.controller;

import com.hotel.billing.dto.HotelServiceRequest;
import com.hotel.billing.dto.HotelServiceResponse;
import com.hotel.billing.service.HotelServiceService;
import com.hotel.common.enums.ServiceCategory;
import com.hotel.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/hotel-services")
public class HotelServiceController {

    private final HotelServiceService hotelServiceService;

    public HotelServiceController(HotelServiceService hotelServiceService) {
        this.hotelServiceService = hotelServiceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Page<HotelServiceResponse>>> getHotelServices(
            @RequestParam(required = false) ServiceCategory category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<HotelServiceResponse> response = hotelServiceService.getHotelServices(category, search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy danh mục dịch vụ thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<HotelServiceResponse>> getHotelServiceById(@PathVariable Long id) {
        HotelServiceResponse response = hotelServiceService.getHotelServiceById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin dịch vụ thành công"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<HotelServiceResponse>> createHotelService(@Valid @RequestBody HotelServiceRequest request) {
        HotelServiceResponse response = hotelServiceService.createHotelService(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.serviceId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Thêm mới dịch vụ thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<HotelServiceResponse>> updateHotelService(
            @PathVariable Long id,
            @Valid @RequestBody HotelServiceRequest request
    ) {
        HotelServiceResponse response = hotelServiceService.updateHotelService(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật dịch vụ thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteHotelService(@PathVariable Long id) {
        hotelServiceService.deleteHotelService(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa dịch vụ thành công"));
    }
}
