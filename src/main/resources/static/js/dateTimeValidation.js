/**
 * Global utility to ensure users cannot select dates/times in the past.
 */
(function () {
    // Calculates the minimum allowed time (Current time + 1 minute)
    function getMinimumDateTime() {
        const minimum = new Date();
        minimum.setMinutes(minimum.getMinutes() + 1, 0, 0);
        return minimum;
    }

    // Converts a Date object into the exact string format required
    function toDateTimeLocalValue(date) {
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}T${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
    }

    // Applies the minimum date/time restriction to a specific input element
    function setMinimumNow(input) {
        input.min = toDateTimeLocalValue(getMinimumDateTime());
    }

    // Validates if a given date string is currently in the future (or exactly now)
    function isNowOrLater(value) {
        if (!value) return false;
        const selected = new Date(value);
        return !Number.isNaN(selected.getTime()) && selected >= getMinimumDateTime();
    }

    // Restricts datetime inputs to stay within an itinerary's start and end dates
    function setItineraryLimits(input, startDate, endDate) {
        if (!input || !startDate || !endDate) {
            return;
        }

        const startStr = typeof startDate === "string" ? startDate.slice(0, 10) : "";
        const endStr = typeof endDate === "string" ? endDate.slice(0, 10) : "";
        
        if (startStr) {
            input.min = `${startStr}T00:00`;
        }

        if (endStr) {
            input.max = `${endStr}T23:59`;
        }
    }

    // Validates if a given datetime value falls within an itinerary's date range
    function isWithinItinerary(value, startDate, endDate) {
        if (!value || !startDate || !endDate) {
            return false;
        }

        const startStr = typeof startDate === "string" ? startDate.slice(0, 10) : "";
        const endStr = typeof endDate === "string" ? endDate.slice(0, 10) : "";
        
        if (!startStr || !endStr) {
            return false;
        }

        const selected = new Date(value);

        if (Number.isNaN(selected.getTime())) {
            return false;
        }

        const minDate = new Date(`${startStr}T00:00:00`);
        const maxDate = new Date(`${endStr}T23:59:59.999`);

        return selected >= minDate && selected <= maxDate;
    }

    document.querySelectorAll('[data-min-datetime="now"]').forEach(setMinimumNow);
    
    // Update the minimum time the exact moment the user clicks into the input box
    document.addEventListener("focusin", event => {
        if (event.target.matches('[data-min-datetime="now"]')) {
            setMinimumNow(event.target);
        }
    });

    // Expose only the necessary functions to the rest of the application
    window.DateTimeValidation = { setMinimumNow, isNowOrLater, setItineraryLimits, isWithinItinerary };
})();