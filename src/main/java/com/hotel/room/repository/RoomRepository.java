package com.hotel.room.repository;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.room.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    Optional<Room> findByRoomNumberAndRoomIdNot(String roomNumber, Long roomId);

    List<Room> findByRoomTypeRoomTypeIdAndIsActiveTrue(Long roomTypeId);

    List<Room> findByRoomTypeRoomTypeIdAndStatusAndHousekeepingStatusInAndIsActiveTrue(
            Long roomTypeId, RoomStatus status, Collection<HousekeepingStatus> housekeepingStatuses
    );

    @Query("SELECT r FROM Room r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:housekeepingStatus IS NULL OR r.housekeepingStatus = :housekeepingStatus) AND " +
           "(:roomTypeId IS NULL OR r.roomType.roomTypeId = :roomTypeId) AND " +
           "(:floorNo IS NULL OR r.floorNo = :floorNo) AND " +
           "(:search IS NULL OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.roomType.name) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:activeOnly IS NULL OR (:activeOnly = true AND r.isActive = true) OR (:activeOnly = false AND r.isActive = false))")
    Page<Room> findWithFilters(
            @Param("status") RoomStatus status,
            @Param("housekeepingStatus") HousekeepingStatus housekeepingStatus,
            @Param("roomTypeId") Long roomTypeId,
            @Param("floorNo") Integer floorNo,
            @Param("search") String search,
            @Param("activeOnly") Boolean activeOnly,
            Pageable pageable
    );

    long countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus housekeepingStatus);

    long countByStatusAndHousekeepingStatusInAndIsActiveTrue(
            RoomStatus status, Collection<HousekeepingStatus> housekeepingStatuses
    );

    long countByStatusAndIsActiveTrue(RoomStatus status);

    long countByIsActiveTrue();
}
