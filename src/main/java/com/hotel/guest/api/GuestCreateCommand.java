package com.hotel.guest.api;

import com.hotel.common.enums.GuestGender;
import com.hotel.common.enums.GuestIdType;

import java.time.LocalDate;

public record GuestCreateCommand(
        String fullName,
        String phone,
        String email,
        GuestGender gender,
        LocalDate dateOfBirth,
        String nationality,
        GuestIdType idType,
        String idNumber,
        String address
) {
}
