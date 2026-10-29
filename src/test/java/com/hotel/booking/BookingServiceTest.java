package com.hotel.booking;

import com.hotel.billing.api.BillingApi;
import com.hotel.booking.dto.*;
import com.hotel.booking.entity.Reservation;
import com.hotel.booking.entity.ReservationRoom;
import com.hotel.booking.repository.ReservationRepository;
import com.hotel.booking.repository.ReservationRoomRepository;
import com.hotel.booking.repository.RoomNightRepository;
import com.hotel.booking.service.BookingService;
import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.ReservationRoomStatus;
import com.hotel.common.enums.ReservationStatus;
import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.common.exception.BusinessException;
import com.hotel.guest.api.GuestApi;
import com.hotel.guest.api.GuestInfoDto;
import com.hotel.room.api.RoomApi;
import com.hotel.room.api.RoomInfoDto;
import com.hotel.room.api.RoomTypeInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationRoomRepository reservationRoomRepository;
    @Mock private RoomNightRepository roomNightRepository;
    @Mock private RoomApi roomApi;
    @Mock private GuestApi guestApi;
    @Mock private BillingApi billingApi;

    private BookingService bookingService;

    private GuestInfoDto guest;
    private RoomTypeInfoDto standardRoomType;
    private RoomInfoDto room101;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                reservationRepository,
                reservationRoomRepository,
                roomNightRepository,
                roomApi,
                guestApi,
                billingApi,
                1, // minDepositNights
                3  // freeCancelDays
        );

        guest = new GuestInfoDto(
                10L, "Nguyễn Văn An", "0901234567", "an@example.com",
                com.hotel.common.enums.GuestGender.MALE, LocalDate.of(1990, 1, 1), "Việt Nam",
                com.hotel.common.enums.GuestIdType.CCCD, "001090123456", "Hà Nội",
                com.hotel.common.enums.GuestLoyaltyTier.NONE, 0
        );

        standardRoomType = new RoomTypeInfoDto(
                1L, "Standard", "Phòng tiêu chuẩn", 2, 1,
                "Double", BigDecimal.valueOf(22), 600000L, true
        );

        room101 = new RoomInfoDto(
                101L, 1L, "101", 1,
                RoomStatus.AVAILABLE, HousekeepingStatus.CLEAN, true, null
        );
    }

    @Test
    @DisplayName("Đặt phòng nhiều đêm thành công và tính giá chính xác")
    void testCreateReservation_Success_MultiNightPricing() {
        LocalDate checkIn = LocalDate.now().plusDays(2);
        LocalDate checkOut = LocalDate.now().plusDays(5); // 3 đêm
        long nights = 3;

        when(guestApi.getGuest(10L)).thenReturn(guest);
        when(roomNightRepository.findOccupiedRoomIdsBetween(checkIn, checkOut)).thenReturn(Collections.emptyList());
        when(roomApi.getRoomType(1L)).thenReturn(standardRoomType);
        when(roomApi.getActiveRoomsByRoomType(1L)).thenReturn(List.of(room101));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setReservationId(500L);
            return r;
        });

        when(reservationRoomRepository.save(any(ReservationRoom.class))).thenAnswer(invocation -> {
            ReservationRoom rr = invocation.getArgument(0);
            rr.setReservationRoomId(1001L);
            return rr;
        });

        ReservationCreateRequest request = new ReservationCreateRequest(
                10L, BookingSource.PHONE, checkIn, checkOut, 2, 0,
                List.of(new ReservationRoomItemRequest(1L, null)),
                "Khách thích phòng thoáng"
        );

        ReservationDetailResponse response = bookingService.createReservation(request);

        assertNotNull(response);
        assertEquals(500L, response.reservationId());
        assertEquals(ReservationStatus.PENDING, response.status());
        assertEquals(3, response.nights());
        assertEquals(600000L * nights, response.totalAmount());
        assertEquals(1, response.rooms().size());
        assertEquals(101L, response.rooms().get(0).roomId());

        // Kiểm tra đã lưu 3 bản ghi RoomNight (3 đêm)
        verify(roomNightRepository).saveAll(argThat(iterable -> {
            List<?> list = (List<?>) iterable;
            return list.size() == 3;
        }));
    }

    @Test
    @DisplayName("Đặt phòng với khoảng ngày không hợp lệ (checkOut <= checkIn) bị từ chối")
    void testCreateReservation_InvalidDateRange_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        ReservationCreateRequest request = new ReservationCreateRequest(
                10L, BookingSource.WEBSITE, checkIn, checkOut, 2, 0,
                List.of(new ReservationRoomItemRequest(1L, null)), null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.createReservation(request));
        assertEquals("INVALID_DATE_RANGE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Đặt phòng khi hết phòng trống ném lỗi ROOM_NOT_AVAILABLE")
    void testCreateReservation_RoomNotAvailable_ThrowsException() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(2);

        when(guestApi.getGuest(10L)).thenReturn(guest);
        when(roomNightRepository.findOccupiedRoomIdsBetween(checkIn, checkOut)).thenReturn(List.of(101L));
        when(roomApi.getRoomType(1L)).thenReturn(standardRoomType);
        when(roomApi.getActiveRoomsByRoomType(1L)).thenReturn(List.of(room101));

        ReservationCreateRequest request = new ReservationCreateRequest(
                10L, BookingSource.WALK_IN, checkIn, checkOut, 2, 0,
                List.of(new ReservationRoomItemRequest(1L, null)), null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.createReservation(request));
        assertEquals("ROOM_NOT_AVAILABLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Trùng phòng đồng thời bắt lỗi ORA-00001 (DataIntegrityViolation) và trả về ROOM_NOT_AVAILABLE")
    void testCreateReservation_ConcurrencyDoubleBooking_CatchesORA00001_ThrowsRoomNotAvailable() {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(3);

        when(guestApi.getGuest(10L)).thenReturn(guest);
        when(roomNightRepository.findOccupiedRoomIdsBetween(checkIn, checkOut)).thenReturn(Collections.emptyList());
        when(roomApi.getRoomType(1L)).thenReturn(standardRoomType);
        when(roomApi.getActiveRoomsByRoomType(1L)).thenReturn(List.of(room101));

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setReservationId(501L);
            return r;
        });
        when(reservationRoomRepository.save(any(ReservationRoom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(new DataIntegrityViolationException("ORA-00001: unique constraint (HOTEL_APP.PK_ROOM_NIGHT) violated"))
                .when(roomNightRepository).flush();

        ReservationCreateRequest request = new ReservationCreateRequest(
                10L, BookingSource.OTA, checkIn, checkOut, 2, 0,
                List.of(new ReservationRoomItemRequest(1L, null)), null
        );

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.createReservation(request));
        assertEquals("ROOM_NOT_AVAILABLE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Đặt cọc chưa đạt ngưỡng tối thiểu vẫn giữ trạng thái PENDING")
    void testRecordDeposit_UnderThreshold_RemainsPending() {
        Reservation res = new Reservation();
        res.setReservationId(200L);
        res.setGuestId(10L);
        res.setStatus(ReservationStatus.PENDING);
        res.setCheckInDate(LocalDate.now().plusDays(2));
        res.setCheckOutDate(LocalDate.now().plusDays(4));
        res.setTotalAmount(1200000L);
        res.setDepositAmount(0L);

        ReservationRoom room = new ReservationRoom();
        room.setPricePerNight(600000L); // Cọc tối thiểu 1 đêm = 600.000đ

        when(reservationRepository.findByIdWithLock(200L)).thenReturn(Optional.of(res));
        when(billingApi.calculateTotalDepositByReservationId(200L)).thenReturn(300000L); // Chỉ cọc 300.000đ
        when(reservationRoomRepository.findByReservationReservationId(200L)).thenReturn(List.of(room));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestApi.getGuest(10L)).thenReturn(guest);

        ReservationDepositRequest depositReq = new ReservationDepositRequest(300000L, PaymentMethod.TRANSFER, "DEP-01");
        ReservationDetailResponse response = bookingService.recordDeposit(200L, depositReq);

        assertEquals(ReservationStatus.PENDING, response.status());
        assertEquals(300000L, response.depositAmount());
        assertFalse(response.isDepositMet());
    }

    @Test
    @DisplayName("Đặt cọc đủ ngưỡng tối thiểu 1 đêm tự động chuyển sang CONFIRMED")
    void testRecordDeposit_ReachesThreshold_TransitionsToConfirmed() {
        Reservation res = new Reservation();
        res.setReservationId(200L);
        res.setGuestId(10L);
        res.setStatus(ReservationStatus.PENDING);
        res.setCheckInDate(LocalDate.now().plusDays(2));
        res.setCheckOutDate(LocalDate.now().plusDays(4));
        res.setTotalAmount(1200000L);
        res.setDepositAmount(0L);

        ReservationRoom room = new ReservationRoom();
        room.setPricePerNight(600000L); // Cọc tối thiểu 1 đêm = 600.000đ

        when(reservationRepository.findByIdWithLock(200L)).thenReturn(Optional.of(res));
        when(billingApi.calculateTotalDepositByReservationId(200L)).thenReturn(600000L); // Cọc đủ 600.000đ
        when(reservationRoomRepository.findByReservationReservationId(200L)).thenReturn(List.of(room));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestApi.getGuest(10L)).thenReturn(guest);

        ReservationDepositRequest depositReq = new ReservationDepositRequest(600000L, PaymentMethod.CASH, "DEP-02");
        ReservationDetailResponse response = bookingService.recordDeposit(200L, depositReq);

        assertEquals(ReservationStatus.CONFIRMED, response.status());
        assertEquals(600000L, response.depositAmount());
        assertTrue(response.isDepositMet());
    }

    @Test
    @DisplayName("Xác nhận đơn đặt phòng thủ công chuyển trạng thái từ PENDING sang CONFIRMED")
    void testConfirmReservation_Success() {
        Reservation res = new Reservation();
        res.setReservationId(200L);
        res.setGuestId(10L);
        res.setStatus(ReservationStatus.PENDING);
        res.setCheckInDate(LocalDate.now().plusDays(2));
        res.setCheckOutDate(LocalDate.now().plusDays(4));

        when(reservationRepository.findByIdWithLock(200L)).thenReturn(Optional.of(res));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestApi.getGuest(10L)).thenReturn(guest);
        when(reservationRoomRepository.findByReservationReservationId(200L)).thenReturn(Collections.emptyList());

        ReservationDetailResponse response = bookingService.confirmReservation(200L);

        assertEquals(ReservationStatus.CONFIRMED, response.status());
    }

    @Test
    @DisplayName("Xác nhận đơn đặt phòng ở trạng thái không phải PENDING ném lỗi INVALID_STATE")
    void testConfirmReservation_InvalidState_ThrowsException() {
        Reservation res = new Reservation();
        res.setReservationId(200L);
        res.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findByIdWithLock(200L)).thenReturn(Optional.of(res));

        BusinessException ex = assertThrows(BusinessException.class, () -> bookingService.confirmReservation(200L));
        assertEquals("INVALID_STATE", ex.getErrorCode());
    }

    @Test
    @DisplayName("Hủy trước 3 ngày được miễn phí phạt và hoàn lại toàn bộ cọc, giải phóng room_night")
    void testCancelReservation_FreeCancellation_FullRefund_DeletesRoomNights() {
        Reservation res = new Reservation();
        res.setReservationId(300L);
        res.setReservationCode("RES-20261101-ABCDEF");
        res.setGuestId(10L);
        res.setStatus(ReservationStatus.CONFIRMED);
        res.setCheckInDate(LocalDate.now().plusDays(5)); // Còn 5 ngày >= 3 ngày freeCancelDays
        res.setCheckOutDate(LocalDate.now().plusDays(7));
        res.setDepositAmount(600000L);

        when(reservationRepository.findByIdWithLock(300L)).thenReturn(Optional.of(res));
        when(reservationRoomRepository.findByReservationReservationId(300L)).thenReturn(Collections.emptyList());
        when(billingApi.calculateTotalDepositByReservationId(300L)).thenReturn(600000L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestApi.getGuest(10L)).thenReturn(guest);

        ReservationDetailResponse response = bookingService.cancelReservation(300L, new ReservationCancelRequest("Khách bận việc đột xuất"));

        assertEquals(ReservationStatus.CANCELLED, response.status());
        verify(billingApi).refundReservationDeposit(eq(300L), eq(600000L), anyString(), any());
        verify(roomNightRepository).deleteByReservationId(300L);
    }

    @Test
    @DisplayName("Hủy muộn (< 3 ngày) bị phạt 1 đêm, chỉ hoàn phần cọc vượt quá mức phạt")
    void testCancelReservation_LateCancellation_OneNightPenalty_DeletesRoomNights() {
        Reservation res = new Reservation();
        res.setReservationId(300L);
        res.setReservationCode("RES-20261101-ABCDEF");
        res.setGuestId(10L);
        res.setStatus(ReservationStatus.CONFIRMED);
        res.setCheckInDate(LocalDate.now().plusDays(1)); // Chỉ còn 1 ngày < 3 ngày freeCancelDays
        res.setCheckOutDate(LocalDate.now().plusDays(4));
        res.setDepositAmount(1500000L);

        ReservationRoom room = new ReservationRoom();
        room.setPricePerNight(600000L); // Phạt 1 đêm = 600.000đ

        when(reservationRepository.findByIdWithLock(300L)).thenReturn(Optional.of(res));
        when(reservationRoomRepository.findByReservationReservationId(300L)).thenReturn(List.of(room));
        when(billingApi.calculateTotalDepositByReservationId(300L)).thenReturn(1500000L);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestApi.getGuest(10L)).thenReturn(guest);

        ReservationDetailResponse response = bookingService.cancelReservation(300L, new ReservationCancelRequest("Hủy sát ngày"));

        assertEquals(ReservationStatus.CANCELLED, response.status());
        // Hoàn phần cọc dư: 1.500.000 - 600.000 = 900.000đ
        verify(billingApi).refundReservationDeposit(eq(300L), eq(900000L), anyString(), any());
        verify(roomNightRepository).deleteByReservationId(300L);
    }

    @Test
    @DisplayName("Đánh dấu khách không đến (No-show) chuyển sang NO_SHOW và giải phóng room_night")
    void testMarkNoShow_Success() {
        Reservation res = new Reservation();
        res.setReservationId(400L);
        res.setGuestId(10L);
        res.setStatus(ReservationStatus.CONFIRMED);
        res.setCheckInDate(LocalDate.now());
        res.setCheckOutDate(LocalDate.now().plusDays(2));

        when(reservationRepository.findByIdWithLock(400L)).thenReturn(Optional.of(res));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guestApi.getGuest(10L)).thenReturn(guest);
        when(reservationRoomRepository.findByReservationReservationId(400L)).thenReturn(Collections.emptyList());

        ReservationDetailResponse response = bookingService.markNoShow(400L);

        assertEquals(ReservationStatus.NO_SHOW, response.status());
        verify(roomNightRepository).deleteByReservationId(400L);
    }
}
