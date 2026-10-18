/**
 * Manage edit, delete and display of hotel
 * Display disaster warning
 */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    const itineraryId = page.dataset.itineraryId;
    const hotelsList = document.getElementById("hotelsList");
    const hotelTemplate = document.getElementById("hotelCard");
    // Shared delete confirmation modal
    const deleteModal = document.getElementById("confirmDeleteModal");
    const deleteConfirmButton = document.getElementById("confirmDeleteModalBtn");
    let hotelPendingDelete = null;
    // Store selected locations separately for each hotel edit form.
    const hotelSelections = new WeakMap();
    let hotelSearchTimer;
    let hotelSearchController;

    async function loadHotels() {
        const loadingMessage = document.getElementById("hotelsLoading");
        const emptyMessage = document.getElementById("noHotels");
        const errorMessage = document.getElementById("hotelsError");

        loadingMessage.classList.remove("d-none");
        emptyMessage.classList.add("d-none");
        errorMessage.classList.add("d-none");
        hotelsList.replaceChildren();

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/all`,
                { credentials: "same-origin" }
            );

            if (!response.ok) {
                throw new Error(`Failed to load hotels (${response.status}).`);
            }

            const hotels = await response.json();
            loadingMessage.classList.add("d-none");
            setItinerarySectionExpanded("collapseHotels", hotels.length > 0);

            if (hotels.length === 0) {
                emptyMessage.classList.remove("d-none");
                return;
            }

            for (const hotel of hotels) {
                const card = hotelTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".hotel-card");
                const nameInput = card.querySelector(".edit-hotel-name");
                const checkInDateInput = card.querySelector(".edit-hotel-checkin");
                const checkOutDateInput = card.querySelector(".edit-hotel-checkout");
                const disasterAlert = card.querySelector(".disaster-alert");

                cardElement.dataset.hotelId = hotel.hotelId;
                cardElement.dataset.originalHotelName = hotel.hotelName || "";
                cardElement.dataset.originalLatitude = hotel.latitude;
                cardElement.dataset.originalLongitude = hotel.longitude;
                cardElement.dataset.originalCheckIn = toDateTimeInputValue(hotel.checkIn);
                cardElement.dataset.originalCheckOut = toDateTimeInputValue(hotel.checkOut);
                card.querySelector(".hotel-name").textContent = hotel.hotelName;
                card.querySelector(".hotel-checkin").textContent =
                    formatVisitDate(hotel.checkIn);
                card.querySelector(".hotel-checkout").textContent =
                    formatVisitDate(hotel.checkOut);
                // card.querySelector(".hotel-location").textContent =
                //     formatCoordinates(hotel.latitude, hotel.longitude);
                nameInput.value = hotel.hotelName || "";
                checkInDateInput.value = toDateTimeInputValue(hotel.checkIn);
                checkOutDateInput.value = toDateTimeInputValue(hotel.checkOut);

                // Display disaster warning if any
                if (hotel.disasterWarning && hotel.disasterWarning.trim() !== "") {
                    // If there is a warning, make it Red and show the message
                    disasterAlert.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> ${hotel.disasterWarning}`;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
                    // If it is null or empty, it will show green status
                    disasterAlert.innerHTML = `<i class="fas fa-check-circle mr-1"></i> Safe area (No active alerts)`;
                    disasterAlert.classList.add("alert-success");
                    disasterAlert.classList.remove("alert-danger");
                }

                hotelsList.appendChild(card);
            }
        } catch (error) {
            console.error("Unable to load itinerary hotels:", error);
            loadingMessage.classList.add("d-none");
            errorMessage.textContent = error.message || "Unable to load hotels for this itinerary.";
            errorMessage.classList.remove("d-none");
        }
    }

    // Handle Edit and Cancel buttons for hotel cards.
    hotelsList.addEventListener("click", function (event) {
        const button = event.target.closest("button");
        if (!button) return;

        const card = button.closest(".hotel-card");
        if (!card) return;

        const details = card.querySelector(".hotel-details");
        const editForm = card.querySelector(".hotel-edit-form");
        const error = card.querySelector(".hotel-edit-error");

        if (button.classList.contains("edit-hotel")) {
            // Reset the selected location when opening the form.
            hotelSelections.delete(editForm);
            // Prepopulate the form using the displayed hotel data.
            editForm.querySelector(".edit-hotel-name").value =
                card.querySelector(".hotel-name").textContent;
            editForm.querySelector(".edit-hotel-checkin").value =
                card.dataset.originalCheckIn;

            editForm.querySelector(".edit-hotel-checkout").value =
                card.dataset.originalCheckOut;
            details.classList.add("d-none");
            editForm.classList.remove("d-none");
            error.classList.add("d-none");

        } else if (button.classList.contains("cancel-hotel-edit")) {
            hotelSelections.delete(editForm);
            clearTimeout(hotelSearchTimer);

            if (hotelSearchController) {
                hotelSearchController.abort();
            }

            // Discard changes and return to hotel details.
            editForm.classList.add("d-none");
            details.classList.remove("d-none");
            error.classList.add("d-none");

            editForm.querySelectorAll(".form-control").forEach(input => {
                input.classList.remove("is-invalid");
            });

            editForm.querySelectorAll(".invalid-feedback").forEach(element => {
                element.textContent = "";
            });

            const suggestions = editForm.querySelector(".hotel-edit-suggestions");
            if (suggestions) {
                suggestions.classList.add("d-none");
                suggestions.replaceChildren();
            }
        } else if (button.classList.contains("delete-hotel")) {
            // Remember which hotel the user wants to delete.
            hotelPendingDelete = card;

            // Identify the item type for the shared modal.
            deleteModal.dataset.deleteType = "hotel";

            // Update confirmation modal content.
            document.getElementById("confirmDeleteModalTitle").textContent =
                "Delete Hotel";

            document.getElementById("confirmDeleteModalBody").textContent =
                `Delete "${card.querySelector(".hotel-name").textContent}"? This action cannot be undone.`;

            // Show the Bootstrap confirmation popup.
            $(deleteModal).modal("show");

        }
    });


    // Search for hotels when editing an existing hotel.
    hotelsList.addEventListener("input", function (event) {
        if (!event.target.classList.contains("edit-hotel-name")) {
            return;
        }

        const input = event.target;
        const form = input.closest(".hotel-edit-form");
        const suggestions = form.querySelector(".hotel-edit-suggestions");

        clearTimeout(hotelSearchTimer);

        if (hotelSearchController) {
            hotelSearchController.abort();
        }

        // Typing invalidates the previous location selection.
        hotelSelections.delete(form);

        input.classList.remove("is-invalid");
        form.querySelector(".edit-hotel-name-error").textContent = "";

        suggestions.replaceChildren();
        suggestions.classList.add("d-none");

        const query = input.value.trim();

        if (query.length < 3) return;

        hotelSearchTimer = setTimeout(async () => {
            const controller = new AbortController();
            hotelSearchController = controller;

            try {
                const response = await fetch(
                    `/api/location/search?query=${encodeURIComponent(query)}`,
                    { signal: controller.signal }
                );

                if (!response.ok) {
                    throw new Error("Unable to search locations.");
                }

                const locations = await response.json();

                if (controller.signal.aborted ||
                    input.value.trim() !== query ||
                    form.classList.contains("d-none")) {
                    return;
                }

                suggestions.replaceChildren();

                if (!Array.isArray(locations) || locations.length === 0) {
                    const message = document.createElement("div");
                    message.className = "list-group-item text-muted";
                    message.textContent = "No matching locations found.";
                    suggestions.appendChild(message);
                } else {
                    locations.forEach(location => {
                        const item = document.createElement("button");
                        item.type = "button";
                        item.className =
                            "list-group-item list-group-item-action";

                        item.textContent = location.displayName;

                        item.addEventListener("click", () => {
                            const latitude = Number(location.lat);
                            const longitude = Number(location.lon);

                            if (!Number.isFinite(latitude) ||
                                !Number.isFinite(longitude)) {
                                return;
                            }

                            hotelSelections.set(form, {
                                hotelName: location.displayName,
                                latitude,
                                longitude
                            });

                            input.value = location.displayName;
                            input.classList.remove("is-invalid");
                            form.querySelector(
                                ".edit-hotel-name-error"
                            ).textContent = "";

                            suggestions.replaceChildren();
                            suggestions.classList.add("d-none");
                        });

                        suggestions.appendChild(item);
                    });
                }

                suggestions.classList.remove("d-none");

            } catch (error) {
                if (error.name !== "AbortError") {
                    console.error("Hotel location search failed:", error);
                    suggestions.replaceChildren();
                    suggestions.classList.add("d-none");
                }
            }
        }, 400);
    });

    // Close suggestions when clicking outside the search field.
    document.addEventListener("click", function (event) {
        hotelsList.querySelectorAll(".hotel-edit-form").forEach(form => {
            const input = form.querySelector(".edit-hotel-name");
            const suggestions = form.querySelector(".hotel-edit-suggestions");

            if (!input.contains(event.target) &&
                !suggestions.contains(event.target)) {
                suggestions.classList.add("d-none");
            }
        });
    });


    // Display an error below an edit field.
    function showHotelEditFieldError(input, errorElement, message) {
        input.classList.add("is-invalid");
        errorElement.textContent = message;
    }

    // Clear field validation messages.
    function clearHotelEditErrors(form, errorBox) {
        form.querySelectorAll(".form-control").forEach(input => {
            input.classList.remove("is-invalid");
        });

        form.querySelectorAll(".invalid-feedback").forEach(element => {
            element.textContent = "";
        });

        errorBox.textContent = "";
        errorBox.classList.add("d-none");
    }

    // Validate hotel details before submitting.
    function validateHotelEdit(form, card) {
        const nameInput = form.querySelector(".edit-hotel-name");
        const checkInInput = form.querySelector(".edit-hotel-checkin");
        const checkOutInput = form.querySelector(".edit-hotel-checkout");

        const nameError = form.querySelector(".edit-hotel-name-error");
        const checkInError = form.querySelector(".edit-hotel-checkin-error");
        const checkOutError = form.querySelector(".edit-hotel-checkout-error");

        const hotelName = nameInput.value.trim();
        const checkIn = checkInInput.value;
        const checkOut = checkOutInput.value;

        const originalName = card.dataset.originalHotelName;
        const selectedLocation = hotelSelections.get(form);

        let isValid = true;

        // Hotel name is required.
        if (!hotelName) {
            showHotelEditFieldError(
                nameInput, nameError, "Hotel name is required."
            );
            isValid = false;
        } else if (
            hotelName !== originalName &&
            (!selectedLocation ||
                selectedLocation.hotelName !== hotelName)
        ) {
            // A changed hotel must come from a search suggestion.
            showHotelEditFieldError(
                nameInput,
                nameError,
                "Please select a hotel from the search results."
            );
            isValid = false;
        }

        // Check-in is required and must be valid.
        if (!checkIn) {
            showHotelEditFieldError(
                checkInInput,
                checkInError,
                "Check-in date and time is required."
            );
            isValid = false;
        } else if (Number.isNaN(new Date(checkIn).getTime())) {
            showHotelEditFieldError(
                checkInInput,
                checkInError,
                "Please enter a valid check-in date and time."
            );
            isValid = false;
        }

        // Check-out is required and must be valid.
        if (!checkOut) {
            showHotelEditFieldError(
                checkOutInput,
                checkOutError,
                "Check-out date and time is required."
            );
            isValid = false;
        } else if (Number.isNaN(new Date(checkOut).getTime())) {
            showHotelEditFieldError(
                checkOutInput,
                checkOutError,
                "Please enter a valid check-out date and time."
            );
            isValid = false;
        } else if (
            checkIn &&
            !Number.isNaN(new Date(checkIn).getTime()) &&
            new Date(checkOut) <= new Date(checkIn)
        ) {
            showHotelEditFieldError(
                checkOutInput,
                checkOutError,
                "Check-out must be later than check-in."
            );
            isValid = false;
        }

        return isValid;
    }

    // Save changes to an existing hotel.
    hotelsList.addEventListener("submit", async function (event) {
        const form = event.target;

        if (!form.classList.contains("hotel-edit-form")) {
            return;
        }

        event.preventDefault();

        const card = form.closest(".hotel-card");
        const errorBox = card.querySelector(".hotel-edit-error");

        clearHotelEditErrors(form, errorBox);

        if (!validateHotelEdit(form, card)) {
            return;
        }

        const hotelId = card.dataset.hotelId;
        const nameInput = form.querySelector(".edit-hotel-name");
        const checkInInput = form.querySelector(".edit-hotel-checkin");
        const checkOutInput = form.querySelector(".edit-hotel-checkout");
        const saveButton = form.querySelector(".save-hotel");

        const newHotelName = nameInput.value.trim();
        const originalName = card.dataset.originalHotelName;

        // Preserve original coordinates if the name is unchanged.
        const selectedLocation = hotelSelections.get(form);

        const hotelData = {
            hotelName: newHotelName,
            latitude: newHotelName === originalName
                ? Number(card.dataset.originalLatitude)
                : selectedLocation.latitude,
            longitude: newHotelName === originalName
                ? Number(card.dataset.originalLongitude)
                : selectedLocation.longitude,
            checkIn: checkInInput.value,
            checkOut: checkOutInput.value
        };

        saveButton.disabled = true;
        saveButton.textContent = "Saving...";

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/${encodeURIComponent(hotelId)}`,
                {
                    method: "PUT",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    credentials: "same-origin",
                    body: JSON.stringify(hotelData)
                }
            );

            if (!response.ok) {
                const responseBody = await response.text();
                let message = `Failed to update hotel (${response.status}).`;

                if (responseBody) {
                    try {
                        const errorData = JSON.parse(responseBody);
                        message = errorData.message ||
                            errorData.detail ||
                            message;
                    } catch {
                        message = responseBody;
                    }
                }

                throw new Error(message);
            }

            // Reload hotel cards to display the saved changes.
            await loadHotels();

        } catch (error) {
            console.error("Unable to update hotel:", error);

            errorBox.textContent =
                error.message || "Unable to update hotel.";

            errorBox.classList.remove("d-none");

        } finally {
            saveButton.disabled = false;
            saveButton.textContent = "Save";
        }
    });
    $(deleteModal).on("hidden.bs.modal", function () {
        hotelPendingDelete = null;
    });


    // Delete hotel after the user confirms in the shared modal.
    deleteConfirmButton.addEventListener("click", async function () {
        // Only handle hotel deletion, not attraction or flight.
        if (deleteModal.dataset.deleteType !== "hotel" ||
            !hotelPendingDelete) {
            return;
        }

        const card = hotelPendingDelete;
        const hotelId = card.dataset.hotelId;

        // Prevent duplicate requests.
        deleteConfirmButton.disabled = true;

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/${encodeURIComponent(hotelId)}`,
                {
                    method: "DELETE",
                    credentials: "same-origin"
                }
            );

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(
                    responseBody || `Failed to delete hotel (${response.status}).`
                );
            }

            // Close the confirmation modal.
            $(deleteModal).modal("hide");
            hotelPendingDelete = null;

            // Refresh the hotel list.
            await loadHotels();

        } catch (error) {
            console.error("Unable to delete hotel:", error);

            // Display the error on the hotel card.
            const errorBox = card.querySelector(".hotel-edit-error");

            if (errorBox) {
                errorBox.textContent =
                    error.message || "Unable to delete this hotel.";
                errorBox.classList.remove("d-none");
            }

            $(deleteModal).modal("hide");
            hotelPendingDelete = null;

        } finally {
            deleteConfirmButton.disabled = false;
        }
    });

    await loadHotels();
});