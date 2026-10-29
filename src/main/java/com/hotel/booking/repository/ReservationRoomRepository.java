package com.hotel.booking.repository;

import com.hotel.booking.entity.ReservationRoom;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRoomRepository extends JpaRepository<ReservationRoom, Long> {

    List<ReservationRoom> findByReservationReservationId(Long reservationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rr FROM ReservationRoom rr WHERE rr.reservationRoomId = :id")
    Optional<ReservationRoom> findByIdWithLock(@Param("id") Long id);
}
