package com.hotel.booking;

import com.hotel.booking.api.BookingApi;
import com.hotel.booking.api.ReservationInfoDto;
import com.hotel.booking.api.WalkInReservationCommand;
import com.hotel.booking.entity.Reservation;
import com.hotel.booking.entity.ReservationRoom;
import com.hotel.booking.repository.ReservationRepository;
import com.hotel.booking.repository.ReservationRoomRepository;
import com.hotel.booking.repository.RoomNightRepository;
import com.hotel.booking.service.BookingApiImpl;
import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.ReservationRoomStatus;
import com.hotel.common.enums.ReservationStatus;
import com.hotel.room.api.RoomApi;
import com.hotel.room.api.RoomTypeInfoDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingApiImplTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private ReservationRoomRepository reservationRoomRepository;
    @Mock private RoomNightRepository roomNightRepository;
    @Mock private RoomApi roomApi;

    private BookingApi bookingApi;

    @BeforeEach
    void setUp() {
        bookingApi = new BookingApiImpl(
                reservationRepository,
                reservationRoomRepository,
                roomNightRepository,
                roomApi
        );
    }

    @Test
    @DisplayName("Lấy thông tin Reservation qua BookingApi thành công")
    void testGetReservation_Success() {
        Reservation res = new Reservation();
        res.setReservationId(10L);
        res.setReservationCode("RES-20261028-001");
        res.setGuestId(5L);
        res.setBookingSource(BookingSource.WALK_IN);
        res.setStatus(ReservationStatus.CONFIRMED);
        res.setCheckInDate(LocalDate.now());
        res.setCheckOutDate(LocalDate.now().plusDays(2));
        res.setAdults(2);
        res.setChildren(1);
        res.setTotalAmount(1200000L);
        res.setDepositAmount(600000L);

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(res));

        ReservationInfoDto dto = bookingApi.getReservation(10L);

        assertNotNull(dto);
        assertEquals(10L, dto.reservationId());
        assertEquals("RES-20261028-001", dto.reservationCode());
        assertEquals(ReservationStatus.CONFIRMED, dto.status());
    }

    @Test
    @DisplayName("Tạo đơn đặt phòng khách vãng lai (Walk-In) và gán phòng thành công")
    void testCreateWalkInReservation_Success() {
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = LocalDate.now().plusDays(2);

        RoomTypeInfoDto rt = new RoomTypeInfoDto(
                1L, "Standard", "Phòng tiêu chuẩn", 2, 1,
                "Double", BigDecimal.valueOf(22), 600000L, true
        );
        when(roomApi.getRoomType(1L)).thenReturn(rt);

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setReservationId(99L);
            return r;
        });
        when(reservationRoomRepository.save(any(ReservationRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        WalkInReservationCommand command = new WalkInReservationCommand(
                5L, checkIn, checkOut, 2, 0,
                List.of(new WalkInReservationCommand.WalkInRoomItemCommand(1L, 101L)),
                "Khách vãng lai"
        );

        Long reservationId = bookingApi.createWalkInReservation(command);

        assertEquals(99L, reservationId);
        verify(roomNightRepository).saveAll(argThat(iterable -> {
            List<?> list = (List<?>) iterable;
            return list.size() == 2; // 2 đêm
        }));
    }

    @Test
    @DisplayName("Đổi phòng (changeRoom) xóa room_night của phòng cũ từ fromDate và thêm room_night mới")
    void testChangeRoom_Success() {
        ReservationRoom room = new ReservationRoom();
        room.setReservationRoomId(50L);
        room.setRoomId(101L);

        when(reservationRoomRepository.findByIdWithLock(50L)).thenReturn(Optional.of(room));

        LocalDate fromDate = LocalDate.now().plusDays(1);
        LocalDate toDate = LocalDate.now().plusDays(3); // 2 đêm đổi sang phòng 102

        bookingApi.changeRoom(50L, 102L, fromDate, toDate);

        // Kiểm tra xóa ngày cũ từ fromDate
        verify(roomNightRepository).deleteByReservationRoomIdAndStayDateFrom(50L, fromDate);

        // Kiểm tra lưu room_night mới cho phòng 102
        verify(roomNightRepository).saveAll(argThat(iterable -> {
            List<?> list = (List<?>) iterable;
            return list.size() == 2;
        }));

        assertEquals(102L, room.getRoomId());
        verify(reservationRoomRepository).save(room);
    }
}
