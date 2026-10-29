package com.hotel.room.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RoomTypeAmenityId implements Serializable {

    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "amenity_id")
    private Long amenityId;

    public RoomTypeAmenityId() {
    }

    public RoomTypeAmenityId(Long roomTypeId, Long amenityId) {
        this.roomTypeId = roomTypeId;
        this.amenityId = amenityId;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public Long getAmenityId() {
        return amenityId;
    }

    public void setAmenityId(Long amenityId) {
        this.amenityId = amenityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoomTypeAmenityId that = (RoomTypeAmenityId) o;
        return Objects.equals(roomTypeId, that.roomTypeId) && Objects.equals(amenityId, that.amenityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomTypeId, amenityId);
    }
}
