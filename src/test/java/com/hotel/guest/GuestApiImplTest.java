package com.hotel.guest;

import com.hotel.common.enums.GuestGender;
import com.hotel.common.enums.GuestIdType;
import com.hotel.guest.api.GuestCreateCommand;
import com.hotel.guest.api.GuestInfoDto;
import com.hotel.guest.entity.Guest;
import com.hotel.guest.repository.GuestRepository;
import com.hotel.guest.service.GuestApiImpl;
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
class GuestApiImplTest {

    @Mock
    private GuestRepository guestRepository;

    @InjectMocks
    private GuestApiImpl guestApi;

    @Test
    void testFindOrCreateGuest_ReturnsExistingByIdentity() {
        Guest guest = new Guest();
        guest.setGuestId(5L);
        guest.setFullName("Tran Thi B");
        guest.setIdType(GuestIdType.CCCD);
        guest.setIdNumber("098765432109");

        GuestCreateCommand command = new GuestCreateCommand(
                "Tran Thi B", "0911222333", "b@hotel.vn",
                GuestGender.FEMALE, LocalDate.of(1995, 5, 5), "Viet Nam",
                GuestIdType.CCCD, "098765432109", "Da Nang"
        );

        when(guestRepository.findByIdTypeAndIdNumber(GuestIdType.CCCD, "098765432109"))
                .thenReturn(Optional.of(guest));

        GuestInfoDto result = guestApi.findOrCreateGuest(command);

        assertEquals(5L, result.guestId());
        assertEquals("Tran Thi B", result.fullName());
        verify(guestRepository, never()).save(any());
    }

    @Test
    void testFindOrCreateGuest_CreatesNewWhenNotFound() {
        GuestCreateCommand command = new GuestCreateCommand(
                "Le Van C", "0933444555", "c@hotel.vn",
                GuestGender.MALE, null, "Viet Nam",
                GuestIdType.PASSPORT, "B1234567", "TP HCM"
        );

        when(guestRepository.findByIdTypeAndIdNumber(GuestIdType.PASSPORT, "B1234567"))
                .thenReturn(Optional.empty());
        when(guestRepository.findByPhone("0933444555")).thenReturn(Optional.empty());
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> {
            Guest g = invocation.getArgument(0);
            g.setGuestId(12L);
            return g;
        });

        GuestInfoDto result = guestApi.findOrCreateGuest(command);

        assertNotNull(result);
        assertEquals(12L, result.guestId());
        assertEquals("Le Van C", result.fullName());
        verify(guestRepository, times(1)).save(any(Guest.class));
    }
}
