package com.hotel.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Cấu trúc phản hồi chuẩn (API Response) dùng chung cho toàn bộ hệ thống.
 *
 * @param <T> Kiểu dữ liệu trả về
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, "SUCCESS", message, data, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Thành công");
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, code, message, null, Instant.now());
    }

    public static <T> ApiResponse<T> error(String code, String message, T errorDetails) {
        return new ApiResponse<>(false, code, message, errorDetails, Instant.now());
    }
}
