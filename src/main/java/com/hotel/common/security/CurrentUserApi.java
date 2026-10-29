package com.hotel.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Tiện ích truy xuất thông tin tài khoản người dùng đang đăng nhập trong phiên làm việc hiện tại.
 */
public final class CurrentUserApi {

    private CurrentUserApi() {
    }

    public static Optional<CustomUserDetails> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(CustomUserDetails::getUserId);
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentUser().map(CustomUserDetails::getUsername);
    }

    public static Optional<Long> getCurrentHotelId() {
        return getCurrentUser().map(CustomUserDetails::getHotelId);
    }

    public static Optional<Long> getCurrentEmployeeId() {
        return getCurrentUser().map(CustomUserDetails::getEmployeeId);
    }
}
