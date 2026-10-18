document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    const itineraryId = page.dataset.itineraryId;
    const flightsList = document.getElementById("flightsList");
    const flightTemplate = document.getElementById("flightCard");
    const loadingMessage = document.getElementById("flightsLoading");
    const emptyMessage = document.getElementById("noFlights");
    const errorMessage = document.getElementById("flightsError");
    const deleteModal = document.getElementById("confirmDeleteModal");
    const deleteConfirmButton = document.getElementById("confirmDeleteModalBtn");
    let flightPendingDelete = null;

    async function loadFlights() {
        loadingMessage.classList.remove("d-none");
        emptyMessage.classList.add("d-none");
        errorMessage.classList.add("d-none");
        flightsList.replaceChildren();

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/flights/all`,
                { credentials: "same-origin" }
            );

            if (!response.ok) {
                throw new Error(`Failed to load flights (${response.status}).`);
            }

            const flights = await response.json();
            loadingMessage.classList.add("d-none");
            setItinerarySectionExpanded("collapseFlights", flights.length > 0);

            if (flights.length === 0) {
                emptyMessage.classList.remove("d-none");
                return;
            }

            for (const flight of flights) {
                const card = flightTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".flight-card");
                const disasterAlert = card.querySelector(".disaster-alert");

                cardElement.dataset.flightId = flight.flightId;
                cardElement.dataset.airline = flight.airline || "";
                cardElement.dataset.flightNumber = flight.flightNumber || "";
                cardElement.dataset.departureAirport = flight.departureAirport || "";
                cardElement.dataset.arrivalAirport = flight.arrivalAirport || "";
                cardElement.dataset.departureTime = toFlightDateTimeInputValue(flight.departureTime);
                cardElement.dataset.arrivalTime = toFlightDateTimeInputValue(flight.arrivalTime);
                card.querySelector(".flight-airline").textContent = flight.airline || "Airline";
                card.querySelector(".flight-number").textContent = flight.flightNumber || "";
                card.querySelector(".flight-dep-airport").textContent =
                    flight.departureAirport || "Departure";
                card.querySelector(".flight-arr-airport").textContent =
                    flight.arrivalAirport || "Arrival";
                card.querySelector(".flight-dep-time").textContent =
                    formatFlightDateTime(flight.departureTime);
                card.querySelector(".flight-arr-time").textContent =
                    formatFlightDateTime(flight.arrivalTime);

                if (flight.disasterWarning && flight.disasterWarning.trim()) {
                    disasterAlert.textContent = flight.disasterWarning;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
                    disasterAlert.innerHTML =
                        '<i class="fas fa-check-circle mr-1"></i> Safe area (No active alerts)';
                    disasterAlert.classList.add("alert-success");
                    disasterAlert.classList.remove("alert-danger");
                }

                card.querySelector(".edit-flight-airline").value = flight.airline || "";
                card.querySelector(".edit-flight-number").value = flight.flightNumber || "";
                card.querySelector(".edit-flight-dep-airport").value = flight.departureAirport || "";
                card.querySelector(".edit-flight-arr-airport").value = flight.arrivalAirport || "";
                card.querySelector(".edit-flight-dep-time").value =
                    toFlightDateTimeInputValue(flight.departureTime);
                card.querySelector(".edit-flight-arr-time").value =
                    toFlightDateTimeInputValue(flight.arrivalTime);
                DateTimeValidation.setMinimumNow(card.querySelector(".edit-flight-dep-time"));
                DateTimeValidation.setMinimumNow(card.querySelector(".edit-flight-arr-time"));

                flightsList.appendChild(card);
            }
        } catch (error) {
            console.error("Unable to load itinerary flights:", error);
            loadingMessage.classList.add("d-none");
            errorMessage.textContent = error.message || "Unable to load flights for this itinerary.";
            errorMessage.classList.remove("d-none");
        }
    }

    flightsList.addEventListener("click", function (event) {
        const button = event.target.closest("button");
        if (!button) {
            return;
        }

        const card = button.closest(".flight-card");
        if (button.classList.contains("edit-flight")) {
            resetFlightEditForm(card);
            card.querySelector(".flight-details").classList.add("d-none");
            card.querySelector(".flight-edit-error").classList.add("d-none");
            card.querySelector(".flight-edit-form").classList.remove("d-none");
        } else if (button.classList.contains("cancel-flight-edit")) {
            resetFlightEditForm(card);
            card.querySelector(".flight-edit-form").classList.add("d-none");
            card.querySelector(".flight-details").classList.remove("d-none");
        } else if (button.classList.contains("delete-flight")) {
            flightPendingDelete = card;
            deleteModal.dataset.deleteType = "flight";
            document.getElementById("confirmDeleteModalTitle").textContent = "Delete Flight";
            document.getElementById("confirmDeleteModalBody").textContent =
                `Delete "${card.querySelector(".flight-airline").textContent} ${card.querySelector(".flight-number").textContent}"? This action cannot be undone.`;
            $(deleteModal).modal("show");
        }
    });

    $(deleteModal).on("hidden.bs.modal", function () {
        flightPendingDelete = null;
        deleteModal.dataset.deleteType = "";
    });

    deleteConfirmButton.addEventListener("click", async function () {
        if (deleteModal.dataset.deleteType !== "flight" || !flightPendingDelete) {
            return;
        }

        const flightId = flightPendingDelete.dataset.flightId;
        deleteConfirmButton.disabled = true;

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/flights/${encodeURIComponent(flightId)}`,
                {
                    method: "DELETE",
                    credentials: "same-origin"
                }
            );

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getFlightErrorMessage(responseBody, response.status));
            }

            $(deleteModal).modal("hide");
            flightPendingDelete = null;
            await loadFlights();
        } catch (deleteError) {
            console.error("Unable to delete flight:", deleteError);
            const error = flightPendingDelete.querySelector(".flight-edit-error");
            error.textContent = deleteError.message || "Unable to delete this flight.";
            error.classList.remove("d-none");
            $(deleteModal).modal("hide");
            flightPendingDelete = null;
        } finally {
            deleteConfirmButton.disabled = false;
        }
    });

    flightsList.addEventListener("submit", async function (event) {
        if (!event.target.matches(".flight-edit-form")) {
            return;
        }
        event.preventDefault();

        const form = event.target;
        const card = form.closest(".flight-card");
        const flightId = card.dataset.flightId;
        const error = card.querySelector(".flight-edit-error");
        const saveButton = form.querySelector(".save-flight");
        const airline = form.querySelector(".edit-flight-airline").value.trim();
        const flightNumber = form.querySelector(".edit-flight-number").value.trim();
        const departureAirport = form.querySelector(".edit-flight-dep-airport").value.trim();
        const arrivalAirport = form.querySelector(".edit-flight-arr-airport").value.trim();
        const departureTime = form.querySelector(".edit-flight-dep-time").value;
        const arrivalTime = form.querySelector(".edit-flight-arr-time").value;

        error.classList.add("d-none");
        clearFlightValidation(form);

        const requiredFields = [
            ["edit-flight-airline", airline],
            ["edit-flight-number", flightNumber],
            ["edit-flight-dep-airport", departureAirport],
            ["edit-flight-arr-airport", arrivalAirport],
            ["edit-flight-dep-time", departureTime],
            ["edit-flight-arr-time", arrivalTime]
        ];
        let isValid = true;

        for (const [selector, value] of requiredFields) {
            if (!value) {
                showFlightFieldError(form, selector, "This field is required.");
                isValid = false;
            }
        }

        for (const [selector, value] of [
            ["edit-flight-dep-time", departureTime],
            ["edit-flight-arr-time", arrivalTime]
        ]) {
            if (value && !DateTimeValidation.isNowOrLater(value)) {
                showFlightFieldError(form, selector, "Date and time must be now or later.");
                isValid = false;
            }
        }

        if (departureTime && arrivalTime && new Date(arrivalTime) <= new Date(departureTime)) {
            showFlightFieldError(
                form,
                "edit-flight-arr-time",
                "Arrival time must be after departure time."
            );
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        saveButton.disabled = true;
        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/flights/${encodeURIComponent(flightId)}`,
                {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    credentials: "same-origin",
                    body: JSON.stringify({
                        airline,
                        flightNumber,
                        departureAirport,
                        arrivalAirport,
                        departureTime,
                        arrivalTime
                    })
                }
            );

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getFlightErrorMessage(responseBody, response.status));
            }

            await loadFlights();
        } catch (updateError) {
            console.error("Unable to update flight:", updateError);
            error.textContent = updateError.message || "Unable to update this flight.";
            error.classList.remove("d-none");
            saveButton.disabled = false;
        }
    });

    await loadFlights();
});

function clearFlightValidation(form) {
    form.querySelectorAll(".form-control").forEach(input => input.classList.remove("is-invalid"));
    form.querySelectorAll(".invalid-feedback").forEach(message => message.textContent = "");
}

function resetFlightEditForm(card) {
    const form = card.querySelector(".flight-edit-form");
    form.querySelector(".edit-flight-airline").value = card.dataset.airline;
    form.querySelector(".edit-flight-number").value = card.dataset.flightNumber;
    form.querySelector(".edit-flight-dep-airport").value = card.dataset.departureAirport;
    form.querySelector(".edit-flight-arr-airport").value = card.dataset.arrivalAirport;

    const departureTimeInput = form.querySelector(".edit-flight-dep-time");
    const arrivalTimeInput = form.querySelector(".edit-flight-arr-time");
    departureTimeInput.value = card.dataset.departureTime;
    arrivalTimeInput.value = card.dataset.arrivalTime;
    DateTimeValidation.setMinimumNow(departureTimeInput);
    DateTimeValidation.setMinimumNow(arrivalTimeInput);

    card.querySelector(".flight-edit-error").classList.add("d-none");
    clearFlightValidation(form);
}

function showFlightFieldError(form, selector, message) {
    form.querySelector(`.${selector}`).classList.add("is-invalid");
    form.querySelector(`.${selector}-error`).textContent = message;
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

function formatFlightDateTime(value) {
    if (!value) {
        return "Not provided";
    }

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function toFlightDateTimeInputValue(value) {
    return value ? value.slice(0, 16) : "";
}
