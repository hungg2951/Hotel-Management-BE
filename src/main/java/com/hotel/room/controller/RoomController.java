package com.hotel.room.controller;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.common.response.ApiResponse;
import com.hotel.room.dto.RoomRequest;
import com.hotel.room.dto.RoomResponse;
import com.hotel.room.dto.RoomStatusUpdateRequest;
import com.hotel.room.service.RoomService;
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
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> getRooms(
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) HousekeepingStatus housekeepingStatus,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Integer floorNo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<RoomResponse> rooms = roomService.getRooms(status, housekeepingStatus, roomTypeId, floorNo, search, activeOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success(rooms, "Lấy danh sách phòng thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT', 'HOUSEKEEPER')")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse response = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin phòng thành công"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.roomId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Tạo mới phòng thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request
    ) {
        RoomResponse response = roomService.updateRoom(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin phòng thành công"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoomStatusUpdateRequest request
    ) {
        RoomResponse response = roomService.updateRoomStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật trạng thái phòng thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa phòng thành công"));
    }
}
