package com.ramdev.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    private String name;

    @NotBlank @Pattern(regexp = "\\d{10}", message = "Enter a valid 10-digit mobile number")
    private String mobile;

    /** Role to assign: ADMIN or USER (SUPER_ADMIN can only be set by DB seed) */
    @NotBlank
    private String role;  // "ADMIN" | "USER"
}
