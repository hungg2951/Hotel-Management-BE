package com.hotel.booking;

import com.hotel.booking.controller.AvailabilityController;
import com.hotel.booking.dto.RoomTypeAvailabilityResponse;
import com.hotel.booking.service.BookingService;
import com.hotel.common.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private AvailabilityController availabilityController;

    @Test
    @DisplayName("Tra cứu phòng trống trả về HTTP 200 OK cùng danh sách hạng phòng")
    void testSearchAvailability() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        RoomTypeAvailabilityResponse standard = new RoomTypeAvailabilityResponse(
                1L, "Standard", "Phòng tiêu chuẩn", 2, 1, "Double",
                BigDecimal.valueOf(22), 600000L, 2, 1200000L, 3, 2, true
        );

        when(bookingService.searchAvailability(checkIn, checkOut, 2, 0))
                .thenReturn(List.of(standard));

        ResponseEntity<ApiResponse<List<RoomTypeAvailabilityResponse>>> response =
                availabilityController.searchAvailability(checkIn, checkOut, 2, 0);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(1, response.getBody().data().size());
        assertEquals("Standard", response.getBody().data().get(0).name());
        assertEquals(2, response.getBody().data().get(0).availableRoomsCount());
    }
}
