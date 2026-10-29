package com.hotel.guest.service;

import com.hotel.common.exception.BusinessException;
import com.hotel.guest.api.GuestApi;
import com.hotel.guest.api.GuestCreateCommand;
import com.hotel.guest.api.GuestInfoDto;
import com.hotel.guest.entity.Guest;
import com.hotel.guest.repository.GuestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class GuestApiImpl implements GuestApi {

    private final GuestRepository guestRepository;

    public GuestApiImpl(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GuestInfoDto getGuest(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy khách hàng có mã: " + guestId));
        return toGuestInfoDto(guest);
    }

    @Override
    @Transactional
    public GuestInfoDto findOrCreateGuest(GuestCreateCommand command) {
        // 1. Tìm theo giấy tờ định danh nếu có
        if (command.idType() != null && command.idNumber() != null && !command.idNumber().isBlank()) {
            Optional<Guest> existingById = guestRepository.findByIdTypeAndIdNumber(command.idType(), command.idNumber());
            if (existingById.isPresent()) {
                return toGuestInfoDto(existingById.get());
            }
        }

        // 2. Tìm theo số điện thoại nếu có
        if (command.phone() != null && !command.phone().isBlank()) {
            Optional<Guest> existingByPhone = guestRepository.findByPhone(command.phone());
            if (existingByPhone.isPresent()) {
                return toGuestInfoDto(existingByPhone.get());
            }
        }

        // 3. Nếu chưa tồn tại, tạo mới
        Guest guest = new Guest();
        guest.setFullName(command.fullName());
        guest.setPhone(command.phone());
        guest.setEmail(command.email());
        guest.setGender(command.gender());
        guest.setDateOfBirth(command.dateOfBirth());
        if (command.nationality() != null && !command.nationality().isBlank()) {
            guest.setNationality(command.nationality());
        }
        guest.setIdType(command.idType());
        guest.setIdNumber(command.idNumber());
        guest.setAddress(command.address());

        Guest saved = guestRepository.save(guest);
        return toGuestInfoDto(saved);
    }

    @Override
    @Transactional
    public void addLoyaltyPoints(Long guestId, Integer points) {
        if (points == null || points <= 0) {
            return;
        }
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy khách hàng có mã: " + guestId));
        guest.setLoyaltyPoints(guest.getLoyaltyPoints() + points);
        guestRepository.save(guest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findGuestIdsByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        return guestRepository.findGuestIdsByKeyword(keyword.trim());
    }

    private GuestInfoDto toGuestInfoDto(Guest guest) {
        return new GuestInfoDto(
                guest.getGuestId(),
                guest.getFullName(),
                guest.getPhone(),
                guest.getEmail(),
                guest.getGender(),
                guest.getDateOfBirth(),
                guest.getNationality(),
                guest.getIdType(),
                guest.getIdNumber(),
                guest.getAddress(),
                guest.getLoyaltyTier(),
                guest.getLoyaltyPoints()
        );
    }
}
