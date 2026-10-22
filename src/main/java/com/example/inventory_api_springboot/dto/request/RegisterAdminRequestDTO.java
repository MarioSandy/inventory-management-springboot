package com.example.inventory_api_springboot.dto.request;

import com.example.inventory_api_springboot.enums.UserRoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterAdminRequestDTO(
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password cannot be empty")
    @Size(message = "Password should be at least 8 characters long", min = 8)
    String password,

    @NotNull(message = "Admin role cannot be empty")
    UserRoleEnum role
) {
}
