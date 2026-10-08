// Handle sign up form submission
document.getElementById("signupForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    // Retrieve input values
    const username = document.getElementById("username").value.trim();
    const mobileNumber = document.getElementById("mobileNumber").value.trim();
    const password = document.getElementById("password").value;
    const hasConsentLocation = document.getElementById("hasConsentLocation");

    // Validate sign up forms
    if (!validateSignUpForm(username, mobileNumber, hasConsentLocation, password)) {
        return;
    }

    // Prepare input values to pass to backend
    const travelerSignUpDetails = {username, mobileNumber, password, hasConsentLocation: hasConsentLocation.checked};

    try {
        const response = await fetch('/api/travelers/signup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(travelerSignUpDetails)
        });

        if (response.ok) {
            // When sign up successful, redirect to login page
            window.location.href = '/';
        } else {
            // When user inputs has some errors, display error message from backend
            const errData = await response.json();

            // Handle duplicate username issue
            if (response.status === 409) {
                showError("username", errData.message || "Username is already in use");
            } 
            // Handle 400 Bad Request (Field Validation Errors from RestExceptionHandler)
            else if (response.status === 400 && errData && typeof errData === 'object') {
                for (const [field, message] of Object.entries(errData)) {
                    showError(field, message);
                }
            } 
            // Handle other errors
            else {
                // Log error debugging purposes
                console.error("Server error details:", errData);
                
                // Notify user of unknown error occured
                alert("An unexpected error occurred. Please try again later.");
            }
        }
    } catch (err) {
        console.error("Unable to complete sign up:", err);
    }
});

// Show the error message
function showError(fieldId, message) {
    const inputElement = document.getElementById(fieldId);
    inputElement.classList.add('is-invalid');

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
    if (!hasConsentLocation.checked) {
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