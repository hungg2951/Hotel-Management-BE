package com.hotel.room.repository;

import com.hotel.room.entity.RoomTypeAmenity;
import com.hotel.room.entity.RoomTypeAmenityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomTypeAmenityRepository extends JpaRepository<RoomTypeAmenity, RoomTypeAmenityId> {
}
