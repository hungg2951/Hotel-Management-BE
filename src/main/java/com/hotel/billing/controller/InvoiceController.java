package com.hotel.billing.controller;

import com.hotel.billing.dto.InvoiceResponse;
import com.hotel.billing.service.BillingService;
import com.hotel.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final BillingService billingService;

    public InvoiceController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse response = billingService.getInvoiceById(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin hóa đơn VAT thành công"));
    }

    @GetMapping("/by-folio/{folioId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByFolioId(@PathVariable Long folioId) {
        InvoiceResponse response = billingService.getInvoiceByFolioId(folioId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin hóa đơn VAT theo Folio thành công"));
    }
}
