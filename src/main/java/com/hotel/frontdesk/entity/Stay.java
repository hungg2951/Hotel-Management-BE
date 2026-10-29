package com.hotel.frontdesk.entity;

import com.hotel.common.enums.StayStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stay")
public class Stay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stay_id")
    private Long stayId;

    @Column(name = "reservation_room_id", nullable = false, unique = true)
    private Long reservationRoomId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "actual_check_in", nullable = false)
    private LocalDateTime actualCheckIn;

    @Column(name = "actual_check_out")
    private LocalDateTime actualCheckOut;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private StayStatus status = StayStatus.IN_HOUSE;

    @Column(name = "checked_in_by")
    private Long checkedInBy;

    @Column(name = "checked_out_by")
    private Long checkedOutBy;

    @Column(name = "early_checkout_penalty")
    private Long earlyCheckoutPenalty = 0L;

    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    @Column(name = "override_by")
    private Long overrideBy;

    public Stay() {
    }

    public Long getStayId() {
        return stayId;
    }

    public void setStayId(Long stayId) {
        this.stayId = stayId;
    }

    public Long getReservationRoomId() {
        return reservationRoomId;
    }

    public void setReservationRoomId(Long reservationRoomId) {
        this.reservationRoomId = reservationRoomId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getActualCheckIn() {
        return actualCheckIn;
    }

    public void setActualCheckIn(LocalDateTime actualCheckIn) {
        this.actualCheckIn = actualCheckIn;
    }

    public LocalDateTime getActualCheckOut() {
        return actualCheckOut;
    }

    public void setActualCheckOut(LocalDateTime actualCheckOut) {
        this.actualCheckOut = actualCheckOut;
    }

    public StayStatus getStatus() {
        return status;
    }

    public void setStatus(StayStatus status) {
        this.status = status;
    }

    public Long getCheckedInBy() {
        return checkedInBy;
    }

    public void setCheckedInBy(Long checkedInBy) {
        this.checkedInBy = checkedInBy;
    }

    public Long getCheckedOutBy() {
        return checkedOutBy;
    }

    public void setCheckedOutBy(Long checkedOutBy) {
        this.checkedOutBy = checkedOutBy;
    }

    public Long getEarlyCheckoutPenalty() {
        return earlyCheckoutPenalty;
    }

    public void setEarlyCheckoutPenalty(Long earlyCheckoutPenalty) {
        this.earlyCheckoutPenalty = earlyCheckoutPenalty;
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
}
