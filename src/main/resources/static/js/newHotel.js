document.addEventListener("DOMContentLoaded", async () => {
    const hotelForm = document.getElementById("hotelForm");
    const nameInput = document.getElementById("hotelName");
    const checkInInput = document.getElementById("checkIn");
    const checkOutInput = document.getElementById("checkOut");
    
    // Hidden inputs populated by the shared detailedItinerary.js search script
    const latInput = document.getElementById("hotelLatitude");
    const lonInput = document.getElementById("hotelLongitude");
    
    const hotelError = document.getElementById("hotelError");
    const saveButton = document.getElementById("saveHotelBtn");

    const itineraryId = hotelForm.dataset.itineraryId;
    let itinerary = null;
    try {
        itinerary = await getItinerary(itineraryId);
        if (itinerary) {
            DateTimeValidation.setItineraryLimits(checkInInput, itinerary.startDate, itinerary.endDate);
            DateTimeValidation.setItineraryLimits(checkOutInput, itinerary.startDate, itinerary.endDate);
        }
    } catch (e) {
        console.error("Unable to load itinerary for hotel date validation:", e);
    }

    // Display an error under a specific field
    function showFieldError(input, errorId, message) {
        input.classList.add("is-invalid");
        const errorElement = document.getElementById(errorId);
        if (errorElement) {
            errorElement.textContent = message;
        }
    }

    // Clear all previous validation errors
    function clearValidationErrors() {
        hotelError.classList.add("d-none");
        hotelError.textContent = "";

        clearFormValidation(hotelForm);

        ["hotelNameError", "checkInError", "checkOutError"].forEach(id => {
            const errorElement = document.getElementById(id);
            if (errorElement) {
                errorElement.textContent = "";
            }
        });
    }

    // Validate the form and submit the hotel to the backend
    hotelForm.addEventListener("submit", async event => {
        event.preventDefault();
        
        clearValidationErrors();

        const hotelName = nameInput.value.trim();
        const checkIn = checkInInput.value;
        const checkOut = checkOutInput.value;
        const latitude = latInput ? latInput.value : "";
        const longitude = lonInput ? lonInput.value : "";

        let isValid = true;

        // Validate Hotel Selection
        if (!hotelName) {
            showFieldError(nameInput, "hotelNameError", "Hotel name is required.");
            isValid = false;
        } else if (!latitude || !longitude) {
            // Missing coordinates means the user typed a name but didn't click a search result
            showFieldError(nameInput, "hotelNameError", "Please select a hotel from the search results.");
            isValid = false;
        }

        // Validate Check-in
        if (!checkIn) {
            showFieldError(checkInInput, "checkInError", "Check-in date and time is required.");
            isValid = false;
        } else if (Number.isNaN(new Date(checkIn).getTime())) {
            showFieldError(checkInInput, "checkInError", "Please enter a valid check-in date and time.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(checkIn, itinerary.startDate, itinerary.endDate)) {
            showFieldError(checkInInput, "checkInError", "Check-in must be within the itinerary dates.");
            isValid = false;
        }

        // Validate Check-out
        if (!checkOut) {
            showFieldError(checkOutInput, "checkOutError", "Check-out date and time is required.");
            isValid = false;
        } else if (Number.isNaN(new Date(checkOut).getTime())) {
            showFieldError(checkOutInput, "checkOutError", "Please enter a valid check-out date and time.");
            isValid = false;
        } else if (itinerary && !DateTimeValidation.isWithinItinerary(checkOut, itinerary.startDate, itinerary.endDate)) {
            showFieldError(checkOutInput, "checkOutError", "Check-out must be within the itinerary dates.");
            isValid = false;
        } else if (checkIn && !Number.isNaN(new Date(checkIn).getTime()) && new Date(checkOut) <= new Date(checkIn)) {
            showFieldError(checkOutInput, "checkOutError", "Check-out must be later than check-in.");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Prevent duplicate submissions
        saveButton.disabled = true;
        saveButton.textContent = "Saving...";

        const hotelData = {
            hotelName: hotelName,
            latitude: Number(latitude),
            longitude: Number(longitude),
            checkIn: checkIn,
            checkOut: checkOut
        };

        try {
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "same-origin",
                body: JSON.stringify(hotelData)
            });

            if (!response.ok) {
                const responseBody = await response.text();
                throw new Error(getApiErrorMessage(responseBody, response.status));
            }

            // Return to the itinerary after successfully adding the hotel
            window.location.href = `/itinerary/view/${encodeURIComponent(itineraryId)}`;

        } catch (error) {
            console.error("Unable to create hotel:", error);
            hotelError.textContent = error.message || "Unable to add hotel. Please try again.";
            hotelError.classList.remove("d-none");
            saveButton.disabled = false;
            saveButton.textContent = "Add Hotel";
        }
    });

    // Clear specific field errors when the user begins typing or changing an input again
    [nameInput, checkInInput, checkOutInput].forEach(input => {
        input.addEventListener("input", () => {
            input.classList.remove("is-invalid");
            const errorElement = document.getElementById(input.id + "Error");
            if (errorElement) {
                errorElement.textContent = "";
            }
        });
    });
});