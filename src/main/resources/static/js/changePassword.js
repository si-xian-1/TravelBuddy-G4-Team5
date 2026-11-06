/**
 * Manage edit user profile and change password
 */

document.addEventListener("DOMContentLoaded", () => {
    const passwordForm = document.getElementById("passwordForm");
    const openPasswordBtn = document.getElementById("openChangePassword");

    // Exit early if the password change form or trigger button does not exist on this page.
    if (!passwordForm || !openPasswordBtn) return;

    // Buttons
    const submitBtn = document.getElementById("changePasswordButton");
    const cancelBtn = document.getElementById("cancelPasswordChange");

    // Alert feedback
    const passwordError = document.getElementById("passwordError");
    const passwordSuccess = document.getElementById("passwordSuccess");

    // Form inputs and visibility toggle buttons
    const passwordFields = ["newPassword", "confirmPassword"];
    const passwordToggles = passwordForm.querySelectorAll("[data-password-toggle]");

    /**
     * Toggle password visibility
     */
    function setPasswordVisibility(button, visible) {
        const input = document.getElementById(button.dataset.passwordToggle);
        if (!input) return;

        // Switch input type between plain text and masked password
        input.type = visible ? "text" : "password";

        button.setAttribute("aria-pressed", String(visible));
        const fieldDescription = input.id === "newPassword" ? "new password" : "confirm new password";
        button.setAttribute("aria-label", `${visible ? "Hide" : "Show"} ${fieldDescription}`);

        const icon = button.querySelector("i, svg");
        if (icon) {
            icon.classList.toggle("fa-eye", !visible);
            icon.classList.toggle("fa-eye-slash", visible);
        }
    }

    /**
     * Resets all password fields back to masked "password" mode.
     */
    function hidePasswords() {
        passwordToggles.forEach(button => setPasswordVisibility(button, false));
    }

    // Attach click listeners to all password reveal eye buttons
    passwordToggles.forEach(button => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            if (input) {
                // If currently masked ("password"), toggle to revealed; otherwise hide
                setPasswordVisibility(button, input.type === "password");
            }
        });
    });

    /**
     * Marks a specific input field as invalid and displays an inline error message.
     */
    function showFieldError(field, message) {
        const input = document.getElementById(field);
        if (!input) return;

        input.classList.add("is-invalid");
        input.setAttribute("aria-invalid", "true");

        const errorFeedback = document.getElementById(field + "ErrMsg");
        if (errorFeedback) {
            errorFeedback.textContent = message;
        }
    }

    /**
     * Clears the validation error state from a specific input field.
     */
    function clearFieldError(field) {
        const input = document.getElementById(field);
        if (!input) return;

        input.classList.remove("is-invalid");
        input.removeAttribute("aria-invalid");

        const errorFeedback = document.getElementById(field + "ErrMsg");
        if (errorFeedback) {
            errorFeedback.textContent = "";
        }
    }

    /**
     * Clears all field validation errors and hides the top-level form error alert.
     */
    function clearAllPasswordErrors() {
        if (passwordError) passwordError.classList.add("d-none");
        passwordFields.forEach(clearFieldError);
    }

    // Clear individual field errors dynamically when the user starts typing
    passwordFields.forEach(field => {
        const input = document.getElementById(field);
        if (input) {
            input.addEventListener("input", () => {
                clearFieldError(field); // Clears error for this specific field
                if (passwordError) passwordError.classList.add("d-none");
                if (passwordSuccess) passwordSuccess.classList.add("d-none");
            });
        }
    });

    /**
     * Shows or hides the password change form and manages focus accessibility.
     */
    function togglePasswordEditing(editing) {
        // Reset inputs, visibility state, and validation errors
        passwordForm.reset();
        hidePasswords();
        clearAllPasswordErrors();

        // Toggle form and surrounding profile view panels
        passwordForm.classList.toggle("d-none", !editing);
        document.getElementById("profileDetails")?.classList.toggle("d-none", editing);
        document.getElementById("editProfile")?.classList.toggle("d-none", editing);
        openPasswordBtn.classList.toggle("d-none", editing);

        // Update card header title
        const cardTitle = document.getElementById("travelerDetailsTitle");
        if (cardTitle) {
            cardTitle.textContent = editing ? "Change Password" : "Traveler Details";
        }

        if (editing) {
            // Hide previous success messages and auto-focus the first input
            if (passwordSuccess) passwordSuccess.classList.add("d-none");
            document.getElementById("profileSuccess")?.classList.add("d-none");
            document.getElementById("newPassword")?.focus();
        } else {
            // Restore keyboard focus back to the "Change Password" button for accessibility
            openPasswordBtn.focus();
        }
    }

    // Open change password view
    openPasswordBtn.addEventListener("click", () => togglePasswordEditing(true));

    // Cancel password change (only allowed if submission is not actively pending)
    if (cancelBtn) {
        cancelBtn.addEventListener("click", () => {
            if (!submitBtn.disabled) {
                togglePasswordEditing(false);
            }
        });
    }

    passwordForm.addEventListener("submit", async event => {
        event.preventDefault();

        // Prevent duplicate submissions if already in progress
        if (submitBtn.disabled) return;

        clearAllPasswordErrors();
        if (passwordSuccess) passwordSuccess.classList.add("d-none");

        // Preserve intentional whitespaces in passwords; do not trim raw values
        const newPassword = document.getElementById("newPassword")?.value || "";
        const confirmPassword = document.getElementById("confirmPassword")?.value || "";

        // Client-side Validation Checks
        if (!newPassword.trim()) {
            showFieldError("newPassword", "New password is required");
        } else if (newPassword.length > 255) {
            showFieldError("newPassword", "Password can be at most 255 characters");
        }

        if (!confirmPassword.trim()) {
            showFieldError("confirmPassword", "Confirm password is required");
        } else if (confirmPassword.length > 255) {
            showFieldError("confirmPassword", "Password can be at most 255 characters");
        } else if (newPassword.trim() && newPassword !== confirmPassword) {
            // Only validate matching if the user has provided a new password
            showFieldError("confirmPassword", "Passwords must match");
        }

        // If any field failed validation, shift focus to the first invalid input and stop
        const firstInvalidInput = passwordForm.querySelector(".is-invalid");
        if (firstInvalidInput) {
            firstInvalidInput.focus();
            return;
        }

        // Disable action buttons and indicate progress
        submitBtn.disabled = true;
        if (cancelBtn) cancelBtn.disabled = true;
        submitBtn.textContent = "Saving...";

        try {
            // Send update request with new credentials
            const response = await fetch("/api/travelers/changePassword", {
                method: "PUT",
                credentials: "same-origin",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ newPassword, confirmPassword })
            });

            // Detect expired sessions or redirected login pages
            if (response.redirected || response.status === 401 || response.status === 403) {
                throw new Error("Your session has expired. Please sign in again.");
            }

            // Handle API error responses
            if (!response.ok) {
                const error = await response.json().catch(() => ({}));

                // 400 Bad Request: validation errors or constraint failures
                if (response.status === 400) {
                    let hasFieldError = false;

                    // Map server-side validation error map to corresponding form fields
                    passwordFields.forEach(field => {
                        if (typeof error[field] === "string") {
                            showFieldError(field, error[field]);
                            hasFieldError = true;
                        }
                    });

                    // Handle generic or business validation messages (e.g., password mismatch or reuse)
                    if (!hasFieldError) {
                        const message = error.message || error.detail || "Please check the form for invalid inputs.";
                        const targetField = message === "Passwords must match" ? "confirmPassword" : "newPassword";
                        showFieldError(targetField, message);
                    }

                    passwordForm.querySelector(".is-invalid")?.focus();
                    return;
                }

                // Generic error fallback for other status codes (e.g. 409 Conflict, 500 Server Error)
                throw new Error(error.message || error.detail || "An unexpected error occurred. Please try again later.");
            }

            // Password changed successfully: close form and display success alert
            togglePasswordEditing(false);
            if (passwordSuccess) passwordSuccess.classList.remove("d-none");

        } catch (error) {
            // Display friendly network or general error message in the alert box
            if (passwordError) {
                passwordError.textContent = error instanceof TypeError
                    ? "Unable to connect to the server. Please check your connection and try again."
                    : error.message;
                passwordError.classList.remove("d-none");
            }
        } finally {
            // Restore button states and labels
            submitBtn.disabled = false;
            if (cancelBtn) cancelBtn.disabled = false;
            submitBtn.textContent = "Save Changes";
        }
    });
});
