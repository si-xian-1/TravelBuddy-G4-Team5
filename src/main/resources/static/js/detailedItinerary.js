/**
 * Shared method across attraction, hotel and flight management
 */

async function getErrorMessage(responseBody, status) {
    if (!responseBody) {
        return `Request failed (${status}).`;
    }

    try {
        const errorData = JSON.parse(responseBody);
        return errorData.message || errorData.detail || responseBody;
    } catch {
        return responseBody;
    }
}

function formatVisitDate(value) {
    if (!value) {
        return "Not provided";
    }

    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function toDateTimeInputValue(value) {
    return value ? value.slice(0, 16) : "";
}

function formatCoordinates(latitude, longitude) {
    if (latitude == null || longitude == null) {
        return "Not available";
    }

    return `${latitude}, ${longitude}`;
}
