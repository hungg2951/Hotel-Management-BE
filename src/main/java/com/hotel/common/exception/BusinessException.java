package com.hotel.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ nghiệp vụ dùng chung, chứa mã lỗi và mã trạng thái HTTP tương ứng.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BusinessException(HttpStatus status, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static BusinessException notFound(String message, String errorCode) {
        return new BusinessException(HttpStatus.NOT_FOUND, errorCode, message);
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static BusinessException badRequest(String message, String errorCode) {
        return new BusinessException(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public static BusinessException validationError(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public static BusinessException invalidDateRange(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", message);
    }

    public static BusinessException overCapacity(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "OVER_CAPACITY", message);
    }

    public static BusinessException promotionInvalid(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "PROMOTION_INVALID", message);
    }

    public static BusinessException invalidState(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_STATE", message);
    }

    public static BusinessException roomNotAvailable(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "ROOM_NOT_AVAILABLE", message);
    }

    public static BusinessException roomNotReady(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "ROOM_NOT_READY", message);
    }

    public static BusinessException folioNotSettled(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "FOLIO_NOT_SETTLED", message);
    }

    public static BusinessException paymentExceedsBalance(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "PAYMENT_EXCEEDS_BALANCE", message);
    }

    public static BusinessException duplicate(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE", message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static BusinessException conflict(String message, String errorCode) {
        return new BusinessException(HttpStatus.CONFLICT, errorCode, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static BusinessException forbidden(String message, String errorCode) {
        return new BusinessException(HttpStatus.FORBIDDEN, errorCode, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static BusinessException unauthorized(String message, String errorCode) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, errorCode, message);
    }
}
