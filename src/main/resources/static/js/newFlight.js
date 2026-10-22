document.getElementById("flightForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    const form = event.currentTarget;
    const errorAlert = document.getElementById("flightError");
    const saveButton = document.getElementById("saveFlightBtn");
    const itineraryId = form.dataset.itineraryId;
    const flight = {
        airline: document.getElementById("airline").value.trim(),
        flightNumber: document.getElementById("flightNumber").value.trim(),
        departureAirport: document.getElementById("departureAirport").value.trim(),
        arrivalAirport: document.getElementById("arrivalAirport").value.trim(),
        departureTime: document.getElementById("departureTime").value,
        arrivalTime: document.getElementById("arrivalTime").value
    };

    errorAlert.classList.add("d-none");
    form.querySelectorAll(".form-control").forEach(input => input.classList.remove("is-invalid"));
    form.querySelectorAll(".invalid-feedback").forEach(message => message.textContent = "");

    let isValid = true;
    for (const field of ["airline", "flightNumber", "departureAirport", "arrivalAirport",
        "departureTime", "arrivalTime"]) {
        if (!flight[field]) {
            showFlightFieldError(field, "This field is required.");
            isValid = false;
        }
    }

    for (const field of ["departureTime", "arrivalTime"]) {
        if (flight[field] && !DateTimeValidation.isNowOrLater(flight[field])) {
            showFlightFieldError(field, "Date and time must be now or later.");
            isValid = false;
        }
    }

    if (flight.departureTime && flight.arrivalTime
        && new Date(flight.arrivalTime) <= new Date(flight.departureTime)) {
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
            throw new Error(getFlightErrorMessage(responseBody, response.status));
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
    document.getElementById(`${field}Error`).textContent = message;
}

function getFlightErrorMessage(responseBody, status) {
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
