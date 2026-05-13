package com.ramdev.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/** Login request — mobile + password */
@Data
public class LoginRequest {
    @NotBlank @Pattern(regexp = "\\d{10}", message = "Enter a valid 10-digit mobile number")
    private String mobile;
    @NotBlank
    private String password;
}
