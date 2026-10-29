package com.hotel.booking.entity;

import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.ReservationStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long reservationId;

    @Column(name = "reservation_code", nullable = false, unique = true, length = 20)
    private String reservationCode;

    @Column(name = "guest_id", nullable = false)
    private Long guestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_source", nullable = false, length = 15)
    private BookingSource bookingSource = BookingSource.WALK_IN;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "adults", nullable = false)
    private Integer adults = 1;

    @Column(name = "children", nullable = false)
    private Integer children = 0;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount = 0L;

    @Column(name = "deposit_amount", nullable = false)
    private Long depositAmount = 0L;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "cancellation_policy_id")
    private Long cancellationPolicyId;

    @Column(name = "early_checkout_policy_id")
    private Long earlyCheckoutPolicyId;

    @Column(name = "cancellation_policy_snapshot", length = 2000)
    private String cancellationPolicySnapshot;

    @Column(name = "early_checkout_policy_snapshot", length = 2000)
    private String earlyCheckoutPolicySnapshot;

    @Column(name = "cancellation_fee")
    private Long cancellationFee;

    @Column(name = "refund_amount")
    private Long refundAmount;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "override_by")
    private Long overrideBy;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Reservation() {
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public Long getGuestId() {
        return guestId;
    }

    public void setGuestId(Long guestId) {
        this.guestId = guestId;
    }

    public BookingSource getBookingSource() {
        return bookingSource;
    }

    public void setBookingSource(BookingSource bookingSource) {
        this.bookingSource = bookingSource;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getAdults() {
        return adults;
    }

    public void setAdults(Integer adults) {
        this.adults = adults;
    }

    public Integer getChildren() {
        return children;
    }

    public void setChildren(Integer children) {
        this.children = children;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(Long depositAmount) {
        this.depositAmount = depositAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getCancellationPolicyId() {
        return cancellationPolicyId;
    }

    public void setCancellationPolicyId(Long cancellationPolicyId) {
        this.cancellationPolicyId = cancellationPolicyId;
    }

    public Long getEarlyCheckoutPolicyId() {
        return earlyCheckoutPolicyId;
    }

    public void setEarlyCheckoutPolicyId(Long earlyCheckoutPolicyId) {
        this.earlyCheckoutPolicyId = earlyCheckoutPolicyId;
    }

    public String getCancellationPolicySnapshot() {
        return cancellationPolicySnapshot;
    }

    public void setCancellationPolicySnapshot(String cancellationPolicySnapshot) {
        this.cancellationPolicySnapshot = cancellationPolicySnapshot;
    }

    public String getEarlyCheckoutPolicySnapshot() {
        return earlyCheckoutPolicySnapshot;
    }

    public void setEarlyCheckoutPolicySnapshot(String earlyCheckoutPolicySnapshot) {
        this.earlyCheckoutPolicySnapshot = earlyCheckoutPolicySnapshot;
    }

    public Long getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(Long cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public Long getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Long refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getOverrideReason() {
        return overrideReason;
    }

    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
    }

    public Long getOverrideBy() {
        return overrideBy;
    }

    public void setOverrideBy(Long overrideBy) {
        this.overrideBy = overrideBy;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
