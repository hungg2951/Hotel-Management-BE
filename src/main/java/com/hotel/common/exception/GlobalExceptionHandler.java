package com.hotel.common.exception;

import com.hotel.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý ngoại lệ tập trung toàn ứng dụng.
 * Chuyển đổi mọi lỗi phát sinh thành cấu trúc ApiResponse chuẩn với thông báo tiếng Việt.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Lỗi nghiệp vụ [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage() != null ? error.getDefaultMessage() : "Không hợp lệ");
        }
        log.warn("Dữ liệu yêu cầu không hợp lệ: {}", fieldErrors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", "Dữ liệu yêu cầu không hợp lệ", fieldErrors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String rootMsg = ex.getRootCause() != null ? ex.getRootCause().getMessage() : ex.getMessage();
        log.error("Vi phạm toàn vẹn dữ liệu cơ sở dữ liệu: {}", rootMsg, ex);

        if (rootMsg != null) {
            if (rootMsg.contains("ORA-00001")) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("DUPLICATE", "Dữ liệu đã tồn tại hoặc bị trùng lặp trong hệ thống (ORA-00001)"));
            }
            if (rootMsg.contains("ORA-02292")) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("DATA_INTEGRITY_VIOLATION", "Không thể thao tác vì dữ liệu đang được tham chiếu tại bảng khác (ORA-02292)"));
            }
            if (rootMsg.contains("ORA-02291")) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("DATA_INTEGRITY_VIOLATION", "Thông tin tham chiếu không tồn tại trong hệ thống (ORA-02291)"));
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DATA_INTEGRITY_VIOLATION", "Dữ liệu không đáp ứng các ràng buộc toàn vẹn của hệ thống"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Từ chối quyền truy cập: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Bạn không có quyền truy cập chức năng này"));
    }

    @ExceptionHandler({org.springframework.security.authentication.DisabledException.class, org.springframework.security.authentication.LockedException.class})
    public ResponseEntity<ApiResponse<Void>> handleAccountLockedException(Exception ex) {
        log.warn("Tài khoản bị vô hiệu hóa hoặc bị khóa: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("ACCOUNT_LOCKED", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Quản trị viên để được hỗ trợ."));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Lỗi xác thực: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHORIZED", "Xác thực không thành công hoặc thông tin đăng nhập không chính xác"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Lỗi hệ thống chưa được phân loại: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_SERVER_ERROR", "Đã xảy ra lỗi hệ thống, vui lòng thử lại sau."));
    }
}
