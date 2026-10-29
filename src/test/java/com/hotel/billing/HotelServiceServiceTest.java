package com.hotel.billing;

import com.hotel.billing.dto.HotelServiceRequest;
import com.hotel.billing.dto.HotelServiceResponse;
import com.hotel.billing.entity.HotelService;
import com.hotel.billing.repository.HotelServiceRepository;
import com.hotel.billing.service.HotelServiceService;
import com.hotel.common.enums.ServiceCategory;
import com.hotel.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceServiceTest {

    @Mock
    private HotelServiceRepository hotelServiceRepository;

    @InjectMocks
    private HotelServiceService hotelServiceService;

    private HotelService service;

    @BeforeEach
    void setUp() {
        service = new HotelService();
        service.setServiceId(1L);
        service.setName("Giặt ủi");
        service.setCategory(ServiceCategory.LAUNDRY);
        service.setUnit("kg");
        service.setUnitPrice(30000L);
        service.setIsActive(true);
    }

    @Test
    void testCreateHotelService_Success() {
        HotelServiceRequest request = new HotelServiceRequest("Nước suối", ServiceCategory.MINIBAR, "chai", 15000L, true);

        when(hotelServiceRepository.findByName("Nước suối")).thenReturn(Optional.empty());
        when(hotelServiceRepository.save(any(HotelService.class))).thenAnswer(inv -> {
            HotelService s = inv.getArgument(0);
            s.setServiceId(2L);
            return s;
        });

        HotelServiceResponse response = hotelServiceService.createHotelService(request);

        assertNotNull(response);
        assertEquals(2L, response.serviceId());
        assertEquals("Nước suối", response.name());
        assertEquals(ServiceCategory.MINIBAR, response.category());
        assertEquals(15000L, response.unitPrice());
        assertTrue(response.isActive());
    }

    @Test
    void testCreateHotelService_DuplicateName_ThrowsBusinessException() {
        HotelServiceRequest request = new HotelServiceRequest("Giặt ủi", ServiceCategory.LAUNDRY, "kg", 35000L, true);

        when(hotelServiceRepository.findByName("Giặt ủi")).thenReturn(Optional.of(service));

        BusinessException ex = assertThrows(BusinessException.class, () -> hotelServiceService.createHotelService(request));
        assertEquals("DUPLICATE", ex.getErrorCode());
    }

    @Test
    void testUpdateHotelService_Success() {
        HotelServiceRequest request = new HotelServiceRequest("Giặt ủi cao cấp", ServiceCategory.LAUNDRY, "kg", 40000L, true);

        when(hotelServiceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(hotelServiceRepository.findByNameAndServiceIdNot("Giặt ủi cao cấp", 1L)).thenReturn(Optional.empty());
        when(hotelServiceRepository.save(any(HotelService.class))).thenReturn(service);

        HotelServiceResponse response = hotelServiceService.updateHotelService(1L, request);

        assertNotNull(response);
        assertEquals("Giặt ủi cao cấp", service.getName());
        assertEquals(40000L, service.getUnitPrice());
    }

    @Test
    void testUpdateHotelService_DuplicateName_ThrowsBusinessException() {
        HotelServiceRequest request = new HotelServiceRequest("Bữa sáng", ServiceCategory.FOOD, "suất", 80000L, true);

        when(hotelServiceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(hotelServiceRepository.findByNameAndServiceIdNot("Bữa sáng", 1L)).thenReturn(Optional.of(new HotelService()));

        BusinessException ex = assertThrows(BusinessException.class, () -> hotelServiceService.updateHotelService(1L, request));
        assertEquals("DUPLICATE", ex.getErrorCode());
    }

    @Test
    void testDeleteHotelService_SoftDelete_SetsIsActiveFalse() {
        when(hotelServiceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(hotelServiceRepository.save(any(HotelService.class))).thenReturn(service);

        hotelServiceService.deleteHotelService(1L);

        assertFalse(service.getIsActive());
        verify(hotelServiceRepository).save(service);
    }

    @Test
    void testGetHotelServices_Filter_Success() {
        Page<HotelService> page = new PageImpl<>(List.of(service));
        when(hotelServiceRepository.findWithFilters(eq(ServiceCategory.LAUNDRY), isNull(), eq(true), any())).thenReturn(page);

        Page<HotelServiceResponse> result = hotelServiceService.getHotelServices(ServiceCategory.LAUNDRY, null, true, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Giặt ủi", result.getContent().get(0).name());
    }
}
