package com.hotel.room.service;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.common.exception.BusinessException;
import com.hotel.room.dto.RoomRequest;
import com.hotel.room.dto.RoomResponse;
import com.hotel.room.dto.RoomStatusUpdateRequest;
import com.hotel.room.entity.Room;
import com.hotel.room.entity.RoomType;
import com.hotel.room.repository.RoomRepository;
import com.hotel.room.repository.RoomTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomService(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getRooms(
            RoomStatus status,
            HousekeepingStatus housekeepingStatus,
            Long roomTypeId,
            Integer floorNo,
            String search,
            Boolean activeOnly,
            Pageable pageable
    ) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return roomRepository.findWithFilters(status, housekeepingStatus, roomTypeId, floorNo, trimmedSearch, activeOnly, pageable)
                .map(RoomResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = findEntityById(id);
        return RoomResponse.fromEntity(room);
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.findByRoomNumber(request.roomNumber()).isPresent()) {
            throw BusinessException.duplicate("Số phòng '" + request.roomNumber() + "' đã tồn tại trong khách sạn");
        }

        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy hạng phòng có mã: " + request.roomTypeId()));

        Room room = new Room();
        room.setRoomType(roomType);
        room.setRoomNumber(request.roomNumber());
        room.setFloorNo(request.floorNo());
        room.setNote(request.note());
        room.setStatus(RoomStatus.AVAILABLE);
        room.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        room.setIsActive(request.isActive() != null ? request.isActive() : true);

        Room saved = roomRepository.save(room);
        return RoomResponse.fromEntity(saved);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = findEntityById(id);

        if (roomRepository.findByRoomNumberAndRoomIdNot(request.roomNumber(), id).isPresent()) {
            throw BusinessException.duplicate("Số phòng '" + request.roomNumber() + "' đã được sử dụng bởi phòng khác");
        }

        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy hạng phòng có mã: " + request.roomTypeId()));

        room.setRoomType(roomType);
        room.setRoomNumber(request.roomNumber());
        room.setFloorNo(request.floorNo());
        room.setNote(request.note());
        if (request.isActive() != null) {
            room.setIsActive(request.isActive());
        }

        Room updated = roomRepository.save(room);
        return RoomResponse.fromEntity(updated);
    }

    @Transactional
    public RoomResponse updateRoomStatus(Long id, RoomStatusUpdateRequest request) {
        Room room = findEntityById(id);
        RoomStatus currentStatus = room.getStatus();
        RoomStatus targetStatus = request.status();

        // Kiểm tra máy trạng thái phòng:
        // room.status chỉnh tay: chỉ AVAILABLE ↔ MAINTENANCE/OUT_OF_ORDER; không đặt OCCUPIED bằng tay; không đổi phòng đang OCCUPIED.
        if (currentStatus == RoomStatus.OCCUPIED) {
            throw BusinessException.invalidState("Không thể thay đổi trạng thái của phòng đang có khách lưu trú (OCCUPIED)");
        }

        if (targetStatus == RoomStatus.OCCUPIED) {
            throw BusinessException.invalidState("Không được phép chuyển phòng sang trạng thái OCCUPIED bằng thao tác thủ công");
        }

        boolean isValidTransition = false;
        if (currentStatus == RoomStatus.AVAILABLE && (targetStatus == RoomStatus.MAINTENANCE || targetStatus == RoomStatus.OUT_OF_ORDER || targetStatus == RoomStatus.AVAILABLE)) {
            isValidTransition = true;
        } else if (currentStatus == RoomStatus.MAINTENANCE && (targetStatus == RoomStatus.AVAILABLE || targetStatus == RoomStatus.OUT_OF_ORDER || targetStatus == RoomStatus.MAINTENANCE)) {
            isValidTransition = true;
        } else if (currentStatus == RoomStatus.OUT_OF_ORDER && (targetStatus == RoomStatus.AVAILABLE || targetStatus == RoomStatus.MAINTENANCE || targetStatus == RoomStatus.OUT_OF_ORDER)) {
            isValidTransition = true;
        }

        if (!isValidTransition) {
            throw BusinessException.invalidState("Chuyển đổi trạng thái từ " + currentStatus + " sang " + targetStatus + " không hợp lệ");
        }

        room.setStatus(targetStatus);
        if (request.note() != null) {
            room.setNote(request.note());
        }

        Room updated = roomRepository.save(room);
        return RoomResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = findEntityById(id);
        room.setIsActive(false);
        roomRepository.save(room);
    }

    public Room findEntityById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng có mã: " + id));
    }
}
