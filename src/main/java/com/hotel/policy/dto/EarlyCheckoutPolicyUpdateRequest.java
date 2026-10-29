package com.hotel.policy.dto;

import com.hotel.common.enums.EarlyCheckoutPenaltyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EarlyCheckoutPolicyUpdateRequest(
        @NotBlank(message = "Tên chính sách không được để trống")
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String description,

        @NotNull(message = "Hình thức phạt là bắt buộc")
        EarlyCheckoutPenaltyType penaltyType,

        BigDecimal penaltyValue
) {
}

