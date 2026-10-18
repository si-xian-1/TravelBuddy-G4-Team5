document.addEventListener("DOMContentLoaded", async function () {
    const page = document.getElementById("itineraryDetails");
    const itineraryId = page.dataset.itineraryId;
    const loadingMessage = document.getElementById("itineraryLoading");
    const errorMessage = document.getElementById("itineraryError");
    const summary = document.getElementById("itinerarySummary");

    try {
        const response = await fetch(
            `/api/itinerary/${encodeURIComponent(itineraryId)}`,
            { credentials: "same-origin" }
        );

        if (!response.ok) {
            throw new Error(`Failed to load travel details (${response.status}).`);
        }

        const itinerary = await response.json();
        document.getElementById("itineraryName").textContent =
            itinerary.itineraryName || "Unnamed itinerary";
        document.getElementById("itineraryDates").textContent =
            `${formatTravelDate(itinerary.startDate)} - ${formatTravelDate(itinerary.endDate)}`;
        loadingMessage.classList.add("d-none");
        summary.classList.remove("d-none");
    } catch (error) {
        console.error("Unable to load itinerary details:", error);
        loadingMessage.classList.add("d-none");
        errorMessage.textContent = error.message || "Unable to load travel details.";
        errorMessage.classList.remove("d-none");
    }
});

function setItinerarySectionExpanded(sectionId, expanded) {
    $(`#${sectionId}`).collapse(expanded ? "show" : "hide");
}

function formatTravelDate(value) {
    if (!value) {
        return "Not provided";
    }

    const [year, month, day] = value.split("-").map(Number);
    const date = new Date(year, month - 1, day);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
}
