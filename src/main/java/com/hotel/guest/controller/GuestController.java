package com.hotel.guest.controller;

import com.hotel.common.response.ApiResponse;
import com.hotel.guest.dto.GuestRequest;
import com.hotel.guest.dto.GuestResponse;
import com.hotel.guest.service.GuestService;
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
@RequestMapping("/api/v1/guests")
public class GuestController {

    private final GuestService guestService;

    public GuestController(GuestService guestService) {
        this.guestService = guestService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<Page<GuestResponse>>> searchGuests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String idNumber,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<GuestResponse> page = guestService.searchGuests(keyword, idNumber, phone, email, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Tìm kiếm danh sách khách hàng thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<GuestResponse>> getGuestById(@PathVariable Long id) {
        GuestResponse response = guestService.getGuestById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin khách hàng thành công"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<GuestResponse>> createGuest(@Valid @RequestBody GuestRequest request) {
        GuestResponse response = guestService.createGuest(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.guestId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Tạo mới thông tin khách hàng thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<GuestResponse>> updateGuest(
            @PathVariable Long id,
            @Valid @RequestBody GuestRequest request
    ) {
        GuestResponse response = guestService.updateGuest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin khách hàng thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteGuest(@PathVariable Long id) {
        guestService.deleteGuest(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa thông tin khách hàng thành công"));
    }
}
