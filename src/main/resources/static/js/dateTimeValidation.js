(function () {
    function getMinimumDateTime() {
        const minimum = new Date();
        minimum.setMinutes(minimum.getMinutes() + 1, 0, 0);
        return minimum;
    }

    function toDateTimeLocalValue(date) {
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}T${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
    }

    function setMinimumNow(input) {
        input.min = toDateTimeLocalValue(getMinimumDateTime());
    }

    function isNowOrLater(value) {
        if (!value) {
            return false;
        }

        const selected = new Date(value);
        return !Number.isNaN(selected.getTime()) && selected >= getMinimumDateTime();
    }

    document.querySelectorAll('[data-min-datetime="now"]').forEach(setMinimumNow);
    document.addEventListener("focusin", event => {
        if (event.target.matches('[data-min-datetime="now"]')) {
            setMinimumNow(event.target);
        }
    });

    window.DateTimeValidation = { setMinimumNow, isNowOrLater };
})();
