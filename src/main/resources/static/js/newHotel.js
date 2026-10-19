
document.addEventListener("DOMContentLoaded", () => {
    const hotelInput = document.getElementById("hotelName");
    const suggestions = document.getElementById("hotelSuggestions");

    let selectedHotel = null;
    let debounceTimer;
    let searchController = null;

    function hideSuggestions() {
        suggestions.classList.add("d-none");
        suggestions.replaceChildren();
    }

    function clearSelection() {
        selectedHotel = null;
        hotelInput.dataset.selected = "false";
    }

    // Search locations when the user types a hotel name.
    hotelInput.addEventListener("input", () => {
        clearTimeout(debounceTimer);

        if (searchController) {
            searchController.abort();
        }

        clearSelection();
        hideSuggestions();

        const query = hotelInput.value.trim();

        // Avoid searching for very short inputs.
        if (query.length < 3) return;

        debounceTimer = setTimeout(async () => {
            const controller = new AbortController();
            searchController = controller;

            try {
                const response = await fetch(
                    `/api/location/hotels/search?query=${encodeURIComponent(query)}`,
                    { signal: controller.signal }
                );

                if (!response.ok) {
                    throw new Error("Location search failed");
                }

                const locations = await response.json();

                if (controller.signal.aborted ||
                    hotelInput.value.trim() !== query) return;

                hideSuggestions();

                if (!Array.isArray(locations) ||
                    locations.length === 0) {
                    const message = document.createElement("div");
                    message.className = "list-group-item text-muted";
                    message.textContent = "No matching locations found";
                    suggestions.appendChild(message);
                } else {
                    locations.forEach(location => {
                        const displayName =
                            location.display_name ?? location.displayName;
                        const item = document.createElement("button");
                        item.type = "button";
                        item.className =
                            "list-group-item list-group-item-action";

                        item.textContent = displayName;

                        item.addEventListener("click", () => {
                            selectedHotel = {
                                hotelName: displayName,
                                latitude: Number(location.lat),
                                longitude: Number(location.lon)
                            };

                            hotelInput.value = selectedHotel.hotelName;
                            hotelInput.dataset.selected = "true";
                            hotelInput.classList.remove("is-invalid");

                            document.getElementById(
                                "hotelNameError"
                            ).textContent = "";

                            hideSuggestions();
                        });

                        suggestions.appendChild(item);
                    });
                }

                suggestions.classList.remove("d-none");
            } catch (error) {
                if (error.name !== "AbortError") {
                    console.error("Hotel search error:", error);
                    suggestions.replaceChildren();
                    const message = document.createElement("div");
                    message.className = "list-group-item text-danger";
                    message.textContent =
                        "Hotel search is unavailable. Please try again.";
                    suggestions.appendChild(message);
                    suggestions.classList.remove("d-none");
                }
            }
        }, 400);
    });

    // Hide suggestions when clicking outside.
    document.addEventListener("click", event => {
        if (!hotelInput.contains(event.target) &&
            !suggestions.contains(event.target)) {
            hideSuggestions();
        }
    });

    // Expose the selected location for form submission later.
    window.getSelectedHotel = () => selectedHotel;

    
    // Hotel form frontend validation
    const hotelForm = document.getElementById("hotelForm");
    const checkInInput = document.getElementById("checkIn");
    const checkOutInput = document.getElementById("checkOut");
    const hotelError = document.getElementById("hotelError");

    // Display an error under a specific field.
    function showFieldError(input, errorId, message) {
        input.classList.add("is-invalid");
        document.getElementById(errorId).textContent = message;
    }

    // Clear previous validation errors.
    function clearValidationErrors() {
        hotelError.classList.add("d-none");
        hotelError.textContent = "";

        [hotelInput, checkInInput, checkOutInput].forEach(input => {
            input.classList.remove("is-invalid");
        });

        [
            "hotelNameError",
            "checkInError",
            "checkOutError"
        ].forEach(id => {
            document.getElementById(id).textContent = "";
        });
    }

    function validateHotelForm() {
        clearValidationErrors();

        let isValid = true;
        const hotelName = hotelInput.value.trim();
        const checkIn = checkInInput.value;
        const checkOut = checkOutInput.value;

        // 1. Validate hotel selection.
        if (!hotelName) {
            showFieldError(
                hotelInput,
                "hotelNameError",
                "Hotel name is required."
            );
            isValid = false;
        } else if (!selectedHotel ||
                   hotelName !== selectedHotel.hotelName) {
            showFieldError(
                hotelInput,
                "hotelNameError",
                "Please select a hotel from the search results."
            );
            isValid = false;
        }

        // 2. Validate check-in.
        if (!checkIn) {
            showFieldError(
                checkInInput,
                "checkInError",
                "Check-in date and time is required."
            );
            isValid = false;
        } else if (Number.isNaN(new Date(checkIn).getTime())) {
            showFieldError(
                checkInInput,
                "checkInError",
                "Please enter a valid check-in date and time."
            );
            isValid = false;
        }

        // 3. Validate check-out.
        if (!checkOut) {
            showFieldError(
                checkOutInput,
                "checkOutError",
                "Check-out date and time is required."
            );
            isValid = false;
        } else if (Number.isNaN(new Date(checkOut).getTime())) {
            showFieldError(
                checkOutInput,
                "checkOutError",
                "Please enter a valid check-out date and time."
            );
            isValid = false;
        } else if (checkIn &&
                   !Number.isNaN(new Date(checkIn).getTime()) &&
                   new Date(checkOut) <= new Date(checkIn)) {
            showFieldError(
                checkOutInput,
                "checkOutError",
                "Check-out must be later than check-in."
            );
            isValid = false;
        }

        return isValid;
    }

    
    // Validate the form and submit the hotel to the backend.
    hotelForm.addEventListener("submit", async event => {
        event.preventDefault();

        if (!validateHotelForm()) {
            return;
        }

        const itineraryId = hotelForm.dataset.itineraryId;
        const saveButton = document.getElementById("saveHotelBtn");

        // Prepare hotel data for the backend.
       
const hotelData = {
    hotelName: selectedHotel.hotelName,
    latitude: selectedHotel.latitude,
    longitude: selectedHotel.longitude,
    checkIn: checkInInput.value,
    checkOut: checkOutInput.value
};


        // Prevent duplicate submissions.
        saveButton.disabled = true;
        saveButton.textContent = "Saving...";

        hotelError.classList.add("d-none");
        hotelError.textContent = "";

        try {
            const response = await fetch(
                `/api/itinerary/${encodeURIComponent(itineraryId)}/hotels/`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    credentials: "same-origin",
                    body: JSON.stringify(hotelData)
                }
            );

            if (!response.ok) {
                const responseBody = await response.text();
                let errorMessage = `Failed to add hotel (${response.status}).`;

                if (responseBody) {
                    try {
                        const errorData = JSON.parse(responseBody);
                        errorMessage =
                            errorData.message ||
                            errorData.detail ||
                            errorMessage;
                    } catch {
                        errorMessage = responseBody;
                    }
                }

                throw new Error(errorMessage);
            }

            // Return to the itinerary after successfully adding the hotel.
            window.location.href =
                `/itinerary/view/${encodeURIComponent(itineraryId)}`;

        } catch (error) {
            console.error("Unable to create hotel:", error);

            hotelError.textContent =
                error.message || "Unable to add hotel. Please try again.";

            hotelError.classList.remove("d-none");

        } finally {
            saveButton.disabled = false;
            saveButton.textContent = "Add Hotel";
        }
    });


    // Clear field errors when the user changes an input.
    [hotelInput, checkInInput, checkOutInput].forEach(input => {
        input.addEventListener("input", () => {
            input.classList.remove("is-invalid");

            const errorElement = document.getElementById(
                input.id + "Error"
            );

            if (errorElement) {
                errorElement.textContent = "";
            }
        });
    });

});
