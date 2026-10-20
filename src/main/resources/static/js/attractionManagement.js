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

    // Set timeout to prevent mutiple calls to api for searching attraction location
    function setCallFrequency(func, delay) {
        let timeout = null;

        return function (event) {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                func(event);
            }, delay);
        };
    }

    // Handle typing in the attraction name input to search locations
    attractionsList.addEventListener("input", setCallFrequency(async function (event) {
        if (!event.target.matches(".edit-attraction-name")) {
            return;
        }

        const input = event.target;
        const card = input.closest(".attraction-card");
        
        // Create the attraction name search result
        const dropdown = card.querySelector(".location-autocomplete-dropdown");
        const latInput = card.querySelector(".edit-attraction-lat");
        const lonInput = card.querySelector(".edit-attraction-lon");
        const query = input.value.trim();

        // Clear hidden coordinates when user typed a new location
        if (latInput && lonInput) {
            latInput.value = "";
            lonInput.value = "";
        }

        if (query.length < 3) {
            dropdown.classList.add("d-none");
            return;
        }

        try {
            // Call the location api to retrieve a list of location to return to user
            const response = await fetch(`/api/location/search?query=${encodeURIComponent(query)}`);

            if (!response.ok) {
                throw new Error("Location search failed");
            }

            const locations = await response.json();
            dropdown.replaceChildren();

            if (locations.length === 0) {
                // When no locations found
                dropdown.innerHTML = '<li class="list-group-item text-muted text-xs">No locations found</li>';
            } else {
                // When there are some location found
                for (const loc of locations) {
                    const li = document.createElement("li");
                    li.className = "list-group-item list-group-item-action text-sm autocomplete-item cursor-pointer";

                    // Retrieve the response
                    const fullAddress = loc.display_name || "Unknown Location";
                    
                    // Shorten location name display for readability
                    const shortName = fullAddress.split(',')[0].trim();

                    li.dataset.name = shortName;
                    li.dataset.lat = loc.lat;
                    li.dataset.lon = loc.lon;

                    // Show the attraction name in bold, and the full address in small text below it
                    li.innerHTML = `<strong>${shortName}</strong><br><small class="text-muted">${fullAddress}</small>`;

                    dropdown.appendChild(li);
                }
            }
            dropdown.classList.remove("d-none");
        } catch (error) {
            console.error("Autocomplete fetch error:", error);
        }
    }, 400));

    // Handle selecting an item from the dropdown or closing it by clicking elsewhere
    document.addEventListener("click", function (event) {
        const item = event.target.closest(".autocomplete-item");

        if (item) {
            const card = item.closest(".attraction-card");
            const nameInput = card.querySelector(".edit-attraction-name");

            nameInput.value = item.dataset.name;
            nameInput.classList.remove("is-invalid");

            card.querySelector(".edit-attraction-lat").value = item.dataset.lat;
            card.querySelector(".edit-attraction-lon").value = item.dataset.lon;
            card.querySelector(".location-autocomplete-dropdown").classList.add("d-none");
        } else {
            // Close all search result
            document.querySelectorAll(".location-autocomplete-dropdown:not(.d-none)").forEach(dropdown => {
                dropdown.classList.add("d-none");
            });
        }
    });

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

                const latInput = card.querySelector(".edit-attraction-lat");
                const lonInput = card.querySelector(".edit-attraction-lon");
                
                if (latInput) {
                    latInput.value = attraction.latitude || "";
                }
                
                if (lonInput) {
                    lonInput.value = attraction.longitude || "";
                }

                card.querySelector(".edit-attraction-date").value = toDateTimeInputValue(attraction.visitDate);
                DateTimeValidation.setMinimumNow(card.querySelector(".edit-attraction-date"));

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

        const latInput = form.querySelector(".edit-attraction-lat");
        const lonInput = form.querySelector(".edit-attraction-lon");
        
        if (latInput) {
            latInput.value = card.dataset.latitude;
        }
        
        if (lonInput) {
            lonInput.value = card.dataset.longitude;
        }

        const dropdown = form.querySelector(".location-autocomplete-dropdown");
        if (dropdown) {
            dropdown.classList.add("d-none");
        }

        const dateInput = form.querySelector(".edit-attraction-date");
        dateInput.value = card.dataset.visitDate;
        DateTimeValidation.setMinimumNow(dateInput);

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
        } else if (!DateTimeValidation.isNowOrLater(visitDate)) {
            showFormFieldError(form, "edit-attraction-date", "Visit date and time must be now or later.");
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
                body: JSON.stringify({ attractionName, visitDate, latitude, longitude })
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