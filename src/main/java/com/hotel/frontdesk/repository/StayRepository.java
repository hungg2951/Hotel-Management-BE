package com.hotel.frontdesk.repository;

import com.hotel.common.enums.StayStatus;
import com.hotel.frontdesk.entity.Stay;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StayRepository extends JpaRepository<Stay, Long> {

    Optional<Stay> findByReservationRoomId(Long reservationRoomId);

    Optional<Stay> findByRoomIdAndStatus(Long roomId, StayStatus status);

    List<Stay> findByStatus(StayStatus status);

    long countByStatus(StayStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stay s WHERE s.stayId = :id")
    Optional<Stay> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT s FROM Stay s WHERE (:status IS NULL OR s.status = :status) ORDER BY s.actualCheckIn DESC")
    Page<Stay> searchStays(@Param("status") StayStatus status, Pageable pageable);
}
