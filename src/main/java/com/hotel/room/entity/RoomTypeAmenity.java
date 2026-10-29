package com.hotel.room.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "room_type_amenity")
public class RoomTypeAmenity {

    @EmbeddedId
    private RoomTypeAmenityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("roomTypeId")
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("amenityId")
    @JoinColumn(name = "amenity_id")
    private Amenity amenity;

    public RoomTypeAmenity() {
    }

    public RoomTypeAmenity(RoomType roomType, Amenity amenity) {
        this.roomType = roomType;
        this.amenity = amenity;
        this.id = new RoomTypeAmenityId(roomType != null ? roomType.getRoomTypeId() : null, amenity != null ? amenity.getAmenityId() : null);
    }

    public RoomTypeAmenityId getId() {
        return id;
    }

    public void setId(RoomTypeAmenityId id) {
        this.id = id;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Amenity getAmenity() {
        return amenity;
    }

    public void setAmenity(Amenity amenity) {
        this.amenity = amenity;
    }
}
