package com.hotel.billing.dto;

import com.hotel.billing.entity.Folio;
import com.hotel.common.enums.FolioStatus;

import java.time.LocalDateTime;
import java.util.List;

public record FolioDetailResponse(
        Long folioId,
        Long stayId,
        FolioStatus status,
        Long totalAmount,
        Long paidAmount,
        Long balance,
        LocalDateTime createdAt,
        LocalDateTime closedAt,
        List<FolioItemResponse> items,
        List<PaymentResponse> payments,
        InvoiceResponse invoice
) {
    public static FolioDetailResponse of(
            Folio folio,
            Long paidAmount,
            Long balance,
            List<FolioItemResponse> items,
            List<PaymentResponse> payments,
            InvoiceResponse invoice
    ) {
        return new FolioDetailResponse(
                folio.getFolioId(),
                folio.getStayId(),
                folio.getStatus(),
                folio.getTotalAmount(),
                paidAmount,
                balance,
                folio.getCreatedAt(),
                folio.getClosedAt(),
                items != null ? items : List.of(),
                payments != null ? payments : List.of(),
                invoice
        );
    }
}
