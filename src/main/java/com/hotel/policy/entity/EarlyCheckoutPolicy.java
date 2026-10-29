package com.hotel.policy.entity;

import com.hotel.common.enums.EarlyCheckoutPenaltyType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "early_checkout_policy")
public class EarlyCheckoutPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "policy_code", nullable = false, unique = true, length = 30)
    private String policyCode;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 30)
    private EarlyCheckoutPenaltyType penaltyType = EarlyCheckoutPenaltyType.NONE;

    @Column(name = "penalty_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal penaltyValue = BigDecimal.ZERO;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public EarlyCheckoutPolicy() {
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyCode() {
        return policyCode;
    }

    public void setPolicyCode(String policyCode) {
        this.policyCode = policyCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EarlyCheckoutPenaltyType getPenaltyType() {
        return penaltyType;
    }

    public void setPenaltyType(EarlyCheckoutPenaltyType penaltyType) {
        this.penaltyType = penaltyType;
    }

    public BigDecimal getPenaltyValue() {
        return penaltyValue;
    }

    public void setPenaltyValue(BigDecimal penaltyValue) {
        this.penaltyValue = penaltyValue;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean aDefault) {
        isDefault = aDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

