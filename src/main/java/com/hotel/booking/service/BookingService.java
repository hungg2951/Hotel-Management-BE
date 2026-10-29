package com.hotel.booking.service;

import com.hotel.common.security.CurrentUserApi;
import com.hotel.billing.api.BillingApi;
import com.hotel.billing.api.PaymentInfoDto;
import com.hotel.booking.dto.*;
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
import com.hotel.guest.api.GuestApi;
import com.hotel.guest.api.GuestInfoDto;
import com.hotel.policy.dto.*;
import com.hotel.policy.entity.CancellationPolicy;
import com.hotel.policy.entity.EarlyCheckoutPolicy;
import com.hotel.policy.service.PolicyService;
import com.hotel.common.enums.CancellationPenaltyType;
import com.hotel.room.api.RoomApi;
import com.hotel.room.api.RoomInfoDto;
import com.hotel.room.api.RoomTypeInfoDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BookingService {

    private final ReservationRepository reservationRepository;
    private final ReservationRoomRepository reservationRoomRepository;
    private final RoomNightRepository roomNightRepository;
    private final RoomApi roomApi;
    private final GuestApi guestApi;
    private final BillingApi billingApi;
    private final PolicyService policyService;

    private final int minDepositNights;
    private final int freeCancelDays;

    @org.springframework.beans.factory.annotation.Autowired
    public BookingService(
            ReservationRepository reservationRepository,
            ReservationRoomRepository reservationRoomRepository,
            RoomNightRepository roomNightRepository,
            RoomApi roomApi,
            GuestApi guestApi,
            BillingApi billingApi,
            PolicyService policyService,
            @Value("${app.booking.min-deposit-nights:1}") int minDepositNights,
            @Value("${app.booking.free-cancel-days:3}") int freeCancelDays
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationRoomRepository = reservationRoomRepository;
        this.roomNightRepository = roomNightRepository;
        this.roomApi = roomApi;
        this.guestApi = guestApi;
        this.billingApi = billingApi;
        this.policyService = policyService;
        this.minDepositNights = minDepositNights;
        this.freeCancelDays = freeCancelDays;
    }

    public BookingService(
            ReservationRepository reservationRepository,
            ReservationRoomRepository reservationRoomRepository,
            RoomNightRepository roomNightRepository,
            RoomApi roomApi,
            GuestApi guestApi,
            BillingApi billingApi,
            int minDepositNights,
            int freeCancelDays
    ) {
        this(reservationRepository, reservationRoomRepository, roomNightRepository, roomApi, guestApi, billingApi, null, minDepositNights, freeCancelDays);
    }

    @Transactional(readOnly = true)
    public List<RoomTypeAvailabilityResponse> searchAvailability(
            LocalDate checkIn,
            LocalDate checkOut,
            Integer adults,
            Integer children
    ) {
        validateDateRange(checkIn, checkOut);

        int reqAdults = adults != null && adults > 0 ? adults : 1;
        int reqChildren = children != null && children >= 0 ? children : 0;
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        List<RoomTypeInfoDto> roomTypes = roomApi.getAllActiveRoomTypes();
        List<Long> occupiedRoomIds = roomNightRepository.findOccupiedRoomIdsBetween(checkIn, checkOut);
        Set<Long> occupiedSet = new HashSet<>(occupiedRoomIds);

        List<RoomTypeAvailabilityResponse> responses = new ArrayList<>();

        for (RoomTypeInfoDto rt : roomTypes) {
            List<RoomInfoDto> activeRooms = roomApi.getActiveRoomsByRoomType(rt.roomTypeId());
            int totalRooms = activeRooms.size();

            long availableCount = activeRooms.stream()
                    .filter(r -> !occupiedSet.contains(r.roomId()))
                    .count();

            boolean accommodates = rt.maxAdults() >= reqAdults && rt.maxChildren() >= reqChildren;
            boolean isAvailable = availableCount > 0 && accommodates;
            long totalPrice = rt.basePrice() * nights;

            responses.add(new RoomTypeAvailabilityResponse(
                    rt.roomTypeId(),
                    rt.name(),
                    rt.description(),
                    rt.maxAdults(),
                    rt.maxChildren(),
                    rt.bedType(),
                    rt.areaSqm(),
                    rt.basePrice(),
                    nights,
                    totalPrice,
                    totalRooms,
                    (int) availableCount,
                    isAvailable
            ));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<AvailableRoomResponse> getRoomsAvailability(LocalDate checkIn, LocalDate checkOut, Long roomTypeId) {
        validateDateRange(checkIn, checkOut);
        List<Long> occupiedRoomIds = roomNightRepository.findOccupiedRoomIdsBetween(checkIn, checkOut);
        Set<Long> occupiedSet = new HashSet<>(occupiedRoomIds);

        List<RoomInfoDto> activeRooms = (roomTypeId != null && roomTypeId > 0)
                ? roomApi.getActiveRoomsByRoomType(roomTypeId)
                : Collections.emptyList();

        return activeRooms.stream()
                .map(r -> new AvailableRoomResponse(
                        r.roomId(),
                        r.roomNumber(),
                        r.floorNo(),
                        r.roomTypeId(),
                        !occupiedSet.contains(r.roomId())
                ))
                .toList();
    }

    @Transactional
    public ReservationDetailResponse createReservation(ReservationCreateRequest request) {
        validateDateRange(request.checkInDate(), request.checkOutDate());

        if (request.checkInDate().isBefore(LocalDate.now())) {
            throw BusinessException.invalidDateRange("Ngày nhận phòng không được ở trong quá khứ");
        }

        // 1. Kiểm tra khách hàng
        GuestInfoDto guest = guestApi.getGuest(request.guestId());

        int nights = (int) ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());

        // 2. Tìm phòng và tính giá cho từng phòng
        List<Long> occupiedRoomIds = roomNightRepository.findOccupiedRoomIdsBetween(
                request.checkInDate(), request.checkOutDate()
        );
        Set<Long> reservedInThisBooking = new HashSet<>(occupiedRoomIds);

        long totalReservationAmount = 0L;

        record RoomAssignment(Long roomTypeId, Long roomId, Long pricePerNight) {}
        List<RoomAssignment> assignments = new ArrayList<>();

        for (ReservationRoomItemRequest item : request.rooms()) {
            RoomTypeInfoDto roomType = roomApi.getRoomType(item.roomTypeId());
            Long assignedRoomId;

            if (item.specificRoomId() != null) {
                RoomInfoDto specificRoom = roomApi.getRoom(item.specificRoomId());
                if (!specificRoom.roomTypeId().equals(roomType.roomTypeId())) {
                    throw BusinessException.badRequest("Phòng " + specificRoom.roomNumber() + " không thuộc hạng phòng " + roomType.name());
                }
                if (reservedInThisBooking.contains(specificRoom.roomId())) {
                    throw BusinessException.roomNotAvailable("Phòng " + specificRoom.roomNumber() + " đã có khách đặt hoặc bị trùng trong các đêm yêu cầu");
                }
                assignedRoomId = specificRoom.roomId();
            } else {
                List<RoomInfoDto> activeRooms = roomApi.getActiveRoomsByRoomType(roomType.roomTypeId());
                RoomInfoDto availableRoom = activeRooms.stream()
                        .filter(r -> !reservedInThisBooking.contains(r.roomId()))
                        .findFirst()
                        .orElseThrow(() -> BusinessException.roomNotAvailable("Không còn phòng trống cho hạng phòng: " + roomType.name()));
                assignedRoomId = availableRoom.roomId();
            }

            reservedInThisBooking.add(assignedRoomId);
            long pricePerNight = roomType.basePrice();
            totalReservationAmount += pricePerNight * nights;

            assignments.add(new RoomAssignment(roomType.roomTypeId(), assignedRoomId, pricePerNight));
        }

        // 3. Khởi tạo Reservation Entity
        Reservation res = new Reservation();
        res.setReservationCode(generateReservationCode());
        res.setGuestId(guest.guestId());
        res.setBookingSource(request.bookingSource() != null ? request.bookingSource() : BookingSource.WALK_IN);
        res.setStatus(ReservationStatus.PENDING);
        res.setCheckInDate(request.checkInDate());
        res.setCheckOutDate(request.checkOutDate());
        res.setAdults(request.adults() != null ? request.adults() : 1);
        res.setChildren(request.children() != null ? request.children() : 0);
        res.setTotalAmount(totalReservationAmount);
        res.setDepositAmount(0L);
        res.setNote(request.note() != null ? request.note().trim() : null);
        res.setCreatedBy(CurrentUserApi.getCurrentUserId().orElse(null));
        res.setCreatedAt(LocalDateTime.now());

        if (policyService != null) {
            CancellationPolicy cancelPol = request.cancellationPolicyId() != null
                    ? policyService.getCancellationPolicyEntity(request.cancellationPolicyId())
                    : policyService.getDefaultCancellationPolicy();
            res.setCancellationPolicyId(cancelPol.getPolicyId());
            res.setCancellationPolicySnapshot(policyService.createCancellationSnapshotJson(cancelPol));

            EarlyCheckoutPolicy earlyPol = request.earlyCheckoutPolicyId() != null
                    ? policyService.getEarlyCheckoutPolicyEntity(request.earlyCheckoutPolicyId())
                    : policyService.getDefaultEarlyCheckoutPolicy();
            res.setEarlyCheckoutPolicyId(earlyPol.getPolicyId());
            res.setEarlyCheckoutPolicySnapshot(policyService.createEarlyCheckoutSnapshotJson(earlyPol));
        }

        Reservation savedReservation = reservationRepository.save(res);

        // 4. Lưu từng ReservationRoom và ghi giữ chỗ RoomNight
        List<RoomNight> allRoomNights = new ArrayList<>();
        List<ReservationRoom> savedRooms = new ArrayList<>();

        for (RoomAssignment assignment : assignments) {
            ReservationRoom rr = new ReservationRoom();
            rr.setReservation(savedReservation);
            rr.setRoomTypeId(assignment.roomTypeId());
            rr.setRoomId(assignment.roomId());
            rr.setPricePerNight(assignment.pricePerNight());
            rr.setStatus(ReservationRoomStatus.BOOKED);

            ReservationRoom savedRr = reservationRoomRepository.save(rr);
            savedRooms.add(savedRr);

            for (LocalDate d = request.checkInDate(); d.isBefore(request.checkOutDate()); d = d.plusDays(1)) {
                allRoomNights.add(new RoomNight(assignment.roomId(), d, savedRr));
            }
        }

        try {
            roomNightRepository.saveAll(allRoomNights);
            roomNightRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw BusinessException.roomNotAvailable("Phòng đã bị đặt bởi một giao dịch khác cùng thời điểm, vui lòng thử lại");
        }

        return toDetailResponse(savedReservation, guest, savedRooms, Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservations(
            ReservationStatus status,
            Long guestId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String keyword,
            Pageable pageable
    ) {
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        List<Long> matchedGuestIds = Collections.emptyList();
        boolean hasGuestIds = false;

        if (trimmedKeyword != null) {
            matchedGuestIds = guestApi.findGuestIdsByKeyword(trimmedKeyword);
            hasGuestIds = matchedGuestIds != null && !matchedGuestIds.isEmpty();
        }

        Collection<Long> queryGuestIds = hasGuestIds ? matchedGuestIds : Collections.singletonList(-1L);

        Page<Reservation> page = reservationRepository.searchReservations(
                status, guestId, checkInDate, checkOutDate, trimmedKeyword, hasGuestIds, queryGuestIds, pageable
        );

        return page.map(r -> {
            String guestName = "N/A";
            String guestPhone = "N/A";
            String guestIdNumber = null;
            try {
                GuestInfoDto g = guestApi.getGuest(r.getGuestId());
                guestName = g.fullName();
                guestPhone = g.phone();
                guestIdNumber = g.idNumber();
            } catch (Exception ignored) {
            }

            int nights = (int) ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
            List<ReservationRoom> rooms = reservationRoomRepository.findByReservationReservationId(r.getReservationId());

            return ReservationResponse.fromEntity(r, guestName, guestPhone, guestIdNumber, nights, rooms.size());
        });
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationDetail(Long id) {
        Reservation res = findEntityById(id);
        GuestInfoDto guest = guestApi.getGuest(res.getGuestId());
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationReservationId(res.getReservationId());
        List<PaymentInfoDto> payments = billingApi.getReservationPayments(res.getReservationId());

        return toDetailResponse(res, guest, rooms, payments);
    }

    @Transactional
    public ReservationDetailResponse recordDeposit(Long id, ReservationDepositRequest request) {
        Reservation res = findEntityByIdWithLock(id);

        if (res.getStatus() != ReservationStatus.PENDING && res.getStatus() != ReservationStatus.CONFIRMED) {
            throw BusinessException.invalidState("Không thể ghi nhận đặt cọc cho đơn đặt phòng ở trạng thái: " + res.getStatus());
        }

        Long currentUserId = CurrentUserApi.getCurrentUserId().orElse(null);
        billingApi.recordReservationDeposit(
                res.getReservationId(),
                request.amount(),
                request.method(),
                request.referenceCode(),
                currentUserId
        );

        Long totalDeposit = billingApi.calculateTotalDepositByReservationId(res.getReservationId());
        res.setDepositAmount(totalDeposit);

        // Kiểm tra ngưỡng cọc tối thiểu để chuyển từ PENDING sang CONFIRMED
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationReservationId(res.getReservationId());
        long requiredDeposit = calculateRequiredDeposit(rooms);

        if (res.getStatus() == ReservationStatus.PENDING && totalDeposit >= requiredDeposit) {
            res.setStatus(ReservationStatus.CONFIRMED);
        }

        Reservation updated = reservationRepository.save(res);
        GuestInfoDto guest = guestApi.getGuest(updated.getGuestId());
        List<PaymentInfoDto> payments = billingApi.getReservationPayments(updated.getReservationId());

        return toDetailResponse(updated, guest, rooms, payments);
    }

    @Transactional
    public ReservationDetailResponse confirmReservation(Long id) {
        Reservation res = findEntityByIdWithLock(id);

        if (res.getStatus() != ReservationStatus.PENDING) {
            throw BusinessException.invalidState("Chỉ có thể xác nhận đơn đặt phòng ở trạng thái PENDING");
        }

        res.setStatus(ReservationStatus.CONFIRMED);
        Reservation updated = reservationRepository.save(res);

        GuestInfoDto guest = guestApi.getGuest(updated.getGuestId());
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationReservationId(updated.getReservationId());
        List<PaymentInfoDto> payments = billingApi.getReservationPayments(updated.getReservationId());

        return toDetailResponse(updated, guest, rooms, payments);
    }

    @Transactional(readOnly = true)
    public CancellationPreviewResponse previewCancellation(Long id) {
        Reservation res = findEntityById(id);
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationReservationId(res.getReservationId());
        long oneNightTotal = rooms.stream().mapToLong(ReservationRoom::getPricePerNight).sum();
        Long totalDeposit = billingApi.calculateTotalDepositByReservationId(res.getReservationId());

        CancellationPolicySnapshot snapshot = policyService != null
                ? policyService.resolveLatestCancellationSnapshot(res.getCancellationPolicyId(), res.getCancellationPolicySnapshot())
                : new CancellationPolicySnapshot(2L, "MODERATE", "Hủy Tiêu chuẩn (3 ngày)", freeCancelDays * 24, CancellationPenaltyType.ONE_NIGHT, BigDecimal.ZERO);

        LocalDateTime checkInDateTime = res.getCheckInDate().atTime(14, 0);
        LocalDateTime now = LocalDateTime.now();

        CancellationCalculationResult calc = policyService != null
                ? policyService.calculateCancellation(snapshot, checkInDateTime, now, res.getTotalAmount(), oneNightTotal, totalDeposit, null, null)
                : new CancellationCalculationResult(
                        now.isBefore(checkInDateTime.minusDays(freeCancelDays)),
                        now.isBefore(checkInDateTime.minusDays(freeCancelDays)) ? 0 : Math.min(oneNightTotal, totalDeposit),
                        totalDeposit,
                        now.isBefore(checkInDateTime.minusDays(freeCancelDays)) ? totalDeposit : Math.max(0, totalDeposit - oneNightTotal),
                        0,
                        "MODERATE",
                        "Hủy Tiêu chuẩn",
                        "Tính theo cấu hình mặc định"
                );

        return new CancellationPreviewResponse(
                res.getReservationId(),
                res.getReservationCode(),
                res.getTotalAmount(),
                calc.totalDeposit(),
                calc.cancellationFee(),
                calc.refundAmount(),
                calc.shortfallAmount(),
                calc.isFreeCancel(),
                calc.policyCode(),
                calc.policyName(),
                calc.explanation()
        );
    }

    @Transactional
    public ReservationDetailResponse cancelReservation(Long id, ReservationCancelRequest request) {
        Reservation res = findEntityByIdWithLock(id);

        if (res.getStatus() != ReservationStatus.PENDING && res.getStatus() != ReservationStatus.CONFIRMED) {
            throw BusinessException.invalidState("Chỉ được hủy đơn đặt phòng ở trạng thái PENDING hoặc CONFIRMED");
        }

        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationReservationId(res.getReservationId());
        Long currentUserId = CurrentUserApi.getCurrentUserId().orElse(null);
        long oneNightTotal = rooms.stream().mapToLong(ReservationRoom::getPricePerNight).sum();
        Long totalDeposit = billingApi.calculateTotalDepositByReservationId(res.getReservationId());

        CancellationPolicySnapshot snapshot = policyService != null
                ? policyService.resolveLatestCancellationSnapshot(res.getCancellationPolicyId(), res.getCancellationPolicySnapshot())
                : new CancellationPolicySnapshot(2L, "MODERATE", "Hủy Tiêu chuẩn (3 ngày)", freeCancelDays * 24, CancellationPenaltyType.ONE_NIGHT, BigDecimal.ZERO);

        LocalDateTime checkInDateTime = res.getCheckInDate().atTime(14, 0);
        LocalDateTime cancelTime = LocalDateTime.now();

        Long customFee = request != null ? request.customFee() : null;
        String overrideReason = request != null ? request.overrideReason() : null;

        CancellationCalculationResult calc = policyService != null
                ? policyService.calculateCancellation(snapshot, checkInDateTime, cancelTime, res.getTotalAmount(), oneNightTotal, totalDeposit, customFee, overrideReason)
                : new CancellationCalculationResult(
                        cancelTime.isBefore(checkInDateTime.minusDays(freeCancelDays)),
                        cancelTime.isBefore(checkInDateTime.minusDays(freeCancelDays)) ? 0 : Math.min(oneNightTotal, totalDeposit),
                        totalDeposit,
                        cancelTime.isBefore(checkInDateTime.minusDays(freeCancelDays)) ? totalDeposit : Math.max(0, totalDeposit - oneNightTotal),
                        0,
                        "MODERATE",
                        "Hủy Tiêu chuẩn",
                        ""
                );

        long fee = calc.cancellationFee();
        long refundAmount = calc.refundAmount();

        if (refundAmount > 0) {
            billingApi.refundReservationDeposit(
                    res.getReservationId(),
                    refundAmount,
                    "Hoàn cọc hủy đơn " + res.getReservationCode() + " (" + calc.explanation() + ")",
                    currentUserId
            );
            res.setDepositAmount(totalDeposit - refundAmount);
        }

        res.setCancellationFee(fee);
        res.setRefundAmount(refundAmount);
        if (customFee != null) {
            res.setOverrideReason(overrideReason);
            res.setOverrideBy(currentUserId);
        }

        // Giải phóng phòng bằng cách xóa tất cả room_night của đơn này
        roomNightRepository.deleteByReservationId(res.getReservationId());

        // Cập nhật trạng thái
        res.setStatus(ReservationStatus.CANCELLED);
        String cancelNote = (request != null && request.reason() != null && !request.reason().isBlank())
                ? "Lý do hủy: " + request.reason().trim()
                : "Hủy bởi người dùng";
        res.setNote(res.getNote() != null ? res.getNote() + " | " + cancelNote : cancelNote);

        for (ReservationRoom rr : rooms) {
            rr.setStatus(ReservationRoomStatus.CANCELLED);
            reservationRoomRepository.save(rr);
        }

        Reservation updated = reservationRepository.save(res);
        GuestInfoDto guest = guestApi.getGuest(updated.getGuestId());
        List<PaymentInfoDto> payments = billingApi.getReservationPayments(updated.getReservationId());

        return toDetailResponse(updated, guest, rooms, payments);
    }

    @Transactional
    public ReservationDetailResponse markNoShow(Long id) {
        Reservation res = findEntityByIdWithLock(id);

        if (res.getStatus() != ReservationStatus.CONFIRMED) {
            throw BusinessException.invalidState("Chỉ được đánh dấu NO_SHOW cho đơn ở trạng thái CONFIRMED");
        }

        // Giải phóng phòng
        roomNightRepository.deleteByReservationId(res.getReservationId());

        res.setStatus(ReservationStatus.NO_SHOW);
        List<ReservationRoom> rooms = reservationRoomRepository.findByReservationReservationId(res.getReservationId());
        for (ReservationRoom rr : rooms) {
            rr.setStatus(ReservationRoomStatus.CANCELLED);
            reservationRoomRepository.save(rr);
        }

        Reservation updated = reservationRepository.save(res);
        GuestInfoDto guest = guestApi.getGuest(updated.getGuestId());
        List<PaymentInfoDto> payments = billingApi.getReservationPayments(updated.getReservationId());

        return toDetailResponse(updated, guest, rooms, payments);
    }

    public Reservation findEntityById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt phòng có mã: " + id));
    }

    public Reservation findEntityByIdWithLock(Long id) {
        return reservationRepository.findByIdWithLock(id)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy đơn đặt phòng có mã: " + id));
    }

    public long calculateRequiredDeposit(List<ReservationRoom> rooms) {
        return rooms.stream()
                .mapToLong(r -> r.getPricePerNight() * minDepositNights)
                .sum();
    }

    private void validateDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw BusinessException.invalidDateRange("Ngày nhận phòng và ngày trả phòng không được để trống");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw BusinessException.invalidDateRange("Ngày trả phòng (" + checkOut + ") phải sau ngày nhận phòng (" + checkIn + ")");
        }
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

    private ReservationDetailResponse toDetailResponse(
            Reservation res,
            GuestInfoDto guest,
            List<ReservationRoom> rooms,
            List<PaymentInfoDto> payments
    ) {
        int nights = (int) ChronoUnit.DAYS.between(res.getCheckInDate(), res.getCheckOutDate());
        long requiredDeposit = calculateRequiredDeposit(rooms);
        boolean isDepositMet = res.getDepositAmount() >= requiredDeposit;

        List<ReservationRoomResponse> roomResponses = rooms.stream().map(r -> {
            String roomTypeName = "N/A";
            try {
                roomTypeName = roomApi.getRoomType(r.getRoomTypeId()).name();
            } catch (Exception ignored) {
            }

            String roomNumber = "Chưa gán";
            if (r.getRoomId() != null) {
                try {
                    roomNumber = roomApi.getRoom(r.getRoomId()).roomNumber();
                } catch (Exception ignored) {
                }
            }

            return ReservationRoomResponse.fromEntity(r, roomTypeName, roomNumber);
        }).toList();

        CancellationPolicySnapshot cancelSnapshot = policyService != null
                ? policyService.resolveLatestCancellationSnapshot(res.getCancellationPolicyId(), res.getCancellationPolicySnapshot())
                : null;
        EarlyCheckoutPolicySnapshot earlySnapshot = policyService != null
                ? policyService.resolveLatestEarlyCheckoutSnapshot(res.getEarlyCheckoutPolicyId(), res.getEarlyCheckoutPolicySnapshot())
                : null;

        return new ReservationDetailResponse(
                res.getReservationId(),
                res.getReservationCode(),
                guest,
                res.getBookingSource(),
                res.getStatus(),
                res.getCheckInDate(),
                res.getCheckOutDate(),
                nights,
                res.getAdults(),
                res.getChildren(),
                res.getTotalAmount(),
                res.getDepositAmount(),
                requiredDeposit,
                isDepositMet,
                res.getNote(),
                res.getCreatedBy(),
                res.getCreatedAt(),
                roomResponses,
                payments,
                cancelSnapshot,
                earlySnapshot,
                res.getCancellationFee(),
                res.getRefundAmount(),
                res.getOverrideReason()
        );
    }
}
