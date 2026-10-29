package com.hotel.room.api;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Cổng giao tiếp công khai của module Room dành cho các module khác (Booking, Frontdesk, Housekeeping).
 * Chỉ truyền nhận các record DTO và kiểu dữ liệu cơ bản, tuyệt đối không lộ Entity.
 */
public interface RoomApi {

    RoomInfoDto getRoom(Long roomId);

    RoomTypeInfoDto getRoomType(Long roomTypeId);

    boolean isRoomReadyForCheckIn(Long roomId);

    void updateRoomStatus(Long roomId, RoomStatus status, HousekeepingStatus housekeepingStatus);

    void updateHousekeepingStatus(Long roomId, HousekeepingStatus housekeepingStatus);

    Long calculateNightPrice(Long roomTypeId, LocalDate date);

    List<RoomInfoDto> getAvailableCleanRooms(Long roomTypeId);

    List<Long> findAvailableCleanRoomIds(Long roomTypeId, List<Long> excludedRoomIds);

    List<RoomInfoDto> getActiveRoomsByRoomType(Long roomTypeId);

    List<RoomTypeInfoDto> getAllActiveRoomTypes();
}
