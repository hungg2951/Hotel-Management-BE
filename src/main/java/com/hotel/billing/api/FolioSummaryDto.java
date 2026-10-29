package com.hotel.billing.api;

import com.hotel.common.enums.FolioStatus;

/**
 * DTO tóm tắt trạng thái và công nợ của Folio phục vụ giao tiếp liên module.
 */
public record FolioSummaryDto(
        Long folioId,
        Long stayId,
        FolioStatus status,
        Long totalAmount,
        Long paidAmount,
        Long balance
) {
    public boolean isSettled() {
        return balance != null && balance == 0L;
    }
}
