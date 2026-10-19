/**
 * SHARED UTILITY FUNCTIONS
 * These functions are used across Flight, Hotel, and Attraction management files.
 */

// Toggles the Bootstrap accordion sections (Show/Hide)
function setItinerarySectionExpanded(sectionId, expanded) {
    $(`#${sectionId}`).collapse(expanded ? "show" : "hide");
}

// Formats date time string to redable format (e.g., "20 Oct 2026, 14:30")
function formatDateTime(value) {
    if (!value) {
        return "Not provided";
    }

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return date.toLocaleString('en-GB', {
        day: 'numeric', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: true
    });
}

// Converts date string into the local timezone format
function toDateTimeInputValue(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value.slice(0, 16);

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}T${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

// Standardizes error messages returned by the Spring Boot backend
function getApiErrorMessage(responseBody, status) {
    if (!responseBody) return `Request failed (${status}).`;
    try {
        const errorData = JSON.parse(responseBody);
        const fieldErrors = Object.entries(errorData)
            .filter(([field]) => field !== "message")
            .map(([field, message]) => `${field}: ${message}`);
        return errorData.message || errorData.detail || fieldErrors.join(" ") || responseBody;
    } catch {
        return responseBody;
    }
}

// Removes red warning borders and clears error messages on a given form
function clearFormValidation(form) {
    form.querySelectorAll(".form-control").forEach(input => input.classList.remove("is-invalid"));
    form.querySelectorAll(".invalid-feedback").forEach(message => message.textContent = "");
}

// Adds a red warning border to a specific input and displays the custom error message
function showFormFieldError(form, selector, message) {
    form.querySelector(`.${selector}`).classList.add("is-invalid");
    form.querySelector(`.${selector}-error`).textContent = message;
}

/**
 * Page initialization
 * Fetches and displays the top-level itinerary summary (Name and Overall Dates).
 */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    // If user not on itinerary details page
    if (!page) {
        return;
    }

    const itineraryId = page.dataset.itineraryId;
    const loadingMessage = document.getElementById("itineraryLoading");
    const errorMessage = document.getElementById("itineraryError");
    const summary = document.getElementById("itinerarySummary");

    try {
        // Fetch core itinerary details
        const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}`,
                                { credentials: "same-origin" });

        if (!response.ok) {
            throw new Error(`Failed to load travel details (${response.status}).`);
        }

        const itinerary = await response.json();
        document.getElementById("itineraryName").textContent = itinerary.itineraryName || "Unnamed itinerary";

        // Format overall trip dates cleanly (excluding time)
        const startDate = itinerary.startDate ? new Date(itinerary.startDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : "Not provided";
        const endDate = itinerary.endDate ? new Date(itinerary.endDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : "Not provided";

        document.getElementById("itineraryDates").textContent = `${startDate} - ${endDate}`;

        // Hide loading state and display summary
        loadingMessage.classList.add("d-none");
        summary.classList.remove("d-none");
    } catch (error) {
        console.error("Unable to load itinerary details:", error);
        loadingMessage.classList.add("d-none");
        errorMessage.textContent = error.message || "Unable to load travel details.";
        errorMessage.classList.remove("d-none");
    }
});