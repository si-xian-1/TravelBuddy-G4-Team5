/**
 * Manage edit, delete and display of hotel
 * Display disaster warning
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
            
            // Expand the accordion if hotels exist
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

                // Store data securely on the element for editing later
                cardElement.dataset.hotelId = hotel.hotelId;
                cardElement.dataset.hotelName = hotel.hotelName || "";
                cardElement.dataset.checkIn = toDateTimeInputValue(hotel.checkIn);
                cardElement.dataset.checkOut = toDateTimeInputValue(hotel.checkOut);

                // Populate display text
                card.querySelector(".hotel-name").textContent = hotel.hotelName;
                card.querySelector(".hotel-checkin").textContent = formatDateTime(hotel.checkIn);
                card.querySelector(".hotel-checkout").textContent = formatDateTime(hotel.checkOut);
                
                // Pre-fill the hidden edit form with current data
                nameInput.value = hotel.hotelName || "";
                checkInDateInput.value = toDateTimeInputValue(hotel.checkIn);
                checkOutDateInput.value = toDateTimeInputValue(hotel.checkOut);

                // Enforce time restrictions on the inputs
                DateTimeValidation.setMinimumNow(checkInDateInput);
                DateTimeValidation.setMinimumNow(checkOutDateInput);

                // Display disaster warning if any (Red if warning exists, Green if safe)
                if (hotel.disasterWarning && hotel.disasterWarning.trim() !== "") {
                    disasterAlert.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> ${hotel.disasterWarning}`;
                    disasterAlert.classList.add("alert-danger");
                    disasterAlert.classList.remove("alert-success");
                } else {
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

    // Boot up the page by triggering the initial load
    await loadHotels();
});