package com.hotel.billing.dto;

import com.hotel.billing.entity.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long invoiceId,
        Long folioId,
        String invoiceNo,
        String buyerName,
        String buyerTaxCode,
        String buyerAddress,
        Long subtotal,
        BigDecimal vatRate,
        Long vatAmount,
        Long totalAmount,
        LocalDateTime issuedAt,
        Long issuedBy
) {
    public static InvoiceResponse fromEntity(Invoice entity) {
        if (entity == null) {
            return null;
        }
        return new InvoiceResponse(
                entity.getInvoiceId(),
                entity.getFolio() != null ? entity.getFolio().getFolioId() : null,
                entity.getInvoiceNo(),
                entity.getBuyerName(),
                entity.getBuyerTaxCode(),
                entity.getBuyerAddress(),
                entity.getSubtotal(),
                entity.getVatRate(),
                entity.getVatAmount(),
                entity.getTotalAmount(),
                entity.getIssuedAt(),
                entity.getIssuedBy()
        );
    }
}
