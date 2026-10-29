package com.hotel.room;

import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.room.entity.Room;
import com.hotel.room.entity.RoomType;
import com.hotel.room.repository.RoomRepository;
import com.hotel.room.repository.RoomTypeRepository;
import com.hotel.room.service.RoomApiImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomApiImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomTypeRepository roomTypeRepository;

    @InjectMocks
    private RoomApiImpl roomApi;

    private Room room;
    private RoomType roomType;

    @BeforeEach
    void setUp() {
        roomType = new RoomType();
        roomType.setRoomTypeId(1L);
        roomType.setName("Standard");
        roomType.setBasePrice(500000L);

        room = new Room();
        room.setRoomId(101L);
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setHousekeepingStatus(HousekeepingStatus.CLEAN);
        room.setIsActive(true);
    }

    @Test
    void testIsRoomReadyForCheckIn_TrueWhenAvailableAndClean() {
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        assertTrue(roomApi.isRoomReadyForCheckIn(101L));
    }

    @Test
    void testIsRoomReadyForCheckIn_FalseWhenDirty() {
        room.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        when(roomRepository.findById(101L)).thenReturn(Optional.of(room));

        assertFalse(roomApi.isRoomReadyForCheckIn(101L));
    }

    @Test
    void testCalculateNightPrice_ReturnsBasePrice() {
        LocalDate date = LocalDate.of(2026, 6, 8);
        when(roomTypeRepository.findById(1L)).thenReturn(Optional.of(roomType));

        Long price = roomApi.calculateNightPrice(1L, date);
        assertEquals(500000L, price);
    }
}
