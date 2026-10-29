package com.hotel.guest;

import com.hotel.common.enums.GuestGender;
import com.hotel.common.enums.GuestIdType;
import com.hotel.common.exception.BusinessException;
import com.hotel.guest.dto.GuestRequest;
import com.hotel.guest.dto.GuestResponse;
import com.hotel.guest.entity.Guest;
import com.hotel.guest.repository.GuestRepository;
import com.hotel.guest.service.GuestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceTest {

    @Mock
    private GuestRepository guestRepository;

    @InjectMocks
    private GuestService guestService;

    private Guest guest;

    @BeforeEach
    void setUp() {
        guest = new Guest();
        guest.setGuestId(1L);
        guest.setFullName("Nguyen Van A");
        guest.setPhone("0901234567");
        guest.setIdType(GuestIdType.CCCD);
        guest.setIdNumber("012345678901");
    }

    @Test
    void testCreateGuest_Success() {
        GuestRequest request = new GuestRequest(
                "Nguyen Van A", "0901234567", "a@example.com",
                GuestGender.MALE, LocalDate.of(1990, 1, 1), "Viet Nam",
                GuestIdType.CCCD, "012345678901", "Ha Noi"
        );

        when(guestRepository.findByIdTypeAndIdNumber(GuestIdType.CCCD, "012345678901"))
                .thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> {
            Guest g = invocation.getArgument(0);
            g.setGuestId(1L);
            return g;
        });

        GuestResponse response = guestService.createGuest(request);

        assertNotNull(response);
        assertEquals("Nguyen Van A", response.fullName());
        assertEquals("012345678901", response.idNumber());
    }

    @Test
    void testCreateGuest_DuplicateIdNumber_ThrowsBusinessException() {
        GuestRequest request = new GuestRequest(
                "Nguyen Van B", "0909999999", "b@example.com",
                GuestGender.MALE, null, "Viet Nam",
                GuestIdType.CCCD, "012345678901", "Ha Noi"
        );

        when(guestRepository.findByIdTypeAndIdNumber(GuestIdType.CCCD, "012345678901"))
                .thenReturn(Optional.of(guest));

        BusinessException ex = assertThrows(BusinessException.class, () -> guestService.createGuest(request));
        assertEquals("DUPLICATE", ex.getErrorCode());
    }
}
