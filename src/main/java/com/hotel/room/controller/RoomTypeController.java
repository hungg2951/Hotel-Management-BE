package com.hotel.room.controller;

import com.hotel.common.response.ApiResponse;
import com.hotel.room.dto.RoomTypeRequest;
import com.hotel.room.dto.RoomTypeResponse;
import com.hotel.room.service.RoomTypeService;
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
@RequestMapping("/api/v1/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<Object>> getAllRoomTypes(
            @RequestParam(required = false) Boolean activeOnly,
            @RequestParam(required = false) Boolean all,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (Boolean.TRUE.equals(all)) {
            List<RoomTypeResponse> roomTypes = roomTypeService.getAllActiveRoomTypes();
            return ResponseEntity.ok(ApiResponse.success(roomTypes, "Lấy danh sách hạng phòng thành công"));
        }
        Page<RoomTypeResponse> roomTypes = roomTypeService.getRoomTypes(activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(roomTypes, "Lấy danh sách hạng phòng thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> getRoomTypeById(@PathVariable Long id) {
        RoomTypeResponse response = roomTypeService.getRoomTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin hạng phòng thành công"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> createRoomType(@Valid @RequestBody RoomTypeRequest request) {
        RoomTypeResponse response = roomTypeService.createRoomType(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.roomTypeId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Tạo mới hạng phòng thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RoomTypeResponse>> updateRoomType(
            @PathVariable Long id,
            @Valid @RequestBody RoomTypeRequest request
    ) {
        RoomTypeResponse response = roomTypeService.updateRoomType(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật hạng phòng thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteRoomType(@PathVariable Long id) {
        roomTypeService.deleteRoomType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa hạng phòng thành công"));
    }
}
