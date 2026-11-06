/**
 * Manage AI-powered itinerary alternative recommendation and booking process
 */

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

    let currentAlternativeRequestId = null;
    let alternativeAbortController = null;

    function cancelAlternativesGeneration() {
        if (alternativeAbortController) {
            alternativeAbortController.abort();
            alternativeAbortController = null;
        }
        if (currentAlternativeRequestId) {
            const cancelId = currentAlternativeRequestId;
            currentAlternativeRequestId = null;
            fetch('/api/chat/cancel?requestId=' + encodeURIComponent(cancelId), {
                method: 'POST',
                credentials: 'same-origin'
            }).catch(err => console.warn("Chat cancellation notice failed:", err));
        }
        document.getElementById('alternativesLoading')?.classList.add('d-none');
    }

    // Extract the AI call into a reusable function so both buttons can use it
    async function fetchAiAlternatives(targetType, selectedItems, userPrompt = null) {
        if (alternativeAbortController) {
            alternativeAbortController.abort();
        }
        currentAlternativeRequestId = "req_alt_" + Date.now() + "_" + Math.random().toString(36).substring(2, 9);
        alternativeAbortController = new AbortController();

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
                requestId: currentAlternativeRequestId,
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
                signal: alternativeAbortController.signal,
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
            if (error.name === 'AbortError') {
                console.log("AI alternative generation was cancelled.");
                return;
            }
            console.error("AI Error:", error);
            document.getElementById('alternativesLoading').classList.add('d-none');
            document.getElementById('alternativesError').innerText =
                error.message || "Failed to load alternatives.";
            document.getElementById('alternativesError').classList.remove('d-none');
        } finally {
            currentAlternativeRequestId = null;
            alternativeAbortController = null;
        }
    }

    document.getElementById('alternativesCloseBtn')?.addEventListener('click', () => {
        if (currentAlternativeRequestId) {
            cancelAlternativesGeneration();
        }
    });

    document.getElementById('alternativesHeaderCloseBtn')?.addEventListener('click', () => {
        if (currentAlternativeRequestId) {
            cancelAlternativesGeneration();
        }
    });

    $('#alternativesModal').on('hide.bs.modal hidden.bs.modal', () => {
        if (currentAlternativeRequestId) {
            cancelAlternativesGeneration();
        }
    });

    window.addEventListener('beforeunload', () => {
        if (currentAlternativeRequestId) {
            navigator.sendBeacon('/api/chat/cancel?requestId=' + encodeURIComponent(currentAlternativeRequestId));
        }
    });

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
