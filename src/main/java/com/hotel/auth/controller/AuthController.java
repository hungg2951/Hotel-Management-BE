package com.hotel.auth.controller;

import com.hotel.auth.dto.ChangePasswordRequest;
import com.hotel.auth.dto.LoginRequest;
import com.hotel.auth.dto.LoginResponse;
import com.hotel.auth.dto.UpdateProfileRequest;
import com.hotel.auth.service.AuthService;
import com.hotel.common.response.ApiResponse;
import com.hotel.common.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> me(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LoginResponse response = authService.getCurrentUserProfile(userDetails);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin tài khoản thành công"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<LoginResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        LoginResponse response = authService.updateCurrentUserProfile(userDetails, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin cá nhân thành công"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }
}
