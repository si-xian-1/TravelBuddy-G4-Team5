/**
 * Manage edit, delete, and display of attractions.
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
    const attractionsList = document.getElementById("attractionsList");
    const attractionTemplate = document.getElementById("attractionCard");
    const deleteConfirmButton = document.getElementById("confirmDeleteModalBtn");
    let attractionPendingDelete = null;

    let itinerary = null;
    try {
        itinerary = await getItinerary(itineraryId);
    } catch (e) {
        console.error("Unable to load itinerary for attraction date validation:", e);
    }

    // Fetches all attractions from the API and populates the UI cards
    async function loadAttractions() {
        const loadingMessage = document.getElementById("attractionsLoading");
        const emptyMessage = document.getElementById("noAttractions");
        const errorMessage = document.getElementById("attractionsError");

        loadingMessage.classList.remove("d-none");
        emptyMessage.classList.add("d-none");
        errorMessage.classList.add("d-none");
        attractionsList.replaceChildren(); 

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/all`, {
                credentials: "same-origin"
            });

            if (!response.ok) {
                throw new Error(`Failed to load attractions (${response.status}).`);
            }

            const attractions = await response.json();
            loadingMessage.classList.add("d-none");

            setItinerarySectionExpanded("collapseAttractions", attractions.length > 0);

            if (attractions.length === 0) {
                emptyMessage.classList.remove("d-none");
                return;
            }

            for (const attraction of attractions) {
                const card = attractionTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".attraction-card");
                const disasterAlert = card.querySelector(".disaster-alert");

                cardElement.dataset.attractionId = attraction.attractionId;
                cardElement.dataset.attractionName = attraction.attractionName || "";
                cardElement.dataset.visitDate = toDateTimeInputValue(attraction.visitDate);
                cardElement.dataset.latitude = attraction.latitude || "";
                cardElement.dataset.longitude = attraction.longitude || "";

                card.querySelector(".attraction-name").textContent = attraction.attractionName;
                card.querySelector(".attraction-visit-date").textContent = formatDateTime(attraction.visitDate);
                card.querySelector(".edit-attraction-name").value = attraction.attractionName || "";

                card.querySelector(".edit-attraction-lat").value = attraction.latitude || "";
                card.querySelector(".edit-attraction-lon").value = attraction.longitude || "";

                const dateInput = card.querySelector(".edit-attraction-date");
                dateInput.value = toDateTimeInputValue(attraction.visitDate);
                
                // Limit user to only select within the itinerary date period
                if (itinerary) {
                    DateTimeValidation.setItineraryLimits(dateInput, itinerary.startDate, itinerary.endDate);
                }

                if (attraction.disasterWarning && attraction.disasterWarning.trim() !== "") {
                    disasterAlert.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> ${attraction.disasterWarning}`;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
                    disasterAlert.innerHTML = `<i class="fas fa-check-circle mr-1"></i> Safe area (No active alerts)`;
                    disasterAlert.classList.add("alert-success");
                    disasterAlert.classList.remove("alert-danger");
                }

                attractionsList.appendChild(card);
            }
        } catch (error) {
            console.error("Unable to load itinerary attractions:", error);
            loadingMessage.classList.add("d-none");
            errorMessage.textContent = error.message || "Unable to load attractions for this itinerary.";
            errorMessage.classList.remove("d-none");
        }
    }

    // Resets the edit form back to the original database values
    function resetAttractionEditForm(card) {
        const form = card.querySelector(".attraction-edit-form");
        form.querySelector(".edit-attraction-name").value = card.dataset.attractionName;

        form.querySelector(".edit-attraction-lat").value = card.dataset.latitude;
        form.querySelector(".edit-attraction-lon").value = card.dataset.longitude;

        const dropdown = form.querySelector(".location-autocomplete-dropdown");
        if (dropdown) {
            dropdown.classList.add("d-none");
        }

        const dateInput = form.querySelector(".edit-attraction-date");
        dateInput.value = card.dataset.visitDate;
        
        // Limit user to only select within the itinerary date period
        if (itinerary) {
            DateTimeValidation.setItineraryLimits(dateInput, itinerary.startDate, itinerary.endDate);
        }

        card.querySelector(".attraction-edit-error").classList.add("d-none");
        clearFormValidation(form);
    }

    // Checks for any edit, cancel, or delete action
    attractionsList.addEventListener("click", function (event) {
        const button = event.target.closest("button");
        
        if (!button) {
            return;
        }

        const card = button.closest(".attraction-card");
        
        if (button.classList.contains("edit-attraction")) {
            resetAttractionEditForm(card);
            card.querySelector(".attraction-details").classList.add("d-none");
            card.querySelector(".attraction-edit-error").classList.add("d-none");
            card.querySelector(".attraction-edit-form").classList.remove("d-none");
        } else if (button.classList.contains("cancel-attraction-edit")) {
            resetAttractionEditForm(card);
            card.querySelector(".attraction-edit-form").classList.add("d-none");
            card.querySelector(".attraction-details").classList.remove("d-none");
        } else if (button.classList.contains("delete-attraction")) {
            attractionPendingDelete = card;
            deleteModal.dataset.deleteType = "attraction";
            document.getElementById("confirmDeleteModalTitle").textContent = "Delete Attraction";
            document.getElementById("confirmDeleteModalBody").textContent = `Delete "${card.dataset.attractionName}"? This action cannot be undone.`;
            $(deleteModal).modal("show");
        }
    });

    // Clear specific field errors when the user begins typing inside the inline edit form
    attractionsList.addEventListener("input", function (event) {
        const input = event.target;
        if (input.classList.contains("form-control")) {
            input.classList.remove("is-invalid");
            
            const identifierClass = Array.from(input.classList).find(cls => cls.startsWith("edit-attraction-") && !cls.endsWith("-lat") && !cls.endsWith("-lon"));
            if (identifierClass) {
                const errorElement = input.closest("form").querySelector(`.${identifierClass}-error`);
                if (errorElement) {
                    errorElement.textContent = "";
                }
            }
        }
    });

    // Cleanup when the delete modal is dismissed
    $(deleteModal).on("hidden.bs.modal", function () {
        if (deleteModal.dataset.deleteType === "attraction") {
            attractionPendingDelete = null;
            deleteModal.dataset.deleteType = "";
        }
    });

    // Executes the actual deletion request to the API
    deleteConfirmButton.addEventListener("click", async function () {
        if (deleteModal.dataset.deleteType !== "attraction" || !attractionPendingDelete) {
            return;
        }

        const attractionId = attractionPendingDelete.dataset.attractionId;
        deleteConfirmButton.disabled = true;

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/${encodeURIComponent(attractionId)}`, {
                method: "DELETE",
                credentials: "same-origin"
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            $(deleteModal).modal("hide");
            attractionPendingDelete = null;
            await loadAttractions();
        } catch (deleteError) {
            console.error("Unable to delete attraction:", deleteError);
            const error = attractionPendingDelete.querySelector(".attraction-edit-error");
            if (error) {
                error.textContent = deleteError.message || "Unable to delete this attraction.";
                error.classList.remove("d-none");
            }
            $(deleteModal).modal("hide");
            attractionPendingDelete = null;
        } finally {
            deleteConfirmButton.disabled = false;
        }
    });

    // Validates and submits the edit form for an attraction
    attractionsList.addEventListener("submit", async function (event) {
        if (!event.target.matches(".attraction-edit-form")) {
            return;
        }
        
        event.preventDefault(); 

        const form = event.target;
        const card = form.closest(".attraction-card");
        const attractionId = card.dataset.attractionId;
        const error = card.querySelector(".attraction-edit-error");
        const saveButton = form.querySelector(".save-attraction");

        const attractionName = form.querySelector(".edit-attraction-name").value.trim();
        const visitDate = form.querySelector(".edit-attraction-date").value;
        const latitude = form.querySelector(".edit-attraction-lat").value;
        const longitude = form.querySelector(".edit-attraction-lon").value;

        error.classList.add("d-none");
        clearFormValidation(form);

        let isValid = true;

        if (!attractionName) {
            showFormFieldError(form, "edit-attraction-name", "Attraction name cannot be empty.");
            isValid = false;
        } else if (!latitude || !longitude) {
            showFormFieldError(form, "edit-attraction-name", "Please select a valid location from the search results.");
            isValid = false;
        }

        if (!visitDate) {
            showFormFieldError(form, "edit-attraction-date", "Visit date and time is required.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(visitDate, itinerary.startDate, itinerary.endDate)) {
            showFormFieldError(form, "edit-attraction-date", "Visit date and time must be within the itinerary dates.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        saveButton.disabled = true;
        
        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/${encodeURIComponent(attractionId)}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify({ 
                    attractionName, 
                    visitDate, 
                    latitude: Number(latitude), 
                    longitude: Number(longitude) 
                })
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            await loadAttractions(); 
        } catch (updateError) {
            console.error("Unable to update attraction:", updateError);
            error.textContent = updateError.message || "Unable to update this attraction.";
            error.classList.remove("d-none");
            saveButton.disabled = false;
        }
    });

    await loadAttractions();
});