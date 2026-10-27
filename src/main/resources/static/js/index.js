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
var currentAiRequestId = null;

/**
 * Changes one CSS property when an element exists.
 *
 * @param {HTMLElement|null} element element to update
 * @param {string} property CSS property name
 * @param {string} value CSS property value
 * @return {void}
 */
function setElementStyle(element, property, value) {
    if (element) {
        element.style[property] = value;
    }
}

/**
 * Changes one DOM property when an element exists.
 *
 * @param {HTMLElement|null} element element to update
 * @param {string} property property name, such as textContent
 * @param {*} value value to assign
 * @return {void}
 */
function setElementText(element, property, value) {
    if (element) {
        element[property] = value;
    }
}

/**
 * Cancels the active AI request and restores the appropriate assessment view.
 */
window.cancelAiGeneration = function () {
    if (activeAiAbortController) {
        activeAiAbortController.abort();
        activeAiAbortController = null;
    }
    stopAiProgressAnimation();

    // Signal backend to immediately terminate in-progress AI generation task
    if (currentAiRequestId) {
        var cancelRequestId = currentAiRequestId;
        currentAiRequestId = null;
        fetch('/api/dashboard/risk-assessment/cancel?requestId=' + encodeURIComponent(cancelRequestId), {
            method: 'POST',
            credentials: 'same-origin'
        }).catch(function (err) {
            console.warn("Backend cancellation notice failed:", err);
        });
    }

    var placeholderSection = document.getElementById("aiPlaceholderSection");
    var loadingSection = document.getElementById("aiLoadingSection");
    var contentSection = document.getElementById("aiContentSection");
    var errorSection = document.getElementById("aiErrorSection");
    var refreshBanner = document.getElementById("aiRefreshingBanner");

    setElementStyle(refreshBanner, "display", "none");

    if (latestAiAssessment != null) {
        // Report already exists: KEEP IT VISIBLE
        setElementStyle(loadingSection, "display", "none");
        setElementStyle(errorSection, "display", "none");
        setElementStyle(contentSection, "display", "block");
    } else {
        // Initial generation cancelled: return to placeholder
        setElementStyle(loadingSection, "display", "none");
        setElementStyle(errorSection, "display", "none");
        setElementStyle(contentSection, "display", "none");
        setElementStyle(placeholderSection, "display", "block");
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

/**
 * Creates the dashboard map, wires controls, and starts initial disaster loading.
 *
 * @return {void}
 */
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

    /**
     * Requests the browser's current location and updates the map layers.
     */
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

    // "My Location" Recenter Button Handler
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

    // "Generate Assessment" and "Refresh Assessment" Button Handlers
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

    // Evacuation Route Selection Handlers
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

    // Clear Route Button Handler
    var btnClearRoute = document.getElementById("btnClearRoute");
    if (btnClearRoute) {
        btnClearRoute.addEventListener("click", function () {
            clearEvacuationRoute();
        });
    }

    // Retry AI Generation Button Handler
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
        if (currentAiRequestId) {
            navigator.sendBeacon('/api/dashboard/risk-assessment/cancel?requestId=' + encodeURIComponent(currentAiRequestId));
        }
    });

    /**
     * Renders a selected evacuation route on the Leaflet map.
     *
     * @param {string} routeType "foot" or "vehicle"
     * @return {void}
     */
    function showEvacuationRouteOnMap(routeType) {
        if (!map || !routeLayerGroup) {
            return;
        }

        var startLat = travelerCoords.lat;
        var startLon = travelerCoords.lon;

        if (startLat == null || startLon == null) {
            showRouteErrorModal("Please click 'My Location' or enable location access so TravelBuddy can plot an evacuation route from your position.");
            return;
        }

        routeLayerGroup.clearLayers();

        var footCard = document.getElementById("cardFootRoute");
        var vehicleCard = document.getElementById("cardVehicleRoute");
        var btnFoot = document.getElementById("btnSelectFootRoute");
        var btnVehicle = document.getElementById("btnSelectVehicleRoute");
        var btnClear = document.getElementById("btnClearRoute");

        if (footCard) {
            footCard.classList.remove("active-route");
        }
        if (vehicleCard) {
            vehicleCard.classList.remove("active-route");
        }
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

        if (!isRenderableRouteOption(routeOption)) {
            showRouteErrorModal("No evacuation route is available for this selection.");
            return;
        }

        var isFoot = (routeType === 'foot');
        var routeColor = isFoot ? '#1cc88a' : '#4e73df';
        var iconHtml = isFoot
            ? '<div class="route-dest-marker route-dest-foot"><i class="fas fa-shield-alt"></i></div>'
            : '<div class="route-dest-marker route-dest-vehicle"><i class="fas fa-car"></i></div>';

        if (isFoot) {
            if (footCard) {
                footCard.classList.add("active-route");
            }
            if (btnFoot) {
                btnFoot.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Active Route on Map';
                btnFoot.className = 'btn btn-sm btn-success btn-block mt-3 font-weight-bold';
            }
        } else {
            if (vehicleCard) {
                vehicleCard.classList.add("active-route");
            }
            if (btnVehicle) {
                btnVehicle.innerHTML = '<i class="fas fa-check-circle mr-1"></i> Active Route on Map';
                btnVehicle.className = 'btn btn-sm btn-primary btn-block mt-3 font-weight-bold';
            }
        }

        var destLat = Number(routeOption.destinationLat);
        var destLon = Number(routeOption.destinationLon);
        if (!Number.isFinite(destLat) || !Number.isFinite(destLon)) {
            showRouteErrorModal("The selected route has no valid destination coordinates.");
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

    /**
     * Removes the active evacuation route and resets route controls.
     */
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

        if (footCard) {
            footCard.classList.remove("active-route");
        }
        if (vehicleCard) {
            vehicleCard.classList.remove("active-route");
        }
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

/**
 * Converts a GDACS event code into a readable disaster type.
 *
 * @param {string|null} rawType GDACS event code or name
 * @return {string} readable disaster type
 */
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

/**
 * Selects colors and badge classes for an alert level.
 *
 * @param {string|null} alertLevel GDACS alert level
 * @return {{hexColor: string, badgeClass: string, markerClass: string}} display settings
 */
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

/**
 * Builds the HTML shown when a disaster marker or circle is opened.
 *
 * @param {string} disasterTitle disaster name shown in the popup
 * @param {string} country affected country or countries
 * @param {string} alertLevel disaster alert level
 * @param {Object} alertConfig colors and CSS classes for the alert
 * @param {number} disasterLat disaster latitude
 * @param {number} disasterLon disaster longitude
 * @param {number|null} userLat traveler latitude
 * @param {number|null} userLon traveler longitude
 * @param {number|null} distanceKm distance from traveler to disaster
 * @param {boolean} isWithinDangerZone whether the traveler is within 120 km
 * @return {string} popup HTML
 */
function buildDisasterPopupHtml(disasterTitle, country, alertLevel, alertConfig, disasterLat, disasterLon, userLat, userLon, distanceKm, isWithinDangerZone) {
    var distanceSection = '';

    if (distanceKm != null) {
        distanceSection = '<div class="disaster-popup-distance">' +
            '<strong>Distance from you:</strong> ' +
            '<span class="font-weight-bold ' + (isWithinDangerZone ? 'text-danger' : 'text-dark') + '">' +
                distanceKm.toLocaleString() + ' km' +
            '</span>' +
            (isWithinDangerZone ? ' <span class="badge badge-danger ml-1">Within Danger Zone!</span>' : '') +
        '</div>';
    } else if (userLat != null && userLon != null) {
        distanceSection = '<div class="disaster-popup-distance text-muted">' +
            '<strong>Location:</strong> ' + disasterLat.toFixed(2) + ', ' + disasterLon.toFixed(2) +
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

/**
 * Replaces the disaster layer with markers and danger-radius circles.
 *
 * @param {L.LayerGroup} layerGroup Leaflet layer group to populate
 * @param {Array<Object>} disasters disaster records from the backend
 * @param {number|null} userLat traveler latitude
 * @param {number|null} userLon traveler longitude
 * @return {void}
 */
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

        var distanceKm = (disaster.distanceKm != null) ? disaster.distanceKm : null;
        var isWithinDangerZone = Boolean(disaster.inDangerZone);

        var popupContent = buildDisasterPopupHtml(
            disasterTitle,
            country,
            alertLevel,
            alertConfig,
            lat,
            lon,
            userLat,
            userLon,
            distanceKm,
            isWithinDangerZone
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

/**
 * Updates the sidebar status using proximity information.
 *
 * @param {Object} data dashboard proximity response
 * @param {number|null} userLat traveler latitude
 * @param {number|null} userLon traveler longitude
 * @return {void}
 */
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

    if (data.inDangerZone) {
        alertBox.className = "alert alert-danger shadow-sm mb-4";
        alertTitle.innerHTML = '<i class="fas fa-exclamation-triangle mr-1"></i> Proximity Alert: Near Disaster Zone!';
        alertBody.textContent = data.warning || "You are within a recorded natural disaster danger zone.";
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

/**
 * Loads disaster data, renders the map, and updates the proximity sidebar.
 *
 * @param {L.Map} map Leaflet map instance
 * @param {L.LayerGroup} disasterLayerGroup layer group for disaster overlays
 * @param {number|null} userLat traveler latitude
 * @param {number|null} userLon traveler longitude
 * @param {function(Array<Object>): void} onSuccess callback receiving disasters
 * @return {Promise<void>} promise resolved after the request finishes
 */
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

/**
 * Stops the AI progress timer.
 *
 * @return {void}
 */
function stopAiProgressAnimation() {
    if (aiProgressInterval) {
        clearInterval(aiProgressInterval);
        aiProgressInterval = null;
    }
}

/**
 * Starts the earlier AI progress animation used by the dashboard.
 *
 * @return {void}
 */
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

    /**
     * Renders one progress step in the earlier AI progress animation.
     *
     * @param {number} idx index of the progress step
     * @return {void}
     */
    function applyStep(idx) {
        var s = steps[idx];
        if (stepBadge && s.badge) {
            stepBadge.innerHTML = s.badge;
        }
        if (msgElem && s.msg) {
            msgElem.textContent = s.msg;
        }
        if (barElem && s.pct) {
            barElem.style.width = s.pct + "%";
        }
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

/**
 * Displays the risk result, route cards, checklist, and disclaimer.
 *
 * @param {Object|null} data risk assessment response
 * @return {void}
 */
function renderAiAssessmentView(data) {
    if (!data) {
        return;
    }

    var riskBadge = document.getElementById("aiRiskBadge");
    var toolBadge = document.getElementById("aiToolBadge");
    var riskSummary = document.getElementById("aiRiskSummary");
    var actionText = document.getElementById("aiActionText");
    var safeZoneSection = document.getElementById("aiSafeZoneSection");
    var escapeRoutesSection = document.getElementById("aiEscapeRoutesSection");
    var checklistContainer = document.getElementById("aiChecklistContainer");
    var footCard = document.getElementById("cardFootRoute");
    var vehicleCard = document.getElementById("cardVehicleRoute");
    var footOption = isRenderableRouteOption(data.footOption) ? data.footOption : null;
    var vehicleOption = isRenderableRouteOption(data.vehicleOption) ? data.vehicleOption : null;

    var level = String(data.riskLevel || "LOW").toUpperCase();
    if (riskBadge) {
        riskBadge.textContent = level;
        riskBadge.className = "badge px-2 py-1 font-weight-bold " + (
            level === "CRITICAL" ? "badge-risk-critical" :
            level === "HIGH" ? "badge-risk-high" :
            level === "MODERATE" ? "badge-risk-moderate" : "badge-risk-low"
        );
    }

    if (toolBadge) {
        toolBadge.textContent = data.modelUsed || "Gemini AI Agent";
    }

    if (riskSummary) {
        riskSummary.textContent = data.riskSummary || "Assessment complete.";
    }

    if (actionText) {
        actionText.textContent = data.recommendedAction || "Monitor local advisories.";
    }

    // Toggle between safe zone confirmation and evacuation routes
    if (data.inSafeZone) {
        setElementStyle(safeZoneSection, "display", "block");
        setElementStyle(escapeRoutesSection, "display", "none");
    } else {
        setElementStyle(safeZoneSection, "display", "none");
        if (footCard && footCard.parentElement) {
            footCard.parentElement.style.display = footOption ? "" : "none";
        }
        if (vehicleCard && vehicleCard.parentElement) {
            vehicleCard.parentElement.style.display = vehicleOption ? "" : "none";
        }
        if (escapeRoutesSection) {
            escapeRoutesSection.style.display = footOption || vehicleOption ? "flex" : "none";
        }

        if (footOption) {
            var footTitle = document.getElementById("footOptionTitle");
            var footBadge = document.getElementById("footDistanceBadge");
            var footDest = document.getElementById("footDestination");
            var footRoute = document.getElementById("footRoute");
            var footTips = document.getElementById("footTips");

            updateRouteOptionCard(footOption, {
                title: footTitle,
                badge: footBadge,
                destination: footDest,
                route: footRoute,
                tips: footTips
            }, {
                title: "Option 1: Nearby Shelter (On Foot)",
                destination: "Designated safe shelter",
                route: "Proceed towards destination.",
                tips: "Stay alert and follow emergency instructions."
            });
        }

        if (vehicleOption) {
            var vehTitle = document.getElementById("vehicleOptionTitle");
            var vehBadge = document.getElementById("vehicleDistanceBadge");
            var vehDest = document.getElementById("vehicleDestination");
            var vehRoute = document.getElementById("vehicleRoute");
            var vehTips = document.getElementById("vehicleTips");

            updateRouteOptionCard(vehicleOption, {
                title: vehTitle,
                badge: vehBadge,
                destination: vehDest,
                route: vehRoute,
                tips: vehTips
            }, {
                title: "Option 2: Regional Evacuation (By Vehicle)",
                destination: "Designated regional haven",
                route: "Follow primary evacuation corridors.",
                tips: "Keep fuel tank full and check road passability."
            });
        }
    }

    // Format emergency checklist pills
    if (checklistContainer) {
        checklistContainer.innerHTML = "";
        if (Array.isArray(data.emergencyChecklist) && data.emergencyChecklist.length > 0) {
            data.emergencyChecklist.forEach(function (item) {
                var pill = document.createElement("span");
                pill.className = "ai-checklist-pill";
                pill.innerHTML = '<i class="fas fa-check text-success mr-1"></i>' + item;
                checklistContainer.appendChild(pill);
            });
        }

    }

    // Display AI disclaimer
    var disclaimerContainer = document.getElementById("aiDisclaimerContainer");
    var disclaimerText = document.getElementById("aiDisclaimerText");
    if (disclaimerContainer) {
        disclaimerContainer.style.display = "block";
        if (disclaimerText && data.aiDisclaimer) {
            disclaimerText.textContent = data.aiDisclaimer;
        }
    }
}

// Populate either route card using the same fallback and DOM-update rules.
/**
 * Fills a route card using its option data and fallback text.
 *
 * @param {Object} option route option returned by the backend
 * @param {Object} elements DOM elements belonging to the card
 * @param {Object} defaults fallback text for missing option values
 * @return {void}
 */
function updateRouteOptionCard(option, elements, defaults) {
    setElementText(elements.title, "textContent", option.title || defaults.title);
    setElementText(elements.badge, "textContent", option.estimatedDistance || "N/A");
    setElementText(elements.destination, "textContent", option.destination || defaults.destination);
    setElementText(elements.route, "textContent", option.routeAdvice || defaults.route);
    setElementText(elements.tips, "textContent", option.safetyTips || defaults.tips);
}

/**
 * Checks whether a route has valid destination coordinates.
 *
 * @param {Object|null} routeOption route option to validate
 * @return {boolean} true when both coordinates are within valid ranges
 */
function isRenderableRouteOption(routeOption) {
    if (!routeOption) {
        return false;
    }

    var destinationLat = Number(routeOption.destinationLat);
    var destinationLon = Number(routeOption.destinationLon);
    return Number.isFinite(destinationLat)
        && destinationLat >= -90
        && destinationLat <= 90
        && Number.isFinite(destinationLon)
        && destinationLon >= -180
        && destinationLon <= 180;
}

/**
 * Shows the route error modal, or logs the error if the modal is unavailable.
 *
 * @param {string} message message shown to the traveler
 * @return {void}
 */
function showRouteErrorModal(message) {
    var modal = document.getElementById("routeErrorModal");
    var modalMessage = document.getElementById("routeErrorModalMessage");

    if (!modal || typeof $ !== "function") {
        console.error("Route error modal is unavailable:", message);
        return;
    }

    if (modalMessage) {
        modalMessage.textContent = message;
    }
    $(modal).modal("show");
}

/**
 * Requests an AI risk assessment and updates the loading/result/error UI.
 *
 * @param {number|null} userLat traveler latitude
 * @param {number|null} userLon traveler longitude
 * @return {Promise<void>} promise resolved after the assessment is handled
 */
async function fetchAiRiskAssessment(userLat, userLon) {
    var placeholderSection = document.getElementById("aiPlaceholderSection");
    var loadingSection = document.getElementById("aiLoadingSection");
    var contentSection = document.getElementById("aiContentSection");
    var errorSection = document.getElementById("aiErrorSection");
    var errorMessageElem = document.getElementById("aiErrorMessage");
    var refreshBanner = document.getElementById("aiRefreshingBanner");
    var btnRefresh = document.getElementById("btnRefreshAi");
    var btnGenerate = document.getElementById("btnGenerateAi");

    // Show full loading view for both initial generation and refresh
    setElementStyle(placeholderSection, "display", "none");
    setElementStyle(contentSection, "display", "none");
    setElementStyle(errorSection, "display", "none");
    setElementStyle(refreshBanner, "display", "none");
    setElementStyle(loadingSection, "display", "block");

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
        currentAiRequestId = "req_" + Date.now() + "_" + Math.random().toString(36).substring(2, 9);
        var params = new URLSearchParams();
        params.append("requestId", currentAiRequestId);
        if (userLat != null && userLon != null) {
            params.append("userLat", userLat);
            // The risk-assessment endpoint uses userLong for longitude.
            params.append("userLong", userLon);
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
        setElementStyle(barElem, "width", "100%");

        latestAiAssessment = data;

        if (typeof clearEvacuationRoute === "function") {
            clearEvacuationRoute();
        }

        // Configure frontend view and display formatting from assessment data
        renderAiAssessmentView(data);

        setElementStyle(loadingSection, "display", "none");
        setElementStyle(contentSection, "display", "block");

    } catch (err) {
        if (err.name === "AbortError") {
            return;
        }

        console.error("AI risk assessment error:", err);
        stopAiProgressAnimation();

        setElementStyle(loadingSection, "display", "none");
        setElementStyle(contentSection, "display", "none");
        setElementStyle(placeholderSection, "display", "none");
        setElementStyle(errorSection, "display", "block");
        
        if (errorMessageElem) {
            errorMessageElem.textContent = err.message || "AI assessment could not be generated at this time. Please retry later.";
        }
    } finally {
        currentAiRequestId = null;
        stopAiProgressAnimation();
        var currentRefreshBanner = document.getElementById("aiRefreshingBanner");
        setElementStyle(currentRefreshBanner, "display", "none");

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

/**
 * Displays staged progress while the AI assessment is being generated.
 *
 * @return {void}
 */
function startControlledProgress() {
    stopAiProgressAnimation();
    var barElem = document.getElementById("aiProgressBar");
    var msgElem = document.getElementById("aiProgressMessage");
    var stepBadge = document.getElementById("aiStepBadge");

    var stages = [
        {
            pct: 35,
            badge: '<i class="fas fa-satellite-dish mr-1"></i> Telemetry Analysis',
            msg: 'Analyzing traveler GPS & active disaster danger perimeters...'
        },
        {
            pct: 65,
            badge: '<i class="fas fa-shield-alt mr-1"></i> Shelter & Routing Evaluation',
            msg: 'Evaluating closest safe shelters & navigation escape corridors...'
        },
        {
            pct: 88,
            badge: '<i class="fas fa-robot mr-1"></i> Gemini AI Synthesis',
            msg: 'Synthesizing situational safety assessment & personal recommendations...'
        }
    ];

    var stageIndex = 0;
    /**
     * Applies one staged progress update to the assessment UI.
     *
     * @param {number} idx index of the current progress stage
     * @return {void}
     */
    function applyStage(idx) {
        var s = stages[idx];
        setElementStyle(barElem, "width", s.pct + "%");
        setElementText(msgElem, "textContent", s.msg);
        setElementText(stepBadge, "innerHTML", s.badge);
    }

    applyStage(0);

    aiProgressInterval = setInterval(function () {
        stageIndex++;
        if (stageIndex < stages.length) {
            applyStage(stageIndex);
        } else {
            if (barElem) {
                var cur = parseInt(barElem.style.width) || 88;
                if (cur < 95) {
                    barElem.style.width = (cur + 2) + "%";
                }
            }
        }
    }, 1200);
}
