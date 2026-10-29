package com.hotel.booking.repository;

import com.hotel.booking.entity.RoomNight;
import com.hotel.booking.entity.RoomNightId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomNightRepository extends JpaRepository<RoomNight, RoomNightId> {

    List<RoomNight> findByIdRoomIdAndIdStayDateBetween(Long roomId, LocalDate from, LocalDate to);

    @Query("SELECT rn FROM RoomNight rn WHERE rn.id.stayDate >= :fromDate AND rn.id.stayDate <= :toDate")
    List<RoomNight> findByStayDateBetween(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("SELECT DISTINCT rn.id.roomId FROM RoomNight rn WHERE rn.id.stayDate >= :checkInDate AND rn.id.stayDate < :checkOutDate")
    List<Long> findOccupiedRoomIdsBetween(@Param("checkInDate") LocalDate checkInDate, @Param("checkOutDate") LocalDate checkOutDate);

    @Query("SELECT COUNT(rn) > 0 FROM RoomNight rn WHERE rn.id.roomId = :roomId AND rn.id.stayDate >= :checkInDate AND rn.id.stayDate < :checkOutDate")
    boolean isRoomOccupiedBetween(@Param("roomId") Long roomId, @Param("checkInDate") LocalDate checkInDate, @Param("checkOutDate") LocalDate checkOutDate);

    List<RoomNight> findByReservationRoomReservationRoomId(Long reservationRoomId);

    List<RoomNight> findByReservationRoomReservationReservationId(Long reservationId);

    @Modifying
    @Query("DELETE FROM RoomNight rn WHERE rn.reservationRoom.reservation.reservationId = :reservationId")
    void deleteByReservationId(@Param("reservationId") Long reservationId);

    @Modifying
    @Query("DELETE FROM RoomNight rn WHERE rn.reservationRoom.reservationRoomId = :reservationRoomId AND rn.id.stayDate >= :fromDate")
    void deleteByReservationRoomIdAndStayDateFrom(@Param("reservationRoomId") Long reservationRoomId, @Param("fromDate") LocalDate fromDate);
}
