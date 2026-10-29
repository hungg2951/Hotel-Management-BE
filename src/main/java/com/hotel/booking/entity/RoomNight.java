package com.hotel.booking.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "room_night")
public class RoomNight {

    @EmbeddedId
    private RoomNightId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_room_id", nullable = false)
    private ReservationRoom reservationRoom;

    public RoomNight() {
    }

    public RoomNight(Long roomId, LocalDate stayDate, ReservationRoom reservationRoom) {
        this.id = new RoomNightId(roomId, stayDate);
        this.reservationRoom = reservationRoom;
    }

    public RoomNightId getId() {
        return id;
    }

    public void setId(RoomNightId id) {
        this.id = id;
    }

    public ReservationRoom getReservationRoom() {
        return reservationRoom;
    }

    public void setReservationRoom(ReservationRoom reservationRoom) {
        this.reservationRoom = reservationRoom;
    }
}
