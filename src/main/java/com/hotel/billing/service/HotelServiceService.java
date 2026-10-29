package com.hotel.billing.service;

import com.hotel.billing.dto.HotelServiceRequest;
import com.hotel.billing.dto.HotelServiceResponse;
import com.hotel.billing.entity.HotelService;
import com.hotel.billing.repository.HotelServiceRepository;
import com.hotel.common.enums.ServiceCategory;
import com.hotel.common.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelServiceService {

    private final HotelServiceRepository hotelServiceRepository;

    public HotelServiceService(HotelServiceRepository hotelServiceRepository) {
        this.hotelServiceRepository = hotelServiceRepository;
    }

    @Transactional(readOnly = true)
    public Page<HotelServiceResponse> getHotelServices(
            ServiceCategory category,
            String search,
            Boolean activeOnly,
            Pageable pageable
    ) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return hotelServiceRepository.findWithFilters(category, trimmedSearch, activeOnly, pageable)
                .map(HotelServiceResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public HotelServiceResponse getHotelServiceById(Long id) {
        HotelService service = findEntityById(id);
        return HotelServiceResponse.fromEntity(service);
    }

    @Transactional
    public HotelServiceResponse createHotelService(HotelServiceRequest request) {
        String trimmedName = request.name().trim();
        if (hotelServiceRepository.findByName(trimmedName).isPresent()) {
            throw BusinessException.duplicate("Dịch vụ với tên '" + trimmedName + "' đã tồn tại");
        }

        HotelService service = new HotelService();
        service.setName(trimmedName);
        service.setCategory(request.category());
        service.setUnit(request.unit().trim());
        service.setUnitPrice(request.unitPrice());
        service.setIsActive(request.isActive() != null ? request.isActive() : true);

        HotelService saved = hotelServiceRepository.save(service);
        return HotelServiceResponse.fromEntity(saved);
    }

    @Transactional
    public HotelServiceResponse updateHotelService(Long id, HotelServiceRequest request) {
        HotelService service = findEntityById(id);
        String trimmedName = request.name().trim();

        if (hotelServiceRepository.findByNameAndServiceIdNot(trimmedName, id).isPresent()) {
            throw BusinessException.duplicate("Tên dịch vụ '" + trimmedName + "' đã được sử dụng bởi dịch vụ khác");
        }

        service.setName(trimmedName);
        service.setCategory(request.category());
        service.setUnit(request.unit().trim());
        service.setUnitPrice(request.unitPrice());
        if (request.isActive() != null) {
            service.setIsActive(request.isActive());
        }

        HotelService updated = hotelServiceRepository.save(service);
        return HotelServiceResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteHotelService(Long id) {
        HotelService service = findEntityById(id);
        service.setIsActive(false);
        hotelServiceRepository.save(service);
    }

    public HotelService findEntityById(Long id) {
        return hotelServiceRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy dịch vụ khách sạn có mã: " + id));
    }
}
