package com.TravelBuddy.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank(message = "New password is required")
        @Size(max = 255, message = "Password can be at most 255 characters")
        String newPassword,
        @NotBlank(message = "Confirm password is required")
        @Size(max = 255, message = "Password can be at most 255 characters")
        String confirmPassword) {
}
