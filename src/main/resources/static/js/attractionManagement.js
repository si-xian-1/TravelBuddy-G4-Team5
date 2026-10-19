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

    // Fetches all attractions from the API and populates the UI cards
    async function loadAttractions() {
        const loadingMessage = document.getElementById("attractionsLoading");
        const emptyMessage = document.getElementById("noAttractions");
        const errorMessage = document.getElementById("attractionsError");

        // Reset UI states
        loadingMessage.classList.remove("d-none");
        emptyMessage.classList.add("d-none");
        errorMessage.classList.add("d-none");
        attractionsList.replaceChildren(); // Clears out existing cards

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/all`, {
                                    credentials: "same-origin" });

            if (!response.ok) {
                throw new Error(`Failed to load attractions (${response.status}).`);
            }
            
            const attractions = await response.json();
            loadingMessage.classList.add("d-none");
            
            // Expand the accordion if attractions exist
            setItinerarySectionExpanded("collapseAttractions", attractions.length > 0);

            if (attractions.length === 0) {
                emptyMessage.classList.remove("d-none");
                return;
            }

            // Generate HTML cards for each attraction using the template
            for (const attraction of attractions) {
                const card = attractionTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".attraction-card");
                const disasterAlert = card.querySelector(".disaster-alert");
                
                // Store data securely on the element for editing later
                cardElement.dataset.attractionId = attraction.attractionId;
                cardElement.dataset.attractionName = attraction.attractionName || "";
                cardElement.dataset.visitDate = toDateTimeInputValue(attraction.visitDate);
                
                // Populate display text
                card.querySelector(".attraction-name").textContent = attraction.attractionName;
                card.querySelector(".attraction-visit-date").textContent = formatDateTime(attraction.visitDate);
                
                // Pre-fill the hidden edit form with current data
                card.querySelector(".edit-attraction-name").value = attraction.attractionName || "";
                card.querySelector(".edit-attraction-date").value = toDateTimeInputValue(attraction.visitDate);
                DateTimeValidation.setMinimumNow(card.querySelector(".edit-attraction-date"));

                // Handle disaster warnings (Red if warning exists, Green if safe)
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
        
        const dateInput = form.querySelector(".edit-attraction-date");
        dateInput.value = card.dataset.visitDate;
        DateTimeValidation.setMinimumNow(dateInput);
        
        card.querySelector(".attraction-edit-error").classList.add("d-none");
        clearFormValidation(form);
    }

    // Event delegation: Listens for Edit, Cancel, and Delete clicks
    attractionsList.addEventListener("click", function (event) {
        const button = event.target.closest("button");
        if (!button) return;

        const card = button.closest(".attraction-card");
        if (button.classList.contains("edit-attraction")) {
            // Show edit form, hide details
            resetAttractionEditForm(card);
            card.querySelector(".attraction-details").classList.add("d-none");
            card.querySelector(".attraction-edit-error").classList.add("d-none");
            card.querySelector(".attraction-edit-form").classList.remove("d-none");
        } else if (button.classList.contains("cancel-attraction-edit")) {
            // Revert changes, hide form, show details
            resetAttractionEditForm(card);
            card.querySelector(".attraction-edit-form").classList.add("d-none");
            card.querySelector(".attraction-details").classList.remove("d-none");
        } else if (button.classList.contains("delete-attraction")) {
            // Store context and trigger Bootstrap confirmation modal
            attractionPendingDelete = card;
            deleteModal.dataset.deleteType = "attraction";
            document.getElementById("confirmDeleteModalTitle").textContent = "Delete Attraction";
            document.getElementById("confirmDeleteModalBody").textContent = `Delete "${card.dataset.attractionName}"? This action cannot be undone.`;
            $(deleteModal).modal("show");
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
        if (deleteModal.dataset.deleteType !== "attraction" || !attractionPendingDelete) return;

        const attractionId = attractionPendingDelete.dataset.attractionId;
        deleteConfirmButton.disabled = true; // Prevent double-clicks

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
            await loadAttractions(); // Refresh the list
        } catch (deleteError) {
            console.error("Unable to delete attraction:", deleteError);
            const error = attractionPendingDelete.querySelector(".attraction-edit-error");
            error.textContent = deleteError.message || "Unable to delete this attraction.";
            error.classList.remove("d-none");
            $(deleteModal).modal("hide");
            attractionPendingDelete = null;
        } finally {
            deleteConfirmButton.disabled = false;
        }
    });

    // Validates and submits the edit form for an attraction
    attractionsList.addEventListener("submit", async function (event) {
        if (!event.target.matches(".attraction-edit-form")) return;
        event.preventDefault(); // Stop standard HTML form submission

        const form = event.target;
        const card = form.closest(".attraction-card");
        const attractionId = card.dataset.attractionId;
        const error = card.querySelector(".attraction-edit-error");
        const saveButton = form.querySelector(".save-attraction");
        
        // Gather input values
        const attractionName = form.querySelector(".edit-attraction-name").value.trim();
        const visitDate = form.querySelector(".edit-attraction-date").value;

        error.classList.add("d-none");
        clearFormValidation(form);

        let isValid = true;
        
        // Validation 1: Required Name
        if (!attractionName) {
            showFormFieldError(form, "edit-attraction-name", "Attraction name cannot be empty.");
            isValid = false;
        }

        // Validation 2: Required Date and Time cannot be in the past
        if (!visitDate) {
            showFormFieldError(form, "edit-attraction-date", "Visit date and time is required.");
            isValid = false;
        } else if (!DateTimeValidation.isNowOrLater(visitDate)) {
            showFormFieldError(form, "edit-attraction-date", "Visit date and time must be now or later.");
            isValid = false;
        }

        if (!isValid) return; // Halt submission if validation failed

        saveButton.disabled = true; // Prevent multiple submissions
        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/${encodeURIComponent(attractionId)}`, {
                method: "PUT",
                headers: { "Content-Type": "application/json" },
                credentials: "same-origin",
                body: JSON.stringify({ attractionName, visitDate })
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            await loadAttractions(); // Reload everything on success
        } catch (updateError) {
            console.error("Unable to update attraction:", updateError);
            error.textContent = updateError.message || "Unable to update this attraction.";
            error.classList.remove("d-none");
            saveButton.disabled = false;
        }
    });

    // Boot up the page by triggering the initial load
    await loadAttractions();
});