package com.example.KendyDigital.dto.email.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicConsultRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
        String name,

        @NotBlank(message = "Số điện thoại/Zalo không được để trống")
        @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
        String phone,

        @Size(max = 150, message = "Ngành hàng tối đa 150 ký tự")
        String industry,

        @Size(max = 100, message = "Ngân sách tối đa 100 ký tự")
        String budget,

        @NotBlank(message = "Mục tiêu tư vấn không được để trống")
        @Size(max = 2000, message = "Mục tiêu tư vấn tối đa 2000 ký tự")
        String goal
) {}
