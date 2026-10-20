// Limits how often the location API is called while the user is typing
function setCallFrequency(func, delay) {
    let timeout = null;
    return function (event) {
        clearTimeout(timeout);
        timeout = setTimeout(() => {
            func(event);
        }, delay);
    };
}

document.addEventListener("DOMContentLoaded", function () {
    const nameInput = document.getElementById("attractionName");
    const dropdown = document.querySelector(".location-autocomplete-dropdown");
    const latInput = document.getElementById("attractionLatitude");
    const lonInput = document.getElementById("attractionLongitude");
    const form = document.getElementById("attractionForm");
    const visitDateInput = document.getElementById("visitDate");
    const errorAlert = document.getElementById("attractionError");
    const saveButton = document.getElementById("saveAttractionBtn");

    // Handle typing in the input to search for locations
    nameInput.addEventListener("input", setCallFrequency(async function (event) {
        const query = nameInput.value.trim();

        // Clear hidden coordinates since the user is typing a new location name
        if (latInput && lonInput) {
            latInput.value = "";
            lonInput.value = "";
        }

        // Only search if the user has typed at least 3 characters
        if (query.length < 3) {
            dropdown.classList.add("d-none");
            return;
        }

        try {
            // Fetch matching locations from the backend location API
            const response = await fetch(`/api/location/search?query=${encodeURIComponent(query)}`);

            if (!response.ok) {
                throw new Error("Location search failed");
            }

            const locations = await response.json();

            // Clear existing dropdown results before adding new ones
            dropdown.replaceChildren();

            if (locations.length === 0) {
                // When no match in location can be found
                dropdown.innerHTML = '<li class="list-group-item text-muted text-xs">No locations found</li>';
            } else {
                // Populate the dropdown with the search results
                for (const loc of locations) {
                    const li = document.createElement("li");
                    li.className = "list-group-item list-group-item-action text-sm autocomplete-item cursor-pointer";

                    // Shorten the location name to improve readability
                    const shortName = loc.display_name.split(',')[0].trim();

                    li.dataset.name = shortName;
                    li.dataset.lat = loc.lat;
                    li.dataset.lon = loc.lon;

                    // Show the attraction name in bold, and the full address in small text below it
                    li.innerHTML = `<strong>${shortName}</strong><br><small class="text-muted">${loc.display_name}</small>`;

                    dropdown.appendChild(li);
                }
            }

            // Display the dropdown
            dropdown.classList.remove("d-none");
        } catch (error) {
            console.error("Autocomplete fetch error:", error);
        }
    }, 400)); // Waits 400ms after the user stops typing before calling the API

    // Handle user selecting a search result or clicking elsewhere on the page
    document.addEventListener("click", function (event) {
        // Check if the user clicked on a dropdown item
        const item = event.target.closest(".autocomplete-item");

        if (item) {
            // Populate the visible input and hidden coordinates with the selected data
            nameInput.value = item.dataset.name;
            nameInput.classList.remove("is-invalid");

            if (latInput && lonInput) {
                latInput.value = item.dataset.lat;
                lonInput.value = item.dataset.lon;
            }

            // Hide the dropdown after selection
            dropdown.classList.add("d-none");
        } else {
            // Hide the dropdown if the user clicks anywhere else on the screen
            dropdown.classList.add("d-none");
        }
    });

    // Handle Form Submission to save the new attraction
    form.addEventListener("submit", async function (event) {
        // Prevent the standard HTML page refresh
        event.preventDefault();

        const itineraryId = form.dataset.itineraryId;
        const attractionName = nameInput.value.trim();
        const visitDate = visitDateInput.value;

        const latitude = latInput ? latInput.value : "";
        const longitude = lonInput ? lonInput.value : "";

        // Reset the general error alert
        errorAlert.classList.add("d-none");

        // Explicitly clear all field-specific validation errors and red borders
        document.querySelectorAll(".form-control").forEach(input => input.classList.remove("is-invalid"));
        document.querySelectorAll(".invalid-feedback").forEach(message => message.textContent = "");

        let isValid = true;

        // Validation Check: Name and Coordinates
        if (!attractionName) {
            nameInput.classList.add("is-invalid");
            document.getElementById("attractionNameError").textContent = "Attraction name cannot be empty.";
            isValid = false;
        } else if (!latitude || !longitude) {
            // If coordinates are missing, it means the user typed a name but didn't pick from the API list
            nameInput.classList.add("is-invalid");
            document.getElementById("attractionNameError").textContent = "Please select a valid location from the search results.";
            isValid = false;
        }

        // Validation Check: Date constraints
        if (!visitDate) {
            visitDateInput.classList.add("is-invalid");
            document.getElementById("visitDateError").textContent = "Visit date and time is required.";
            isValid = false;
        } else if (!DateTimeValidation.isNowOrLater(visitDate)) {
            // Check against the global DateTimeValidation utility to ensure no past dates
            visitDateInput.classList.add("is-invalid");
            document.getElementById("visitDateError").textContent = "Visit date and time must be now or later.";
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        // Disable the save button to prevent double-clicking and duplicate submissions
        saveButton.disabled = true;

        try {
            // Send the POST request to the backend API to create the attraction
            const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: "same-origin",
                // Ensure latitude and longitude are included in the JSON payload
                body: JSON.stringify({ attractionName, visitDate, latitude, longitude })
            });

            // If the server returns a 4xx or 5xx status code
            if (!response.ok) {
                const responseBody = await response.text();
                let errorMessage = responseBody || `Request failed (${response.status}).`;

                // Attempt to parse the custom backend error message mapping
                if (responseBody) {
                    try {
                        const errorData = JSON.parse(responseBody);
                        errorMessage = errorData.message || errorData.detail || responseBody;
                    } catch {
                        errorMessage = responseBody;
                    }
                }

                throw new Error(errorMessage);
            }

            // Redirect back to the detailed itinerary view upon success
            window.location.href = `/itinerary/view/${encodeURIComponent(itineraryId)}`;
        } catch (error) {
            console.error("Unable to create attraction:", error);
            // Display the specific error message to the user
            errorAlert.textContent = error.message || "Unable to create the attraction. Please try again.";
            errorAlert.classList.remove("d-none");

            // Re-enable the button so they can fix the error and try again
            saveButton.disabled = false;
        }
    });
});