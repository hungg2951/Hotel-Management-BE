package com.hotel.report.service;

import com.hotel.auth.repository.EmployeeRepository;
import com.hotel.billing.entity.Folio;
import com.hotel.billing.entity.FolioItem;
import com.hotel.billing.entity.Payment;
import com.hotel.billing.repository.FolioItemRepository;
import com.hotel.billing.repository.FolioRepository;
import com.hotel.billing.repository.PaymentRepository;
import com.hotel.booking.entity.Reservation;
import com.hotel.booking.entity.RoomNight;
import com.hotel.booking.repository.ReservationRepository;
import com.hotel.booking.repository.RoomNightRepository;
import com.hotel.common.enums.*;
import com.hotel.common.security.CurrentUserApi;
import com.hotel.common.security.CustomUserDetails;
import com.hotel.frontdesk.repository.StayRepository;
import com.hotel.housekeeping.repository.HousekeepingTaskRepository;
import com.hotel.report.dto.*;
import com.hotel.room.repository.RoomRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final RoomRepository roomRepository;
    private final RoomNightRepository roomNightRepository;
    private final PaymentRepository paymentRepository;
    private final FolioItemRepository folioItemRepository;
    private final ReservationRepository reservationRepository;
    private final StayRepository stayRepository;
    private final FolioRepository folioRepository;
    private final HousekeepingTaskRepository housekeepingTaskRepository;
    private final EmployeeRepository employeeRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public ReportService(
            RoomRepository roomRepository,
            RoomNightRepository roomNightRepository,
            PaymentRepository paymentRepository,
            FolioItemRepository folioItemRepository,
            ReservationRepository reservationRepository,
            StayRepository stayRepository,
            FolioRepository folioRepository,
            HousekeepingTaskRepository housekeepingTaskRepository,
            EmployeeRepository employeeRepository
    ) {
        this.roomRepository = roomRepository;
        this.roomNightRepository = roomNightRepository;
        this.paymentRepository = paymentRepository;
        this.folioItemRepository = folioItemRepository;
        this.reservationRepository = reservationRepository;
        this.stayRepository = stayRepository;
        this.folioRepository = folioRepository;
        this.housekeepingTaskRepository = housekeepingTaskRepository;
        this.employeeRepository = employeeRepository;
    }

    public ReportService(
            RoomRepository roomRepository,
            RoomNightRepository roomNightRepository,
            PaymentRepository paymentRepository,
            FolioItemRepository folioItemRepository
    ) {
        this(roomRepository, roomNightRepository, paymentRepository, folioItemRepository, null, null, null, null, null);
    }

    @Transactional(readOnly = true)
    public RoomsSummaryResponse getRoomsSummary() {
        long totalRooms = roomRepository.countByIsActiveTrue();
        long available = roomRepository.countByStatusAndIsActiveTrue(RoomStatus.AVAILABLE);
        long readyRooms = roomRepository.countByStatusAndHousekeepingStatusInAndIsActiveTrue(
                RoomStatus.AVAILABLE,
                Set.of(HousekeepingStatus.CLEAN, HousekeepingStatus.INSPECTED)
        );
        long occupied = roomRepository.countByStatusAndIsActiveTrue(RoomStatus.OCCUPIED);
        long maintenance = roomRepository.countByStatusAndIsActiveTrue(RoomStatus.MAINTENANCE);
        long outOfOrder = roomRepository.countByStatusAndIsActiveTrue(RoomStatus.OUT_OF_ORDER);

        long clean = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEAN);
        long dirty = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.DIRTY);
        long cleaning = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEANING);
        long inspected = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.INSPECTED);

        return new RoomsSummaryResponse(
                totalRooms,
                available,
                readyRooms,
                occupied,
                maintenance,
                outOfOrder,
                clean,
                dirty,
                cleaning,
                inspected
        );
    }

    @Transactional(readOnly = true)
    public OccupancyResponse getOccupancy(LocalDate date) {
        return getOccupancyPeriod("DAY", date, date);
    }

    @Transactional(readOnly = true)
    public OccupancyResponse getOccupancyPeriod(String period, LocalDate startDate, LocalDate endDate) {
        String p = period != null ? period.toUpperCase() : "DAY";
        LocalDate[] range = resolveDateRange(p, startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        long totalRooms = roomRepository.countByIsActiveTrue();
        long maintenanceRooms = roomRepository.countByStatusAndIsActiveTrue(RoomStatus.MAINTENANCE);
        long outOfOrderRooms = roomRepository.countByStatusAndIsActiveTrue(RoomStatus.OUT_OF_ORDER);
        long operationalRooms = Math.max(0, totalRooms - maintenanceRooms - outOfOrderRooms);

        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        long totalRoomNightsAvailable = operationalRooms * totalDays;

        List<RoomNight> roomNights = roomNightRepository.findByStayDateBetween(start, end);
        long totalRoomNightsOccupied = roomNights.size();

        double averageRate = totalRoomNightsAvailable > 0
                ? Math.round((totalRoomNightsOccupied * 1000.0) / totalRoomNightsAvailable) / 10.0
                : 0.0;

        // Group room nights by date
        Map<LocalDate, Long> occupiedByDate = roomNights.stream()
                .collect(Collectors.groupingBy(rn -> rn.getId().getStayDate(), Collectors.counting()));

        List<OccupancyTimelinePoint> timeline = buildOccupancyTimeline(p, start, end, operationalRooms, occupiedByDate, roomNights);

        return new OccupancyResponse(
                p,
                start,
                end,
                totalRooms,
                operationalRooms,
                totalRoomNightsAvailable,
                totalRoomNightsOccupied,
                maintenanceRooms,
                outOfOrderRooms,
                averageRate,
                timeline
        );
    }

    @Transactional(readOnly = true)
    public RevenueResponse getRevenue(LocalDate date) {
        return getRevenuePeriod("DAY", date, date);
    }

    @Transactional(readOnly = true)
    public RevenueResponse getRevenuePeriod(String period, LocalDate startDate, LocalDate endDate) {
        String p = period != null ? period.toUpperCase() : "DAY";
        LocalDate[] range = resolveDateRange(p, startDate, endDate);
        LocalDate start = range[0];
        LocalDate end = range[1];

        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.plusDays(1).atStartOfDay();

        long successPayments = paymentRepository.calculateTotalSuccessBetween(startDateTime, endDateTime);
        long refundedPayments = paymentRepository.calculateTotalRefundedBetween(startDateTime, endDateTime);
        long collectedRevenue = successPayments - refundedPayments;

        long recognizedRevenue = folioItemRepository.calculateTotalRecognizedBetween(startDateTime, endDateTime);
        long roomRevenue = folioItemRepository.calculateTotalByTypeBetween(FolioItemType.ROOM, startDateTime, endDateTime);
        long serviceRevenue = folioItemRepository.calculateTotalByTypeBetween(FolioItemType.SERVICE, startDateTime, endDateTime);
        long discountTotal = folioItemRepository.calculateTotalByTypeBetween(FolioItemType.DISCOUNT, startDateTime, endDateTime);

        List<Payment> payments = paymentRepository.findByPaidAtBetween(startDateTime, endDateTime);
        List<FolioItem> folioItems = folioItemRepository.findByPostedAtBetween(startDateTime, endDateTime);

        List<RevenueTimelinePoint> timeline = buildRevenueTimeline(p, start, end, payments, folioItems);

        return new RevenueResponse(
                p,
                start,
                end,
                collectedRevenue,
                recognizedRevenue,
                roomRevenue,
                serviceRevenue,
                discountTotal,
                timeline
        );
    }

    private LocalDate[] resolveDateRange(String period, LocalDate startDate, LocalDate endDate) {
        LocalDate baseDate = startDate != null ? startDate : (endDate != null ? endDate : LocalDate.now());
        LocalDate start;
        LocalDate end;

        switch (period) {
            case "WEEK" -> {
                start = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
            }
            case "MONTH" -> {
                start = baseDate.withDayOfMonth(1);
                end = baseDate.withDayOfMonth(baseDate.lengthOfMonth());
            }
            case "YEAR" -> {
                start = LocalDate.of(baseDate.getYear(), 1, 1);
                end = LocalDate.of(baseDate.getYear(), 12, 31);
            }
            case "CUSTOM" -> {
                start = startDate != null ? startDate : baseDate;
                end = endDate != null ? endDate : start;
                if (end.isBefore(start)) {
                    LocalDate tmp = start;
                    start = end;
                    end = tmp;
                }
            }
            default -> { // "DAY"
                start = baseDate;
                end = baseDate;
            }
        }
        return new LocalDate[]{start, end};
    }

    private List<RevenueTimelinePoint> buildRevenueTimeline(
            String period,
            LocalDate start,
            LocalDate end,
            List<Payment> payments,
            List<FolioItem> folioItems
    ) {
        List<RevenueTimelinePoint> timeline = new ArrayList<>();

        if ("YEAR".equals(period)) {
            // Group by 12 months
            for (int m = 1; m <= 12; m++) {
                final int monthIndex = m;
                LocalDate monthStart = LocalDate.of(start.getYear(), m, 1);
                long collected = payments.stream()
                        .filter(p -> p.getPaidAt().getMonthValue() == monthIndex)
                        .mapToLong(p -> p.getStatus() == PaymentStatus.SUCCESS ? p.getAmount() : -p.getAmount())
                        .sum();

                List<FolioItem> mItems = folioItems.stream()
                        .filter(f -> f.getPostedAt().getMonthValue() == monthIndex)
                        .toList();

                long recognized = mItems.stream().mapToLong(FolioItem::getAmount).sum();
                long room = mItems.stream().filter(f -> f.getItemType() == FolioItemType.ROOM).mapToLong(FolioItem::getAmount).sum();
                long service = mItems.stream().filter(f -> f.getItemType() == FolioItemType.SERVICE).mapToLong(FolioItem::getAmount).sum();

                String label = "Tháng " + (m < 10 ? "0" + m : m);
                timeline.add(new RevenueTimelinePoint(label, monthStart, collected, recognized, room, service));
            }
        } else {
            // Daily points for WEEK, MONTH, DAY, CUSTOM
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                final LocalDate currentDay = d;

                long collected = payments.stream()
                        .filter(p -> p.getPaidAt().toLocalDate().equals(currentDay))
                        .mapToLong(p -> p.getStatus() == PaymentStatus.SUCCESS ? p.getAmount() : -p.getAmount())
                        .sum();

                List<FolioItem> dayItems = folioItems.stream()
                        .filter(f -> f.getPostedAt().toLocalDate().equals(currentDay))
                        .toList();

                long recognized = dayItems.stream().mapToLong(FolioItem::getAmount).sum();
                long room = dayItems.stream().filter(f -> f.getItemType() == FolioItemType.ROOM).mapToLong(FolioItem::getAmount).sum();
                long service = dayItems.stream().filter(f -> f.getItemType() == FolioItemType.SERVICE).mapToLong(FolioItem::getAmount).sum();

                String label = formatDayLabel(d, period);
                timeline.add(new RevenueTimelinePoint(label, d, collected, recognized, room, service));
            }
        }

        return timeline;
    }

    private List<OccupancyTimelinePoint> buildOccupancyTimeline(
            String period,
            LocalDate start,
            LocalDate end,
            long operationalRooms,
            Map<LocalDate, Long> occupiedByDate,
            List<RoomNight> allRoomNights
    ) {
        List<OccupancyTimelinePoint> timeline = new ArrayList<>();

        if ("YEAR".equals(period)) {
            // Group by 12 months
            for (int m = 1; m <= 12; m++) {
                final int monthIndex = m;
                LocalDate monthStart = LocalDate.of(start.getYear(), m, 1);
                int daysInMonth = monthStart.lengthOfMonth();
                long available = operationalRooms * daysInMonth;

                long occupied = allRoomNights.stream()
                        .filter(rn -> rn.getId().getStayDate().getMonthValue() == monthIndex)
                        .count();

                double rate = available > 0
                        ? Math.round((occupied * 1000.0) / available) / 10.0
                        : 0.0;

                String label = "Tháng " + (m < 10 ? "0" + m : m);
                timeline.add(new OccupancyTimelinePoint(label, monthStart, Math.round((double) occupied / daysInMonth), rate));
            }
        } else {
            // Daily points for WEEK, MONTH, DAY, CUSTOM
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                long occupied = occupiedByDate.getOrDefault(d, 0L);
                double rate = operationalRooms > 0
                        ? Math.round((occupied * 1000.0) / operationalRooms) / 10.0
                        : 0.0;

                String label = formatDayLabel(d, period);
                timeline.add(new OccupancyTimelinePoint(label, d, occupied, rate));
            }
        }

        return timeline;
    }

    private String formatDayLabel(LocalDate date, String period) {
        if ("WEEK".equals(period)) {
            String dayName = switch (date.getDayOfWeek()) {
                case MONDAY -> "T2";
                case TUESDAY -> "T3";
                case WEDNESDAY -> "T4";
                case THURSDAY -> "T5";
                case FRIDAY -> "T6";
                case SATURDAY -> "T7";
                case SUNDAY -> "CN";
            };
            return String.format("%s (%02d/%02d)", dayName, date.getDayOfMonth(), date.getMonthValue());
        }
        return String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue());
    }

    @Transactional(readOnly = true)
    public OperationsSummaryResponse getOperationsSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();

        long todayArrivals = 0;
        long pendingArrivals = 0;
        long todayDepartures = 0;
        long pendingDepartures = 0;

        if (reservationRepository != null) {
            List<Reservation> todayArrivalsList = reservationRepository.findByCheckInDateAndStatusIn(
                    today,
                    Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN)
            );
            todayArrivals = todayArrivalsList.size();
            pendingArrivals = todayArrivalsList.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED)
                    .count();

            List<Reservation> todayDeparturesList = reservationRepository.findByCheckOutDateAndStatus(
                    today,
                    ReservationStatus.CHECKED_IN
            );
            todayDepartures = todayDeparturesList.size();
            pendingDepartures = todayDepartures;
        }

        long activeStays = stayRepository != null ? stayRepository.countByStatus(StayStatus.IN_HOUSE) : 0;

        long todaySuccess = paymentRepository.calculateTotalSuccessBetween(startToday, endToday);
        long todayRefund = paymentRepository.calculateTotalRefundedBetween(startToday, endToday);
        long todayRevenue = todaySuccess - todayRefund;

        long inHouseUnpaid = 0L;
        if (folioRepository != null) {
            List<Folio> openFolios = folioRepository.findByStatus(FolioStatus.OPEN, Pageable.unpaged()).getContent();
            for (Folio f : openFolios) {
                Long paid = paymentRepository.calculatePaidSuccessByFolioId(f.getFolioId());
                Long ref = paymentRepository.calculateRefundedByFolioId(f.getFolioId());
                long net = (paid != null ? paid : 0L) - (ref != null ? ref : 0L);
                long bal = Math.max(0L, f.getTotalAmount() - net);
                inHouseUnpaid += bal;
            }
        }

        long readyRooms = roomRepository.countByStatusAndHousekeepingStatusInAndIsActiveTrue(
                RoomStatus.AVAILABLE,
                Set.of(HousekeepingStatus.CLEAN, HousekeepingStatus.INSPECTED)
        );
        long dirtyRooms = roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.DIRTY);

        return new OperationsSummaryResponse(
                todayArrivals,
                todayDepartures,
                activeStays,
                todayRevenue,
                inHouseUnpaid,
                pendingArrivals,
                pendingDepartures,
                readyRooms,
                dirtyRooms
        );
    }

    @Transactional(readOnly = true)
    public HousekeeperPerformanceResponse getHousekeeperPerformance() {
        long totalTasks = housekeepingTaskRepository != null ? housekeepingTaskRepository.count() : 0;
        long pendingTasks = housekeepingTaskRepository != null ? housekeepingTaskRepository.countByStatus(HousekeepingTaskStatus.PENDING) : 0;
        long inProgressTasks = housekeepingTaskRepository != null ? housekeepingTaskRepository.countByStatus(HousekeepingTaskStatus.IN_PROGRESS) : 0;
        long doneTasks = housekeepingTaskRepository != null ? housekeepingTaskRepository.countByStatus(HousekeepingTaskStatus.DONE) : 0;

        Long currentEmpId = CurrentUserApi.getCurrentEmployeeId().orElse(null);
        long myAssigned = 0L;
        long myDone = 0L;
        long myPending = 0L;
        long myInProgress = 0L;

        if (currentEmpId != null && housekeepingTaskRepository != null) {
            myAssigned = housekeepingTaskRepository.countByAssignedTo(currentEmpId);
            myDone = housekeepingTaskRepository.countByAssignedToAndStatus(currentEmpId, HousekeepingTaskStatus.DONE);
            myPending = housekeepingTaskRepository.countByAssignedToAndStatus(currentEmpId, HousekeepingTaskStatus.PENDING);
            myInProgress = housekeepingTaskRepository.countByAssignedToAndStatus(currentEmpId, HousekeepingTaskStatus.IN_PROGRESS);
        }

        CustomUserDetails user = CurrentUserApi.getCurrentUser().orElse(null);
        boolean isManagerOrAdmin = user != null && user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));

        List<HousekeeperPerformanceResponse.PerformerStat> topPerformers = new ArrayList<>();
        if (isManagerOrAdmin && housekeepingTaskRepository != null && employeeRepository != null) {
            List<Object[]> rows = housekeepingTaskRepository.findTopPerformers();
            for (Object[] r : rows) {
                Long empId = (Long) r[0];
                long count = ((Number) r[1]).longValue();
                String name = employeeRepository.findById(empId)
                        .map(com.hotel.auth.entity.Employee::getFullName)
                        .orElse("Nhân viên #" + empId);
                topPerformers.add(new HousekeeperPerformanceResponse.PerformerStat(empId, name, count));
                if (topPerformers.size() >= 5) break;
            }
        }

        return new HousekeeperPerformanceResponse(
                totalTasks,
                pendingTasks,
                inProgressTasks,
                doneTasks,
                myAssigned,
                myDone,
                myPending,
                myInProgress,
                topPerformers
        );
    }

    @Transactional(readOnly = true)
    public FinancialSummaryResponse getFinancialSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.plusDays(1).atStartOfDay();

        LocalDateTime startMonth = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime endMonth = today.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay();

        long todaySuccess = paymentRepository.calculateTotalSuccessBetween(startToday, endToday);
        long todayRefund = paymentRepository.calculateTotalRefundedBetween(startToday, endToday);
        long todayRevenue = todaySuccess - todayRefund;

        long monthSuccess = paymentRepository.calculateTotalSuccessBetween(startMonth, endMonth);
        long monthRefund = paymentRepository.calculateTotalRefundedBetween(startMonth, endMonth);
        long monthRevenue = monthSuccess - monthRefund;

        long advanceDepositHeld = 0L;
        if (reservationRepository != null) {
            List<Reservation> upcoming = reservationRepository.findAll().stream()
                    .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.PENDING)
                    .toList();
            for (Reservation r : upcoming) {
                Long dep = paymentRepository.calculateDepositSuccessByReservationId(r.getReservationId());
                Long depRef = paymentRepository.calculateDepositRefundedByReservationId(r.getReservationId());
                advanceDepositHeld += (dep != null ? dep : 0L) - (depRef != null ? depRef : 0L);
            }
        }

        long inHouseUnpaid = 0L;
        if (folioRepository != null) {
            List<Folio> openFolios = folioRepository.findByStatus(FolioStatus.OPEN, Pageable.unpaged()).getContent();
            for (Folio f : openFolios) {
                Long paid = paymentRepository.calculatePaidSuccessByFolioId(f.getFolioId());
                Long ref = paymentRepository.calculateRefundedByFolioId(f.getFolioId());
                long net = (paid != null ? paid : 0L) - (ref != null ? ref : 0L);
                inHouseUnpaid += Math.max(0L, f.getTotalAmount() - net);
            }
        }

        long cancelFee = folioItemRepository.calculateTotalByTypeBetween(FolioItemType.PENALTY, startMonth, endMonth);

        OccupancyResponse occ = getOccupancyPeriod("MONTH", today.with(TemporalAdjusters.firstDayOfMonth()), today.with(TemporalAdjusters.lastDayOfMonth()));
        double occupancyRate = occ.occupancyRate();

        long maintenanceRooms = roomRepository.countByStatusAndIsActiveTrue(RoomStatus.MAINTENANCE) +
                roomRepository.countByStatusAndIsActiveTrue(RoomStatus.OUT_OF_ORDER);

        return new FinancialSummaryResponse(
                todayRevenue,
                monthRevenue,
                advanceDepositHeld,
                monthRefund,
                inHouseUnpaid,
                cancelFee,
                occupancyRate,
                maintenanceRooms
        );
    }
}
