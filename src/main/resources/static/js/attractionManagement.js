/**
 * Manage edit, delete and display of attraction
 * Display disaster warning
 */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    const itineraryId = page.dataset.itineraryId;
    const attractionsList = document.getElementById("attractionsList");
    const attractionTemplate = document.getElementById("attractionCard");
    const deleteModal = document.getElementById("confirmDeleteModal");
    const deleteConfirmButton = document.getElementById("confirmDeleteModalBtn");
    let attractionPendingDelete = null;

    async function loadAttractions() {
        const loadingMessage = document.getElementById("attractionsLoading");
        const emptyMessage = document.getElementById("noAttractions");
        const errorMessage = document.getElementById("attractionsError");

        loadingMessage.classList.remove("d-none");
        emptyMessage.classList.add("d-none");
        errorMessage.classList.add("d-none");
        attractionsList.replaceChildren();

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/all`,
                { credentials: "same-origin" }
            );

            if (!response.ok) {
                throw new Error(`Failed to load attractions (${response.status}).`);
            }

            const attractions = await response.json();
            loadingMessage.classList.add("d-none");

            if (attractions.length === 0) {
                emptyMessage.classList.remove("d-none");
                return;
            }

            for (const attraction of attractions) {
                const card = attractionTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".attraction-card");
                const nameInput = card.querySelector(".edit-attraction-name");
                const dateInput = card.querySelector(".edit-attraction-date");
                const disasterAlert = card.querySelector(".disaster-alert");

                cardElement.dataset.attractionId = attraction.attractionId;
                card.querySelector(".attraction-name").textContent = attraction.attractionName;
                card.querySelector(".attraction-visit-date").textContent =
                    formatVisitDate(attraction.visitDate);
                // card.querySelector(".attraction-coordinates").textContent =
                //     formatCoordinates(attraction.latitude, attraction.longitude);
                nameInput.value = attraction.attractionName || "";
                dateInput.value = toDateTimeInputValue(attraction.visitDate);

                // Display disaster warning if any
                if (attraction.disasterWarning && attraction.disasterWarning.trim() !== "") {
                    // If there is a warning, make it Red and show the message
                    disasterAlert.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> ${attraction.disasterWarning}`;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
                    // If it is null or empty, it will show green status
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

    attractionsList.addEventListener("click", function (event) {
        const button = event.target.closest("button");
        if (!button) {
            return;
        }

        const card = button.closest(".attraction-card");
        if (button.classList.contains("edit-attraction")) {
            card.querySelector(".attraction-details").classList.add("d-none");
            card.querySelector(".attraction-edit-error").classList.add("d-none");
            card.querySelector(".attraction-edit-form").classList.remove("d-none");
        } else if (button.classList.contains("cancel-attraction-edit")) {
            card.querySelector(".attraction-edit-form").classList.add("d-none");
            card.querySelector(".attraction-details").classList.remove("d-none");
        } else if (button.classList.contains("delete-attraction")) {
            attractionPendingDelete = card;
            document.getElementById("confirmDeleteModalTitle").textContent = "Delete Attraction";
            document.getElementById("confirmDeleteModalBody").textContent =
                `Delete "${card.querySelector(".attraction-name").textContent}"? This action cannot be undone.`;
            $(deleteModal).modal("show");
        }
    });

    deleteConfirmButton.addEventListener("click", async function () {
        if (!attractionPendingDelete) {
            return;
        }

        const attractionId = attractionPendingDelete.dataset.attractionId;
        deleteConfirmButton.disabled = true;

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/${encodeURIComponent(attractionId)}`,
                {
                    method: "DELETE",
                    credentials: "same-origin"
                }
            );

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(await getErrorMessage(responseBody, response.status));
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

    attractionsList.addEventListener("submit", async function (event) {
        if (!event.target.matches(".attraction-edit-form")) {
            return;
        }
        event.preventDefault();

        const form = event.target;
        const card = form.closest(".attraction-card");
        const attractionId = card.dataset.attractionId;
        const nameInput = form.querySelector(".edit-attraction-name");
        const dateInput = form.querySelector(".edit-attraction-date");
        const error = card.querySelector(".attraction-edit-error");
        const saveButton = form.querySelector(".save-attraction");
        const attractionName = nameInput.value.trim();
        const visitDate = dateInput.value;

        nameInput.classList.toggle("is-invalid", !attractionName);
        dateInput.classList.toggle("is-invalid", !visitDate);
        error.classList.add("d-none");

        if (!attractionName || !visitDate) {
            return;
        }

        saveButton.disabled = true;
        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/${encodeURIComponent(attractionId)}`,
                {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    credentials: "same-origin",
                    body: JSON.stringify({ attractionName, visitDate })
                }
            );

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(await getErrorMessage(responseBody, response.status));
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