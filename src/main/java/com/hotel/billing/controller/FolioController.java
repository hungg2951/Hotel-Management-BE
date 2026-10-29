package com.hotel.billing.controller;

import com.hotel.billing.api.FolioSummaryDto;
import com.hotel.billing.dto.*;
import com.hotel.billing.service.BillingService;
import com.hotel.common.enums.FolioStatus;
import com.hotel.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import com.hotel.billing.api.BillingApi;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/folios")
public class FolioController {

    private final BillingService billingService;
    private final BillingApi billingApi;

    public FolioController(BillingService billingService, BillingApi billingApi) {
        this.billingService = billingService;
        this.billingApi = billingApi;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<Page<FolioSummaryDto>>> getFolios(
            @RequestParam(required = false) FolioStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<FolioSummaryDto> page = billingService.getFolios(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách Folio thành công"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<Long>> openFolio(@RequestParam Long stayId) {
        Long folioId = billingApi.openFolio(stayId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(folioId, "Mở Folio mới thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FolioDetailResponse>> getFolioById(@PathVariable Long id) {
        FolioDetailResponse response = billingService.getFolioDetail(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết Folio thành công"));
    }

    @GetMapping("/by-stay/{stayId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FolioDetailResponse>> getFolioByStayId(@PathVariable Long stayId) {
        FolioDetailResponse response = billingService.getFolioDetailByStayId(stayId);
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy chi tiết Folio theo lượt lưu trú thành công"));
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<FolioItemResponse>> addFolioItem(
            @PathVariable Long id,
            @Valid @RequestBody FolioItemRequest request
    ) {
        FolioItemResponse response = billingService.addFolioItem(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{itemId}")
                .buildAndExpand(response.itemId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Thêm khoản phí vào Folio thành công"));
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeFolioItem(
            @PathVariable Long id,
            @PathVariable Long itemId
    ) {
        billingService.removeFolioItem(id, itemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa khoản phí khỏi Folio thành công"));
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'RECEPTIONIST', 'ACCOUNTANT')")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = billingService.recordPayment(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(response.paymentId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Ghi nhận thanh toán thành công"));
    }

    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'ACCOUNTANT', 'RECEPTIONIST')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) InvoiceRequest request
    ) {
        InvoiceRequest invoiceRequest = request != null ? request : new InvoiceRequest(null, null, null);
        InvoiceResponse response = billingService.createInvoice(id, invoiceRequest);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/invoices/{invoiceId}")
                .buildAndExpand(response.invoiceId())
                .toUri();
        return ResponseEntity.created(location)
                .body(ApiResponse.success(response, "Xuất hóa đơn VAT thành công"));
    }
}
