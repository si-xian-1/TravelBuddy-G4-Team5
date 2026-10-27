// Track whether a login request is already in progress to prevent duplicate clicks
let isSubmitting = false;

// Handle login form submission
document.getElementById("loginForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    if (isSubmitting) {
        return;
    }

    // Clear previous error alert and field errors
    clearAlert();
    clearError("username");
    clearError("password");

    // Retrieve input values
    const usernameInput = document.getElementById("username");
    const passwordInput = document.getElementById("password");
    const username = usernameInput ? usernameInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value : "";

    // Validate login forms
    if (!validateLoginForm(username, password)) {
        return;
    }

    // Prepare input values to pass to backend
    const travelerLoginDetails = { username, password };

    const loginBtn = document.getElementById("loginBtn");
    const originalBtnHtml = loginBtn ? loginBtn.innerHTML : "Login";

    // Disable button and show spinner to provide immediate feedback and prevent duplicate submissions
    isSubmitting = true;
    if (loginBtn) {
        loginBtn.disabled = true;
        loginBtn.innerHTML = '<span class="spinner-border spinner-border-sm mr-2" role="status" aria-hidden="true"></span>Logging in...';
    }

    try {
        // Call POST method to authenticate this user
        const response = await fetch('/api/travelers/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(travelerLoginDetails)
        });

        if (response.ok) {
            // Update button to indicate redirection while dashboard is loading
            if (loginBtn) {
                loginBtn.innerHTML = '<span class="spinner-border spinner-border-sm mr-2" role="status" aria-hidden="true"></span>Redirecting...';
            }
            // Redirect to dashboard page
            window.location.replace('/');
            return;
        } else {
            // Re-enable button on error
            isSubmitting = false;
            if (loginBtn) {
                loginBtn.disabled = false;
                loginBtn.innerHTML = originalBtnHtml;
            }

            // When user inputs has some errors, display error message from backend
            const errData = await response.json().catch(() => ({}));

            // Handle invalid Credentials
            if (response.status === 401 || response.status === 403 || (response.status === 400 && errData.message && !errData.errors)) {
                showGeneralAlert(errData.message || "Invalid username or password.");
            }
            // Handle form field validation errors (Field Validation Errors from RestExceptionHandler)
            else if (response.status === 400 && errData && typeof errData === 'object') {
                let hasFieldErrors = false;
                for (const [field, message] of Object.entries(errData)) {
                    if (document.getElementById(field)) {
                        showError(field, message);
                        hasFieldErrors = true;
                    }
                }
                if (!hasFieldErrors) {
                    showGeneralAlert("Invalid username or password.");
                }
            }
            // Handle other errors
            else {
                // Log error debugging purposes
                console.error("Server error details:", errData);
                showGeneralAlert(errData.message || "An unexpected error occurred. Please try again later.");
            }
        }
    } catch (err) {
        console.error("Unable to complete login:", err);
        isSubmitting = false;
        if (loginBtn) {
            loginBtn.disabled = false;
            loginBtn.innerHTML = originalBtnHtml;
        }
        showGeneralAlert("Unable to connect to the server. Please check your connection and try again.");
    }
});

// Show the general error alert
function showGeneralAlert(message) {
    const alertBox = document.getElementById('loginErrAlert');
    const alertMsg = document.getElementById('loginErrAlertMsg');
    if (alertBox && alertMsg) {
        alertMsg.textContent = message;
        alertBox.classList.remove('d-none');
    }
}

// Clear the general error alert
function clearAlert() {
    const alertBox = document.getElementById('loginErrAlert');
    const alertMsg = document.getElementById('loginErrAlertMsg');
    if (alertBox) {
        alertBox.classList.add('d-none');
    }
    if (alertMsg) {
        alertMsg.textContent = '';
    }
}

// Show the error message
function showError(fieldId, message) {
    const inputElement = document.getElementById(fieldId);
    if (inputElement) {
        inputElement.classList.add('is-invalid');
    }

    const feedbackElement = document.getElementById(fieldId + 'ErrMsg');
    if (feedbackElement) {
        feedbackElement.textContent = message;
    }
}

// Clear error message
function clearError(fieldId) {
    const inputElement = document.getElementById(fieldId);
    if (inputElement) {
        inputElement.classList.remove('is-invalid');
    }
    const feedbackElement = document.getElementById(fieldId + 'ErrMsg');
    if (feedbackElement) {
        feedbackElement.textContent = '';
    }
}

// Validate username
function validateUsername(username) {
    if (username.length === 0) {
        showError("username", "Username is required");
        return false;
    }

    clearError("username");
    return true;
}

// Validate Password
function validatePassword(password) {
    if (password.length === 0) {
        showError("password", "Password is required");
        return false;
    }

    clearError("password");
    return true;
}

// Validate Login Form
function validateLoginForm(username, password) {
    let isValidUsername = validateUsername(username);
    let isValidPassword = validatePassword(password);
    return isValidUsername && isValidPassword;
}

// Clear error message and alert when user types in inputs
['username', 'password'].forEach(id => {
    const input = document.getElementById(id);
    if (input) {
        input.addEventListener('input', function () {
            clearError(id);
            clearAlert();
        });
    }
});

// Allow alert to re-appear again if user continues to submit invalid crediential details
document.getElementById('loginErrAlertClose')?.addEventListener('click', function () {
    clearAlert();
});