/**
 * TravelBuddy Dashboard - Live Disaster Monitoring & Proximity Map
 *
 * Manages Leaflet map display, global disaster feeds from GDACS API,
 * traveler geolocation detection, real-time proximity calculations,
 * map recentering, and Gemini AI risk assessment with evacuation routing.
 */

var latestAiAssessment = null;
var activeAiAbortController = null;
var aiProgressInterval = null;

// Expose cancellation globally so inline onclick="cancelAiGeneration()" works
window.cancelAiGeneration = function () {
    if (activeAiAbortController) {
        activeAiAbortController.abort();
        activeAiAbortController = null;
    }
    stopAiProgressAnimation();

    var placeholderSection = document.getElementById("aiPlaceholderSection");
    var loadingSection = document.getElementById("aiLoadingSection");
    var contentSection = document.getElementById("aiContentSection");
    var errorSection = document.getElementById("aiErrorSection");
    var refreshBanner = document.getElementById("aiRefreshingBanner");

    if (refreshBanner) refreshBanner.style.display = "none";

    if (latestAiAssessment != null) {
        // Report already exists: KEEP IT VISIBLE
        if (loadingSection) loadingSection.style.display = "none";
        if (errorSection) errorSection.style.display = "none";
        if (contentSection) contentSection.style.display = "block";
    } else {
        // Initial generation cancelled: return to placeholder
        if (loadingSection) loadingSection.style.display = "none";
        if (errorSection) errorSection.style.display = "none";
        if (contentSection) contentSection.style.display = "none";
        if (placeholderSection) placeholderSection.style.display = "block";
    }

    var btnRefresh = document.getElementById("btnRefreshAi");
    var btnGenerate = document.getElementById("btnGenerateAi");
    if (btnRefresh) {
        btnRefresh.disabled = false;
        btnRefresh.innerHTML = '<i class="fas fa-sync-alt mr-1"></i> Refresh Assessment';
    }
    if (btnGenerate) {
        btnGenerate.disabled = false;
        btnGenerate.innerHTML = '<i class="fas fa-magic mr-1"></i> Generate AI Risk Assessment';
    }
};

document.addEventListener("DOMContentLoaded", function () {
    // 1. Initialize Leaflet map and base tile layer
    var map = L.map('dashboardMap').setView([20.0, 0.0], 2);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(map);

    setTimeout(function () {
        map.invalidateSize();
    }, 200);

    var disasterLayerGroup = L.layerGroup().addTo(map);
    var userLayerGroup = L.layerGroup().addTo(map);
    var routeLayerGroup = L.layerGroup().addTo(map);

    var travelerCoords = { lat: null, lon: null };
    var currentUserMarker = null;
    var cachedDisasters = [];

    // Load active disasters immediately on page load
    loadDisasterMapData(map, disasterLayerGroup, null, null, function (disasters) {
        cachedDisasters = disasters;
    });

    // 2. Request traveler coordinates via HTML5 Geolocation API
    function requestTravelerLocation() {
        if (!navigator.geolocation) {
            return;
        }

        navigator.geolocation.getCurrentPosition(
            function (position) {
                var userLat = position.coords.latitude;
                var userLon = position.coords.longitude;
                travelerCoords.lat = userLat;
                travelerCoords.lon = userLon;

                var locationStatus = document.getElementById("locationStatus");
                if (locationStatus) {
                    locationStatus.textContent = "Location Acquired";
                    locationStatus.className = "badge badge-success";
                }

                map.setView([userLat, userLon], 5);
                userLayerGroup.clearLayers();

                var userIcon = L.divIcon({
                    className: 'user-marker-wrapper',
                    html: '<div class="user-marker-dot"></div>',
                    iconSize: [16, 16],
                    iconAnchor: [8, 8],
                    popupAnchor: [0, -8]
                });

                currentUserMarker = L.marker([userLat, userLon], { icon: userIcon }).addTo(userLayerGroup);
                currentUserMarker.bindPopup(
                    "<b>You are here</b><br>Coordinates: " + userLat.toFixed(4) + ", " + userLon.toFixed(4)
                ).openPopup();

                var userPerimeter = L.circle([userLat, userLon], {
                    color: '#007bff',
                    fillColor: '#007bff',
                    fillOpacity: 0.1,
                    radius: 120000 // 120 km in meters
                }).addTo(userLayerGroup);
                userPerimeter.bindPopup("Your 120 km monitoring radius.");

                if (cachedDisasters && cachedDisasters.length > 0) {
                    renderDisasterMarkers(disasterLayerGroup, cachedDisasters, userLat, userLon);
                }

                loadDisasterMapData(map, disasterLayerGroup, userLat, userLon, function (disasters) {
                    cachedDisasters = disasters;
                });
            },
            function (err) {
                console.warn("Geolocation denied or unavailable:", err.message);
                var locationStatus = document.getElementById("locationStatus");
                if (locationStatus) {
                    locationStatus.textContent = "Global View";
                    locationStatus.className = "badge badge-secondary";
                }
            },
            {
                enableHighAccuracy: false,
                timeout: 10000,
                maximumAge: 60000
            }
        );
    }

    requestTravelerLocation();

    // 3. "My Location" Recenter Button Handler
    var btnRecenter = document.getElementById("btnRecenterLocation");
    if (btnRecenter) {
        btnRecenter.addEventListener("click", function () {
            if (travelerCoords.lat != null && travelerCoords.lon != null) {
                map.flyTo([travelerCoords.lat, travelerCoords.lon], 6, {
                    animate: true,
                    duration: 1.2
                });
                if (currentUserMarker) {
                    currentUserMarker.openPopup();
                }
            } else {
                var locationStatus = document.getElementById("locationStatus");
                if (locationStatus) {
                    locationStatus.textContent = "Acquiring GPS...";
                    locationStatus.className = "badge badge-warning";
                }
                requestTravelerLocation();
            }
        });
    }

    // 4. "Generate Assessment" and "Refresh Assessment" Button Handlers
    var btnGenerateAi = document.getElementById("btnGenerateAi");
    if (btnGenerateAi) {
        btnGenerateAi.addEventListener("click", function () {
            fetchAiRiskAssessment(travelerCoords.lat, travelerCoords.lon);
        });
    }

    var btnRefreshAi = document.getElementById("btnRefreshAi");
    if (btnRefreshAi) {
        btnRefreshAi.addEventListener("click", function () {
            fetchAiRiskAssessment(travelerCoords.lat, travelerCoords.lon);
        });
    }

    // 5. Evacuation Route Selection Handlers
    var btnSelectFoot = document.getElementById("btnSelectFootRoute");
    if (btnSelectFoot) {
        btnSelectFoot.addEventListener("click", function (e) {
            e.stopPropagation();
            showEvacuationRouteOnMap('foot');
        });
    }

    var cardFoot = document.getElementById("cardFootRoute");
    if (cardFoot) {
        cardFoot.addEventListener("click", function () {
            showEvacuationRouteOnMap('foot');
        });
    }

    var btnSelectVehicle = document.getElementById("btnSelectVehicleRoute");
    if (btnSelectVehicle) {
        btnSelectVehicle.addEventListener("click", function (e) {
            e.stopPropagation();
            showEvacuationRouteOnMap('vehicle');
        });
    }

    var cardVehicle = document.getElementById("cardVehicleRoute");
    if (cardVehicle) {
        cardVehicle.addEventListener("click", function () {
            showEvacuationRouteOnMap('vehicle');
        });
    }

    // 6. Clear Route Button Handler
    var btnClearRoute = document.getElementById("btnClearRoute");
    if (btnClearRoute) {
        btnClearRoute.addEventListener("click", function () {
            clearEvacuationRoute();
        });
    }

    // 7. Retry AI Generation Button Handler
    var btnRetryAi = document.getElementById("btnRetryAi");
    if (btnRetryAi) {
        btnRetryAi.addEventListener("click", function () {
            fetchAiRiskAssessment(travelerCoords.lat, travelerCoords.lon);
        });
    }

    window.addEventListener("pagehide", function () {
        if (activeAiAbortController) {
            activeAiAbortController.abort();
        }
    });

    /**
     * Renders evacuation route onto the Leaflet map.
     */
    function showEvacuationRouteOnMap(routeType) {
        if (!map || !routeLayerGroup) return;

        var startLat = travelerCoords.lat;
        var startLon = travelerCoords.lon;

        if (startLat == null || startLon == null) {
            alert("Please click 'My Location' or enable location access so TravelBuddy can plot an evacuation route from your position.");
            return;
        }

        routeLayerGroup.clearLayers();

        var footCard = document.getElementById("cardFootRoute");
        var vehicleCard = document.getElementById("cardVehicleRoute");
        var btnFoot = document.getElementById("btnSelectFootRoute");
        var btnVehicle = document.getElementById("btnSelectVehicleRoute");
        var btnClear = document.getElementById("btnClearRoute");

        if (footCard) footCard.classList.remove("active-route");
        if (vehicleCard) vehicleCard.classList.remove("active-route");
        if (btnFoot) {
            btnFoot.innerHTML = '<i class="fas fa-walking mr-1"></i> Follow This Route (Show on Map)';
            btnFoot.className = 'btn btn-sm btn-outline-success btn-block mt-3 font-weight-bold';
        }
        if (btnVehicle) {
            btnVehicle.innerHTML = '<i class="fas fa-car mr-1"></i> Follow This Route (Show on Map)';
            btnVehicle.className = 'btn btn-sm btn-outline-primary btn-block mt-3 font-weight-bold';
        }

        var routeOption = (routeType === 'foot')
            ? (latestAiAssessment && latestAiAssessment.footOption)
            : (latestAiAssessment && latestAiAssessment.vehicleOption);

        if (!routeOption) {
            alert("No evacuation route available for this selection.");
            return;
        }

        var isFoot = (routeType === 'foot');
        var routeColor = isFoot ? '#1cc88a' : '#4e73df';
        var iconHtml = isFoot
            ? '<div class="route-dest-marker route-dest-foot"><i class="fas fa-shield-alt"></i></div>'
            : '<div class="route-dest-marker route-dest-vehicle"><i class="fas fa-car"></i></div>';

        if (isFoot) {
            if (footCard) footCard.classList.add("active-route");
            if (btnFoot) {
                btnFoot.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Active Route on Map';
                btnFoot.className = 'btn btn-sm btn-success btn-block mt-3 font-weight-bold';
            }
        } else {
            if (vehicleCard) vehicleCard.classList.add("active-route");
            if (btnVehicle) {
                btnVehicle.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Active Route on Map';
                btnVehicle.className = 'btn btn-sm btn-primary btn-block mt-3 font-weight-bold';
            }
        }

        var destLat = Number(routeOption.destinationLat);
        var destLon = Number(routeOption.destinationLon);
        if (!Number.isFinite(destLat) || !Number.isFinite(destLon)) {
            alert("The selected route has no valid destination coordinates.");
            return;
        }

        var routeLatLngs = [];
        if (Array.isArray(routeOption.coordinates)) {
            routeLatLngs = routeOption.coordinates
                .filter(function (coordinate) {
                    return Array.isArray(coordinate)
                        && coordinate.length >= 2
                        && Number.isFinite(Number(coordinate[0]))
                        && Number.isFinite(Number(coordinate[1]));
                })
                .map(function (coordinate) {
                    return [Number(coordinate[1]), Number(coordinate[0])];
                });
        }
        if (routeLatLngs.length < 2) {
            routeLatLngs = [[startLat, startLon], [destLat, destLon]];
        }

        var routeLine = L.polyline(routeLatLngs, {
            color: routeColor,
            weight: 5,
            opacity: 0.88,
            dashArray: routeOption.routeVerified ? null : '6, 8',
            lineCap: 'round',
            lineJoin: 'round'
        }).addTo(routeLayerGroup);

        var destIcon = L.divIcon({
            className: 'route-dest-icon-wrapper',
            html: iconHtml,
            iconSize: [28, 28],
            iconAnchor: [14, 14],
            popupAnchor: [0, -14]
        });

        var navInfo;
        if (routeOption.routeVerified) {
            navInfo = '<div class="small text-success mt-1"><i class="fas fa-shield-alt mr-1"></i><strong>OSRM Verified Route:</strong> ' +
                (routeOption.estimatedDistance || '') + ' - Navigation corridor cleared against active hazards.</div>';
        } else if (routeOption.roadBlocked) {
            navInfo = '<div class="small text-warning mt-1"><i class="fas fa-exclamation-triangle mr-1"></i><strong>Detour Advice:</strong> Route bypasses ' +
                (routeOption.blockedHazardName || 'hazard zone') + '.</div>';
        } else {
            navInfo = '<div class="small text-muted mt-1"><i class="fas fa-info-circle mr-1"></i>Direct escape corridor guidance.</div>';
        }

        var destMarker = L.marker([destLat, destLon], { icon: destIcon }).addTo(routeLayerGroup);
        destMarker.bindPopup(
            '<div class="disaster-popup-card">' +
                '<div class="font-weight-bold ' + (isFoot ? 'text-success' : 'text-primary') + '">' +
                    (isFoot ? '<i class="fas fa-walking mr-1"></i> Foot Evacuation Shelter' : '<i class="fas fa-car mr-1"></i> Vehicle Evacuation Haven') +
                '</div>' +
                '<div class="small font-weight-bold mt-1">' + (routeOption.destination || routeOption.title || '') + '</div>' +
                '<div class="small text-muted mt-1">' + (routeOption.routeAdvice || '') + '</div>' +
                navInfo +
                '<div class="small mt-2 border-top pt-1 text-secondary">' +
                    '<strong>Est. Distance:</strong> ' + (routeOption.estimatedDistance || 'N/A') +
                '</div>' +
            '</div>'
        ).openPopup();

        if (btnClear) {
            btnClear.style.display = "inline-block";
        }

        map.fitBounds(routeLine.getBounds(), {
            padding: [60, 60],
            maxZoom: 15
        });

        var mapCard = document.getElementById("dashboardMap");
        if (mapCard) {
            mapCard.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    }

    function clearEvacuationRoute() {
        if (routeLayerGroup) {
            routeLayerGroup.clearLayers();
        }
        var btnClear = document.getElementById("btnClearRoute");
        if (btnClear) {
            btnClear.style.display = "none";
        }
        var footCard = document.getElementById("cardFootRoute");
        var vehicleCard = document.getElementById("cardVehicleRoute");
        var btnFoot = document.getElementById("btnSelectFootRoute");
        var btnVehicle = document.getElementById("btnSelectVehicleRoute");

        if (footCard) footCard.classList.remove("active-route");
        if (vehicleCard) vehicleCard.classList.remove("active-route");
        if (btnFoot) {
            btnFoot.innerHTML = '<i class="fas fa-walking mr-1"></i> Follow This Route (Show on Map)';
            btnFoot.className = 'btn btn-sm btn-outline-success btn-block mt-3 font-weight-bold';
        }
        if (btnVehicle) {
            btnVehicle.innerHTML = '<i class="fas fa-car mr-1"></i> Follow This Route (Show on Map)';
            btnVehicle.className = 'btn btn-sm btn-outline-primary btn-block mt-3 font-weight-bold';
        }
    }
});

function calculatePythagoreanDistanceKm(userLat, userLon, disasterLat, disasterLon) {
    var latDiff = userLat - disasterLat;
    var lonDiff = userLon - disasterLon;
    return Math.round(Math.sqrt((latDiff * latDiff) + (lonDiff * lonDiff)) * 111.0);
}

function getDisasterTypeName(rawType) {
    var eventTypeMap = {
        'TC': 'Tropical Cyclone',
        'EQ': 'Earthquake',
        'FL': 'Flood',
        'VO': 'Volcano',
        'WF': 'Wildfire',
        'DR': 'Drought'
    };
    return eventTypeMap[rawType] || rawType || 'Disaster';
}

function getAlertConfig(alertLevel) {
    var level = String(alertLevel || '').toLowerCase();
    if (level === 'green') {
        return {
            hexColor: '#28a745',
            badgeClass: 'badge-success',
            markerClass: 'disaster-marker-green'
        };
    }
    if (level === 'orange' || level === 'yellow') {
        return {
            hexColor: '#fd7e14',
            badgeClass: 'badge-warning',
            markerClass: 'disaster-marker-orange'
        };
    }
    return {
        hexColor: '#dc3545',
        badgeClass: 'badge-danger',
        markerClass: 'disaster-marker-red'
    };
}

function buildDisasterPopupHtml(disasterTitle, country, alertLevel, alertConfig, disasterLat, disasterLon, userLat, userLon) {
    var distanceSection = '';

    if (userLat != null && userLon != null) {
        var distanceKm = calculatePythagoreanDistanceKm(userLat, userLon, disasterLat, disasterLon);
        var isWithinDangerZone = distanceKm <= 120;

        distanceSection = '<div class="disaster-popup-distance">' +
            '<strong>Distance from you:</strong> ' +
            '<span class="font-weight-bold ' + (isWithinDangerZone ? 'text-danger' : 'text-dark') + '">' +
                distanceKm.toLocaleString() + ' km' +
            '</span>' +
            (isWithinDangerZone ? ' <span class="badge badge-danger ml-1">Within Danger Zone!</span>' : '') +
        '</div>';
    } else {
        distanceSection = '<div class="disaster-popup-distance text-muted font-italic">' +
            '<i class="fas fa-location-arrow mr-1"></i>Enable GPS to see distance' +
        '</div>';
    }

    var isMultipleCountries = country && country.indexOf(',') !== -1;
    var countryLabel = isMultipleCountries ? 'Affected Countries' : 'Affected Country';

    return '<div class="disaster-popup-card">' +
        '<div class="disaster-popup-title text-danger">' +
            '<i class="fas fa-exclamation-triangle mr-1"></i>' + disasterTitle +
        '</div>' +
        '<div class="disaster-popup-location">' +
            '<i class="fas fa-globe-americas mr-1"></i><strong>' + countryLabel + ':</strong> ' + (country || 'Unknown') +
        '</div>' +
        '<div class="disaster-popup-meta">' +
            'Alert Level: <span class="badge ' + alertConfig.badgeClass + '">' + (alertLevel || 'Unknown') + '</span>' +
        '</div>' +
        distanceSection +
    '</div>';
}

function renderDisasterMarkers(layerGroup, disasters, userLat, userLon) {
    layerGroup.clearLayers();

    if (!Array.isArray(disasters)) {
        return;
    }

    disasters.forEach(function (disaster) {
        var coords = (disaster.geometry && disaster.geometry.coordinates) ||
                     (disaster.locationCoordinates && disaster.locationCoordinates.coordinates);

        if (!coords || !Array.isArray(coords) || coords.length < 2) {
            return;
        }

        var lon = Number(coords[0]);
        var lat = Number(coords[1]);

        if (isNaN(lat) || isNaN(lon)) {
            return;
        }

        var details = disaster.properties || disaster.disasterDetails || {};

        var rawType = details.eventtype ||
            (details.disasterType ? (details.disasterType.disasterName || details.disasterType) : null);
        var disasterType = getDisasterTypeName(rawType);
        var disasterTitle = details.name || disasterType;

        var country = details.country || details.disasterCountryLocation || "Unknown";
        var alertLevel = details.alertlevel || details.alertLevel || "Unknown";
        var alertConfig = getAlertConfig(alertLevel);

        var popupContent = buildDisasterPopupHtml(
            disasterTitle,
            country,
            alertLevel,
            alertConfig,
            lat,
            lon,
            userLat,
            userLon
        );

        var markerIcon = L.divIcon({
            className: 'disaster-marker-wrapper',
            html: '<div class="disaster-marker-dot ' + alertConfig.markerClass + '"></div>',
            iconSize: [12, 12],
            iconAnchor: [6, 6],
            popupAnchor: [0, -6]
        });

        var marker = L.marker([lat, lon], { icon: markerIcon }).bindPopup(popupContent);
        layerGroup.addLayer(marker);

        var circle = L.circle([lat, lon], {
            color: alertConfig.hexColor,
            fillColor: alertConfig.hexColor,
            fillOpacity: 0.25,
            weight: 1.5,
            radius: 120000
        }).bindPopup(popupContent);
        layerGroup.addLayer(circle);
    });
}

function updateSidebarAlerts(data, userLat, userLon) {
    var alertBox = document.getElementById("userProximityAlert");
    var alertTitle = document.getElementById("proximityAlertTitle");
    var alertBody = document.getElementById("proximityAlertBody");
    var distanceText = document.getElementById("nearestDistanceText");

    if (!alertBox || !alertTitle || !alertBody || !distanceText) {
        return;
    }

    if (data.nearestDistanceKm != null) {
        distanceText.textContent = data.nearestDistanceKm + " km (" + (data.nearestDisasterName || "Recorded Event") + ")";
    } else if (userLat == null || userLon == null) {
        distanceText.textContent = "Waiting for location coordinates...";
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
        alertTitle.innerHTML = '<i class="fas fa-info-circle mr-1"></i> Global Monitoring Mode';
        alertBody.textContent = "Displaying global disaster zones. Enable browser GPS location to calculate your exact proximity.";
    }
}

async function loadDisasterMapData(map, disasterLayerGroup, userLat, userLon, onSuccess) {
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
        var disasters = Array.isArray(data.disasters) ? data.disasters : [];

        renderDisasterMarkers(disasterLayerGroup, disasters, userLat, userLon);
        updateSidebarAlerts(data, userLat, userLon);

        if (typeof onSuccess === "function") {
            onSuccess(disasters);
        }

    } catch (err) {
        console.error("Failed to load dashboard overview data:", err);
    }
}

function stopAiProgressAnimation() {
    if (aiProgressInterval) {
        clearInterval(aiProgressInterval);
        aiProgressInterval = null;
    }
}

function startAiProgressAnimation() {
    stopAiProgressAnimation();

    var steps = [
        {
            pct: 30,
            badge: '<i class="fas fa-satellite-dish mr-1"></i> Telemetry Check',
            msg: 'Checking coordinates and proximity to active hazards...'
        },
        {
            pct: 65,
            badge: '<i class="fas fa-route mr-1"></i> Routing Analysis',
            msg: 'Verifying navigation corridors avoiding disaster perimeters...'
        },
        {
            pct: 90,
            badge: '<i class="fas fa-robot mr-1"></i> AI Personalization',
            msg: 'Synthesizing situational safety assessment & recommendations...'
        }
    ];

    var stepBadge = document.getElementById("aiStepBadge");
    var msgElem = document.getElementById("aiProgressMessage");
    var barElem = document.getElementById("aiProgressBar");
    var refreshStepText = document.getElementById("aiRefreshStepText");

    function applyStep(idx) {
        var s = steps[idx];
        if (stepBadge && s.badge) stepBadge.innerHTML = s.badge;
        if (msgElem && s.msg) msgElem.textContent = s.msg;
        if (barElem && s.pct) barElem.style.width = s.pct + "%";
        if (refreshStepText && s.msg) {
            refreshStepText.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> ' + s.msg;
        }
    }

    applyStep(0);
    var currentStep = 0;
    aiProgressInterval = setInterval(function () {
        currentStep++;
        if (currentStep < steps.length) {
            applyStep(currentStep);
        } else {
            stopAiProgressAnimation();
        }
    }, 600);
}

async function fetchAiRiskAssessment(userLat, userLon) {
    var placeholderSection = document.getElementById("aiPlaceholderSection");
    var loadingSection = document.getElementById("aiLoadingSection");
    var contentSection = document.getElementById("aiContentSection");
    var errorSection = document.getElementById("aiErrorSection");
    var errorMessageElem = document.getElementById("aiErrorMessage");
    var refreshBanner = document.getElementById("aiRefreshingBanner");
    var btnRefresh = document.getElementById("btnRefreshAi");
    var btnGenerate = document.getElementById("btnGenerateAi");

    var isInitialGeneration = (latestAiAssessment == null);

    if (isInitialGeneration) {
        if (placeholderSection) placeholderSection.style.display = "none";
        if (contentSection) contentSection.style.display = "none";
        if (errorSection) errorSection.style.display = "none";
        if (loadingSection) loadingSection.style.display = "block";
    } else {
        if (loadingSection) loadingSection.style.display = "none";
        if (errorSection) errorSection.style.display = "none";
        if (contentSection) contentSection.style.display = "block";
        if (refreshBanner) refreshBanner.style.display = "flex";
    }

    if (btnRefresh) {
        btnRefresh.style.display = "inline-block";
        btnRefresh.disabled = true;
        btnRefresh.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Analyzing...';
    }
    if (btnGenerate) {
        btnGenerate.disabled = true;
        btnGenerate.innerHTML = '<i class="fas fa-spinner fa-spin mr-1"></i> Generating...';
    }

    if (activeAiAbortController) {
        activeAiAbortController.abort();
    }
    activeAiAbortController = new AbortController();

    // Start progress, but cap it at step 2 so it doesn't prematurely claim "Synthesizing"
    startControlledProgress();

    try {
        var params = new URLSearchParams();
        if (userLat != null && userLon != null) {
            params.append("userLat", userLat);
            params.append("userLon", userLon);
        }

        var queryString = params.toString();
        var url = '/api/dashboard/risk-assessment' + (queryString ? '?' + queryString : '');

        var response = await fetch(url, {
            method: "GET",
            signal: activeAiAbortController.signal,
            headers: {
                "Accept": "application/json"
            },
            credentials: "same-origin"
        });

        if (!response.ok) {
            throw new Error("HTTP error " + response.status);
        }

        var data = await response.json();
        
        if (data.error || (data.riskLevel === 'UNKNOWN' && data.riskSummary && data.riskSummary.includes("temporarily unavailable"))) {
            throw new Error(data.error || data.riskSummary);
        }

        // SUCCESS: Stop progress animation and snap to 100%
        stopAiProgressAnimation();
        var barElem = document.getElementById("aiProgressBar");
        if (barElem) barElem.style.width = "100%";

        latestAiAssessment = data;

        if (typeof clearEvacuationRoute === "function") {
            clearEvacuationRoute();
        }

        // Render UI elements as normal...
        // [Keep the rendering code for badges, summaries, routes, and checklist here]

        if (loadingSection) loadingSection.style.display = "none";
        if (contentSection) contentSection.style.display = "block";

    } catch (err) {
        if (err.name === "AbortError") {
            return;
        }

        console.error("AI risk assessment error:", err);
        stopAiProgressAnimation();

        if (loadingSection) loadingSection.style.display = "none";
        if (contentSection) contentSection.style.display = "none";
        if (placeholderSection) placeholderSection.style.display = "none";
        if (errorSection) errorSection.style.display = "block";
        
        if (errorMessageElem) {
            errorMessageElem.textContent = err.message || "AI assessment could not be generated at this time. Please retry later.";
        }
    } finally {
        stopAiProgressAnimation();
        var currentRefreshBanner = document.getElementById("aiRefreshingBanner");
        if (currentRefreshBanner) {
            currentRefreshBanner.style.display = "none";
        }

        if (btnRefresh) {
            btnRefresh.disabled = false;
            btnRefresh.innerHTML = '<i class="fas fa-sync-alt mr-1"></i> Refresh Assessment';
        }
        if (btnGenerate) {
            btnGenerate.disabled = false;
            btnGenerate.innerHTML = '<i class="fas fa-magic mr-1"></i> Generate AI Risk Assessment';
        }
    }
}

// Helper to keep progress realistic while waiting for backend tool loops
function startControlledProgress() {
    stopAiProgressAnimation();
    var barElem = document.getElementById("aiProgressBar5"); // or aiProgressBar
    var msgElem = document.getElementById("aiProgressMessage");
    
    if (barElem) barElem.style.width = "40%";
    if (msgElem) msgElem.textContent = "AI Agent executing tool calls (Proximity & Shelters)...";

    aiProgressInterval = setInterval(function () {
        if (barElem) {
            var currentWidth = parseInt(barElem.style.width) || 40;
            if (currentWidth < 85) {
                barElem.style.width = (currentWidth + 5) + "%";
            }
        }
    }, 1000);
}