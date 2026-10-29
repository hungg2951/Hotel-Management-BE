package com.hotel.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresInMs,
        String username,
        Long userId,
        List<String> roles,
        Long employeeId,
        String fullName,
        String email,
        String phone
) {
    public LoginResponse(String accessToken, String tokenType, Long expiresInMs, String username, Long userId, List<String> roles) {
        this(accessToken, tokenType, expiresInMs, username, userId, roles, null, null, null, null);
    }

    public LoginResponse(String accessToken, String tokenType, Long expiresInMs, String username, Long userId, List<String> roles, Long employeeId, String fullName) {
        this(accessToken, tokenType, expiresInMs, username, userId, roles, employeeId, fullName, null, null);
    }
}
