package com.example.KendyDigital.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @Pattern(regexp = "^$|^0[0-9]{9,10}$", message = "Số điện thoại phải từ 10 đến 11 chữ số")
        @Size(max = 11, message = "Số điện thoại tối đa 11 ký tự")
        String phone) {
}
