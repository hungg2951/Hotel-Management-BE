package com.hotel.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
        String fullName,

        @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
        String email,

        @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
        String phone
) {}

