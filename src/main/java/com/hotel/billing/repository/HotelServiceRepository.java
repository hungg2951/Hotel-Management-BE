package com.hotel.billing.repository;

import com.hotel.billing.entity.HotelService;
import com.hotel.common.enums.ServiceCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HotelServiceRepository extends JpaRepository<HotelService, Long> {

    List<HotelService> findByIsActiveTrue();

    Optional<HotelService> findByName(String name);

    Optional<HotelService> findByNameAndServiceIdNot(String name, Long serviceId);

    @Query("SELECT s FROM HotelService s WHERE " +
            "(:category IS NULL OR s.category = :category) AND " +
            "(:activeOnly IS NULL OR :activeOnly = false OR s.isActive = true) AND " +
            "(:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<HotelService> findWithFilters(
            @Param("category") ServiceCategory category,
            @Param("search") String search,
            @Param("activeOnly") Boolean activeOnly,
            Pageable pageable
    );
}
