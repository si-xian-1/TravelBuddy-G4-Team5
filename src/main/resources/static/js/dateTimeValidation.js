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

    document.querySelectorAll('[data-min-datetime="now"]').forEach(setMinimumNow);
    
    // Update the minimum time the exact moment the user clicks into the input box
    document.addEventListener("focusin", event => {
        if (event.target.matches('[data-min-datetime="now"]')) {
            setMinimumNow(event.target);
        }
    });

    // Expose only the necessary functions to the rest of the application
    window.DateTimeValidation = { setMinimumNow, isNowOrLater };
})();