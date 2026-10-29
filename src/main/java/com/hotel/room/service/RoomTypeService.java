package com.hotel.room.service;

import com.hotel.common.exception.BusinessException;
import com.hotel.room.dto.RoomTypeRequest;
import com.hotel.room.dto.RoomTypeResponse;
import com.hotel.room.entity.RoomType;
import com.hotel.room.repository.RoomTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;

    public RoomTypeService(RoomTypeRepository roomTypeRepository) {
        this.roomTypeRepository = roomTypeRepository;
    }

    @Transactional(readOnly = true)
    public Page<RoomTypeResponse> getRoomTypes(Boolean activeOnly, Pageable pageable) {
        Page<RoomType> page;
        if (Boolean.TRUE.equals(activeOnly)) {
            page = roomTypeRepository.findByIsActiveTrue(pageable);
        } else {
            page = roomTypeRepository.findAll(pageable);
        }
        return page.map(RoomTypeResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> getAllActiveRoomTypes() {
        return roomTypeRepository.findByIsActiveTrue().stream()
                .map(RoomTypeResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomTypeResponse getRoomTypeById(Long id) {
        RoomType roomType = findEntityById(id);
        return RoomTypeResponse.fromEntity(roomType);
    }

    @Transactional
    public RoomTypeResponse createRoomType(RoomTypeRequest request) {
        if (roomTypeRepository.findByName(request.name()).isPresent()) {
            throw BusinessException.duplicate("Hạng phòng '" + request.name() + "' đã tồn tại trong khách sạn");
        }

        RoomType roomType = new RoomType();
        populateEntity(roomType, request);
        roomType.setIsActive(request.isActive() != null ? request.isActive() : true);

        RoomType saved = roomTypeRepository.save(roomType);
        return RoomTypeResponse.fromEntity(saved);
    }

    @Transactional
    public RoomTypeResponse updateRoomType(Long id, RoomTypeRequest request) {
        RoomType roomType = findEntityById(id);

        if (roomTypeRepository.findByNameAndRoomTypeIdNot(request.name(), id).isPresent()) {
            throw BusinessException.duplicate("Tên hạng phòng '" + request.name() + "' đã được sử dụng bởi hạng phòng khác");
        }

        populateEntity(roomType, request);
        if (request.isActive() != null) {
            roomType.setIsActive(request.isActive());
        }

        RoomType updated = roomTypeRepository.save(roomType);
        return RoomTypeResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteRoomType(Long id) {
        RoomType roomType = findEntityById(id);
        roomType.setIsActive(false);
        roomTypeRepository.save(roomType);
    }

    public RoomType findEntityById(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy hạng phòng có mã: " + id));
    }

    private void populateEntity(RoomType entity, RoomTypeRequest request) {
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setMaxAdults(request.maxAdults());
        entity.setMaxChildren(request.maxChildren());
        entity.setBedType(request.bedType());
        entity.setAreaSqm(request.areaSqm());
        entity.setBasePrice(request.basePrice());
    }
}
