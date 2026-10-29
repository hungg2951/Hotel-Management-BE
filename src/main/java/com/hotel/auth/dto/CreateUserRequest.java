package com.hotel.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Tên đăng nhập không được để trống")
        String username,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, message = "Mật khẩu phải có tối thiểu 6 ký tự")
        String password,

        @NotBlank(message = "Vai trò không được để trống")
        String role,

        @NotBlank(message = "Họ tên không được để trống")
        String fullName,

        String email,
        String phone,
        String position
) {}
