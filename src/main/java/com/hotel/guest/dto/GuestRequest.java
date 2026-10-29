package com.hotel.guest.dto;

import com.hotel.common.enums.GuestGender;
import com.hotel.common.enums.GuestIdType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record GuestRequest(
        @NotBlank(message = "Họ và tên khách hàng không được để trống")
        @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
        String fullName,

        @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
        String phone,

        @Email(message = "Địa chỉ email không đúng định dạng")
        @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
        String email,

        GuestGender gender,

        LocalDate dateOfBirth,

        @Size(max = 50, message = "Quốc tịch không được vượt quá 50 ký tự")
        String nationality,

        GuestIdType idType,

        @Size(max = 30, message = "Số định danh không được vượt quá 30 ký tự")
        String idNumber,

        @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
        String address
) {
}
