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
    if (!value) {
        return "";
    }
    
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) { 
        return value.slice(0, 16);
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");

    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// Standardizes error messages returned by the Spring Boot backend
function getApiErrorMessage(responseBody, status) {
    if (!responseBody) {
        return `Request failed (${status}).`;
    }

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

// Handle search bar logic that calls the location api to return search result
// Limits how often the location API is called
function setCallFrequency(func, delay) {
    let timeout = null;
    return function (event) {
        clearTimeout(timeout);
        timeout = setTimeout(() => { func(event); }, delay);
    };
}

// Handle typing in any autocomplete input to search for locations
document.addEventListener("input", setCallFrequency(async function (event) {
    // Checks that input requires the use of search location function
    if (!event.target.matches(".location-autocomplete-input")) {
        return;
    }

    // Retreive the search fields and dropdown for use of displaying result
    const input = event.target;
    const wrapper = input.closest(".location-autocomplete-wrapper");
    const dropdown = wrapper.querySelector(".location-autocomplete-dropdown");
    const latInput = wrapper.querySelector(".location-autocomplete-lat");
    const lonInput = wrapper.querySelector(".location-autocomplete-lon");
    // Used for search type checking (is searchig for hotel or not)
    const searchType = input.dataset.searchType;
    const query = input.value.trim();

    // Clear latitude and longitude input
    if (latInput && lonInput) {
        latInput.value = "";
        lonInput.value = "";
    }

    // Show search result when at least 3 characters is typed
    if (query.length < 3) {
        dropdown.classList.add("d-none");
        return;
    }

    try {
        // Call location api
        let apiUrl = `/api/location/search?query=${encodeURIComponent(query)}`;
        // Check whether search is for hotel
        if (searchType === "hotel") {
            apiUrl += "&isHotel=true";
        }

        const response = await fetch(apiUrl);
        if (!response.ok) {
            throw new Error("Location search failed");
        }

        // Display results returned from location api
        const locations = await response.json();
        dropdown.replaceChildren();

        if (locations.length === 0) {
            // When no matching location
            dropdown.innerHTML = '<li class="list-group-item text-muted text-xs">No locations found</li>';
        } else {
            // When found matching location
            for (const loc of locations) {
                const li = document.createElement("li");
                li.className = "list-group-item list-group-item-action text-sm autocomplete-item cursor-pointer";

                const fullAddress = loc.display_name || "Unknown Location";
                // Split out into location name and address for readability
                const locationName = fullAddress.split(',')[0].trim();

                li.dataset.name = locationName;
                li.dataset.lat = loc.lat;
                li.dataset.lon = loc.lon;
                li.innerHTML = `<strong>${locationName}</strong><br><small class="text-muted">${fullAddress}</small>`;

                dropdown.appendChild(li);
            }
        }
        dropdown.classList.remove("d-none");
    } catch (error) {
        console.error("Search autocomplete error:", error);
    }
}, 400)); // Waits 400ms after the user stops typing before calling the API

// Handle selecting an item from the dropdown or closing it
document.addEventListener("click", function (event) {
    // Check if the user clicked on a dropdown item
    const item = event.target.closest(".autocomplete-item");

    if (item) {
        // Populate the visible input and hidden coordinates with the selected data
        const wrapper = item.closest(".location-autocomplete-wrapper");
        const nameInput = wrapper.querySelector(".location-autocomplete-input");
        const latInput = wrapper.querySelector(".location-autocomplete-lat");
        const lonInput = wrapper.querySelector(".location-autocomplete-lon");

        nameInput.value = item.dataset.name;
        nameInput.classList.remove("is-invalid");

        if (latInput && lonInput) {
            latInput.value = item.dataset.lat;
            lonInput.value = item.dataset.lon;
        }
        // Hide the dropdown after selection
        wrapper.querySelector(".location-autocomplete-dropdown").classList.add("d-none");
    } else {
        // Hide the dropdown if the user clicks anywhere else on the screen
        document.querySelectorAll(".location-autocomplete-dropdown:not(.d-none)").forEach(dropdown => {
            dropdown.classList.add("d-none");
        });
    }
});

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