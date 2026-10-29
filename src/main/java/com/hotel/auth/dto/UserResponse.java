package com.hotel.auth.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        String role,
        String position,
        boolean isActive,
        LocalDateTime createdAt
) {}
