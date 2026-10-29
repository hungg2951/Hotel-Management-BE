package com.hotel.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank(message = "Vai trò không được để trống")
        String role,

        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        String email,
        String phone,
        String position
) {}
