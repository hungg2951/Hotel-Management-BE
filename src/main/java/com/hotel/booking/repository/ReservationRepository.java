package com.hotel.booking.repository;

import com.hotel.booking.entity.Reservation;
import com.hotel.common.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReservationCode(String reservationCode);

    boolean existsByReservationCode(String reservationCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.reservationId = :id")
    Optional<Reservation> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT r FROM Reservation r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:guestId IS NULL OR r.guestId = :guestId) AND " +
           "(:checkInDate IS NULL OR r.checkInDate >= :checkInDate) AND " +
           "(:checkOutDate IS NULL OR r.checkOutDate <= :checkOutDate) AND " +
           "(:keyword IS NULL OR " +
           " LOWER(r.reservationCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " (:hasGuestIds = true AND r.guestId IN :guestIds))")
    Page<Reservation> searchReservations(
            @Param("status") ReservationStatus status,
            @Param("guestId") Long guestId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("keyword") String keyword,
            @Param("hasGuestIds") boolean hasGuestIds,
            @Param("guestIds") java.util.Collection<Long> guestIds,
            Pageable pageable
    );
    List<Reservation> findByCheckInDateAndStatusIn(LocalDate checkInDate, java.util.Collection<ReservationStatus> statuses);

    List<Reservation> findByCheckOutDateAndStatus(LocalDate checkOutDate, ReservationStatus status);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Reservation r SET r.cancellationPolicyId = NULL WHERE r.cancellationPolicyId = :policyId")
    void detachCancellationPolicy(@Param("policyId") Long policyId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Reservation r SET r.earlyCheckoutPolicyId = NULL WHERE r.earlyCheckoutPolicyId = :policyId")
    void detachEarlyCheckoutPolicy(@Param("policyId") Long policyId);
}
