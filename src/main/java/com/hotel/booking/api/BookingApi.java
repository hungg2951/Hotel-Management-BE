package com.hotel.booking.api;

import java.time.LocalDate;
import java.util.List;

/**
 * Cổng giao tiếp công khai của module Booking dành cho các module khác (Frontdesk, Report).
 * Tuyệt đối không lộ Entity ra bên ngoài.
 */
public interface BookingApi {

    ReservationInfoDto getReservation(Long reservationId);

    List<ReservationRoomInfoDto> getReservationRooms(Long reservationId);

    ReservationRoomInfoDto getReservationRoom(Long reservationRoomId);

    Long createWalkInReservation(WalkInReservationCommand command);

    void markReservationCheckedIn(Long reservationId);

    void markReservationCheckedOut(Long reservationId);

    void markReservationRoomCheckedIn(Long reservationRoomId);

    void markReservationRoomCheckedOut(Long reservationRoomId);

    void changeRoom(Long reservationRoomId, Long newRoomId, LocalDate fromDate, LocalDate toDate);

    void assignRoom(Long reservationRoomId, Long roomId);

    void releaseRemainingRoomNights(Long reservationRoomId, LocalDate fromDate);

    List<ReservationInfoDto> getExpectedArrivals(LocalDate date);

    List<ReservationInfoDto> getExpectedDepartures(LocalDate date);
}
