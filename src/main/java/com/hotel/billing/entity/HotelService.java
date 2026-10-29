package com.hotel.billing.entity;

import com.hotel.common.enums.ServiceCategory;
import jakarta.persistence.*;

@Entity
@Table(name = "hotel_service")
public class HotelService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 15)
    private ServiceCategory category = ServiceCategory.OTHER;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit = "lan";

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public HotelService() {
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceCategory getCategory() {
        return category;
    }

    public void setCategory(ServiceCategory category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
