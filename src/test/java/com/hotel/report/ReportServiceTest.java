package com.hotel.report;

import com.hotel.billing.entity.FolioItem;
import com.hotel.billing.entity.Payment;
import com.hotel.billing.repository.FolioItemRepository;
import com.hotel.billing.repository.PaymentRepository;
import com.hotel.booking.entity.RoomNight;
import com.hotel.booking.entity.RoomNightId;
import com.hotel.booking.repository.RoomNightRepository;
import com.hotel.common.enums.FolioItemType;
import com.hotel.common.enums.HousekeepingStatus;
import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.PaymentStatus;
import com.hotel.common.enums.RoomStatus;
import com.hotel.report.dto.OccupancyResponse;
import com.hotel.report.dto.RevenueResponse;
import com.hotel.report.dto.RoomsSummaryResponse;
import com.hotel.report.service.ReportService;
import com.hotel.room.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomNightRepository roomNightRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FolioItemRepository folioItemRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void testGetRoomsSummary_Success() {
        when(roomRepository.countByIsActiveTrue()).thenReturn(20L);
        when(roomRepository.countByStatusAndIsActiveTrue(RoomStatus.AVAILABLE)).thenReturn(12L);
        when(roomRepository.countByStatusAndHousekeepingStatusInAndIsActiveTrue(eq(RoomStatus.AVAILABLE), any())).thenReturn(9L);
        when(roomRepository.countByStatusAndIsActiveTrue(RoomStatus.OCCUPIED)).thenReturn(6L);
        when(roomRepository.countByStatusAndIsActiveTrue(RoomStatus.MAINTENANCE)).thenReturn(1L);
        when(roomRepository.countByStatusAndIsActiveTrue(RoomStatus.OUT_OF_ORDER)).thenReturn(1L);

        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEAN)).thenReturn(10L);
        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.DIRTY)).thenReturn(5L);
        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.CLEANING)).thenReturn(3L);
        when(roomRepository.countByHousekeepingStatusAndIsActiveTrue(HousekeepingStatus.INSPECTED)).thenReturn(2L);

        RoomsSummaryResponse resp = reportService.getRoomsSummary();
        assertNotNull(resp);
        assertEquals(20L, resp.totalRooms());
        assertEquals(12L, resp.availableRooms());
        assertEquals(9L, resp.readyRooms());
        assertEquals(6L, resp.occupiedRooms());
        assertEquals(10L, resp.cleanRooms());
    }

    @Test
    void testGetOccupancy_Today_Success() {
        when(roomRepository.countByIsActiveTrue()).thenReturn(20L);
        when(roomRepository.countByStatusAndIsActiveTrue(RoomStatus.MAINTENANCE)).thenReturn(2L);
        when(roomRepository.countByStatusAndIsActiveTrue(RoomStatus.OUT_OF_ORDER)).thenReturn(0L);

        RoomNight rn = new RoomNight();
        rn.setId(new RoomNightId(101L, LocalDate.now()));
        when(roomNightRepository.findByStayDateBetween(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rn));

        OccupancyResponse resp = reportService.getOccupancy(LocalDate.now());
        assertNotNull(resp);
        assertEquals(20L, resp.totalRooms());
        assertEquals(18L, resp.operationalRooms());
        assertEquals(1L, resp.totalRoomNightsOccupied());
        assertEquals(5.6, resp.occupancyRate());
    }

    @Test
    void testGetRevenue_Success() {
        when(paymentRepository.calculateTotalSuccessBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5000000L);
        when(paymentRepository.calculateTotalRefundedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(200000L);

        when(folioItemRepository.calculateTotalRecognizedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(4800000L);
        when(folioItemRepository.calculateTotalByTypeBetween(eq(FolioItemType.ROOM), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(4000000L);
        when(folioItemRepository.calculateTotalByTypeBetween(eq(FolioItemType.SERVICE), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(800000L);
        when(folioItemRepository.calculateTotalByTypeBetween(eq(FolioItemType.DISCOUNT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        when(paymentRepository.findByPaidAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(folioItemRepository.findByPostedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        RevenueResponse resp = reportService.getRevenue(LocalDate.now());
        assertNotNull(resp);
        assertEquals(4800000L, resp.collectedRevenue());
        assertEquals(4800000L, resp.recognizedRevenue());
        assertEquals(4000000L, resp.roomRevenue());
        assertEquals(800000L, resp.serviceRevenue());
    }

    @Test
    void testGetRevenuePeriod_Week_Success() {
        when(paymentRepository.calculateTotalSuccessBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10000000L);
        when(paymentRepository.calculateTotalRefundedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        when(folioItemRepository.calculateTotalRecognizedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10000000L);
        when(folioItemRepository.calculateTotalByTypeBetween(eq(FolioItemType.ROOM), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(9000000L);
        when(folioItemRepository.calculateTotalByTypeBetween(eq(FolioItemType.SERVICE), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(1000000L);
        when(folioItemRepository.calculateTotalByTypeBetween(eq(FolioItemType.DISCOUNT), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);

        when(paymentRepository.findByPaidAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(folioItemRepository.findByPostedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        RevenueResponse resp = reportService.getRevenuePeriod("WEEK", LocalDate.now(), null);
        assertNotNull(resp);
        assertEquals("WEEK", resp.period());
        assertEquals(7, resp.timeline().size());
        assertEquals(10000000L, resp.collectedRevenue());
    }
}
