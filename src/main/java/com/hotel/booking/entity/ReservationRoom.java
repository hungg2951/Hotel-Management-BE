package com.hotel.booking.entity;

import com.hotel.common.enums.ReservationRoomStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "reservation_room")
public class ReservationRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_room_id")
    private Long reservationRoomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "price_per_night", nullable = false)
    private Long pricePerNight;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private ReservationRoomStatus status = ReservationRoomStatus.BOOKED;

    public ReservationRoom() {
    }

    public Long getReservationRoomId() {
        return reservationRoomId;
    }

    public void setReservationRoomId(Long reservationRoomId) {
        this.reservationRoomId = reservationRoomId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Long pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public ReservationRoomStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationRoomStatus status) {
        this.status = status;
    }
}
