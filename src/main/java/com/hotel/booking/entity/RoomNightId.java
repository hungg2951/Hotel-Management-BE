package com.hotel.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class RoomNightId implements Serializable {

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "stay_date")
    private LocalDate stayDate;

    public RoomNightId() {
    }

    public RoomNightId(Long roomId, LocalDate stayDate) {
        this.roomId = roomId;
        this.stayDate = stayDate;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public LocalDate getStayDate() {
        return stayDate;
    }

    public void setStayDate(LocalDate stayDate) {
        this.stayDate = stayDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomNightId that = (RoomNightId) o;
        return Objects.equals(roomId, that.roomId) && Objects.equals(stayDate, that.stayDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomId, stayDate);
    }
}
