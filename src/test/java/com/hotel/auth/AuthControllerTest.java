package com.hotel.auth;

import com.hotel.auth.controller.AuthController;
import com.hotel.auth.dto.LoginRequest;
import com.hotel.auth.dto.LoginResponse;
import com.hotel.auth.service.AuthService;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testLoginSuccess() {
        LoginResponse mockResponse = new LoginResponse(
                "mock-jwt-token-xyz",
                "Bearer",
                86400000L,
                "admin",
                1L,
                List.of("ROLE_ADMIN")
        );
        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        var response = authController.login(new LoginRequest("admin", "Admin@Hotel2026!"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("mock-jwt-token-xyz", response.getBody().data().accessToken());
        assertEquals("admin", response.getBody().data().username());
    }

    @Test
    void testLoginFailure_InvalidCredentials() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(BusinessException.unauthorized("Tên đăng nhập hoặc mật khẩu không chính xác", "INVALID_CREDENTIALS"));

        BusinessException thrown = assertThrows(BusinessException.class, () ->
                authController.login(new LoginRequest("admin", "wrong_password"))
        );

        var errorResponse = exceptionHandler.handleBusinessException(thrown);
        assertEquals(HttpStatus.UNAUTHORIZED, errorResponse.getStatusCode());
        assertNotNull(errorResponse.getBody());
        assertFalse(errorResponse.getBody().success());
        assertEquals("INVALID_CREDENTIALS", errorResponse.getBody().code());
    }

    @Test
    void testLoginFailure_AccountLocked() {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(BusinessException.forbidden("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên để được hỗ trợ.", "ACCOUNT_LOCKED"));

        BusinessException thrown = assertThrows(BusinessException.class, () ->
                authController.login(new LoginRequest("locked_user", "password123"))
        );

        var errorResponse = exceptionHandler.handleBusinessException(thrown);
        assertEquals(HttpStatus.FORBIDDEN, errorResponse.getStatusCode());
        assertNotNull(errorResponse.getBody());
        assertFalse(errorResponse.getBody().success());
        assertEquals("ACCOUNT_LOCKED", errorResponse.getBody().code());
        assertEquals("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên để được hỗ trợ.", errorResponse.getBody().message());
    }

    @Test
    void testChangePasswordSuccess() {
        com.hotel.common.security.CustomUserDetails userDetails = new com.hotel.common.security.CustomUserDetails(
                1L, 1L, "admin", "hash", true, List.of()
        );
        com.hotel.auth.dto.ChangePasswordRequest request = new com.hotel.auth.dto.ChangePasswordRequest(
                "oldPass123", "newPass123"
        );

        var response = authController.changePassword(userDetails, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals("Đổi mật khẩu thành công", response.getBody().message());
    }
}
