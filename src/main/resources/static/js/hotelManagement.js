/**
 * Manage edit, delete and display of hotel
 * Display disaster warning
 */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    const itineraryId = page.dataset.itineraryId;
    const hotelsList = document.getElementById("hotelsList");
    const hotelTemplate = document.getElementById("hotelCard");

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

    await loadHotels();
});