package com.hotel.common;

import com.hotel.common.exception.BusinessException;
import com.hotel.common.exception.GlobalExceptionHandler;
import com.hotel.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleBusinessException() {
        BusinessException ex = BusinessException.notFound("Không tìm thấy phòng với ID: 101");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleBusinessException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("NOT_FOUND", response.getBody().code());
        assertEquals("Không tìm thấy phòng với ID: 101", response.getBody().message());
    }

    @Test
    void testHandleDataIntegrityViolation_ORA00001_Duplicate() {
        SQLException sqlEx = new SQLException("ORA-00001: unique constraint (HOTEL_APP.PK_ROOM_NIGHT) violated");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Conflict", sqlEx);

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("DUPLICATE", response.getBody().code());
    }

    @Test
    void testHandleGeneralException() {
        Exception ex = new RuntimeException("Lỗi ngầm");

        ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleGeneralException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_SERVER_ERROR", response.getBody().code());
        assertFalse(response.getBody().message().contains("Lỗi ngầm"));
    }
}
