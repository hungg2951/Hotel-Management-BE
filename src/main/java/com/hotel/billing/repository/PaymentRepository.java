package com.hotel.billing.repository;

import com.hotel.billing.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByFolioFolioId(Long folioId);

    List<Payment> findByFolioFolioIdOrderByPaidAtAsc(Long folioId);

    List<Payment> findByReservationId(Long reservationId);

    List<Payment> findByReservationIdAndFolioIsNull(Long reservationId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.folio.folioId = :folioId AND p.status = 'SUCCESS'")
    Long calculatePaidSuccessByFolioId(@Param("folioId") Long folioId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.folio.folioId = :folioId AND p.status = 'REFUNDED'")
    Long calculateRefundedByFolioId(@Param("folioId") Long folioId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.reservationId = :reservationId AND p.status = 'SUCCESS'")
    Long calculateDepositSuccessByReservationId(@Param("reservationId") Long reservationId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.reservationId = :reservationId AND p.status = 'REFUNDED'")
    Long calculateDepositRefundedByReservationId(@Param("reservationId") Long reservationId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt >= :start AND p.paidAt < :end")
    Long calculateTotalSuccessBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'REFUNDED' AND p.paidAt >= :start AND p.paidAt < :end")
    Long calculateTotalRefundedBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    List<Payment> findByPaidAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
