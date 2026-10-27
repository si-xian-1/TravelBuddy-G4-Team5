/**
 * Manage edit, delete, and display of flights.
 * Displays disaster warnings if applicable.
 */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    const deleteModal = document.getElementById("confirmDeleteModal");

    // Ensure this script only runs on pages with the correct DOM elements
    if (!page || !deleteModal) {
        return;
    }

    const itineraryId = page.dataset.itineraryId;
    const flightsList = document.getElementById("flightsList");
    const flightTemplate = document.getElementById("flightCard");
    const deleteConfirmButton = document.getElementById("confirmDeleteModalBtn");
    let flightPendingDelete = null;

    // Fetches all flights from the API and populates the UI cards
    async function loadFlights() {
        const loadingMessage = document.getElementById("flightsLoading");
        const emptyMessage = document.getElementById("noFlights");
        const errorMessage = document.getElementById("flightsError");

        // Reset UI states
        loadingMessage.classList.remove("d-none");
        emptyMessage.classList.add("d-none");
        errorMessage.classList.add("d-none");
        flightsList.replaceChildren(); // Clears out existing cards

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/flights/all`, {
                credentials: "same-origin"
            });

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

            // Generate HTML cards for each flight using the template
            for (const flight of flights) {
                const card = flightTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".flight-card");
                const disasterAlert = card.querySelector(".disaster-alert");

                // Store data securely on the element for editing later
                cardElement.dataset.flightId = flight.flightId;
                cardElement.dataset.airline = flight.airline || "";
                cardElement.dataset.flightNumber = flight.flightNumber || "";
                cardElement.dataset.departureAirport = flight.departureAirport || "";
                cardElement.dataset.arrivalAirport = flight.arrivalAirport || "";
                cardElement.dataset.departureLatitude = flight.departureLatitude || "";
                cardElement.dataset.departureLongitude = flight.departureLongitude || "";
                cardElement.dataset.arrivalLatitude = flight.arrivalLatitude || "";
                cardElement.dataset.arrivalLongitude = flight.arrivalLongitude || "";

                const depTimeStr = toDateTimeInputValue(flight.departureTime);
                const arrTimeStr = toDateTimeInputValue(flight.arrivalTime);
                cardElement.dataset.departureTime = depTimeStr;
                cardElement.dataset.arrivalTime = arrTimeStr;

                // Populate display text
                card.querySelector(".flight-airline").textContent = flight.airline || "Airline";
                card.querySelector(".flight-number").textContent = flight.flightNumber || "";
                card.querySelector(".flight-dep-airport").textContent = flight.departureAirport || "Departure";
                card.querySelector(".flight-arr-airport").textContent = flight.arrivalAirport || "Arrival";
                card.querySelector(".flight-dep-time").textContent = formatDateTime(flight.departureTime);
                card.querySelector(".flight-arr-time").textContent = formatDateTime(flight.arrivalTime);

                // Handle disaster warnings
                if (flight.disasterWarning && flight.disasterWarning.trim()) {
                    disasterAlert.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> ${flight.disasterWarning}`;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
                    disasterAlert.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Safe area (No active alerts)';
                    disasterAlert.classList.add("alert-success");
                    disasterAlert.classList.remove("alert-danger");
                }

                // Pre-fill the hidden edit form with current data
                card.querySelector(".edit-flight-airline").value = flight.airline || "";
                card.querySelector(".edit-flight-number").value = flight.flightNumber || "";
                card.querySelector(".edit-flight-dep-airport").value = flight.departureAirport || "";
                card.querySelector(".edit-flight-arr-airport").value = flight.arrivalAirport || "";

                card.querySelector(".edit-flight-dep-lat").value = flight.departureLatitude || "";
                card.querySelector(".edit-flight-dep-lon").value = flight.departureLongitude || "";
                card.querySelector(".edit-flight-arr-lat").value = flight.arrivalLatitude || "";
                card.querySelector(".edit-flight-arr-lon").value = flight.arrivalLongitude || "";

                const depTimeInput = card.querySelector(".edit-flight-dep-time");
                const arrTimeInput = card.querySelector(".edit-flight-arr-time");

                DateTimeValidation.setMinimumNow(depTimeInput);
                DateTimeValidation.setMinimumNow(arrTimeInput);

                // Store warning text on the element so applyToCard can read it
                cardElement.dataset.disasterWarning = flight.disasterWarning || "";

                flightsList.appendChild(card);

                // Reveal "Get Alternatives" button if this flight has an active alert
                if (window.AlternativesModal) {
                    window.AlternativesModal.applyToCard(cardElement, flight.disasterWarning || "");
                }
            }
        } catch (error) {
            console.error("Unable to load itinerary flights:", error);
            loadingMessage.classList.add("d-none");
            errorMessage.textContent = error.message || "Unable to load flights for this itinerary.";
            errorMessage.classList.remove("d-none");
        }
    }

    // Resets the edit form back to the original database values in case the user hit "Cancel"
    function resetFlightEditForm(card) {
        const form = card.querySelector(".flight-edit-form");
        form.querySelector(".edit-flight-airline").value = card.dataset.airline;
        form.querySelector(".edit-flight-number").value = card.dataset.flightNumber;
        form.querySelector(".edit-flight-dep-airport").value = card.dataset.departureAirport;
        form.querySelector(".edit-flight-arr-airport").value = card.dataset.arrivalAirport;

        form.querySelector(".edit-flight-dep-lat").value = card.dataset.departureLatitude || "";
        form.querySelector(".edit-flight-dep-lon").value = card.dataset.departureLongitude || "";
        form.querySelector(".edit-flight-arr-lat").value = card.dataset.arrivalLatitude || "";
        form.querySelector(".edit-flight-arr-lon").value = card.dataset.arrivalLongitude || "";

        // Hide any open search dropdowns
        form.querySelectorAll(".location-autocomplete-dropdown").forEach(dd => dd.classList.add("d-none"));

        const departureTimeInput = form.querySelector(".edit-flight-dep-time");
        const arrivalTimeInput = form.querySelector(".edit-flight-arr-time");
        departureTimeInput.value = card.dataset.departureTime;
        arrivalTimeInput.value = card.dataset.arrivalTime;

        DateTimeValidation.setMinimumNow(departureTimeInput);
        DateTimeValidation.setMinimumNow(arrivalTimeInput);

        card.querySelector(".flight-edit-error").classList.add("d-none");
        clearFormValidation(form);
    }

    // Event delegation: Listens for Edit, Cancel, and Delete clicks on any flight card
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
            document.getElementById("confirmDeleteModalBody").textContent = `Delete "${card.dataset.airline} ${card.dataset.flightNumber}"? This action cannot be undone.`;
            $(deleteModal).modal("show");
        }
    });

    // Clear specific field errors when the user begins typing inside the inline edit form
    flightsList.addEventListener("input", function (event) {
        const input = event.target;
        if (input.classList.contains("form-control")) {
            input.classList.remove("is-invalid");

            const identifierClass = Array.from(input.classList).find(cls => cls.startsWith("edit-flight-") && !cls.endsWith("-lat") && !cls.endsWith("-lon"));
            if (identifierClass) {
                const errorElement = input.closest("form").querySelector(`.${identifierClass}-error`);
                if (errorElement) errorElement.textContent = "";
            }
        }
    });

    // Cleanup when the delete modal is dismissed
    $(deleteModal).on("hidden.bs.modal", function () {
        if (deleteModal.dataset.deleteType === "flight") {
            flightPendingDelete = null;
            deleteModal.dataset.deleteType = "";
        }
    });

    // Executes the actual deletion request to the API
    deleteConfirmButton.addEventListener("click", async function () {
        if (deleteModal.dataset.deleteType !== "flight" || !flightPendingDelete) {
            return;
        }

        const flightId = flightPendingDelete.dataset.flightId;
        deleteConfirmButton.disabled = true; // Prevent double-clicks

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/flights/${encodeURIComponent(flightId)}`, {
                method: "DELETE",
                credentials: "same-origin"
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            $(deleteModal).modal("hide");
            flightPendingDelete = null;
            await loadFlights(); // Refresh the list
        } catch (deleteError) {
            console.error("Unable to delete flight:", deleteError);
            const error = flightPendingDelete.querySelector(".flight-edit-error");
            if (error) {
                error.textContent = deleteError.message || "Unable to delete this flight.";
                error.classList.remove("d-none");
            }
            $(deleteModal).modal("hide");
            flightPendingDelete = null;
        } finally {
            deleteConfirmButton.disabled = false;
        }
    });

    // Validates and submits the edit form for a flight
    flightsList.addEventListener("submit", async function (event) {
        // Check if edit form is the one submitted
        if (!event.target.matches(".flight-edit-form")) {
            return;
        }

        event.preventDefault();

        const form = event.target;
        const card = form.closest(".flight-card");
        const flightId = card.dataset.flightId;
        const error = card.querySelector(".flight-edit-error");
        const saveButton = form.querySelector(".save-flight");

        // Gather input values
        const airline = form.querySelector(".edit-flight-airline").value.trim();
        const flightNumber = form.querySelector(".edit-flight-number").value.trim();
        const departureAirport = form.querySelector(".edit-flight-dep-airport").value.trim();
        const arrivalAirport = form.querySelector(".edit-flight-arr-airport").value.trim();
        const departureTime = form.querySelector(".edit-flight-dep-time").value;
        const arrivalTime = form.querySelector(".edit-flight-arr-time").value;

        // Gather coordinates
        const departureLatitude = form.querySelector(".edit-flight-dep-lat").value;
        const departureLongitude = form.querySelector(".edit-flight-dep-lon").value;
        const arrivalLatitude = form.querySelector(".edit-flight-arr-lat").value;
        const arrivalLongitude = form.querySelector(".edit-flight-arr-lon").value;

        error.classList.add("d-none");
        clearFormValidation(form);
        let isValid = true;

        // Check for empty required text fields
        const requiredFields = [
            ["edit-flight-airline", airline],
            ["edit-flight-number", flightNumber]
        ];

        for (const [selector, value] of requiredFields) {
            if (!value) {
                showFormFieldError(form, selector, "This field is required.");
                isValid = false;
            }
        }

        // Ensure Locations were selected from the dropdown
        if (!departureAirport || !departureLatitude || !departureLongitude) {
            showFormFieldError(form, "edit-flight-dep-airport", "Please select a valid departure airport from the search results.");
            isValid = false;
        }
        if (!arrivalAirport || !arrivalLatitude || !arrivalLongitude) {
            showFormFieldError(form, "edit-flight-arr-airport", "Please select a valid arrival airport from the search results.");
            isValid = false;
        }

        // Ensure dates provided are not in the past
        if (!departureTime) {
            showFormFieldError(form, "edit-flight-dep-time", "Departure time is required.");
            isValid = false;
        } else if (!DateTimeValidation.isNowOrLater(departureTime)) {
            showFormFieldError(form, "edit-flight-dep-time", "Date and time must be now or later.");
            isValid = false;
        }

        if (!arrivalTime) {
            showFormFieldError(form, "edit-flight-arr-time", "Arrival time is required.");
            isValid = false;
        } else if (!DateTimeValidation.isNowOrLater(arrivalTime)) {
            showFormFieldError(form, "edit-flight-arr-time", "Date and time must be now or later.");
            isValid = false;
        }

        // Arrival time must occur after departure time
        if (departureTime && arrivalTime && new Date(arrivalTime) <= new Date(departureTime)) {
            showFormFieldError(form, "edit-flight-arr-time", "Arrival time must be after departure time.");
            isValid = false;
        }

        if (!isValid) {
            return; // Halt submission if validation failed
        }

        saveButton.disabled = true; // Prevent multiple submissions
        try {
            const flightData = {
                airline,
                flightNumber,
                departureAirport,
                arrivalAirport,
                departureTime,
                arrivalTime,
                departureLatitude: Number(departureLatitude),
                departureLongitude: Number(departureLongitude),
                arrivalLatitude: Number(arrivalLatitude),
                arrivalLongitude: Number(arrivalLongitude)
            };

            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/flights/${encodeURIComponent(flightId)}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify(flightData)
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            await loadFlights(); // Reload everything on success
        } catch (updateError) {
            console.error("Unable to update flight:", updateError);
            error.textContent = updateError.message || "Unable to update this flight.";
            error.classList.remove("d-none");
            saveButton.disabled = false;
        }
    });

    // Boot up the page by triggering the initial load
    await loadFlights();
});