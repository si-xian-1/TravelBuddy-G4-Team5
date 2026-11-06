/**
 * Manage edit, delete and display of hotels
 * Displays disaster warning if applicable.
 */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    
    // Ensure this script only runs on pages with the correct DOM elements
    if (!page) {
        return; 
    }

    const itineraryId = page.dataset.itineraryId;
    const hotelsList = document.getElementById("hotelsList");
    const hotelTemplate = document.getElementById("hotelCard");
    // Shared delete confirmation modal
    const deleteModal = document.getElementById("confirmDeleteModal");
    const deleteConfirmButton = document.getElementById("confirmDeleteModalBtn");
    let hotelPendingDelete = null;

    let itinerary = null;
    try {
        itinerary = await getItinerary(itineraryId);
    } catch (e) {
        console.error("Unable to load itinerary for hotel date validation:", e);
    }

    // Fetches all hotels from the API and populates the UI cards
    async function loadHotels() {
        const loadingMessage = document.getElementById("hotelsLoading");
        const emptyMessage = document.getElementById("noHotels");
        const errorMessage = document.getElementById("hotelsError");

        loadingMessage.classList.remove("d-none");
        emptyMessage.classList.add("d-none");
        errorMessage.classList.add("d-none");
        hotelsList.replaceChildren();

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/all`, {
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error(`Failed to load hotels (${response.status}).`);
            }

            const hotels = await response.json();
            loadingMessage.classList.add("d-none");
            
            // Expand the accordion if hotels exist
            setItinerarySectionExpanded("collapseHotels", hotels.length > 0);

            if (hotels.length === 0) {
                emptyMessage.classList.remove("d-none");
                return;
            }

            for (const hotel of hotels) {
                const card = hotelTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".hotel-card");
                const disasterAlert = card.querySelector(".disaster-alert");

                // Store data securely on the element for editing later
                cardElement.dataset.hotelId = hotel.hotelId;
                cardElement.dataset.originalHotelName = hotel.hotelName || "";
                cardElement.dataset.latitude = hotel.latitude || "";
                cardElement.dataset.longitude = hotel.longitude || "";
                cardElement.dataset.originalCheckIn = toDateTimeInputValue(hotel.checkIn);
                cardElement.dataset.originalCheckOut = toDateTimeInputValue(hotel.checkOut);
                
                // Populate display text
                card.querySelector(".hotel-name").textContent = hotel.hotelName;
                card.querySelector(".hotel-checkin").textContent = formatDateTime(hotel.checkIn);
                card.querySelector(".hotel-checkout").textContent = formatDateTime(hotel.checkOut);
                
                // Pre-fill the hidden edit form with current data
                card.querySelector(".edit-hotel-name").value = hotel.hotelName || "";
                
                card.querySelector(".edit-hotel-lat").value = hotel.latitude || "";
                card.querySelector(".edit-hotel-lon").value = hotel.longitude || "";

                const checkInDateInput = card.querySelector(".edit-hotel-checkin");
                const checkOutDateInput = card.querySelector(".edit-hotel-checkout");
                checkInDateInput.value = toDateTimeInputValue(hotel.checkIn);
                checkOutDateInput.value = toDateTimeInputValue(hotel.checkOut);

                // Enforce time restrictions on the inputs based on itinerary dates
                if (itinerary) {
                    DateTimeValidation.setItineraryLimits(checkInDateInput, itinerary.startDate, itinerary.endDate);
                    DateTimeValidation.setItineraryLimits(checkOutDateInput, itinerary.startDate, itinerary.endDate);
                }

                // Handle disaster warnings
                if (hotel.disasterWarning && hotel.disasterWarning.trim() !== "") {
                    disasterAlert.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> ${hotel.disasterWarning}`;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
                    disasterAlert.innerHTML = `<i class="fas fa-check-circle mr-1"></i> Safe area (No active alerts)`;
                    disasterAlert.classList.add("alert-success");
                    disasterAlert.classList.remove("alert-danger");
                }

                // Store warning text on the element so applyToCard can read it
                cardElement.dataset.disasterWarning = hotel.disasterWarning || "";

                hotelsList.appendChild(card);

                // Reveal "Get Alternatives" button if this hotel has an active alert
                if (window.AlternativesModal) {
                    window.AlternativesModal.applyToCard(cardElement, hotel.disasterWarning || "");
                }
            }
        } catch (error) {
            console.error("Unable to load itinerary hotels:", error);
            loadingMessage.classList.add("d-none");
            errorMessage.textContent = error.message || "Unable to load hotels for this itinerary.";
            errorMessage.classList.remove("d-none");
        }
    }

    // Resets the edit form back to the original database values
    function resetHotelEditForm(card) {
        const form = card.querySelector(".hotel-edit-form");
        form.querySelector(".edit-hotel-name").value = card.dataset.originalHotelName;
        
        form.querySelector(".edit-hotel-lat").value = card.dataset.latitude;
        form.querySelector(".edit-hotel-lon").value = card.dataset.longitude;

        const dropdown = form.querySelector(".location-autocomplete-dropdown");
        if (dropdown) {
            dropdown.classList.add("d-none");
        }

        const checkIn = form.querySelector(".edit-hotel-checkin");
        const checkOut = form.querySelector(".edit-hotel-checkout");
        checkIn.value = card.dataset.originalCheckIn;
        checkOut.value = card.dataset.originalCheckOut;
        
        if (itinerary) {
            DateTimeValidation.setItineraryLimits(checkIn, itinerary.startDate, itinerary.endDate);
            DateTimeValidation.setItineraryLimits(checkOut, itinerary.startDate, itinerary.endDate);
        }

        card.querySelector(".hotel-edit-error").classList.add("d-none");
        clearFormValidation(form);
    }

    // Handle Edit and Cancel buttons for hotel cards
    hotelsList.addEventListener("click", function (event) {
        const button = event.target.closest("button");
        if (!button) {
            return;
        }

        const card = button.closest(".hotel-card");
        if (!card) {
            return;
        }

        if (button.classList.contains("edit-hotel")) {
            resetHotelEditForm(card);
            card.querySelector(".hotel-details").classList.add("d-none");
            card.querySelector(".hotel-edit-error").classList.add("d-none");
            card.querySelector(".hotel-edit-form").classList.remove("d-none");
        } else if (button.classList.contains("cancel-hotel-edit")) {
            resetHotelEditForm(card);
            card.querySelector(".hotel-edit-form").classList.add("d-none");
            card.querySelector(".hotel-details").classList.remove("d-none");
        } else if (button.classList.contains("delete-hotel")) {
            // Remember which hotel the user wants to delete.
            hotelPendingDelete = card;

            // Identify the item type for the shared modal.
            deleteModal.dataset.deleteType = "hotel";
            document.getElementById("confirmDeleteModalTitle").textContent = "Delete Hotel";
            document.getElementById("confirmDeleteModalBody").textContent = `Delete "${card.querySelector(".hotel-name").textContent}"? This action cannot be undone.`;
            $(deleteModal).modal("show");
        }
    });

    // Clear specific field errors when the user begins typing inside the inline edit form
    hotelsList.addEventListener("input", function (event) {
        const input = event.target;
        if (input.classList.contains("form-control")) {
            input.classList.remove("is-invalid");

            const identifierClass = Array.from(input.classList).find(cls => cls.startsWith("edit-hotel-") && !cls.endsWith("-lat") && !cls.endsWith("-lon"));
            if (identifierClass) {
                const errorElement = input.closest("form").querySelector(`.${identifierClass}-error`);
                if (errorElement) {
                    errorElement.textContent = "";
                }
            }
        }
    });

    // Validates and submits the edit form for a hotel
    hotelsList.addEventListener("submit", async function (event) {
        if (!event.target.classList.contains("hotel-edit-form")) {
            return;
        }
        event.preventDefault();

        const form = event.target;
        const card = form.closest(".hotel-card");
        const hotelId = card.dataset.hotelId;
        const errorBox = card.querySelector(".hotel-edit-error");
        const saveButton = form.querySelector(".save-hotel");

        const hotelName = form.querySelector(".edit-hotel-name").value.trim();
        const checkIn = form.querySelector(".edit-hotel-checkin").value;
        const checkOut = form.querySelector(".edit-hotel-checkout").value;
        
        const latitude = form.querySelector(".edit-hotel-lat").value;
        const longitude = form.querySelector(".edit-hotel-lon").value;

        errorBox.classList.add("d-none");
        clearFormValidation(form);

        let isValid = true;

        // Validate Name and Coordinates
        if (!hotelName) {
            showFormFieldError(form, "edit-hotel-name", "Hotel name is required.");
            isValid = false;
        } else if (!latitude || !longitude) {
            showFormFieldError(form, "edit-hotel-name", "Please select a valid hotel from the search results.");
            isValid = false;
        }

        // Validate Check-in
        if (!checkIn) {
            showFormFieldError(form, "edit-hotel-checkin", "Check-in date and time is required.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(checkIn, itinerary.startDate, itinerary.endDate)) {
            showFormFieldError(form, "edit-hotel-checkin", "Check-in must be within the itinerary dates.");
            isValid = false;
        }

        // Validate Check-out
        if (!checkOut) {
            showFormFieldError(form, "edit-hotel-checkout", "Check-out date and time is required.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(checkOut, itinerary.startDate, itinerary.endDate)) {
            showFormFieldError(form, "edit-hotel-checkout", "Check-out must be within the itinerary dates.");
            isValid = false;
        } else if (checkIn && new Date(checkOut) <= new Date(checkIn)) {
            showFormFieldError(form, "edit-hotel-checkout", "Check-out must be later than check-in.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        saveButton.disabled = true;
        saveButton.textContent = "Saving...";

        const hotelData = { 
            hotelName, 
            checkIn, 
            checkOut, 
            latitude: Number(latitude), 
            longitude: Number(longitude) 
        };

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/${encodeURIComponent(hotelId)}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify(hotelData)
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            // Reload hotel cards to display the saved changes.
            await loadHotels();
        } catch (error) {
            console.error("Unable to update hotel:", error);
            errorBox.textContent = error.message || "Unable to update hotel.";
            errorBox.classList.remove("d-none");
            saveButton.disabled = false;
            saveButton.textContent = "Save";
        }
    });

    // Cleanup when the delete modal is dismissed
    $(deleteModal).on("hidden.bs.modal", function () {
        if (deleteModal.dataset.deleteType === "hotel") {
            hotelPendingDelete = null;
            deleteModal.dataset.deleteType = "";
        }
    });

    // Delete hotel after the user confirms in the shared modal
    deleteConfirmButton.addEventListener("click", async function () {
        if (deleteModal.dataset.deleteType !== "hotel" || !hotelPendingDelete) {
            return;
        }

        const card = hotelPendingDelete;
        const hotelId = card.dataset.hotelId;

        // Prevent duplicate requests.
        deleteConfirmButton.disabled = true;

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/${encodeURIComponent(hotelId)}`, {
                method: "DELETE",
                credentials: "same-origin"
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
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
                errorBox.textContent = error.message || "Unable to delete this hotel.";
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