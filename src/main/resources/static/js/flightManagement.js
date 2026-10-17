/* To be updated */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    const itineraryId = page.dataset.itineraryId;
    const flightsList = document.getElementById("flightsList");
    const flightTemplate = document.getElementById("flightCard");

    async function loadFlights() {
        const loadingMessage = document.getElementById("flightsLoading");
        const emptyMessage = document.getElementById("noFlights");
        const errorMessage = document.getElementById("flightsError");

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

            if (flights.length === 0) {
                emptyMessage.classList.remove("d-none");
                return;
            }

            for (const flight of flights) {
                const card = flightTemplate.content.cloneNode(true);
                const cardElement = card.querySelector(".flight-card");
                const nameInput = card.querySelector(".edit-flight-number");
                const dateInput = card.querySelector(".edit-flight-date");
                const disasterAlert = card.querySelector(".disaster-alert");

                cardElement.dataset.attractionId = flight.attractionId;
                card.querySelector(".attraction-name").textContent = flight.attractionName;
                card.querySelector(".attraction-visit-date").textContent =
                    formatVisitDate(flight.visitDate);
                card.querySelector(".attraction-coordinates").textContent =
                    formatCoordinates(flight.latitude, flight.longitude);
                nameInput.value = flight.attractionName || "";
                dateInput.value = toDateTimeInputValue(flight.visitDate);

                // Display disaster warning if any
                if (flight.disasterWarning && flight.disasterWarning.trim() !== "") {
                    // If there is a warning, make it Red and show the message
                    disasterAlert.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> ${flight.disasterWarning}`;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
                    // If it is null or empty, it will show green status
                    disasterAlert.innerHTML = `<i class="fas fa-check-circle mr-1"></i> Safe area (No active alerts)`;
                    disasterAlert.classList.add("alert-success");
                    disasterAlert.classList.remove("alert-danger");
                }

                flightsList.appendChild(card);
            }
        } catch (error) {
            console.error("Unable to load itinerary attractions:", error);
            loadingMessage.classList.add("d-none");
            errorMessage.textContent = error.message || "Unable to load attractions for this itinerary.";
            errorMessage.classList.remove("d-none");
        }
    }

    await loadFlights();
});