// Track whether a sign up request is already in progress to prevent duplicate clicks
let isSubmitting = false;

// Handle sign up form submission
document.getElementById("signupForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    if (isSubmitting) {
        return;
    }

    // Clear previous error alert and field errors
    clearAlert();
    clearError("username");
    clearError("mobileNumber");
    clearError("password");
    clearError("hasConsentLocation");

    // Retrieve input values
    const usernameInput = document.getElementById("username");
    const mobileNumberInput = document.getElementById("mobileNumber");
    const passwordInput = document.getElementById("password");
    const hasConsentLocation = document.getElementById("hasConsentLocation");

    const username = usernameInput ? usernameInput.value.trim() : "";
    const mobileNumber = mobileNumberInput ? mobileNumberInput.value.trim() : "";
    const password = passwordInput ? passwordInput.value : "";

    // Validate sign up forms
    if (!validateSignUpForm(username, mobileNumber, hasConsentLocation, password)) {
        return;
    }

    // Prepare input values to pass to backend
    const travelerSignUpDetails = {
        username,
        mobileNumber,
        password,
        hasConsentLocation: hasConsentLocation ? hasConsentLocation.checked : false
    };

    const signupBtn = document.getElementById("signupBtn");
    const originalBtnHtml = signupBtn ? signupBtn.innerHTML : "Sign up";

    // Disable button and show spinner to provide immediate feedback and prevent duplicate submissions
    isSubmitting = true;
    if (signupBtn) {
        signupBtn.disabled = true;
        signupBtn.innerHTML = '<span class="spinner-border spinner-border-sm mr-2" role="status" aria-hidden="true"></span>Creating account...';
    }

    try {
        // Call POST method to register this user
        const response = await fetch('/api/travelers/signup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(travelerSignUpDetails)
        });

        if (response.ok) {
            // Update button text while redirecting to login page
            if (signupBtn) {
                signupBtn.innerHTML = '<span class="spinner-border spinner-border-sm mr-2" role="status" aria-hidden="true"></span>Redirecting to login...';
            }
            // When sign up successful, redirect to login page
            window.location.replace('/login');
            return;
        } else {
            // Re-enable button on error
            isSubmitting = false;
            if (signupBtn) {
                signupBtn.disabled = false;
                signupBtn.innerHTML = originalBtnHtml;
            }

            // When user inputs has some errors, display error message from backend
            const errData = await response.json().catch(() => ({}));

            // Handle duplicate username issue
            if (response.status === 409) {
                showError("username", errData.message || "Username is already in use");
            } 
            // Handle 400 Bad Request (Field Validation Errors from RestExceptionHandler)
            else if (response.status === 400 && errData && typeof errData === 'object') {
                let hasFieldErrors = false;
                for (const [field, message] of Object.entries(errData)) {
                    if (document.getElementById(field)) {
                        showError(field, message);
                        hasFieldErrors = true;
                    }
                }
                if (!hasFieldErrors) {
                    showGeneralAlert(errData.message || "Please check the form for invalid inputs.");
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
        console.error("Unable to complete sign up:", err);
        isSubmitting = false;
        if (signupBtn) {
            signupBtn.disabled = false;
            signupBtn.innerHTML = originalBtnHtml;
        }
        showGeneralAlert("Unable to connect to the server. Please check your connection and try again.");
    }
});

// Show the general error alert
function showGeneralAlert(message) {
    const alertBox = document.getElementById('signupErrAlert');
    const alertMsg = document.getElementById('signupErrAlertMsg');
    if (alertBox && alertMsg) {
        alertMsg.textContent = message;
        alertBox.classList.remove('d-none');
    }
}

// Clear the general error alert
function clearAlert() {
    const alertBox = document.getElementById('signupErrAlert');
    const alertMsg = document.getElementById('signupErrAlertMsg');
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
    else if (username.length > 50) {
        showError("username", "Username can be at most 50 characters");
        return false;
    }

    clearError("username");
    return true;
}

// Validate mobile number
function validateMobileNumber(mobileNumber) {
    if (mobileNumber.length === 0) {
        showError("mobileNumber", "Mobile number is required");
        return false;
    }
    else if (mobileNumber.length > 8) {
        showError("mobileNumber", "Mobile number can be at most 8 characters");
        return false;
    }
    else if (!(/^\d+$/.test(mobileNumber))) {
        showError("mobileNumber", "Mobile number should only contain numbers");
        return false;
    }

    clearError("mobileNumber");
    return true;
}

// Validate location consent
function validateLocationConsent(hasConsentLocation) {
    if (!hasConsentLocation || !hasConsentLocation.checked) {
        showError("hasConsentLocation", "Invalid consent location");
        return false;
    }

    clearError("hasConsentLocation");
    return true;
}

// Validate Password
function validatePassword(password) {
    if (password.length === 0) {
        showError("password", "Password is required");
        return false;
    }
    if (password.length > 255) {
        showError("password", "Password can be at most 255 characters");
        return false;
    }

    clearError("password");
    return true;
}

// Validate Sign Up Form
function validateSignUpForm(username, mobileNumber, hasConsentLocation, password) {
    let isValidUsername = validateUsername(username);
    let isValidMobileNumber = validateMobileNumber(mobileNumber);
    let isValidLocationConsent = validateLocationConsent(hasConsentLocation);
    let isValidPassword = validatePassword(password);
    return isValidUsername && isValidMobileNumber && isValidLocationConsent && isValidPassword;
}

// Clear error message and alert when user interacts with inputs
['username', 'mobileNumber', 'password'].forEach(id => {
    const input = document.getElementById(id);
    if (input) {
        input.addEventListener('input', function () {
            clearError(id);
            clearAlert();
        });
    }
});

const consentCheck = document.getElementById('hasConsentLocation');
if (consentCheck) {
    consentCheck.addEventListener('change', function () {
        clearError('hasConsentLocation');
        clearAlert();
    });
}

// Allow alert to re-appear again if user continues to submit invalid credential details
document.getElementById('signupErrAlertClose')?.addEventListener('click', function () {
    clearAlert();
});