document.addEventListener("DOMContentLoaded", function () {
    // 1. Initialize map and tile layer following the Leaflet Quick Start Guide
    var map = L.map('dashboardMap').setView([15.0, 10.0], 2);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    // Ensure map tiles render properly if inside responsive containers
    setTimeout(function () {
        map.invalidateSize();
    }, 200);

    // 2. Obtain traveler coordinates via Geolocation
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function (position) {
                var userLat = position.coords.latitude;
                var userLon = position.coords.longitude;

                var locationStatus = document.getElementById("locationStatus");
                if (locationStatus) {
                    locationStatus.textContent = "Location Acquired";
                }

                // Center view to user's location
                map.setView([userLat, userLon], 5);

                // Add standard marker and popup for the user
                var userMarker = L.marker([userLat, userLon]).addTo(map);
                userMarker.bindPopup("<b>You are here</b><br>Coordinates: " + userLat.toFixed(4) + ", " + userLon.toFixed(4)).openPopup();

                // Add 120 km monitoring circle around user
                var userPerimeter = L.circle([userLat, userLon], {
                    color: '#007bff',
                    fillColor: '#007bff',
                    fillOpacity: 0.1,
                    radius: 120000 // 120 km in meters
                }).addTo(map);
                userPerimeter.bindPopup("Your 120 km monitoring radius.");

                // Load disaster layers and calculate proximity
                loadDisasterMapData(map, userLat, userLon);
            },
            function (err) {
                console.warn("Geolocation denied or unavailable:", err.message);
                var locationStatus = document.getElementById("locationStatus");
                if (locationStatus) {
                    locationStatus.textContent = "Global View";
                }
                loadDisasterMapData(map, null, null);
            },
            {
                enableHighAccuracy: false,
                timeout: 10000,
                maximumAge: 0
            }
        );
    } else {
        loadDisasterMapData(map, null, null);
    }
});

/**
 * Load disaster locations and render markers/circles using Leaflet Quick Start conventions
 */
async function loadDisasterMapData(map, userLat, userLon) {
    try {
        var params = new URLSearchParams();
        if (userLat != null && userLon != null) {
            params.append("userLat", userLat);
            params.append("userLon", userLon);
        }

        var queryString = params.toString();
        var url = '/api/dashboard/disaster-overview' + (queryString ? '?' + queryString : '');

        var response = await fetch(url, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            },
            credentials: "same-origin"
        });

        if (!response.ok) {
            console.error("Backend error loading disaster data: " + response.status);
            return;
        }

        var data = await response.json();

        // 3. Render markers, circles, and popups for each disaster
        if (Array.isArray(data.disasters)) {
            data.disasters.forEach(function (d) {
                if (!d.locationCoordinates || !d.locationCoordinates.coordinates) {
                    return;
                }

                // GeoJSON coordinates format is [longitude, latitude]
                var lon = d.locationCoordinates.coordinates[0];
                var lat = d.locationCoordinates.coordinates[1];
                var details = d.disasterDetails || {};
                var disasterType = details.disasterType ? details.disasterType.disasterName : "Disaster";
                var country = details.disasterCountryLocation || "Unknown";
                var alertLevel = details.alertLevel || "Unknown";

                var popupHtml = "<b>" + disasterType + "</b><br>" +
                    "Location: " + country + "<br>" +
                    "Alert Level: " + alertLevel + "<br>" +
                    "Danger Zone: 120 km radius";

                // Add standard marker (ensures pin is visible at world/country zoom levels)
                var disasterMarker = L.marker([lat, lon]).addTo(map);
                disasterMarker.bindPopup(popupHtml);

                // Add 120 km perimeter circle as shown in Leaflet guide
                var disasterCircle = L.circle([lat, lon], {
                    color: 'red',
                    fillColor: '#f03',
                    fillOpacity: 0.35,
                    radius: 120000 // 120 km in meters
                }).addTo(map);
                disasterCircle.bindPopup(popupHtml);
            });
        }

        // 4. Update the sidebar UI cards
        var alertBox = document.getElementById("userProximityAlert");
        var alertTitle = document.getElementById("proximityAlertTitle");
        var alertBody = document.getElementById("proximityAlertBody");
        var distanceText = document.getElementById("nearestDistanceText");

        if (!alertBox || !alertTitle || !alertBody || !distanceText) {
            return;
        }

        if (data.nearestDistanceKm != null) {
            distanceText.textContent = data.nearestDistanceKm + " km (" + (data.nearestDisasterName || "Recorded Event") + ")";
        } else {
            distanceText.textContent = "No active disasters recorded";
        }

        if (data.warning) {
            alertBox.className = "alert alert-danger shadow-sm mb-4";
            alertTitle.innerHTML = '<i class="fas fa-exclamation-triangle mr-1"></i> Proximity Alert: Near Disaster Zone!';
            alertBody.textContent = data.warning;
        } else if (userLat != null && userLon != null) {
            alertBox.className = "alert alert-success shadow-sm mb-4";
            alertTitle.innerHTML = '<i class="fas fa-shield-alt mr-1"></i> Current Location Status: Safe';
            alertBody.textContent = "You are outside all recorded 120km disaster zones.";
        } else {
            alertBox.className = "alert alert-secondary shadow-sm mb-4";
            alertTitle.innerHTML = '<i class="fas fa-info-circle mr-1"></i> Location Unavailable';
            alertBody.textContent = "Allow browser location access or override coordinates in dev tools to calculate distance.";
        }

    } catch (err) {
        console.error("Failed to load dashboard overview data:", err);
    }
}