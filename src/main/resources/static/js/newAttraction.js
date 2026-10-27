document.addEventListener("DOMContentLoaded", async function () {
    const form = document.getElementById("attractionForm");
    const nameInput = document.getElementById("attractionName");
    const latInput = document.getElementById("attractionLatitude");
    const lonInput = document.getElementById("attractionLongitude");
    const visitDateInput = document.getElementById("visitDate");
    const errorAlert = document.getElementById("attractionError");
    const saveButton = document.getElementById("saveAttractionBtn");

    const itineraryId = form.dataset.itineraryId;
    let itinerary = null;
    try {
        itinerary = await getItinerary(itineraryId);
        if (itinerary) {
            DateTimeValidation.setItineraryLimits(visitDateInput, itinerary.startDate, itinerary.endDate);
        }
    } catch (e) {
        console.error("Unable to load itinerary for attraction date validation:", e);
    }

    // Display an error under a specific field
    function showFieldError(input, errorId, message) {
        input.classList.add("is-invalid");
        document.getElementById(errorId).textContent = message;
    }

    // Clear all previous validation errors
    function clearValidationErrors() {
        errorAlert.classList.add("d-none");
        errorAlert.textContent = "";

        // Uses the shared utility function from detailedItinerary.js
        clearFormValidation(form);
        
        document.getElementById("attractionNameError").textContent = "";
        document.getElementById("visitDateError").textContent = "";
    }

    // Handle Form Submission to save the new attraction
    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        clearValidationErrors();

        const attractionName = nameInput.value.trim();
        const visitDate = visitDateInput.value;
        const latitude = latInput ? latInput.value : "";
        const longitude = lonInput ? lonInput.value : "";

        let isValid = true;

        // Validation Check: Name and Coordinates
        if (!attractionName) {
            showFieldError(nameInput, "attractionNameError", "Attraction name cannot be empty.");
            isValid = false;
        } else if (!latitude || !longitude) {
            showFieldError(nameInput, "attractionNameError", "Please select a valid location from the search results.");
            isValid = false;
        }

        // Validation Check: Date constraints
        if (!visitDate) {
            showFieldError(visitDateInput, "visitDateError", "Visit date and time is required.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(visitDate, itinerary.startDate, itinerary.endDate)) {
            showFieldError(visitDateInput, "visitDateError", "Visit date and time must be within the itinerary dates.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Disable the save button to prevent double-clicking and duplicate submissions
        saveButton.disabled = true;

        try {
            // Send the POST request to the backend API to create the attraction
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "same-origin",
                body: JSON.stringify({
                    attractionName,
                    visitDate,
                    latitude: Number(latitude),
                    longitude: Number(longitude)
                })
            });

            // If the server returns a 4xx or 5xx status code
            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            // Redirect back to the detailed itinerary view upon success
            window.location.href = `/itinerary/view/${encodeURIComponent(itineraryId)}`;
        } catch (error) {
            console.error("Unable to create attraction:", error);
            // Display the specific error message to the user
            errorAlert.textContent = error.message || "Unable to create the attraction. Please try again.";
            errorAlert.classList.remove("d-none");

            // Re-enable the button so they can fix the error and try again
            saveButton.disabled = false;
        }
    });

    // Clear specific field errors when the user begins typing or changing an input again
    [nameInput, visitDateInput].forEach(input => {
        input.addEventListener("input", () => {
            input.classList.remove("is-invalid");
            const errorElement = document.getElementById(input.id + "Error");
            if (errorElement) {
                errorElement.textContent = "";
            }
        });
    });
});