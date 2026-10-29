package com.hotel.billing;

import com.hotel.billing.controller.FolioController;
import com.hotel.billing.dto.*;
import com.hotel.billing.service.BillingService;
import com.hotel.common.enums.FolioItemType;
import com.hotel.common.enums.FolioStatus;
import com.hotel.common.enums.PaymentMethod;
import com.hotel.common.enums.PaymentStatus;
import com.hotel.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FolioControllerTest {

    @Mock
    private BillingService billingService;

    @InjectMocks
    private FolioController folioController;

    @BeforeEach
    void setupRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void testGetFolioById() {
        FolioDetailResponse detail = new FolioDetailResponse(
                100L, 200L, FolioStatus.OPEN, 1000000L, 400000L, 600000L,
                LocalDateTime.now(), null, List.of(), List.of(), null
        );
        when(billingService.getFolioDetail(100L)).thenReturn(detail);

        ResponseEntity<ApiResponse<FolioDetailResponse>> response = folioController.getFolioById(100L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().success());
        assertEquals(100L, response.getBody().data().folioId());
        assertEquals(600000L, response.getBody().data().balance());
    }

    @Test
    void testAddFolioItem() {
        FolioItemRequest request = new FolioItemRequest(FolioItemType.SERVICE, 1L, "Giặt ủi", 2, 30000L);
        FolioItemResponse itemResponse = new FolioItemResponse(
                10L, 100L, FolioItemType.SERVICE, 1L, "Giặt ủi", "Giặt ủi", 2, 30000L, 60000L, LocalDateTime.now(), 1L
        );
        when(billingService.addFolioItem(eq(100L), any(FolioItemRequest.class))).thenReturn(itemResponse);

        ResponseEntity<ApiResponse<FolioItemResponse>> response = folioController.addFolioItem(100L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(10L, response.getBody().data().itemId());
        assertEquals(60000L, response.getBody().data().amount());
    }

    @Test
    void testRemoveFolioItem() {
        doNothing().when(billingService).removeFolioItem(100L, 10L);

        ResponseEntity<ApiResponse<Void>> response = folioController.removeFolioItem(100L, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingService).removeFolioItem(100L, 10L);
    }

    @Test
    void testRecordPayment() {
        PaymentRequest request = new PaymentRequest(PaymentMethod.CASH, 500000L, PaymentStatus.SUCCESS, "Tiền mặt", null);
        PaymentResponse paymentResponse = new PaymentResponse(
                1L, 100L, null, PaymentMethod.CASH, 500000L, PaymentStatus.SUCCESS, "Tiền mặt", LocalDateTime.now(), 1L
        );
        when(billingService.recordPayment(eq(100L), any(PaymentRequest.class))).thenReturn(paymentResponse);

        ResponseEntity<ApiResponse<PaymentResponse>> response = folioController.recordPayment(100L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500000L, response.getBody().data().amount());
    }

    @Test
    void testCreateInvoice() {
        InvoiceRequest request = new InvoiceRequest("Khách hàng A", "0100000000", "Hà Nội");
        InvoiceResponse invoiceResponse = new InvoiceResponse(
                1L, 100L, "INV-20261028-123456", "Khách hàng A", "0100000000", "Hà Nội",
                1000000L, BigDecimal.valueOf(8.00), 80000L, 1080000L, LocalDateTime.now(), 1L
        );
        when(billingService.createInvoice(eq(100L), any(InvoiceRequest.class))).thenReturn(invoiceResponse);

        ResponseEntity<ApiResponse<InvoiceResponse>> response = folioController.createInvoice(100L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INV-20261028-123456", response.getBody().data().invoiceNo());
        assertEquals(1080000L, response.getBody().data().totalAmount());
    }
}
