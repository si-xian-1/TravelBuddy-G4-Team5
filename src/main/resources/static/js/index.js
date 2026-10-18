/**
 * Initialize the map display
 */


document.addEventListener("DOMContentLoaded", function () {
    // 1. Initialize Map
    const map = L.map('dashboardMap').setView([15.0, 10.0], 2);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18,
        attribution: '&copy; OpenStreetMap'
    }).addTo(map);

    setTimeout(() => map.invalidateSize(), 200);

    // 2. Fetch User Location & Sync with Backend
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const userLat = position.coords.latitude;
                const userLon = position.coords.longitude;
                document.getElementById("locationStatus").textContent = "Location Acquired";

                // User marker icon
                const userIcon = L.divIcon({
                    className: 'user-pin',
                    html: '<div style="background-color: #4e73df; width: 14px; height: 14px; border-radius: 50%; border: 2px solid white; box-shadow: 0 0 6px rgba(0,0,0,0.5);"></div>',
                    iconSize: [14, 14],
                    iconAnchor: [7, 7]
                });

                L.marker([userLat, userLon], { icon: userIcon })
                    .addTo(map)
                    .bindPopup("<strong>Your Location</strong>")
                    .openPopup();

                // 120km safe/danger perimeter ring
                L.circle([userLat, userLon], {
                    radius: 120000,
                    color: '#4e73df',
                    fillColor: '#4e73df',
                    fillOpacity: 0.08,
                    weight: 1,
                    dashArray: '3, 4'
                }).addTo(map);

                map.setView([userLat, userLon], 5);

                loadDisasterMapData(map, userLat, userLon);
            },
            (err) => {
                console.warn("Geolocation denied or unavailable:", err.message);
                document.getElementById("locationStatus").textContent = "Global View";
                loadDisasterMapData(map, null, null);
            },
            { enableHighAccuracy: true, timeout: 8000 }
        );
    } else {
        loadDisasterMapData(map, null, null);
    }
});

// 3. Load Disasters and Update Right-hand Alert Box
async function loadDisasterMapData(map, userLat, userLon) {
    try {
        let url = '/api/dashboard/disaster-overview';
        if (userLat != null && userLon != null) {
            url += `?userLat=${userLat}&userLon=${userLon}`;
        }

        const response = await fetch(url);
        if (!response.ok) return;

        const data = await response.json();

        // Plot Disasters on Leaflet
        if (data.disasters && data.disasters.length > 0) {
            data.disasters.forEach(d => {
                const coords = d.locationCoordinates.coordinates; // [lon, lat]
                const lat = coords[1];
                const lon = coords[0];
                const details = d.disasterDetails;

                // Red circle for disaster zones
                const disasterCircle = L.circle([lat, lon], {
                    radius: 120000,
                    color: '#e74a3b',
                    fillColor: '#e74a3b',
                    fillOpacity: 0.2,
                    weight: 1
                }).addTo(map);

                disasterCircle.bindPopup(`
                            <strong>${details.disasterType.disasterName}</strong><br>
                            Location: ${details.disasterCountryLocation}<br>
                            Alert Level: ${details.alertLevel}<br>
                            Status: Active
                        `);
            });
        }

        // Update UI Alerts on the Right Column
        const alertBox = document.getElementById("userProximityAlert");
        const alertTitle = document.getElementById("proximityAlertTitle");
        const alertBody = document.getElementById("proximityAlertBody");
        const distanceText = document.getElementById("nearestDistanceText");

        if (data.nearestDistanceKm != null) {
            distanceText.textContent = `${data.nearestDistanceKm} km (${data.nearestDisasterName})`;
        } else {
            distanceText.textContent = "No active disasters recorded";
        }

        if (data.warning) {
            alertBox.className = "alert alert-danger shadow-sm mb-4";
            alertTitle.innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> Proximity Alert: Near Disaster Zone!`;
            alertBody.textContent = data.warning;
        } else if (userLat != null) {
            alertBox.className = "alert alert-success shadow-sm mb-4";
            alertTitle.innerHTML = `<i class="fas fa-shield-alt mr-1"></i> Current Location Status: Safe`;
            alertBody.textContent = "You are outside all recorded 120km disaster zones.";
        } else {
            alertBox.className = "alert alert-secondary shadow-sm mb-4";
            alertTitle.innerHTML = `<i class="fas fa-info-circle mr-1"></i> Location Unavailable`;
            alertBody.textContent = "Allow location access or override coordinates in browser dev tools to inspect proximity.";
        }

    } catch (err) {
        console.error("Failed to load dashboard overview data:", err);
    }
}