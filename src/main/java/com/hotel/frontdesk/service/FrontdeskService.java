package com.hotel.frontdesk.service;

import com.hotel.billing.api.BillingApi;
import com.hotel.billing.api.FolioSummaryDto;
import com.hotel.billing.dto.PaymentRequest;
import com.hotel.billing.service.BillingService;
import com.hotel.booking.api.BookingApi;
import com.hotel.booking.api.ReservationInfoDto;
import com.hotel.booking.api.ReservationRoomInfoDto;
import com.hotel.booking.api.WalkInReservationCommand;
import com.hotel.common.enums.*;
import com.hotel.common.exception.BusinessException;
import com.hotel.common.security.CurrentUserApi;
import com.hotel.frontdesk.dto.*;
import com.hotel.frontdesk.entity.Stay;
import com.hotel.frontdesk.repository.StayRepository;
import com.hotel.guest.api.GuestApi;
import com.hotel.guest.api.GuestInfoDto;
import com.hotel.housekeeping.api.HousekeepingApi;
import com.hotel.policy.dto.EarlyCheckoutCalculationResult;
import com.hotel.policy.dto.EarlyCheckoutPolicySnapshot;
import com.hotel.frontdesk.dto.EarlyCheckoutPreviewResponse;
import com.hotel.policy.service.PolicyService;
import com.hotel.common.enums.EarlyCheckoutPenaltyType;
import com.hotel.common.enums.FolioItemType;
import com.hotel.room.api.RoomApi;
import com.hotel.room.api.RoomInfoDto;
import com.hotel.room.api.RoomTypeInfoDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FrontdeskService {

    private final StayRepository stayRepository;
    private final BookingApi bookingApi;
    private final RoomApi roomApi;
    private final BillingApi billingApi;
    private final BillingService billingService;
    private final GuestApi guestApi;
    private final HousekeepingApi housekeepingApi;
    private final PolicyService policyService;

    @org.springframework.beans.factory.annotation.Autowired
    public FrontdeskService(
            StayRepository stayRepository,
            BookingApi bookingApi,
            RoomApi roomApi,
            BillingApi billingApi,
            BillingService billingService,
            GuestApi guestApi,
            HousekeepingApi housekeepingApi,
            PolicyService policyService
    ) {
        this.stayRepository = stayRepository;
        this.bookingApi = bookingApi;
        this.roomApi = roomApi;
        this.billingApi = billingApi;
        this.billingService = billingService;
        this.guestApi = guestApi;
        this.housekeepingApi = housekeepingApi;
        this.policyService = policyService;
    }

    public FrontdeskService(
            StayRepository stayRepository,
            BookingApi bookingApi,
            RoomApi roomApi,
            BillingApi billingApi,
            BillingService billingService,
            GuestApi guestApi,
            HousekeepingApi housekeepingApi
    ) {
        this(stayRepository, bookingApi, roomApi, billingApi, billingService, guestApi, housekeepingApi, null);
    }

    @Transactional
    public StayResponse checkIn(CheckInRequest request) {
        // 1. Kiểm tra xem phòng đặt này đã từng check-in chưa
        Optional<Stay> existingStay = stayRepository.findByReservationRoomId(request.reservationRoomId());
        if (existingStay.isPresent() && existingStay.get().getStatus() == StayStatus.IN_HOUSE) {
            throw BusinessException.invalidState("Phòng đặt #" + request.reservationRoomId() + " đang trong trạng thái lưu trú (IN_HOUSE)");
        }

        ReservationRoomInfoDto roomInfo = bookingApi.getReservationRoom(request.reservationRoomId());
        ReservationInfoDto res = bookingApi.getReservation(roomInfo.reservationId());

        if (res.status() == ReservationStatus.CANCELLED || res.status() == ReservationStatus.CHECKED_OUT) {
            throw BusinessException.invalidState("Không thể check-in đơn đặt phòng ở trạng thái: " + res.status());
        }

        // 2. Xác định phòng vật lý
        Long targetRoomId = request.specificRoomId() != null ? request.specificRoomId() : roomInfo.roomId();
        if (targetRoomId == null) {
            throw BusinessException.badRequest("Đơn đặt phòng chưa được gán số phòng cụ thể. Vui lòng chọn phòng để làm thủ tục nhận phòng.");
        }

        RoomInfoDto roomDto = roomApi.getRoom(targetRoomId);

        // Kiểm tra xem phòng có đang bị ở bởi lượt lưu trú khác không
        Optional<Stay> activeStayInRoom = stayRepository.findByRoomIdAndStatus(targetRoomId, StayStatus.IN_HOUSE);
        if (activeStayInRoom.isPresent()) {
            throw BusinessException.roomNotAvailable("Phòng " + roomDto.roomNumber() + " hiện đang có khách lưu trú!");
        }

        // Cập nhật gán phòng nếu có thay đổi
        if (!targetRoomId.equals(roomInfo.roomId())) {
            bookingApi.assignRoom(roomInfo.reservationRoomId(), targetRoomId);
        }

        // 3. Khởi tạo Stay Entity
        Stay stay = new Stay();
        stay.setReservationRoomId(roomInfo.reservationRoomId());
        stay.setRoomId(targetRoomId);
        stay.setActualCheckIn(LocalDateTime.now());
        stay.setStatus(StayStatus.IN_HOUSE);
        stay.setCheckedInBy(CurrentUserApi.getCurrentUserId().orElse(null));

        Stay savedStay = stayRepository.save(stay);

        // 4. Cập nhật trạng thái Room -> OCCUPIED
        roomApi.updateRoomStatus(targetRoomId, RoomStatus.OCCUPIED, HousekeepingStatus.CLEAN);

        // 5. Cập nhật trạng thái Reservation & ReservationRoom -> CHECKED_IN
        bookingApi.markReservationRoomCheckedIn(roomInfo.reservationRoomId());
        bookingApi.markReservationCheckedIn(res.reservationId());

        // 6. Khởi tạo Folio và kết nối cọc
        Long folioId = billingApi.openFolio(savedStay.getStayId());
        billingApi.attachReservationDepositsToFolio(res.reservationId(), folioId);

        // 7. Ghi nhận tiền phòng vào Folio
        long nights = ChronoUnit.DAYS.between(res.checkInDate(), res.checkOutDate());
        if (nights <= 0) nights = 1;
        long totalRoomCharge = roomInfo.pricePerNight() * nights;

        billingApi.postRoomCharge(
                folioId,
                totalRoomCharge,
                "Tiền phòng " + roomDto.roomNumber() + " (" + nights + " đêm: " + res.checkInDate() + " -> " + res.checkOutDate() + ")",
                CurrentUserApi.getCurrentUserId().orElse(null)
        );

        return toStayResponse(savedStay);
    }

    @Transactional(readOnly = true)
    public EarlyCheckoutPreviewResponse previewEarlyCheckout(Long stayId) {
        Stay stay = stayRepository.findById(stayId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lượt lưu trú #" + stayId));

        ReservationRoomInfoDto roomInfo = bookingApi.getReservationRoom(stay.getReservationRoomId());
        ReservationInfoDto res = bookingApi.getReservation(roomInfo.reservationId());

        LocalDate today = LocalDate.now();
        long originalNights = ChronoUnit.DAYS.between(res.checkInDate(), res.checkOutDate());
        long actualNights = ChronoUnit.DAYS.between(stay.getActualCheckIn().toLocalDate(), today);
        if (actualNights <= 0) actualNights = 1;

        EarlyCheckoutPolicySnapshot snapshot = policyService != null
                ? policyService.resolveLatestEarlyCheckoutSnapshot(res.earlyCheckoutPolicyId(), res.earlyCheckoutPolicySnapshot())
                : new EarlyCheckoutPolicySnapshot(1L, "FLEXIBLE", "Trả sớm Linh hoạt (Miễn phạt)", EarlyCheckoutPenaltyType.NONE, BigDecimal.ZERO);

        EarlyCheckoutCalculationResult calc = policyService != null
                ? policyService.calculateEarlyCheckout(snapshot, originalNights, actualNights, roomInfo.pricePerNight(), null, null)
                : new EarlyCheckoutCalculationResult(originalNights, actualNights, Math.max(0, originalNights - actualNights), roomInfo.pricePerNight(), (originalNights - actualNights) * roomInfo.pricePerNight(), 0, (originalNights - actualNights) * roomInfo.pricePerNight(), "FLEXIBLE", "Miễn phạt", "");

        FolioSummaryDto folioSummary = billingApi.getFolioSummaryByStayId(stay.getStayId());
        long currentBalance = folioSummary.balance();
        long estimatedFinalBalance = currentBalance - calc.netAdjustment();

        return new EarlyCheckoutPreviewResponse(
                stayId,
                originalNights,
                actualNights,
                calc.unusedNights(),
                roomInfo.pricePerNight(),
                calc.unusedRoomAmount(),
                calc.earlyCheckoutPenalty(),
                calc.netAdjustment(),
                calc.policyCode(),
                calc.policyName(),
                calc.explanation(),
                currentBalance,
                estimatedFinalBalance
        );
    }

    @Transactional
    public StayResponse checkOut(CheckOutRequest request) {
        Stay stay = stayRepository.findByIdWithLock(request.stayId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lượt lưu trú #" + request.stayId()));

        if (stay.getStatus() != StayStatus.IN_HOUSE) {
            throw BusinessException.invalidState("Chỉ có thể check-out lượt lưu trú đang ở (IN_HOUSE)");
        }

        ReservationRoomInfoDto roomInfo = bookingApi.getReservationRoom(stay.getReservationRoomId());
        ReservationInfoDto res = bookingApi.getReservation(roomInfo.reservationId());

        LocalDate today = LocalDate.now();

        // 1. Xử lý Trả phòng sớm (Early Check-Out)
        boolean isEarly = (request.isEarlyCheckOut() != null && request.isEarlyCheckOut()) || today.isBefore(res.checkOutDate());
        if (isEarly && today.isBefore(res.checkOutDate())) {
            // Giải phóng các đêm chưa ở từ hôm nay trở đi
            bookingApi.releaseRemainingRoomNights(roomInfo.reservationRoomId(), today);

            // Tính số đêm thực tế đã ở
            long actualNights = ChronoUnit.DAYS.between(stay.getActualCheckIn().toLocalDate(), today);
            if (actualNights <= 0) actualNights = 1; // Tối thiểu tính 1 đêm

            long originalNights = ChronoUnit.DAYS.between(res.checkInDate(), res.checkOutDate());
            if (actualNights < originalNights) {
                EarlyCheckoutPolicySnapshot snapshot = policyService != null
                        ? policyService.resolveLatestEarlyCheckoutSnapshot(res.earlyCheckoutPolicyId(), res.earlyCheckoutPolicySnapshot())
                        : new EarlyCheckoutPolicySnapshot(1L, "FLEXIBLE", "Trả sớm Linh hoạt (Miễn phạt)", EarlyCheckoutPenaltyType.NONE, BigDecimal.ZERO);

                Long currentUserId = CurrentUserApi.getCurrentUserId().orElse(null);
                EarlyCheckoutCalculationResult calc = policyService != null
                        ? policyService.calculateEarlyCheckout(snapshot, originalNights, actualNights, roomInfo.pricePerNight(), request.customPenalty(), request.overrideReason())
                        : new EarlyCheckoutCalculationResult(originalNights, actualNights, originalNights - actualNights, roomInfo.pricePerNight(), (originalNights - actualNights) * roomInfo.pricePerNight(), 0, (originalNights - actualNights) * roomInfo.pricePerNight(), "FLEXIBLE", "Miễn phạt", "");

                FolioSummaryDto folioSummary = billingApi.getFolioSummaryByStayId(stay.getStayId());

                // Khấu trừ tiền phòng các đêm chưa ở
                if (calc.unusedRoomAmount() > 0) {
                    billingApi.postAdjustment(
                            folioSummary.folioId(),
                            FolioItemType.DISCOUNT,
                            calc.unusedRoomAmount(),
                            "Khấu trừ tiền phòng trả sớm (" + calc.unusedNights() + " đêm)",
                            currentUserId
                    );
                }

                // Ghi nhận phí phạt trả sớm nếu có
                if (calc.earlyCheckoutPenalty() > 0) {
                    billingApi.postAdjustment(
                            folioSummary.folioId(),
                            FolioItemType.PENALTY,
                            calc.earlyCheckoutPenalty(),
                            "Phí phạt trả phòng sớm (" + calc.explanation() + ")",
                            currentUserId
                    );
                }

                stay.setEarlyCheckoutPenalty(calc.earlyCheckoutPenalty());
                if (request.customPenalty() != null) {
                    stay.setOverrideReason(request.overrideReason());
                    stay.setOverrideBy(currentUserId);
                }
            }
        }

        // 2. Xử lý thanh toán / cân bằng công nợ Folio
        FolioSummaryDto summary = billingApi.getFolioSummaryByStayId(stay.getStayId());
        if (summary.balance() != 0L) {
            if (Boolean.TRUE.equals(request.autoSettle())) {
                PaymentMethod method = request.settleMethod() != null ? request.settleMethod() : PaymentMethod.CASH;
                if (summary.balance() > 0) {
                    // Thu nốt phần tiền còn thiếu
                    billingService.recordPayment(summary.folioId(), new PaymentRequest(
                            method,
                            summary.balance(),
                            PaymentStatus.SUCCESS,
                            request.settleRef() != null ? request.settleRef() : "Thanh toán số dư trả phòng #" + stay.getStayId(),
                            res.reservationId()
                    ));
                } else {
                    // Hoàn lại tiền thừa cho khách
                    billingService.recordPayment(summary.folioId(), new PaymentRequest(
                            method,
                            Math.abs(summary.balance()),
                            PaymentStatus.REFUNDED,
                            request.settleRef() != null ? request.settleRef() : "Hoàn tiền cọc dư trả phòng #" + stay.getStayId(),
                            res.reservationId()
                    ));
                }
            } else {
                throw BusinessException.folioNotSettled(
                        "Folio #" + summary.folioId() + " chưa được quyết toán cân bằng (số dư: " + summary.balance() + " VNĐ). Vui lòng thanh toán hoặc tích chọn Tự động quyết toán để trả phòng."
                );
            }
        }

        // 3. Đóng Folio
        billingApi.closeFolio(summary.folioId());

        // 4. Cập nhật Stay -> CHECKED_OUT
        stay.setStatus(StayStatus.CHECKED_OUT);
        stay.setActualCheckOut(LocalDateTime.now());
        stay.setCheckedOutBy(CurrentUserApi.getCurrentUserId().orElse(null));
        Stay savedStay = stayRepository.save(stay);

        // 5. Cập nhật ReservationRoom -> CHECKED_OUT
        bookingApi.markReservationRoomCheckedOut(roomInfo.reservationRoomId());

        // Kiểm tra nếu tất cả các phòng trong đơn đã check-out -> Cập nhật Reservation -> CHECKED_OUT
        List<ReservationRoomInfoDto> allRooms = bookingApi.getReservationRooms(res.reservationId());
        boolean allCheckedOut = allRooms.stream()
                .allMatch(r -> r.reservationRoomId().equals(roomInfo.reservationRoomId()) || r.status() == ReservationRoomStatus.CHECKED_OUT || r.status() == ReservationRoomStatus.CANCELLED);
        if (allCheckedOut) {
            bookingApi.markReservationCheckedOut(res.reservationId());
        }

        // 6. Chuyển phòng sang AVAILABLE và buồng phòng sang DIRTY (cần dọn dẹp)
        roomApi.updateRoomStatus(stay.getRoomId(), RoomStatus.AVAILABLE, HousekeepingStatus.DIRTY);
        housekeepingApi.createCleaningTask(
                stay.getRoomId(),
                HousekeepingTaskType.CLEANING,
                "Dọn phòng sau khi khách trả phòng (Lượt lưu trú #" + stay.getStayId() + ")"
        );

        return toStayResponse(savedStay);
    }

    @Transactional
    public StayResponse changeRoom(RoomChangeRequest request) {
        Stay stay = stayRepository.findByIdWithLock(request.stayId())
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lượt lưu trú #" + request.stayId()));

        if (stay.getStatus() != StayStatus.IN_HOUSE) {
            throw BusinessException.invalidState("Chỉ có thể đổi phòng cho khách đang lưu trú (IN_HOUSE)");
        }

        if (request.newRoomId().equals(stay.getRoomId())) {
            throw BusinessException.badRequest("Phòng mới phải khác phòng hiện tại");
        }

        // Kiểm tra phòng mới có sẵn và sạch không
        RoomInfoDto newRoom = roomApi.getRoom(request.newRoomId());
        Optional<Stay> activeInNew = stayRepository.findByRoomIdAndStatus(request.newRoomId(), StayStatus.IN_HOUSE);
        if (activeInNew.isPresent()) {
            throw BusinessException.roomNotAvailable("Phòng " + newRoom.roomNumber() + " hiện đang có khách ở!");
        }

        ReservationRoomInfoDto roomInfo = bookingApi.getReservationRoom(stay.getReservationRoomId());
        ReservationInfoDto res = bookingApi.getReservation(roomInfo.reservationId());

        LocalDate today = LocalDate.now();

        // Đổi phòng trong module Booking (cập nhật room_night các đêm tiếp theo)
        bookingApi.changeRoom(stay.getReservationRoomId(), request.newRoomId(), today, res.checkOutDate());

        // Cập nhật trạng thái phòng: Phòng cũ -> DIRTY, phòng mới -> OCCUPIED
        Long oldRoomId = stay.getRoomId();
        roomApi.updateRoomStatus(oldRoomId, RoomStatus.AVAILABLE, HousekeepingStatus.DIRTY);
        roomApi.updateRoomStatus(request.newRoomId(), RoomStatus.OCCUPIED, HousekeepingStatus.CLEAN);

        // Tạo công việc dọn dẹp phòng cũ
        housekeepingApi.createCleaningTask(
                oldRoomId,
                HousekeepingTaskType.CLEANING,
                "Dọn phòng cũ sau khi đổi sang phòng " + newRoom.roomNumber() + " (Lượt lưu trú #" + stay.getStayId() + ")"
        );

        // Cập nhật Stay
        stay.setRoomId(request.newRoomId());
        Stay savedStay = stayRepository.save(stay);

        return toStayResponse(savedStay);
    }

    @Transactional
    public StayResponse walkIn(WalkInCheckInRequest request) {
        LocalDate checkInDate = LocalDate.now();
        if (!request.checkOutDate().isAfter(checkInDate)) {
            throw BusinessException.invalidDateRange("Ngày trả phòng phải sau ngày nhận phòng hôm nay");
        }

        // 1. Tạo đơn đặt phòng Walk-in
        WalkInReservationCommand.WalkInRoomItemCommand roomItem = new WalkInReservationCommand.WalkInRoomItemCommand(
                request.roomTypeId(),
                request.roomId()
        );

        WalkInReservationCommand cmd = new WalkInReservationCommand(
                request.guestId(),
                checkInDate,
                request.checkOutDate(),
                request.adults() != null && request.adults() > 0 ? request.adults() : 1,
                request.children() != null && request.children() >= 0 ? request.children() : 0,
                List.of(roomItem),
                request.note() != null ? "Khách vãng lai: " + request.note().trim() : "Khách vãng lai nhận phòng ngay"
        );

        Long reservationId = bookingApi.createWalkInReservation(cmd);

        // 2. Nếu có đặt cọc ngay
        if (request.depositAmount() != null && request.depositAmount() > 0) {
            billingApi.recordReservationDeposit(
                    reservationId,
                    request.depositAmount(),
                    request.depositMethod() != null ? request.depositMethod() : PaymentMethod.CASH,
                    request.depositRef(),
                    CurrentUserApi.getCurrentUserId().orElse(null)
            );
        }

        // 3. Lấy phòng đặt vừa tạo và thực hiện check-in ngay
        List<ReservationRoomInfoDto> createdRooms = bookingApi.getReservationRooms(reservationId);
        if (createdRooms.isEmpty()) {
            throw BusinessException.badRequest("Lỗi hệ thống: Không thể khởi tạo phòng cho đơn Walk-in");
        }

        ReservationRoomInfoDto firstRoom = createdRooms.get(0);
        return checkIn(new CheckInRequest(firstRoom.reservationRoomId(), request.roomId(), request.note()));
    }

    @Transactional(readOnly = true)
    public Page<StayResponse> getStays(StayStatus status, String keyword, Pageable pageable) {
        Page<Stay> page = stayRepository.searchStays(status, pageable);
        List<StayResponse> content = page.getContent().stream()
                .map(this::toStayResponse)
                .filter(res -> {
                    if (keyword == null || keyword.isBlank()) return true;
                    String q = keyword.trim().toLowerCase();
                    return res.roomNumber().toLowerCase().contains(q) ||
                            res.guestName().toLowerCase().contains(q) ||
                            res.guestPhone().toLowerCase().contains(q) ||
                            res.reservationCode().toLowerCase().contains(q);
                })
                .toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public StayResponse getStayDetail(Long stayId) {
        Stay stay = stayRepository.findById(stayId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy lượt lưu trú #" + stayId));
        return toStayResponse(stay);
    }

    @Transactional(readOnly = true)
    public List<ReservationInfoDto> getExpectedArrivals(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return bookingApi.getExpectedArrivals(queryDate);
    }

    @Transactional(readOnly = true)
    public List<StayResponse> getExpectedDepartures(LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<Stay> inHouseStays = stayRepository.findByStatus(StayStatus.IN_HOUSE);

        return inHouseStays.stream()
                .map(this::toStayResponse)
                .filter(s -> s.expectedCheckOut() != null && s.expectedCheckOut().isEqual(queryDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public FrontdeskSummaryDto getSummary() {
        long inHouse = stayRepository.countByStatus(StayStatus.IN_HOUSE);
        long arrivals = bookingApi.getExpectedArrivals(LocalDate.now()).size();
        long departures = getExpectedDepartures(LocalDate.now()).size();

        long cleanAvailable = roomApi.getAllActiveRoomTypes().stream()
                .mapToLong(rt -> roomApi.getAvailableCleanRooms(rt.roomTypeId()).size())
                .sum();

        return new FrontdeskSummaryDto(inHouse, arrivals, departures, cleanAvailable);
    }

    private StayResponse toStayResponse(Stay s) {
        ReservationRoomInfoDto roomInfo = bookingApi.getReservationRoom(s.getReservationRoomId());
        ReservationInfoDto res = bookingApi.getReservation(roomInfo.reservationId());
        GuestInfoDto guest = guestApi.getGuest(res.guestId());
        RoomInfoDto room = roomApi.getRoom(s.getRoomId());
        RoomTypeInfoDto rt = roomApi.getRoomType(room.roomTypeId());

        FolioSummaryDto folioSummary = null;
        try {
            folioSummary = billingApi.getFolioSummaryByStayId(s.getStayId());
        } catch (Exception ignored) {
        }

        return new StayResponse(
                s.getStayId(),
                s.getReservationRoomId(),
                res.reservationId(),
                res.reservationCode(),
                guest.guestId(),
                guest.fullName(),
                guest.phone(),
                room.roomId(),
                room.roomNumber(),
                room.floorNo(),
                rt.roomTypeId(),
                rt.name(),
                s.getActualCheckIn(),
                s.getActualCheckOut(),
                res.checkInDate(),
                res.checkOutDate(),
                s.getStatus(),
                folioSummary != null ? folioSummary.folioId() : null,
                folioSummary != null ? folioSummary.totalAmount() : 0L,
                folioSummary != null ? folioSummary.paidAmount() : 0L,
                folioSummary != null ? folioSummary.balance() : 0L,
                folioSummary != null && folioSummary.isSettled(),
                s.getCheckedInBy(),
                s.getCheckedOutBy()
        );
    }
}
