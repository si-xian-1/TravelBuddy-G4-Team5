const visitDateInput = document.getElementById("visitDate");

document.getElementById("attractionForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    const form = event.currentTarget;
    const nameInput = document.getElementById("attractionName");
    const visitDateInput = document.getElementById("visitDate");
    const errorAlert = document.getElementById("attractionError");
    const saveButton = document.getElementById("saveAttractionBtn");
    const itineraryId = form.dataset.itineraryId;
    const attractionName = nameInput.value.trim();
    const visitDate = visitDateInput.value;

    errorAlert.classList.add("d-none");
    document.querySelectorAll(".form-control").forEach(input => input.classList.remove("is-invalid"));
    document.querySelectorAll(".invalid-feedback").forEach(message => message.textContent = "");

    let isValid = true;
    if (!attractionName) {
        nameInput.classList.add("is-invalid");
        document.getElementById("attractionNameError").textContent =
            "Attraction name cannot be empty.";
        isValid = false;
    }

    if (!visitDate) {
        visitDateInput.classList.add("is-invalid");
        document.getElementById("visitDateError").textContent =
            "Visit date and time is required.";
        isValid = false;
    } else if (!DateTimeValidation.isNowOrLater(visitDate)) {
        visitDateInput.classList.add("is-invalid");
        document.getElementById("visitDateError").textContent =
            "Visit date and time must be now or later.";
        isValid = false;
    }

    if (!isValid) {
        return;
    }

    saveButton.disabled = true;

    try {
        const response = await fetch(`/api/itinerary/${encodeURIComponent(itineraryId)}/attractions/`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            credentials: "same-origin",
            body: JSON.stringify({ attractionName, visitDate })
        });

        if (!response.ok) {
            const responseBody = await response.text();
            let errorMessage = responseBody || `Request failed (${response.status}).`;

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

        window.location.href = `/itinerary/view/${encodeURIComponent(itineraryId)}`;
    } catch (error) {
        console.error("Unable to create attraction:", error);
        errorAlert.textContent = error.message || "Unable to create the attraction. Please try again.";
        errorAlert.classList.remove("d-none");
        saveButton.disabled = false;
    }
});
