package com.hotel.room;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.common.exception.BusinessException;
import com.hotel.room.dto.RoomRequest;
import com.hotel.room.dto.RoomResponse;
import com.hotel.room.dto.RoomStatusUpdateRequest;
import com.hotel.room.entity.Room;
import com.hotel.room.entity.RoomType;
import com.hotel.room.repository.RoomRepository;
import com.hotel.room.repository.RoomTypeRepository;
import com.hotel.room.service.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomService roomService;

    private Room room;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        roomType = new RoomType();
        roomType.setRoomTypeId(1L);
        roomType.setName("Deluxe");

        room = new Room();
        room.setRoomId(101L);
        room.setRoomNumber("101");
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        room.setIsActive(true);
    }

    @Test
    void testCreateRoom_Success() {
        RoomRequest request = new RoomRequest(1L, "102", 1, "Phòng hướng biển", true);

        when(roomRepository.findByRoomNumber("102")).thenReturn(Optional.empty());
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> {
            Room r = invocation.getArgument(0);
            r.setRoomId(102L);
            return r;
        });

        RoomResponse response = roomService.createRoom(request);

        assertNotNull(response);
        assertEquals("102", response.roomNumber());
        assertEquals(RoomStatus.AVAILABLE, response.status());
        assertEquals(HousekeepingStatus.CLEAN, response.housekeepingStatus());
    }

    @Test
    void testCreateRoom_DuplicateNumber_ThrowsBusinessException() {
        RoomRequest request = new RoomRequest(1L, "101", 1, null, true);

        when(roomRepository.findByRoomNumber("101")).thenReturn(Optional.of(room));

        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.createRoom(request));
        assertEquals("DUPLICATE", ex.getErrorCode());
    }

    @Test
    void testUpdateRoomStatus_AvailableToMaintenance_Success() {
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        RoomStatusUpdateRequest request = new RoomStatusUpdateRequest(RoomStatus.MAINTENANCE, "Hỏng điều hòa");
        RoomResponse response = roomService.updateRoomStatus(101L, request);

        assertEquals(RoomStatus.MAINTENANCE, response.status());
    }

    @Test
    void testUpdateRoomStatus_CannotChangeOccupiedRoom_ThrowsBusinessException() {
        room.setStatus(RoomStatus.OCCUPIED);
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        RoomStatusUpdateRequest request = new RoomStatusUpdateRequest(RoomStatus.MAINTENANCE, "Bảo trì");

        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.updateRoomStatus(101L, request));
        assertEquals("INVALID_STATE", ex.getErrorCode());
    }

    @Test
    void testUpdateRoomStatus_CannotSetOccupiedManually_ThrowsBusinessException() {
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        RoomStatusUpdateRequest request = new RoomStatusUpdateRequest(RoomStatus.OCCUPIED, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> roomService.updateRoomStatus(101L, request));
        assertEquals("INVALID_STATE", ex.getErrorCode());
    }
}
