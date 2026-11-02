/**
 * SHARED UTILITY FUNCTIONS
 * These functions are used across Flight, Hotel, and Attraction management files.
 */
function setItinerarySectionExpanded(sectionId, expanded) {
    $(`#${sectionId}`).collapse(expanded ? "show" : "hide");
}

function formatDateTime(value) {
    if (!value) return "Not provided";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return date.toLocaleString('en-GB', {
        day: 'numeric', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hour12: true
    });
}

function toDateTimeInputValue(value) {
    if (!value) return "";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value.slice(0, 16);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function getApiErrorMessage(responseBody, status) {
    if (!responseBody) return `Request failed (${status}).`;
    try {
        const errorData = JSON.parse(responseBody);
        const fieldErrors = Object.entries(errorData)
            .filter(([field]) => field !== "message")
            .map(([field, message]) => `${field}: ${message}`);
        return errorData.message || errorData.detail || fieldErrors.join(" ") || responseBody;
    } catch {
        return responseBody;
    }
}

function clearFormValidation(form) {
    form.querySelectorAll(".form-control").forEach(input => input.classList.remove("is-invalid"));
    form.querySelectorAll(".invalid-feedback").forEach(message => message.textContent = "");
}

function showFormFieldError(form, selector, message) {
    form.querySelector(`.${selector}`).classList.add("is-invalid");
    form.querySelector(`.${selector}-error`).textContent = message;
}

function setCallFrequency(func, delay) {
    let timeout = null;
    return function (event) {
        clearTimeout(timeout);
        timeout = setTimeout(() => { func(event); }, delay);
    };
}

document.addEventListener("input", setCallFrequency(async function (event) {
    if (!event.target.matches(".location-autocomplete-input")) return;

    const input = event.target;
    const wrapper = input.closest(".location-autocomplete-wrapper");
    const dropdown = wrapper.querySelector(".location-autocomplete-dropdown");
    const latInput = wrapper.querySelector(".location-autocomplete-lat");
    const lonInput = wrapper.querySelector(".location-autocomplete-lon");
    const searchType = input.dataset.searchType;
    const query = input.value.trim();

    if (latInput && lonInput) { latInput.value = ""; lonInput.value = ""; }

    if (query.length < 3) {
        dropdown.classList.add("d-none");
        return;
    }

    try {
        let apiUrl = `/api/location/search?query=${encodeURIComponent(query)}`;
        if (searchType === "hotel") apiUrl += "&isHotel=true";

        const response = await fetch(apiUrl);
        if (!response.ok) throw new Error("Location search failed");

        const locations = await response.json();
        dropdown.replaceChildren();

        if (locations.length === 0) {
            dropdown.innerHTML = '<li class="list-group-item text-muted text-xs">No locations found</li>';
        } else {
            for (const loc of locations) {
                const li = document.createElement("li");
                li.className = "list-group-item list-group-item-action text-sm autocomplete-item cursor-pointer";
                const fullAddress = loc.display_name || "Unknown Location";
                const locationName = fullAddress.split(',')[0].trim();

                li.dataset.name = locationName;
                li.dataset.lat = loc.lat;
                li.dataset.lon = loc.lon;
                li.innerHTML = `<strong>${locationName}</strong><br><small class="text-muted">${fullAddress}</small>`;
                dropdown.appendChild(li);
            }
        }
        dropdown.classList.remove("d-none");
    } catch (error) {
        console.error("Search autocomplete error:", error);
    }
}, 400));

document.addEventListener("click", function (event) {
    const item = event.target.closest(".autocomplete-item");
    if (item) {
        const wrapper = item.closest(".location-autocomplete-wrapper");
        const nameInput = wrapper.querySelector(".location-autocomplete-input");
        const latInput = wrapper.querySelector(".location-autocomplete-lat");
        const lonInput = wrapper.querySelector(".location-autocomplete-lon");

        nameInput.value = item.dataset.name;
        nameInput.classList.remove("is-invalid");

        if (latInput && lonInput) {
            latInput.value = item.dataset.lat;
            lonInput.value = item.dataset.lon;
        }
        wrapper.querySelector(".location-autocomplete-dropdown").classList.add("d-none");
    } else {
        document.querySelectorAll(".location-autocomplete-dropdown:not(.d-none)").forEach(dropdown => {
            dropdown.classList.add("d-none");
        });
    }
});

/**
 * Shared Itinerary Data Cache
 * Caches the itinerary fetch promise so multiple scripts don't refetch the same data.
 */
let itineraryPromise = null;
let cachedItineraryId = null;

function getItinerary(itineraryId) {
    if (!itineraryId) {
        return Promise.reject(new Error("No itinerary ID provided"));
    }
    if (itineraryPromise && cachedItineraryId === String(itineraryId)) {
        return itineraryPromise;
    }
    cachedItineraryId = String(itineraryId);
    itineraryPromise = fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}`, { credentials: "same-origin" })
        .then(response => {
            if (!response.ok) {
                throw new Error(`Failed to load itinerary (${response.status}).`);
            }
            return response.json();
        })
        .catch(err => {
            itineraryPromise = null;
            cachedItineraryId = null;
            throw err;
        });
    return itineraryPromise;
}

/**
 * Page initialization
 * Fetches and displays the top-level itinerary summary (Name and Overall Dates).
 */
document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    if (!page) return;

    const itineraryId = page.dataset.itineraryId;
    const loadingMessage = document.getElementById("itineraryLoading");
    const errorMessage = document.getElementById("itineraryError");
    const summary = document.getElementById("itinerarySummary");

    try {
        // Fetch core itinerary details
        const itinerary = await getItinerary(itineraryId);
        document.getElementById("itineraryName").textContent = itinerary.itineraryName || "Unnamed itinerary";
        const startDate = itinerary.startDate ? new Date(itinerary.startDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : "Not provided";
        const endDate = itinerary.endDate ? new Date(itinerary.endDate).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' }) : "Not provided";

        document.getElementById("itineraryDates").textContent = `${startDate} - ${endDate}`;
        loadingMessage.classList.add("d-none");
        summary.classList.remove("d-none");
    } catch (error) {
        loadingMessage.classList.add("d-none");
        errorMessage.textContent = error.message || "Unable to load travel details.";
        errorMessage.classList.remove("d-none");
    }
});

// ==============================================================================
// AI REPLAN FEATURE LOGIC
// ==============================================================================
document.addEventListener("DOMContentLoaded", () => {

    function checkRedAlerts() {
        const redAlerts = document.querySelectorAll('.disaster-alert.alert-danger');
        const replanBtn = document.getElementById('replanWithAiBtn');
        if (replanBtn && redAlerts.length > 0) {
            replanBtn.style.display = 'inline-block';
        } else if (replanBtn) {
            replanBtn.style.display = 'none';
        }
    }
    setInterval(checkRedAlerts, 1000);

    document.getElementById('replanWithAiBtn')?.addEventListener('click', () => {
        const listContainer = document.getElementById('affectedItemsList');
        listContainer.innerHTML = '';

        document.querySelectorAll('.disaster-alert.alert-danger').forEach((alert, index) => {
            const card = alert.closest('.card');
            let itemName = "Unknown Item";
            let type = "";
            let itemId = "";
            let context = {};

            if (card.classList.contains('hotel-card')) {
                itemName = card.querySelector('.hotel-name').innerText;
                type = "HOTEL";
                itemId = card.dataset.hotelId;
                context = {
                    itemType: type,
                    itemId,
                    hotelName: card.dataset.originalHotelName,
                    latitude: Number(card.dataset.latitude),
                    longitude: Number(card.dataset.longitude),
                    checkIn: card.dataset.originalCheckIn,
                    checkOut: card.dataset.originalCheckOut,
                    disasterWarning: card.dataset.disasterWarning || ""
                };
            } else if (card.classList.contains('flight-card')) {
                itemName = card.querySelector('.flight-airline').innerText + " " + card.querySelector('.flight-number').innerText;
                type = "FLIGHT";
                itemId = card.dataset.flightId;
                context = {
                    itemType: type,
                    itemId,
                    airline: card.dataset.airline,
                    flightNumber: card.dataset.flightNumber,
                    departureAirport: card.dataset.departureAirport,
                    arrivalAirport: card.dataset.arrivalAirport,
                    departureLatitude: Number(card.dataset.departureLatitude),
                    departureLongitude: Number(card.dataset.departureLongitude),
                    arrivalLatitude: Number(card.dataset.arrivalLatitude),
                    arrivalLongitude: Number(card.dataset.arrivalLongitude),
                    departureTime: card.dataset.departureTime,
                    arrivalTime: card.dataset.arrivalTime,
                    disasterWarning: card.dataset.disasterWarning || ""
                };
            }

            if (type !== "" && itemId) {
                const wrapper = document.createElement("div");
                wrapper.className = "form-check mb-2";

                const checkbox = document.createElement("input");
                checkbox.className = "form-check-input affected-item-checkbox";
                checkbox.type = "checkbox";
                checkbox.value = type;
                checkbox.dataset.itemId = itemId;
                checkbox.dataset.context = JSON.stringify(context);
                checkbox.id = `affectedItem${index}`;
                checkbox.checked = true;

                const label = document.createElement("label");
                label.className = "form-check-label";
                label.htmlFor = checkbox.id;
                label.textContent = `${type}: ${itemName}`;

                wrapper.append(checkbox, label);
                listContainer.appendChild(wrapper);
            }
        });
        $('#replanSelectionModal').modal('show');
    });

    // Extract the AI call into a reusable function so both buttons can use it
    async function fetchAiAlternatives(targetType, selectedItems, userPrompt = null) {
        $('#replanSelectionModal').modal('hide');
        $('#alternativesModal').modal('show');

        document.getElementById('alternativesResult').classList.add('d-none');
        document.getElementById('proceedToBookingBtn').classList.add('d-none');
        document.getElementById('alternativesMoreBtn').classList.add('d-none');
        document.getElementById('alternativesLoading').classList.remove('d-none');
        document.getElementById('alternativesError').classList.add('d-none');

        const itineraryDetailsDiv = document.getElementById("itineraryDetails");
        const currentItineraryId = itineraryDetailsDiv ? parseInt(itineraryDetailsDiv.dataset.itineraryId, 10) : null;
        try {
            const itinerary = await getItinerary(currentItineraryId);
            const firstFlight = selectedItems.find(item => item.itemType === "FLIGHT");
            const firstItem = selectedItems[0];
            const location = firstFlight?.departureAirport || firstItem?.location || "";
            const destinationLocation = firstFlight?.arrivalAirport || location;
            const disasterWarning = selectedItems
                .map(item => item.disasterWarning)
                .filter(warning => warning && warning.trim())
                .join(" | ");

            if (!itinerary.startDate || !itinerary.endDate) {
                throw new Error("The itinerary does not contain valid travel dates to search for alternatives.");
            }

            const payload = {
                itineraryId: currentItineraryId,
                itemType: targetType,
                location: location || null,
                destinationLocation,
                startDate: itinerary.startDate.slice(0, 10),
                endDate: itinerary.endDate.slice(0, 10),
                disasterWarning,
                userPrompt,
                context: JSON.stringify(selectedItems)
            };

            const response = await fetch('/api/chat/recommend-alternative', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (!response.ok) throw new Error("API Error");

            const html = await response.text();
            document.getElementById('alternativesLoading').classList.add('d-none');
            const resultText = document.getElementById('alternativesResultText');
            resultText.innerHTML = html;
            const options = resultText.querySelectorAll('input[name="selectedAlternative"]');
            options.forEach((input, index) => {
                let alternative;
                try {
                    alternative = JSON.parse(input.value);
                } catch (error) {
                    console.warn("Skipping alternative with invalid selection data.", error);
                    input.disabled = true;
                    return;
                }

                if (alternative.itemType === "FLIGHT") {
                    input.type = "radio";
                    input.name = "selectedAlternativeFlight";
                    input.dataset.itemType = "FLIGHT";
                    input.id = input.id || `flight-alternative-${index}`;
                } else if (alternative.itemType === "HOTEL") {
                    input.type = "checkbox";
                    input.name = "selectedAlternative";
                    input.dataset.itemType = "HOTEL";
                }
            });
            resultText.querySelectorAll('.form-check').forEach(option => {
                option.classList.add('ai-alternative-option');
            });
            document.getElementById('alternativesResult').classList.remove('d-none');
            document.getElementById('proceedToBookingBtn').classList.toggle('d-none', options.length === 0);
            document.getElementById('alternativesMoreBtn').classList.remove('d-none');
        } catch (error) {
            console.error("AI Error:", error);
            document.getElementById('alternativesLoading').classList.add('d-none');
            document.getElementById('alternativesError').innerText =
                error.message || "Failed to load alternatives.";
            document.getElementById('alternativesError').classList.remove('d-none');
        }
    }

    document.getElementById('confirmReplanBtn')?.addEventListener('click', () => {
        const checkboxes = document.querySelectorAll('.affected-item-checkbox:checked');
        if (checkboxes.length === 0) {
            alert("Please select at least one item to replan.");
            return;
        }

        const types = Array.from(checkboxes).map(cb => cb.value);
        let targetType = "NONE";
        if (types.includes("HOTEL") && types.includes("FLIGHT")) targetType = "BOTH";
        else if (types.includes("HOTEL")) targetType = "HOTEL";
        else if (types.includes("FLIGHT")) targetType = "FLIGHT";

        const selectedItems = Array.from(checkboxes).map(checkbox => JSON.parse(checkbox.dataset.context));

        // Save context for the mock booking page
        sessionStorage.setItem('replanItineraryId', document.getElementById("itineraryDetails").dataset.itineraryId);
        sessionStorage.setItem('replanItemType', targetType);
        sessionStorage.setItem('replanItems', JSON.stringify(selectedItems));
        getItinerary(document.getElementById("itineraryDetails").dataset.itineraryId).then(itinerary => {
            sessionStorage.setItem('replanStartDate', itinerary.startDate);
            sessionStorage.setItem('replanEndDate', itinerary.endDate);
        });

        fetchAiAlternatives(targetType, selectedItems);
    });

    // Wire up the "Get More Options" button
    document.getElementById('alternativesMoreBtn')?.addEventListener('click', () => {
        const targetType = sessionStorage.getItem('replanItemType') || "HOTEL";
        const selectedItems = JSON.parse(sessionStorage.getItem('replanItems') || "[]");
        const prompt = "The previous options weren't quite right. Please search again and provide completely different, new alternative options.";
        fetchAiAlternatives(targetType, selectedItems, prompt);
    });

    document.getElementById('proceedToBookingBtn')?.addEventListener('click', () => {
        const selected = document.querySelector(
            'input[name="selectedAlternative"]:checked, input[name="selectedAlternativeFlight"]:checked'
        );
        if (!selected) {
            alert("Please select an alternative option first.");
            return;
        }

        const selectedAlternatives = Array.from(
            document.querySelectorAll(
                'input[name="selectedAlternative"]:checked, input[name="selectedAlternativeFlight"]:checked'
            )
        ).map(input => {
            const alternative = JSON.parse(input.value);
            if (!alternative.itemType && input.dataset.itemType) {
                alternative.itemType = input.dataset.itemType;
            }
            return alternative;
        });
        sessionStorage.setItem('selectedAlternatives', JSON.stringify(selectedAlternatives));
        window.location.href = '/mockBooking';
    });
});