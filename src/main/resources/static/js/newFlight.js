document.addEventListener("DOMContentLoaded", async function () {
    const form = document.getElementById("flightForm");
    if (!form) return;

    const errorAlert = document.getElementById("flightError");
    const saveButton = document.getElementById("saveFlightBtn");
    const itineraryId = form.dataset.itineraryId;

    const depTimeInput = document.getElementById("departureTime");
    const arrTimeInput = document.getElementById("arrivalTime");

    let itinerary = null;
    try {
        itinerary = await getItinerary(itineraryId);
        if (itinerary) {
            DateTimeValidation.setItineraryLimits(depTimeInput, itinerary.startDate, itinerary.endDate);
            DateTimeValidation.setItineraryLimits(arrTimeInput, itinerary.startDate, itinerary.endDate);
        }
    } catch (e) {
        console.error("Unable to load itinerary for flight date validation:", e);
    }

    form.addEventListener("submit", async function (event) {
        event.preventDefault();

        // Extract hidden coordinates safely
        const departureLatitude = document.getElementById("departureLatitude")?.value || "";
        const departureLongitude = document.getElementById("departureLongitude")?.value || "";
        const arrivalLatitude = document.getElementById("arrivalLatitude")?.value || "";
        const arrivalLongitude = document.getElementById("arrivalLongitude")?.value || "";

        const flight = {
            airline: document.getElementById("airline").value.trim(),
            flightNumber: document.getElementById("flightNumber").value.trim(),
            departureAirport: document.getElementById("departureAirport").value.trim(),
            arrivalAirport: document.getElementById("arrivalAirport").value.trim(),
            departureTime: depTimeInput.value,
            arrivalTime: arrTimeInput.value,
            departureLatitude: Number(departureLatitude),
            departureLongitude: Number(departureLongitude),
            arrivalLatitude: Number(arrivalLatitude),
            arrivalLongitude: Number(arrivalLongitude)
        };

        errorAlert.classList.add("d-none");
        form.querySelectorAll(".form-control").forEach(input => input.classList.remove("is-invalid"));
        form.querySelectorAll(".invalid-feedback").forEach(message => message.textContent = "");

        let isValid = true;

        // Validate basic text fields
        for (const field of ["airline", "flightNumber"]) {
            if (!flight[field]) {
                showFlightFieldError(field, "This field is required.");
                isValid = false;
            }
        }

        // Validate Departure Airport Selection
        if (!flight.departureAirport || !departureLatitude || !departureLongitude) {
            showFlightFieldError("departureAirport", "Please select a valid departure airport from the search results.");
            isValid = false;
        }

        // Validate Arrival Airport Selection
        if (!flight.arrivalAirport || !arrivalLatitude || !arrivalLongitude) {
            showFlightFieldError("arrivalAirport", "Please select a valid arrival airport from the search results.");
            isValid = false;
        }

        // Validate Dates
        if (!flight.departureTime) {
            showFlightFieldError("departureTime", "This field is required.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(flight.departureTime, itinerary.startDate, itinerary.endDate)) {
            showFlightFieldError("departureTime", "Departure date and time must be within the itinerary dates.");
            isValid = false;
        }

        if (!flight.arrivalTime) {
            showFlightFieldError("arrivalTime", "This field is required.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(flight.arrivalTime, itinerary.startDate, itinerary.endDate)) {
            showFlightFieldError("arrivalTime", "Arrival date and time must be within the itinerary dates.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        saveButton.disabled = true;

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/flights/`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify(flight)
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            window.location.href = `/itinerary/view/${encodeURIComponent(itineraryId)}`;
        } catch (error) {
            console.error("Unable to create flight:", error);
            errorAlert.textContent = error.message || "Unable to create the flight. Please try again.";
            errorAlert.classList.remove("d-none");
            saveButton.disabled = false;
        }
    });

    // Clear specific field errors when the user begins typing or changing an input again
    form.querySelectorAll(".form-control").forEach(input => {
        input.addEventListener("input", () => {
            input.classList.remove("is-invalid");
            const errorContainer = document.getElementById(`${input.id}Error`);
            if (errorContainer) {
                errorContainer.textContent = "";
            }
        });
    });
});

function showFlightFieldError(field, message) {
    const fieldElement = document.getElementById(field);
    if (fieldElement) {
        fieldElement.classList.add("is-invalid");
    }
    const errorContainer = document.getElementById(`${field}Error`);
    if (errorContainer) {
        errorContainer.textContent = message;
    }
}
