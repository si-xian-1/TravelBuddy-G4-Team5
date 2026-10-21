document.getElementById("flightForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    const form = event.currentTarget;
    const errorAlert = document.getElementById("flightError");
    const saveButton = document.getElementById("saveFlightBtn");
    const itineraryId = form.dataset.itineraryId;

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
        departureTime: document.getElementById("departureTime").value,
        arrivalTime: document.getElementById("arrivalTime").value,
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
    for (const field of ["departureTime", "arrivalTime"]) {
        if (!flight[field]) {
            showFlightFieldError(field, "This field is required.");
            isValid = false;
        } else if (!DateTimeValidation.isNowOrLater(flight[field])) {
            showFlightFieldError(field, "Date and time must be now or later.");
            isValid = false;
        }
    }

    if (flight.departureTime && flight.arrivalTime && new Date(flight.arrivalTime) <= new Date(flight.departureTime)) {
        showFlightFieldError("arrivalTime", "Arrival time must be after departure time.");
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

function showFlightFieldError(field, message) {
    document.getElementById(field).classList.add("is-invalid");
    const errorContainer = document.getElementById(`${field}Error`);
    if (errorContainer) {
        errorContainer.textContent = message;
    }
}