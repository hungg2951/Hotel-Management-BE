package com.hotel.billing;

import com.hotel.billing.controller.HotelServiceController;
import com.hotel.billing.dto.HotelServiceRequest;
import com.hotel.billing.dto.HotelServiceResponse;
import com.hotel.billing.service.HotelServiceService;
import com.hotel.common.enums.ServiceCategory;
import com.hotel.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceControllerTest {

    @Mock
    private HotelServiceService hotelServiceService;

    @InjectMocks
    private HotelServiceController hotelServiceController;

    @BeforeEach
    void setupRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testGetHotelServices() {
        HotelServiceResponse item = new HotelServiceResponse(1L, "Giặt ủi", ServiceCategory.LAUNDRY, "kg", 30000L, true);
        Page<HotelServiceResponse> page = new PageImpl<>(List.of(item));
        when(hotelServiceService.getHotelServices(any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<ApiResponse<Page<HotelServiceResponse>>> response =
                hotelServiceController.getHotelServices(null, null, null, PageRequest.of(0, 10));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().data().getTotalElements());
    }

    @Test
    void testCreateHotelService() {
        HotelServiceRequest request = new HotelServiceRequest("Nước suối", ServiceCategory.MINIBAR, "chai", 15000L, true);
        HotelServiceResponse created = new HotelServiceResponse(2L, "Nước suối", ServiceCategory.MINIBAR, "chai", 15000L, true);
        when(hotelServiceService.createHotelService(any(HotelServiceRequest.class))).thenReturn(created);

        ResponseEntity<ApiResponse<HotelServiceResponse>> response = hotelServiceController.createHotelService(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2L, response.getBody().data().serviceId());
    }

    @Test
    void testDeleteHotelService() {
        doNothing().when(hotelServiceService).deleteHotelService(1L);

        ResponseEntity<ApiResponse<Void>> response = hotelServiceController.deleteHotelService(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(hotelServiceService).deleteHotelService(1L);
    }
}
