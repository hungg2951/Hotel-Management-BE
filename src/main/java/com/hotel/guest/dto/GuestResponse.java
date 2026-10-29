package com.hotel.guest.dto;

import com.hotel.common.enums.GuestGender;
import com.hotel.common.enums.GuestIdType;
import com.hotel.common.enums.GuestLoyaltyTier;
import com.hotel.guest.entity.Guest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GuestResponse(
        Long guestId,
        String fullName,
        String phone,
        String email,
        GuestGender gender,
        LocalDate dateOfBirth,
        String nationality,
        GuestIdType idType,
        String idNumber,
        String address,
        GuestLoyaltyTier loyaltyTier,
        Integer loyaltyPoints,
        LocalDateTime createdAt
) {
    public static GuestResponse fromEntity(Guest entity) {
        return new GuestResponse(
                entity.getGuestId(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getGender(),
                entity.getDateOfBirth(),
                entity.getNationality(),
                entity.getIdType(),
                entity.getIdNumber(),
                entity.getAddress(),
                entity.getLoyaltyTier(),
                entity.getLoyaltyPoints(),
                entity.getCreatedAt()
        );
    }
}
