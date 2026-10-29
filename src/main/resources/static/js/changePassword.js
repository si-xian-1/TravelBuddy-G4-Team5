const passwordToggles = document.querySelectorAll("[data-password-toggle]");
function setPasswordVisibility(button, visible) {
    const input = document.getElementById(button.dataset.passwordToggle);
    input.type = visible ? "text" : "password";
    button.setAttribute("aria-pressed", String(visible));
    const label = input.id === "newPassword" ? "new password" : "confirm new password";
    button.setAttribute("aria-label", `${visible ? "Hide" : "Show"} ${label}`);
    const icon = button.querySelector("i");
    icon.classList.toggle("fa-eye", !visible);
    icon.classList.toggle("fa-eye-slash", visible);
}
passwordToggles.forEach(button => button.addEventListener("click", () => {
    const input = document.getElementById(button.dataset.passwordToggle);
    setPasswordVisibility(button, input.type === "password");
}));
function hidePasswords() {
    passwordToggles.forEach(button => setPasswordVisibility(button, false));
}

const passwordForm = document.getElementById("passwordForm");
const passwordFields = ["newPassword", "confirmPassword"];
const passwordError = document.getElementById("passwordError");
const passwordSuccess = document.getElementById("passwordSuccess");
function showPasswordFieldError(field, message) {
    const input = document.getElementById(field);
    input.classList.add("is-invalid");
    input.setAttribute("aria-invalid", "true");
    document.getElementById(field + "ErrMsg").textContent = message;
}
function clearPasswordErrors() {
    passwordError.classList.add("d-none");
    passwordFields.forEach(field => {
        const input = document.getElementById(field);
        input.classList.remove("is-invalid");
        input.removeAttribute("aria-invalid");
        document.getElementById(field + "ErrMsg").textContent = "";
    });
}
passwordFields.forEach(field => document.getElementById(field).addEventListener("input", () => {
    clearPasswordErrors();
    passwordSuccess.classList.add("d-none");
}));
passwordForm.addEventListener("submit", async event => {
    event.preventDefault();
    const button = document.getElementById("changePasswordButton");
    if (button.disabled) return;
    clearPasswordErrors();
    passwordSuccess.classList.add("d-none");
    // Preserve whitespace in passwords; do not trim the submitted values.
    const newPassword = document.getElementById("newPassword").value;
    const confirmPassword = document.getElementById("confirmPassword").value;
    if (!newPassword.trim()) showPasswordFieldError("newPassword", "New password is required");
    else if (newPassword.length > 255) showPasswordFieldError("newPassword", "Password can be at most 255 characters");
    if (!confirmPassword.trim()) showPasswordFieldError("confirmPassword", "Confirm password is required");
    else if (confirmPassword.length > 255) showPasswordFieldError("confirmPassword", "Password can be at most 255 characters");
    else if (newPassword !== confirmPassword) showPasswordFieldError("confirmPassword", "Passwords must match");
    if (passwordForm.querySelector(".is-invalid")) {
        passwordForm.querySelector(".is-invalid").focus();
        return;
    }
    button.disabled = true;
    button.textContent = "Saving...";
    try {
        const response = await fetch("/api/travelers/changePassword", {
            method: "PUT",
            credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ newPassword, confirmPassword })
        });
        if (response.redirected || response.status === 401 || response.status === 403) {
            throw new Error("Your session has expired. Please sign in again.");
        }
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            if (response.status === 400) {
                let fieldError = false;
                passwordFields.forEach(field => {
                    if (typeof error[field] === "string") {
                        showPasswordFieldError(field, error[field]);
                        fieldError = true;
                    }
                });
                if (!fieldError) {
                    const message = error.message || error.detail || "Please check the form for invalid inputs.";
                    showPasswordFieldError(message === "Passwords must match" ? "confirmPassword" : "newPassword", message);
                }
                passwordForm.querySelector(".is-invalid").focus();
                return;
            }
            throw new Error(error.message || error.detail || "An unexpected error occurred. Please try again later.");
        }
        passwordForm.reset();
    hidePasswords();
        button.disabled = false;
        togglePasswordEditing(false);
        passwordSuccess.classList.remove("d-none");
    } catch (error) {
        passwordError.textContent = error instanceof TypeError
            ? "Unable to connect to the server. Please check your connection and try again."
            : error.message;
        passwordError.classList.remove("d-none");
    } finally {
        button.disabled = false;
        button.textContent = "Save Changes";
    }
});

function togglePasswordEditing(editing) {
    passwordForm.reset();
    hidePasswords();
    clearPasswordErrors();
    passwordForm.classList.toggle("d-none", !editing);
    document.getElementById("profileDetails").classList.toggle("d-none", editing);
    document.getElementById("editProfile").classList.toggle("d-none", editing);
    document.getElementById("openChangePassword").classList.toggle("d-none", editing);
    document.getElementById("travelerDetailsTitle").textContent = editing ? "Change Password" : "Traveler Details";
    if (editing) {
        passwordSuccess.classList.add("d-none");
        document.getElementById("profileSuccess").classList.add("d-none");
        document.getElementById("newPassword").focus();
    }
}
document.getElementById("openChangePassword").addEventListener("click", () => togglePasswordEditing(true));
document.getElementById("cancelPasswordChange").addEventListener("click", () => {
    if (!document.getElementById("changePasswordButton").disabled) togglePasswordEditing(false);
});
