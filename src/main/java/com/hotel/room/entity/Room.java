package com.hotel.room.entity;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "room_number", nullable = false, unique = true, length = 10)
    private String roomNumber;

    @Column(name = "floor_no")
    private Integer floorNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "housekeeping_status", nullable = false, length = 20)
    private HousekeepingStatus housekeepingStatus = HousekeepingStatus.CLEAN;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "note", length = 500)
    private String note;

    public Room() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Integer getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Integer floorNo) {
        this.floorNo = floorNo;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public HousekeepingStatus getHousekeepingStatus() {
        return housekeepingStatus;
    }

    public void setHousekeepingStatus(HousekeepingStatus housekeepingStatus) {
        this.housekeepingStatus = housekeepingStatus;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
