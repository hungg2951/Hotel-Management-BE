package com.hotel.guest.service;

import com.hotel.common.exception.BusinessException;
import com.hotel.guest.dto.GuestRequest;
import com.hotel.guest.dto.GuestResponse;
import com.hotel.guest.entity.Guest;
import com.hotel.guest.repository.GuestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestService {

    private final GuestRepository guestRepository;

    public GuestService(GuestRepository guestRepository) {
        this.guestRepository = guestRepository;
    }

    @Transactional(readOnly = true)
    public Page<GuestResponse> searchGuests(String keyword, String idNumber, String phone, String email, Pageable pageable) {
        return guestRepository.searchGuests(keyword, idNumber, phone, email, pageable)
                .map(GuestResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public GuestResponse getGuestById(Long id) {
        Guest guest = findEntityById(id);
        return GuestResponse.fromEntity(guest);
    }

    @Transactional
    public GuestResponse createGuest(GuestRequest request) {
        if (request.idType() != null && request.idNumber() != null && !request.idNumber().isBlank()) {
            if (guestRepository.findByIdTypeAndIdNumber(request.idType(), request.idNumber()).isPresent()) {
                throw BusinessException.duplicate(
                        "Thông tin giấy tờ định danh (" + request.idType() + ": " + request.idNumber() + ") đã tồn tại trong hệ thống"
                );
            }
        }

        Guest guest = new Guest();
        populateEntity(guest, request);

        Guest saved = guestRepository.save(guest);
        return GuestResponse.fromEntity(saved);
    }

    @Transactional
    public GuestResponse updateGuest(Long id, GuestRequest request) {
        Guest guest = findEntityById(id);

        if (request.idType() != null && request.idNumber() != null && !request.idNumber().isBlank()) {
            if (guestRepository.findByIdTypeAndIdNumberAndGuestIdNot(request.idType(), request.idNumber(), id).isPresent()) {
                throw BusinessException.duplicate(
                        "Thông tin giấy tờ định danh (" + request.idType() + ": " + request.idNumber() + ") đã được đăng ký bởi khách hàng khác"
                );
            }
        }

        populateEntity(guest, request);

        Guest updated = guestRepository.save(guest);
        return GuestResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteGuest(Long id) {
        Guest guest = findEntityById(id);
        guestRepository.delete(guest);
    }

    public Guest findEntityById(Long id) {
        return guestRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy thông tin khách hàng có mã: " + id));
    }

    private void populateEntity(Guest guest, GuestRequest request) {
        guest.setFullName(request.fullName());
        guest.setPhone(request.phone());
        guest.setEmail(request.email());
        guest.setGender(request.gender());
        guest.setDateOfBirth(request.dateOfBirth());
        if (request.nationality() != null && !request.nationality().isBlank()) {
            guest.setNationality(request.nationality());
        }
        guest.setIdType(request.idType());
        guest.setIdNumber(request.idNumber());
        guest.setAddress(request.address());
    }
}
