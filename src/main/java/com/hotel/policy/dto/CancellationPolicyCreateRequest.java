package com.hotel.policy.dto;

import com.hotel.common.enums.CancellationPenaltyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CancellationPolicyCreateRequest(
        @NotBlank(message = "Mã chính sách không được để trống")
        @Size(max = 30)
        String policyCode,

        @NotBlank(message = "Tên chính sách không được để trống")
        @Size(max = 150)
        String name,

        @Size(max = 500)
        String description,

        @NotNull(message = "Số giờ hủy miễn phí là bắt buộc")
        Integer freeCancelHours,

        @NotNull(message = "Hình thức phạt là bắt buộc")
        CancellationPenaltyType penaltyType,

        BigDecimal penaltyValue,

        Boolean isDefault,

        Boolean isActive
) {
}

