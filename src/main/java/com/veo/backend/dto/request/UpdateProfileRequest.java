package com.veo.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @NotBlank(message = "Họ tên không được để trống.")
    @Pattern(
            regexp = "^[\\p{L}\\s]+$",
            message = "Họ tên không được chứa số hoặc ký tự đặc biệt."
    )
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống.")
    @Pattern(
            regexp = "^\\d{10}$",
            message = "Số điện thoại phải gồm đúng 10 chữ số."
    )
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống.")
    @Pattern(
            regexp = "^[\\p{L}\\d\\s]+$",
            message = "Địa chỉ không được chứa ký tự đặc biệt."
    )
    private String address;
}
