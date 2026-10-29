package com.hotel.room.service;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.common.exception.BusinessException;
import com.hotel.room.api.RoomApi;
import com.hotel.room.api.RoomInfoDto;
import com.hotel.room.api.RoomTypeInfoDto;
import com.hotel.room.entity.Room;
import com.hotel.room.entity.RoomType;
import com.hotel.room.repository.RoomRepository;
import com.hotel.room.repository.RoomTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class RoomApiImpl implements RoomApi {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public RoomApiImpl(
            RoomRepository roomRepository,
            RoomTypeRepository roomTypeRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RoomInfoDto getRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng có mã: " + roomId));
        return toRoomInfoDto(room);
    }

    @Override
    @Transactional(readOnly = true)
    public RoomTypeInfoDto getRoomType(Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy hạng phòng có mã: " + roomTypeId));
        return toRoomTypeInfoDto(roomType);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRoomReadyForCheckIn(Long roomId) {
        return roomRepository.findById(roomId)
                .map(room -> room.getStatus() == RoomStatus.AVAILABLE &&
                             Boolean.TRUE.equals(room.getIsActive()) &&
                             (room.getHousekeepingStatus() == HousekeepingStatus.CLEAN ||
                              room.getHousekeepingStatus() == HousekeepingStatus.INSPECTED))
                .orElse(false);
    }

    @Override
    @Transactional
    public void updateRoomStatus(Long roomId, RoomStatus status, HousekeepingStatus housekeepingStatus) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng có mã: " + roomId));
        if (status != null) {
            room.setStatus(status);
        }
        if (housekeepingStatus != null) {
            room.setHousekeepingStatus(housekeepingStatus);
        }
        roomRepository.save(room);
    }

    @Override
    @Transactional
    public void updateHousekeepingStatus(Long roomId, HousekeepingStatus housekeepingStatus) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng có mã: " + roomId));
        room.setHousekeepingStatus(housekeepingStatus);
        roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public Long calculateNightPrice(Long roomTypeId, LocalDate date) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy hạng phòng có mã: " + roomTypeId));
        return roomType.getBasePrice();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomInfoDto> getAvailableCleanRooms(Long roomTypeId) {
        return roomRepository.findByRoomTypeRoomTypeIdAndStatusAndHousekeepingStatusInAndIsActiveTrue(
                roomTypeId, RoomStatus.AVAILABLE, Set.of(HousekeepingStatus.CLEAN, HousekeepingStatus.INSPECTED)
        ).stream().map(this::toRoomInfoDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAvailableCleanRoomIds(Long roomTypeId, List<Long> excludedRoomIds) {
        List<Room> rooms = roomRepository.findByRoomTypeRoomTypeIdAndStatusAndHousekeepingStatusInAndIsActiveTrue(
                roomTypeId, RoomStatus.AVAILABLE, Set.of(HousekeepingStatus.CLEAN, HousekeepingStatus.INSPECTED)
        );
        return rooms.stream()
                .map(Room::getRoomId)
                .filter(id -> excludedRoomIds == null || !excludedRoomIds.contains(id))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomInfoDto> getActiveRoomsByRoomType(Long roomTypeId) {
        return roomRepository.findByRoomTypeRoomTypeIdAndIsActiveTrue(roomTypeId)
                .stream()
                .filter(r -> r.getStatus() != RoomStatus.OUT_OF_ORDER)
                .map(this::toRoomInfoDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeInfoDto> getAllActiveRoomTypes() {
        return roomTypeRepository.findByIsActiveTrue()
                .stream()
                .map(this::toRoomTypeInfoDto)
                .toList();
    }

    private RoomInfoDto toRoomInfoDto(Room room) {
        return new RoomInfoDto(
                room.getRoomId(),
                room.getRoomType() != null ? room.getRoomType().getRoomTypeId() : null,
                room.getRoomNumber(),
                room.getFloorNo(),
                room.getStatus(),
                room.getHousekeepingStatus(),
                room.getIsActive(),
                room.getNote()
        );
    }

    private RoomTypeInfoDto toRoomTypeInfoDto(RoomType type) {
        return new RoomTypeInfoDto(
                type.getRoomTypeId(),
                type.getName(),
                type.getDescription(),
                type.getMaxAdults(),
                type.getMaxChildren(),
                type.getBedType(),
                type.getAreaSqm(),
                type.getBasePrice(),
                type.getIsActive()
        );
    }
}
