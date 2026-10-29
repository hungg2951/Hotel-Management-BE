package com.hotel.room;

import com.hotel.room.controller.RoomTypeController;
import com.hotel.room.dto.RoomTypeResponse;
import com.hotel.room.service.RoomTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomTypeControllerTest {

    @Mock
    private RoomTypeService roomTypeService;

    @InjectMocks
    private RoomTypeController roomTypeController;

    @Test
    void testGetAllRoomTypes() {
        List<RoomTypeResponse> mockList = List.of(
                new RoomTypeResponse(1L, "Standard", "Phòng tiêu chuẩn", 2, 1, "Double", BigDecimal.valueOf(22.0), 600000L, true),
                new RoomTypeResponse(2L, "Deluxe", "Phòng cao cấp", 2, 2, "King", BigDecimal.valueOf(30.0), 900000L, true),
                new RoomTypeResponse(3L, "Suite", "Phòng suite", 3, 2, "King", BigDecimal.valueOf(48.0), 1600000L, true)
        );
        when(roomTypeService.getAllActiveRoomTypes()).thenReturn(mockList);

        var response = roomTypeController.getAllRoomTypes(true, true, org.springframework.data.domain.Pageable.unpaged());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        @SuppressWarnings("unchecked")
        List<RoomTypeResponse> data = (List<RoomTypeResponse>) response.getBody().data();
        assertEquals(3, data.size());
        assertEquals("Standard", data.get(0).name());
        assertEquals("Deluxe", data.get(1).name());
        assertEquals("Suite", data.get(2).name());
    }
}
