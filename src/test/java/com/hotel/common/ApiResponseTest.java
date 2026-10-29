package com.hotel.common;

import com.hotel.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void testSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("Dữ liệu thử nghiệm", "Thành công");

        assertTrue(response.success());
        assertEquals("SUCCESS", response.code());
        assertEquals("Thành công", response.message());
        assertEquals("Dữ liệu thử nghiệm", response.data());
        assertNotNull(response.timestamp());
    }

    @Test
    void testErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "Không tìm thấy tài nguyên");

        assertFalse(response.success());
        assertEquals("NOT_FOUND", response.code());
        assertEquals("Không tìm thấy tài nguyên", response.message());
        assertNull(response.data());
        assertNotNull(response.timestamp());
    }
}
