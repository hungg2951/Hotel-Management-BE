package com.hotel.booking.service;

import com.hotel.common.security.CurrentUserApi;
import com.hotel.booking.api.BookingApi;
import com.hotel.booking.api.ReservationInfoDto;
import com.hotel.booking.api.ReservationRoomInfoDto;
import com.hotel.booking.api.WalkInReservationCommand;
import com.hotel.booking.entity.Reservation;
import com.hotel.booking.entity.ReservationRoom;
import com.hotel.booking.entity.RoomNight;
import com.hotel.booking.repository.ReservationRepository;
import com.hotel.booking.repository.ReservationRoomRepository;
import com.hotel.booking.repository.RoomNightRepository;
import com.hotel.common.enums.BookingSource;
import com.hotel.common.enums.ReservationRoomStatus;
import com.hotel.common.enums.ReservationStatus;
import com.hotel.common.exception.BusinessException;
import com.hotel.room.api.RoomApi;
import com.hotel.room.api.RoomTypeInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BookingApiImpl implements BookingApi {

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomNightRepository roomNightRepository;
    private final RoomApi roomApi;

    @Autowired(required = false)
    private com.hotel.policy.service.PolicyService policyService;

    public BookingApiImpl(
            ReservationRepository reservationRepository,
            ReservationRoomRepository reservationRoomRepository,
            RoomNightRepository roomNightRepository,
            RoomApi roomApi
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomNightRepository = roomNightRepository;
        this.roomApi = roomApi;
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationInfoDto getReservation(Long reservationId) {
        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt phòng có mã: " + reservationId));

        return new ReservationInfoDto(
                res.getReservationId(),
                res.getReservationCode(),
                res.getGuestId(),
                res.getBookingSource(),
                res.getStatus(),
                res.getCheckInDate(),
                res.getCheckOutDate(),
                res.getAdults(),
                res.getChildren(),
                res.getTotalAmount(),
                res.getDepositAmount(),
                res.getCancellationPolicySnapshot(),
                res.getEarlyCheckoutPolicySnapshot(),
                res.getCancellationPolicyId(),
                res.getEarlyCheckoutPolicyId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationRoomInfoDto> getReservationRooms(Long reservationId) {
        return reservationRoomRepository.findByReservationReservationId(reservationId).stream()
                .map(this::toRoomInfoDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationRoomInfoDto getReservationRoom(Long reservationRoomId) {
        ReservationRoom room = reservationRoomRepository.findById(reservationRoomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng đặt có mã: " + reservationRoomId));
        return toRoomInfoDto(room);
    }

    @Override
    @Transactional
    public Long createWalkInReservation(WalkInReservationCommand command) {
        if (!command.checkOutDate().isAfter(command.checkInDate())) {
            throw BusinessException.invalidDateRange("Ngày trả phòng phải sau ngày nhận phòng");
        }

        int nights = (int) ChronoUnit.DAYS.between(command.checkInDate(), command.checkOutDate());
        long totalAmount = 0L;

        for (WalkInReservationCommand.WalkInRoomItemCommand item : command.rooms()) {
            RoomTypeInfoDto rt = roomApi.getRoomType(item.roomTypeId());
            totalAmount += rt.basePrice() * nights;
        }

        Reservation res = new Reservation();
        res.setReservationCode(generateReservationCode());
        res.setGuestId(command.guestId());
        res.setBookingSource(BookingSource.WALK_IN);
        res.setStatus(ReservationStatus.CONFIRMED);
        res.setCheckInDate(command.checkInDate());
        res.setCheckOutDate(command.checkOutDate());
        res.setAdults(command.adults() != null ? command.adults() : 1);
        res.setChildren(command.children() != null ? command.children() : 0);
        res.setTotalAmount(totalAmount);
        res.setDepositAmount(0L);
        res.setNote(command.note() != null ? command.note().trim() : "Khách vãng lai (Walk-in)");
        res.setCreatedBy(CurrentUserApi.getCurrentUserId().orElse(null));
        res.setCreatedAt(LocalDateTime.now());

        if (policyService != null) {
            com.hotel.policy.entity.CancellationPolicy cancelPol = policyService.getActiveCancellationPolicy();
            com.hotel.policy.entity.EarlyCheckoutPolicy earlyPol = policyService.getActiveEarlyCheckoutPolicy();
            res.setCancellationPolicyId(cancelPol.getPolicyId());
            res.setCancellationPolicySnapshot(policyService.createCancellationSnapshotJson(cancelPol));
            res.setEarlyCheckoutPolicyId(earlyPol.getPolicyId());
            res.setEarlyCheckoutPolicySnapshot(policyService.createEarlyCheckoutSnapshotJson(earlyPol));
        }

        Reservation savedReservation = reservationRepository.save(res);

        List<RoomNight> nightsToSave = new ArrayList<>();
        for (WalkInReservationCommand.WalkInRoomItemCommand item : command.rooms()) {
            RoomTypeInfoDto rt = roomApi.getRoomType(item.roomTypeId());

            ReservationRoom rr = new ReservationRoom();
            rr.setReservation(savedReservation);
            rr.setRoomTypeId(item.roomTypeId());
            rr.setRoomId(item.roomId());
            rr.setPricePerNight(rt.basePrice());
            rr.setStatus(ReservationRoomStatus.BOOKED);

            ReservationRoom savedRr = reservationRoomRepository.save(rr);

            for (LocalDate d = command.checkInDate(); d.isBefore(command.checkOutDate()); d = d.plusDays(1)) {
                nightsToSave.add(new RoomNight(item.roomId(), d, savedRr));
            }
        }

        roomNightRepository.saveAll(nightsToSave);
        return savedReservation.getReservationId();
    }

    @Override
    @Transactional
    public void markReservationCheckedIn(Long reservationId) {
        Reservation res = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt phòng: " + reservationId));
        if (res.getStatus() != ReservationStatus.CONFIRMED && res.getStatus() != ReservationStatus.PENDING) {
            throw BusinessException.invalidState("Không thể check-in đơn ở trạng thái: " + res.getStatus());
        }
        res.setStatus(ReservationStatus.CHECKED_IN);
        reservationRepository.save(res);
    }

    @Override
    @Transactional
    public void markReservationCheckedOut(Long reservationId) {
        Reservation res = reservationRepository.findByIdWithLock(reservationId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt phòng: " + reservationId));
        res.setStatus(ReservationStatus.CHECKED_OUT);
        reservationRepository.save(res);
    }

    @Override
    @Transactional
    public void markReservationRoomCheckedIn(Long reservationRoomId) {
        ReservationRoom room = reservationRoomRepository.findByIdWithLock(reservationRoomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng đặt: " + reservationRoomId));
        room.setStatus(ReservationRoomStatus.CHECKED_IN);
        reservationRoomRepository.save(room);
    }

    @Override
    @Transactional
    public void markReservationRoomCheckedOut(Long reservationRoomId) {
        ReservationRoom room = reservationRoomRepository.findByIdWithLock(reservationRoomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng đặt: " + reservationRoomId));
        room.setStatus(ReservationRoomStatus.CHECKED_OUT);
        reservationRoomRepository.save(room);
    }

    @Override
    @Transactional
    public void changeRoom(Long reservationRoomId, Long newRoomId, LocalDate fromDate, LocalDate toDate) {
        ReservationRoom rr = reservationRoomRepository.findByIdWithLock(reservationRoomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng đặt: " + reservationRoomId));

        // 1. Xóa room_night của phòng cũ từ fromDate
        roomNightRepository.deleteByReservationRoomIdAndStayDateFrom(reservationRoomId, fromDate);

        // 2. Tạo room_night mới cho phòng mới từ fromDate đến toDate
        List<RoomNight> newNights = new ArrayList<>();
        for (LocalDate d = fromDate; d.isBefore(toDate); d = d.plusDays(1)) {
            newNights.add(new RoomNight(newRoomId, d, rr));
        }
        roomNightRepository.saveAll(newNights);

        // 3. Cập nhật roomId mới cho reservationRoom
        rr.setRoomId(newRoomId);
        reservationRoomRepository.save(rr);
    }

    @Override
    @Transactional
    public void assignRoom(Long reservationRoomId, Long roomId) {
        ReservationRoom rr = reservationRoomRepository.findByIdWithLock(reservationRoomId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phòng đặt: " + reservationRoomId));
        rr.setRoomId(roomId);
        reservationRoomRepository.save(rr);
    }

    @Override
    @Transactional
    public void releaseRemainingRoomNights(Long reservationRoomId, LocalDate fromDate) {
        roomNightRepository.deleteByReservationRoomIdAndStayDateFrom(reservationRoomId, fromDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationInfoDto> getExpectedArrivals(LocalDate date) {
        List<Reservation> list = reservationRepository.findByCheckInDateAndStatusIn(
                date,
                List.of(ReservationStatus.CONFIRMED, ReservationStatus.PENDING)
        );
        return list.stream().map(this::toReservationInfoDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationInfoDto> getExpectedDepartures(LocalDate date) {
        List<Reservation> list = reservationRepository.findByCheckOutDateAndStatus(
                date,
                ReservationStatus.CHECKED_IN
        );
        return list.stream().map(this::toReservationInfoDto).toList();
    }

    private ReservationInfoDto toReservationInfoDto(Reservation res) {
        return new ReservationInfoDto(
                res.getReservationId(),
                res.getReservationCode(),
                res.getGuestId(),
                res.getBookingSource(),
                res.getStatus(),
                res.getCheckInDate(),
                res.getCheckOutDate(),
                res.getAdults(),
                res.getChildren(),
                res.getTotalAmount(),
                res.getDepositAmount(),
                res.getCancellationPolicySnapshot(),
                res.getEarlyCheckoutPolicySnapshot(),
                res.getCancellationPolicyId(),
                res.getEarlyCheckoutPolicyId()
        );
    }

    private ReservationRoomInfoDto toRoomInfoDto(ReservationRoom room) {
        return new ReservationRoomInfoDto(
                room.getReservationRoomId(),
                room.getReservation() != null ? room.getReservation().getReservationId() : null,
                room.getRoomTypeId(),
                room.getRoomId(),
                room.getPricePerNight(),
                room.getStatus()
        );
    }

    private String generateReservationCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String code;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            code = "RES-" + datePart + "-" + suffix;
        } while (reservationRepository.existsByReservationCode(code));
        return code;
    }
}
