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

            if (card.classList.contains('hotel-card')) {
                itemName = card.querySelector('.hotel-name').innerText;
                type = "HOTEL";
                // Assuming your delete/edit buttons store the ID, extract it
                itemId = card.querySelector('.edit-hotel')?.dataset.id || "1";
            } else if (card.classList.contains('flight-card')) {
                itemName = card.querySelector('.flight-airline').innerText + " " + card.querySelector('.flight-number').innerText;
                type = "FLIGHT";
                itemId = card.querySelector('.edit-flight')?.dataset.id || "1";
            }

            if (type !== "") {
                listContainer.innerHTML += `
                    <div class="form-check mb-2">
                        <input class="form-check-input affected-item-checkbox" type="checkbox" value="${type}" data-item-id="${itemId}" id="affectedItem${index}" checked>
                        <label class="form-check-label" for="affectedItem${index}">
                            <strong>${type}:</strong> ${itemName}
                        </label>
                    </div>
                `;
            }
        });
        $('#replanSelectionModal').modal('show');
    });

    // Extract the AI call into a reusable function so both buttons can use it
    function fetchAiAlternatives(targetType, userPrompt = null) {
        $('#replanSelectionModal').modal('hide');
        $('#alternativesModal').modal('show');

        document.getElementById('alternativesResult').classList.add('d-none');
        document.getElementById('proceedToBookingBtn').classList.add('d-none');
        document.getElementById('alternativesMoreBtn').classList.add('d-none');
        document.getElementById('alternativesLoading').classList.remove('d-none');
        document.getElementById('alternativesError').classList.add('d-none');

        const itineraryDetailsDiv = document.getElementById("itineraryDetails");
        const currentItineraryId = itineraryDetailsDiv ? parseInt(itineraryDetailsDiv.dataset.itineraryId, 10) : null;

        const payload = {
            itineraryId: currentItineraryId,
            itemType: targetType,
            location: "TPE",
            startDate: "2026-10-20",
            endDate: "2026-10-22",
            disasterWarning: "Red Alert",
            userPrompt: userPrompt // Passes the "Get More" instruction if provided
        };

        fetch('/api/chat/recommend-alternative', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(response => {
                if (!response.ok) throw new Error("API Error");
                return response.text();
            })
            .then(html => {
                document.getElementById('alternativesLoading').classList.add('d-none');
                document.getElementById('alternativesResultText').innerHTML = html;
                document.getElementById('alternativesResult').classList.remove('d-none');
                document.getElementById('proceedToBookingBtn').classList.remove('d-none');
                document.getElementById('alternativesMoreBtn').classList.remove('d-none'); // Show Get More button
            })
            .catch(error => {
                console.error("AI Error:", error);
                document.getElementById('alternativesLoading').classList.add('d-none');
                document.getElementById('alternativesError').innerText = "Failed to load alternatives.";
                document.getElementById('alternativesError').classList.remove('d-none');
            });
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

        // Save context for the mock booking page
        sessionStorage.setItem('replanItineraryId', document.getElementById("itineraryDetails").dataset.itineraryId);
        sessionStorage.setItem('replanItemType', checkboxes[0].value); // Assuming 1 type for now
        sessionStorage.setItem('replanOriginalItemId', checkboxes[0].dataset.itemId);

        fetchAiAlternatives(targetType);
    });

    // Wire up the "Get More Options" button
    document.getElementById('alternativesMoreBtn')?.addEventListener('click', () => {
        const targetType = sessionStorage.getItem('replanItemType') || "HOTEL";
        const prompt = "The previous options weren't quite right. Please search again and provide completely different, new alternative options.";
        fetchAiAlternatives(targetType, prompt);
    });

    document.getElementById('proceedToBookingBtn')?.addEventListener('click', () => {
        const selected = document.querySelector('input[name="selectedAlternative"]:checked');
        if (!selected) {
            alert("Please select an alternative option first.");
            return;
        }

        sessionStorage.setItem('selectedAlternative', selected.value);
        window.location.href = '/mockBooking';
    });
});