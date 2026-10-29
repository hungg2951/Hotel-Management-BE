package com.hotel.booking;

import com.hotel.booking.controller.ReservationController;
import com.hotel.booking.dto.*;
import com.hotel.booking.service.BookingService;
import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.ReservationStatus;
import com.hotel.common.response.ApiResponse;
import com.hotel.guest.api.GuestInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private ReservationController reservationController;

    private ReservationDetailResponse sampleDetail;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        GuestInfoDto guest = new GuestInfoDto(
                10L, "Nguyễn Văn An", "0901234567", "an@example.com",
                com.hotel.common.enums.GuestGender.MALE, LocalDate.of(1990, 1, 1), "Việt Nam",
                com.hotel.common.enums.GuestIdType.CCCD, "001090123456", "Hà Nội",
                com.hotel.common.enums.GuestLoyaltyTier.NONE, 0
        );

        sampleDetail = new ReservationDetailResponse(
                100L, "RES-20261028-001", guest, BookingSource.PHONE,
                ReservationStatus.PENDING, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
                2, 2, 0, 1200000L, 0L, 600000L, false,
                "Ghi chú", 1L, LocalDateTime.now(), List.of(), List.of()
        );
    }

    @Test
    @DisplayName("Tạo đơn đặt phòng trả về HTTP 201 Created kèm Header Location")
    void testCreateReservation() {
        when(bookingService.createReservation(any(ReservationCreateRequest.class))).thenReturn(sampleDetail);

        ReservationCreateRequest req = new ReservationCreateRequest(
                10L, BookingSource.PHONE, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4),
                2, 0, List.of(new ReservationRoomItemRequest(1L, null)), "Ghi chú"
        );

        ResponseEntity<ApiResponse<ReservationDetailResponse>> response = reservationController.createReservation(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains("/100"));
        assertNotNull(response.getBody());
        assertEquals(100L, response.getBody().data().reservationId());
    }

    @Test
    @DisplayName("Lấy danh sách đơn đặt phòng có phân trang thành công")
    void testGetReservations() {
        ReservationResponse res = new ReservationResponse(
                100L, "RES-20261028-001", 10L, "Nguyễn Văn An", "0901234567",
                BookingSource.PHONE, ReservationStatus.PENDING, LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(4), 2, 2, 0, 1, 1200000L, 0L, "Ghi chú", 1L, LocalDateTime.now()
        );
        Page<ReservationResponse> page = new PageImpl<>(List.of(res), PageRequest.of(0, 20), 1);

        when(bookingService.getReservations(any(), any(), any(), any(), any(), any())).thenReturn(page);

        ResponseEntity<ApiResponse<Page<ReservationResponse>>> response = reservationController.getReservations(
                null, null, null, null, null, null, PageRequest.of(0, 20)
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().data().getTotalElements());
    }

    @Test
    @DisplayName("Lấy chi tiết đơn đặt phòng theo ID thành công")
    void testGetReservationById() {
        when(bookingService.getReservationDetail(100L)).thenReturn(sampleDetail);

        ResponseEntity<ApiResponse<ReservationDetailResponse>> response = reservationController.getReservationById(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RES-20261028-001", response.getBody().data().reservationCode());
    }

    @Test
    @DisplayName("Ghi nhận đặt cọc thành công")
    void testRecordDeposit() {
        when(bookingService.recordDeposit(eq(100L), any(ReservationDepositRequest.class))).thenReturn(sampleDetail);

        ReservationDepositRequest req = new ReservationDepositRequest(600000L, PaymentMethod.TRANSFER, "REF123");
        ResponseEntity<ApiResponse<ReservationDetailResponse>> response = reservationController.recordDeposit(100L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Xác nhận đơn đặt phòng thành công")
    void testConfirmReservation() {
        when(bookingService.confirmReservation(100L)).thenReturn(sampleDetail);

        ResponseEntity<ApiResponse<ReservationDetailResponse>> response = reservationController.confirmReservation(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Hủy đơn đặt phòng thành công")
    void testCancelReservation() {
        when(bookingService.cancelReservation(eq(100L), any())).thenReturn(sampleDetail);

        ResponseEntity<ApiResponse<ReservationDetailResponse>> response = reservationController.cancelReservation(100L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("Cập nhật vắng mặt (No-Show) thành công")
    void testMarkNoShow() {
        when(bookingService.markNoShow(100L)).thenReturn(sampleDetail);

        ResponseEntity<ApiResponse<ReservationDetailResponse>> response = reservationController.markNoShow(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
