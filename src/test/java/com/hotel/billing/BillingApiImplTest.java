package com.hotel.billing;

import com.hotel.billing.api.FolioSummaryDto;
import com.hotel.billing.entity.Folio;
import com.hotel.billing.entity.FolioItem;
import com.hotel.billing.entity.Payment;
import com.hotel.billing.repository.FolioItemRepository;
import com.hotel.billing.repository.FolioRepository;
import com.hotel.billing.repository.PaymentRepository;
import com.hotel.billing.service.BillingApiImpl;
import com.hotel.billing.service.BillingService;
import com.hotel.common.enums.FolioItemType;
import com.hotel.common.enums.FolioStatus;
import com.hotel.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingApiImplTest {

    @Mock
    private FolioRepository folioRepository;

    @Mock
    private FolioItemRepository folioItemRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BillingService billingService;

    @InjectMocks
    private BillingApiImpl billingApi;

    private Folio folio;

    @BeforeEach
    void setUp() {
        folio = new Folio();
        folio.setFolioId(100L);
        folio.setStayId(200L);
        folio.setStatus(FolioStatus.OPEN);
        folio.setTotalAmount(0L);
    }

    @Test
    void testOpenFolio_Success() {
        when(folioRepository.existsByStayId(200L)).thenReturn(false);
        when(folioRepository.save(any(Folio.class))).thenAnswer(inv -> {
            Folio f = inv.getArgument(0);
            f.setFolioId(101L);
            return f;
        });

        Long folioId = billingApi.openFolio(200L);

        assertEquals(101L, folioId);
        verify(folioRepository).save(any(Folio.class));
    }

    @Test
    void testOpenFolio_DuplicateStayId_ThrowsBusinessException() {
        when(folioRepository.existsByStayId(200L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> billingApi.openFolio(200L));
        assertEquals("DUPLICATE", ex.getErrorCode());
    }

    @Test
    void testGetFolioSummary_Success() {
        folio.setTotalAmount(1200000L);
        when(billingService.findFolioEntityById(100L)).thenReturn(folio);
        when(billingService.calculateNetPaid(100L)).thenReturn(1200000L);

        FolioSummaryDto summary = billingApi.getFolioSummary(100L);

        assertNotNull(summary);
        assertEquals(100L, summary.folioId());
        assertEquals(200L, summary.stayId());
        assertEquals(1200000L, summary.totalAmount());
        assertEquals(1200000L, summary.paidAmount());
        assertEquals(0L, summary.balance());
        assertTrue(summary.isSettled());
    }

    @Test
    void testPostRoomCharge_Success() {
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));

        billingApi.postRoomCharge(100L, 800000L, "Tiền phòng đêm 1", 5L);

        assertEquals(800000L, folio.getTotalAmount());
        verify(folioItemRepository).save(any(FolioItem.class));
        verify(folioRepository).save(folio);
    }

    @Test
    void testAttachReservationDepositsToFolio_Success() {
        Payment deposit1 = new Payment();
        deposit1.setPaymentId(1L);
        deposit1.setReservationId(300L);
        deposit1.setAmount(500000L);

        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(paymentRepository.findByReservationIdAndFolioIsNull(300L)).thenReturn(List.of(deposit1));

        billingApi.attachReservationDepositsToFolio(300L, 100L);

        assertEquals(folio, deposit1.getFolio());
        verify(paymentRepository).save(deposit1);
    }

    @Test
    void testCloseFolio_Unsettled_ThrowsFolioNotSettled() {
        folio.setTotalAmount(500000L);
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(billingService.calculateNetPaid(100L)).thenReturn(300000L); // remaining balance 200000

        BusinessException ex = assertThrows(BusinessException.class, () -> billingApi.closeFolio(100L));
        assertEquals("FOLIO_NOT_SETTLED", ex.getErrorCode());
    }

    @Test
    void testCloseFolio_Settled_Success() {
        folio.setTotalAmount(500000L);
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(billingService.calculateNetPaid(100L)).thenReturn(500000L);

        billingApi.closeFolio(100L);

        assertEquals(FolioStatus.CLOSED, folio.getStatus());
        assertNotNull(folio.getClosedAt());
        verify(folioRepository).save(folio);
    }
}
