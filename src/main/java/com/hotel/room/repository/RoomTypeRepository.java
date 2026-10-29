package com.hotel.room.repository;

import com.hotel.room.entity.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByIsActiveTrue();
    Page<RoomType> findByIsActiveTrue(Pageable pageable);
    Optional<RoomType> findByName(String name);
    Optional<RoomType> findByNameAndRoomTypeIdNot(String name, Long roomTypeId);
}
