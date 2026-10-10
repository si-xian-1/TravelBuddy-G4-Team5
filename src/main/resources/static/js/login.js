// Handle login form submission
document.getElementById("loginForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    // Retrieve input values
    const username = document.getElementById("username").value.trim();
    const password = document.getElementById("password").value;

    // Validate login forms
    if (!validateLoginForm(username, password)) {
        return;
    }

    // Prepare input values to pass to backend
    const travelerLoginDetails = { username, password };

    try {
        // Call POST method to authenticate this user
        const response = await fetch('/api/travelers/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
            body: JSON.stringify(travelerLoginDetails)
        });

        if (response.ok) {
            // When login successful, redirect to dashboard page
            window.location.href = '/';
            return;
        } else {
            // When user inputs has some errors, display error message from backend
            const errData = await response.json().catch(() => ({}));

            // Handle invalid Credentials
            if (response.status === 401 || response.status === 403 || (response.status === 400 && errData.message && !errData.errors)) {
                const alertBox = document.getElementById('loginErrAlert');
                const alertMsg = document.getElementById('loginErrAlertMsg');
                // Show alert invalid login credentials
                if (alertBox && alertMsg) {
                    alertMsg.textContent = "Invalid username or password.";
                    alertBox.classList.remove('d-none');
                }
            }
            // Handle form field validation errors (Field Validation Errors from RestExceptionHandler)
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
        console.error("Unable to complete login:", err);
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

// Allow alert to re-appear again if user continues to submit invalid crediential details
document.getElementById('loginErrAlertClose')?.addEventListener('click', function () {
    document.getElementById('loginErrAlert')?.classList.add('d-none');
});