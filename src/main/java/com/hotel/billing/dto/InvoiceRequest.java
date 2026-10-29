package com.hotel.billing.dto;

import jakarta.validation.constraints.Size;

public record InvoiceRequest(
        @Size(max = 200, message = "Tên người mua / đơn vị không được vượt quá 200 ký tự")
        String buyerName,

        @Size(max = 20, message = "Mã số thuế không được vượt quá 20 ký tự")
        String buyerTaxCode,

        @Size(max = 500, message = "Địa chỉ người mua không được vượt quá 500 ký tự")
        String buyerAddress
) {
}
