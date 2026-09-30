package com.example.KendyDigital.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @Email @NotBlank @Size(max = 100) String email,
        @Pattern(regexp = "^$|^0[0-9]{9,10}$", message = "Số điện thoại phải từ 10 đến 11 chữ số")
        @Size(max = 11, message = "Số điện thoại tối đa 11 ký tự")
        String phone,
        @NotBlank @Size(min = 8, max = 64) String password) {
}
