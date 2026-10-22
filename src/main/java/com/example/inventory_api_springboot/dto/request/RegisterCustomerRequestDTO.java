package com.example.inventory_api_springboot.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequestDTO (
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password cannot be empty")
    @Size(message = "Password should be at least 8 characters long", min = 8)
    String password,

    @NotBlank(message = "First name cannot be empty")
    @JsonProperty("first_name")
    String firstName,

    @JsonProperty("last_name")
    String lastName,

    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(
        regexp = "^08[1-9][0-9]{7,11}$",
        message = "Invalid phone number format, please follow indonesian phone number format"
    )
    @JsonProperty("phone_number")
    String phoneNumber,

    @NotBlank(message = "Address cannot be empty")
    String address
){}
