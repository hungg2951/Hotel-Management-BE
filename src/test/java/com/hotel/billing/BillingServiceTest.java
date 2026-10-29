package com.hotel.billing;

import com.hotel.billing.dto.*;
import com.hotel.billing.entity.*;
import com.hotel.billing.repository.*;
import com.hotel.billing.service.BillingService;
import com.hotel.common.enums.FolioItemType;
import com.hotel.common.enums.FolioStatus;
import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.PaymentStatus;
import com.hotel.common.enums.ServiceCategory;
import com.hotel.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private FolioRepository folioRepository;

    @Mock
    private FolioItemRepository folioItemRepository;

    @Mock
    private HotelServiceRepository hotelServiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    private BillingService billingService;

    private Folio folio;
    private HotelService hotelService;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(
                folioRepository,
                folioItemRepository,
                hotelServiceRepository,
                paymentRepository,
                invoiceRepository,
                BigDecimal.valueOf(8.00)
        );

        folio = new Folio();
        folio.setFolioId(100L);
        folio.setStayId(200L);
        folio.setStatus(FolioStatus.OPEN);
        folio.setTotalAmount(1000000L);

        hotelService = new HotelService();
        hotelService.setServiceId(1L);
        hotelService.setName("Bữa sáng");
        hotelService.setCategory(ServiceCategory.FOOD);
        hotelService.setUnit("suất");
        hotelService.setUnitPrice(80000L);
        hotelService.setIsActive(true);
    }

    @Test
    void testAddFolioItem_ServiceItem_Success() {
        FolioItemRequest request = new FolioItemRequest(FolioItemType.SERVICE, 1L, null, 2, null);

        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(hotelServiceRepository.findById(1L)).thenReturn(Optional.of(hotelService));
        when(folioItemRepository.save(any(FolioItem.class))).thenAnswer(inv -> {
            FolioItem item = inv.getArgument(0);
            item.setItemId(10L);
            return item;
        });

        FolioItemResponse response = billingService.addFolioItem(100L, request);

        assertNotNull(response);
        assertEquals(FolioItemType.SERVICE, response.itemType());
        assertEquals(80000L, response.unitPrice());
        assertEquals(160000L, response.amount());
        assertEquals("Bữa sáng", response.description());
        assertEquals(1160000L, folio.getTotalAmount());
        verify(folioRepository).save(folio);
    }

    @Test
    void testAddFolioItem_DiscountItem_NegativeAmount() {
        FolioItemRequest request = new FolioItemRequest(FolioItemType.DISCOUNT, null, "Giảm giá VIP", 1, 100000L);

        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(folioItemRepository.save(any(FolioItem.class))).thenAnswer(inv -> {
            FolioItem item = inv.getArgument(0);
            item.setItemId(11L);
            return item;
        });

        FolioItemResponse response = billingService.addFolioItem(100L, request);

        assertNotNull(response);
        assertEquals(FolioItemType.DISCOUNT, response.itemType());
        assertEquals(-100000L, response.amount());
        assertEquals(900000L, folio.getTotalAmount());
    }

    @Test
    void testAddFolioItem_ClosedFolio_ThrowsInvalidState() {
        folio.setStatus(FolioStatus.CLOSED);
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));

        FolioItemRequest request = new FolioItemRequest(FolioItemType.PENALTY, null, "Làm vỡ ly", 1, 50000L);

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.addFolioItem(100L, request));
        assertEquals("INVALID_STATE", ex.getErrorCode());
    }

    @Test
    void testAddFolioItem_InactiveService_ThrowsBadRequest() {
        hotelService.setIsActive(false);
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(hotelServiceRepository.findById(1L)).thenReturn(Optional.of(hotelService));

        FolioItemRequest request = new FolioItemRequest(FolioItemType.SERVICE, 1L, null, 1, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.addFolioItem(100L, request));
        assertEquals("BAD_REQUEST", ex.getErrorCode());
    }

    @Test
    void testRemoveFolioItem_Success() {
        FolioItem item = new FolioItem();
        item.setItemId(10L);
        item.setFolio(folio);
        item.setItemType(FolioItemType.SERVICE);
        item.setAmount(160000L);

        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(folioItemRepository.findByItemIdAndFolioFolioId(10L, 100L)).thenReturn(Optional.of(item));

        billingService.removeFolioItem(100L, 10L);

        assertEquals(840000L, folio.getTotalAmount());
        verify(folioRepository).save(folio);
        verify(folioItemRepository).delete(item);
    }

    @Test
    void testRemoveFolioItem_RoomCharge_ThrowsBusinessException() {
        FolioItem item = new FolioItem();
        item.setItemId(10L);
        item.setFolio(folio);
        item.setItemType(FolioItemType.ROOM);
        item.setAmount(500000L);

        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(folioItemRepository.findByItemIdAndFolioFolioId(10L, 100L)).thenReturn(Optional.of(item));

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.removeFolioItem(100L, 10L));
        assertEquals("BAD_REQUEST", ex.getErrorCode());
        assertTrue(ex.getMessage().contains("Không được phép xóa trực tiếp khoản tiền thuê phòng"));
        verify(folioItemRepository, never()).delete(any());
    }

    @Test
    void testRemoveFolioItem_NotFound_ThrowsBusinessException() {
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(folioItemRepository.findByItemIdAndFolioFolioId(999L, 100L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.removeFolioItem(100L, 999L));
        assertEquals("NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void testRecordPayment_Success_WithinBalance() {
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(paymentRepository.calculatePaidSuccessByFolioId(100L)).thenReturn(400000L);
        when(paymentRepository.calculateRefundedByFolioId(100L)).thenReturn(0L);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId(50L);
            return p;
        });

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, 600000L, PaymentStatus.SUCCESS, "Tiền mặt", null);
        PaymentResponse response = billingService.recordPayment(100L, request);

        assertNotNull(response);
        assertEquals(50L, response.paymentId());
        assertEquals(600000L, response.amount());
        assertEquals(PaymentMethod.CASH, response.method());
    }

    @Test
    void testRecordPayment_Overpayment_ThrowsPaymentExceedsBalance() {
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(paymentRepository.calculatePaidSuccessByFolioId(100L)).thenReturn(400000L);
        when(paymentRepository.calculateRefundedByFolioId(100L)).thenReturn(0L);

        PaymentRequest request = new PaymentRequest(PaymentMethod.CARD, 700000L, PaymentStatus.SUCCESS, "Quẹt thẻ", null);

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.recordPayment(100L, request));
        assertEquals("PAYMENT_EXCEEDS_BALANCE", ex.getErrorCode());
    }

    @Test
    void testRecordPayment_RefundWithinPaid_Success() {
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(paymentRepository.calculatePaidSuccessByFolioId(100L)).thenReturn(600000L);
        when(paymentRepository.calculateRefundedByFolioId(100L)).thenReturn(100000L); // net paid = 500000L

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setPaymentId(51L);
            return p;
        });

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, 200000L, PaymentStatus.REFUNDED, "Hoàn cọc", null);
        PaymentResponse response = billingService.recordPayment(100L, request);

        assertNotNull(response);
        assertEquals(PaymentStatus.REFUNDED, response.status());
        assertEquals(200000L, response.amount());
    }

    @Test
    void testRecordPayment_RefundExceedsPaid_ThrowsBadRequest() {
        when(folioRepository.findByIdWithLock(100L)).thenReturn(Optional.of(folio));
        when(paymentRepository.calculatePaidSuccessByFolioId(100L)).thenReturn(600000L);
        when(paymentRepository.calculateRefundedByFolioId(100L)).thenReturn(200000L); // net paid = 400000L

        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, 500000L, PaymentStatus.REFUNDED, "Hoàn quá số đã trả", null);

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.recordPayment(100L, request));
        assertEquals("BAD_REQUEST", ex.getErrorCode());
    }

    @Test
    void testCreateInvoice_UnsettledFolio_ThrowsFolioNotSettled() {
        when(folioRepository.findById(100L)).thenReturn(Optional.of(folio));
        when(invoiceRepository.existsByFolioFolioId(100L)).thenReturn(false);
        when(paymentRepository.calculatePaidSuccessByFolioId(100L)).thenReturn(600000L);
        when(paymentRepository.calculateRefundedByFolioId(100L)).thenReturn(0L); // remaining balance = 400000L != 0

        InvoiceRequest request = new InvoiceRequest("Công ty ABC", "0101234567", "Hà Nội");

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.createInvoice(100L, request));
        assertEquals("FOLIO_NOT_SETTLED", ex.getErrorCode());
    }

    @Test
    void testCreateInvoice_DuplicateInvoice_ThrowsConflict() {
        when(folioRepository.findById(100L)).thenReturn(Optional.of(folio));
        when(invoiceRepository.existsByFolioFolioId(100L)).thenReturn(true);

        InvoiceRequest request = new InvoiceRequest("Công ty ABC", "0101234567", "Hà Nội");

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.createInvoice(100L, request));
        assertEquals("CONFLICT", ex.getErrorCode());
    }

    @Test
    void testCreateInvoice_SettledFolio_CalculatesVat8Percent_KeepsFolioOpen() {
        folio.setTotalAmount(1080000L);
        when(folioRepository.findById(100L)).thenReturn(Optional.of(folio));
        when(invoiceRepository.existsByFolioFolioId(100L)).thenReturn(false);
        when(paymentRepository.calculatePaidSuccessByFolioId(100L)).thenReturn(1080000L);
        when(paymentRepository.calculateRefundedByFolioId(100L)).thenReturn(0L);
        when(invoiceRepository.existsByInvoiceNo(anyString())).thenReturn(false);

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setInvoiceId(1L);
            return i;
        });

        InvoiceRequest request = new InvoiceRequest("Công ty TNHH Demo", "0123456789", "TP. Hồ Chí Minh");
        InvoiceResponse response = billingService.createInvoice(100L, request);

        assertNotNull(response);
        assertEquals(1L, response.invoiceId());
        assertEquals("Công ty TNHH Demo", response.buyerName());
        assertEquals(1080000L, response.totalAmount());
        // Giá niêm yết đã bao gồm VAT 8%:
        // vatAmount = round(1080000 * 8 / 108) = 80000
        // subtotal = 1080000 - 80000 = 1000000
        assertEquals(80000L, response.vatAmount());
        assertEquals(1000000L, response.subtotal());
        assertEquals(BigDecimal.valueOf(8.00).setScale(2), response.vatRate());

        // CRITICAL CHECK: Folio remains OPEN (not closed until check-out in Stage 5)
        assertEquals(FolioStatus.OPEN, folio.getStatus());
    }

    @Test
    void testGetFolioDetail_Success() {
        when(folioRepository.findById(100L)).thenReturn(Optional.of(folio));
        when(paymentRepository.calculatePaidSuccessByFolioId(100L)).thenReturn(500000L);
        when(paymentRepository.calculateRefundedByFolioId(100L)).thenReturn(0L);
        when(folioItemRepository.findByFolioFolioIdOrderByPostedAtAsc(100L)).thenReturn(List.of());
        when(paymentRepository.findByFolioFolioIdOrderByPaidAtAsc(100L)).thenReturn(List.of());
        when(invoiceRepository.findByFolioFolioId(100L)).thenReturn(Optional.empty());

        FolioDetailResponse response = billingService.getFolioDetail(100L);

        assertNotNull(response);
        assertEquals(100L, response.folioId());
        assertEquals(1000000L, response.totalAmount());
        assertEquals(500000L, response.paidAmount());
        assertEquals(500000L, response.balance());
        assertEquals(FolioStatus.OPEN, response.status());
    }
}
