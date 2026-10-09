/**
 * Handling the itinerary management
 * Make API call to retrieve itinerary records belonging to user and render it
 * Allow user to create itinerary and make API call to save the result
 * Allow user to delete itinerary and prompt comfirmation before deletion
 * Allow user to update itinerary and make API call to update the record
 */

/**
 * Retrieve all itinerary record and set up create button
 */
document.addEventListener("DOMContentLoaded", async function () {
    const userId = 1; // Temporary test ID until login is implemented

    // When create itinerary button is clicked, show create pop up modal for user to add new itinerary record
    document.getElementById('createItineraryBtn').addEventListener('click', function () {
        // Clear the hidden ID to indicate create mode
        document.getElementById('itineraryId').value = "";

        // Clear input fields
        document.getElementById('itineraryName').value = "";
        document.getElementById('itineraryStartDate').value = "";
        document.getElementById('itineraryEndDate').value = "";

        // Clear general error alerts
        document.getElementById('itineraryModalErrorAlert').classList.add('d-none');
        document.querySelectorAll('.form-control').forEach(el => el.classList.remove('is-invalid'));

        // Update modal title and show modal
        document.getElementById('itineraryModalTitle').textContent = "Create Itinerary";
        $('#itineraryModal').modal('show');
    });

    // Load all itinerary that belongs to user and display
    try {
        const response = await fetch(`/api/itinerary/all/${userId}`);

        if (response.ok) {
            const itineraries = await response.json();
            renderItineraries(itineraries);
        } else {
            console.error("Failed to load itineraries");
        }

    } catch (err) {
        console.error("Error connecting to API:", err);
    }
});

/**
 * Create or Update itinerary and validation of form fields
 */
document.getElementById('itineraryForm').addEventListener('submit', async function (event) {
    event.preventDefault();

    // Retrieve hidden itineraryId value if any to detect create or edit mode
    const itineraryId = document.getElementById('itineraryId').value;
    const isEditing = itineraryId !== "";

    // Retrieve user input values
    const userId = 1;
    const name = document.getElementById('itineraryName').value;
    const startDate = document.getElementById('itineraryStartDate').value;
    const endDate = document.getElementById('itineraryEndDate').value;

    // Validate itinerary form fields
    if (!validateForm(name, startDate, endDate)) {
        return;
    }

    // Set up json ready data to send to API
    const itineraryData = {
        userId: userId,
        itineraryName: name,
        startDate: startDate,
        endDate: endDate
    };

    // Set up the corresponding API to call and method
    const url = isEditing ? `/api/itinerary/${itineraryId}?userId=${userId}` : `/api/itinerary/?userId=${userId}`;
    const method = isEditing ? 'PUT' : 'POST';

    try {
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(itineraryData)
        });

        if (response.ok) {
            // When success create or update of itinerary
            // Hide the pop up modal
            $('#itineraryModal').modal('hide');

            // Craft success alert message
            const successCreate = "Sucessfully created itinerary: " + name;
            const successUpdate = "Successfully update itinerary: " + name;

            // Show the success alert banner
            const alertBox = document.getElementById('successAlert');
            document.getElementById('successAlertMessage').textContent = isEditing ? successUpdate : successCreate;
            alertBox.classList.remove('d-none');

            // Wait 1.2 seconds for user to view success alert before page reload to show updated itinerary records
            setTimeout(() => {
                location.reload();
            }, 1200);
        } else {
            const errorAlert = document.getElementById('itineraryModalErrorAlert');
            errorAlert.textContent = "Failed to save itinerary. If issue persists, please contact our helpdesk.";
            errorAlert.classList.remove('d-none');
        }
    } catch (err) {
        console.error("Save error:", err);
    }
});

/**
 * Validation for create or edit itinerary form submission
 * @param itineraryName 
 * @param itineraryStartDate 
 * @param itineraryEndDate 
 * @returns boolean if success validation
 */
function validateForm(itineraryName, itineraryStartDate, itineraryEndDate) {
    // Tracking if form fields are all validated successfully
    let isValid = true;

    // Clear previous inline error states
    document.querySelectorAll('.form-control').forEach(el => el.classList.remove('is-invalid'));
    document.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');

    // Validate Itinerary Name
    if (!itineraryName || itineraryName.trim() === "") {
        document.getElementById('itineraryName').classList.add('is-invalid');
        document.getElementById('itineraryNameError').textContent = "Itinerary name cannot be empty.";
        isValid = false;
    }

    // Validate Start Date
    if (!itineraryStartDate) {
        document.getElementById('itineraryStartDate').classList.add('is-invalid');
        document.getElementById('itineraryStartDateError').textContent = "Start date is required.";
        isValid = false;
    }

    // Validate End Date
    if (!itineraryEndDate) {
        document.getElementById('itineraryEndDate').classList.add('is-invalid');
        document.getElementById('itineraryEndDateError').textContent = "End date is required.";
        isValid = false;
    } 
    // Ensure end date cannot be before start date
    else if (itineraryStartDate && itineraryEndDate && new Date(itineraryEndDate) < new Date(itineraryStartDate)) {
        document.getElementById('itineraryEndDate').classList.add('is-invalid');
        document.getElementById('itineraryEndDateError').textContent = "End date cannot be earlier than start date.";
        isValid = false;
    }

    return isValid;
}

/* Render the itinerary in frontend */
function renderItineraries(itineraries) {
    const allItineraries = document.getElementById("showItinerary");
    const itineraryTemplate = document.getElementById("itineraryCard");

    // Clear existing itinerary display format if any
    allItineraries.innerHTML = "";

    // When traveler has no existing itineraries
    if (itineraries.length === 0) {
        allItineraries.innerHTML = `<div class="col-12 text-center text-muted py-4">No itineraries found.</div>`;
        return;
    }

    // When traveler has existing itineraries, clone each template for each itinerary
    itineraries.forEach(itinerary => {
        // Clone the template structure from the HTML file
        const itineraryDetails = itineraryTemplate.content.cloneNode(true);
        // Set the data attribute on the outer wrapper using the itinerary ID
        const itineraryCardId = itineraryDetails.querySelector("[data-card-id]");
        itineraryCardId.setAttribute("data-card-id", itinerary.itineraryId);

        // Populate the itinerary cards that user has
        itineraryDetails.querySelector(".itinerary-name").textContent = itinerary.itineraryName;
        itineraryDetails.querySelector(".itinerary-start-date").textContent = itinerary.startDate;
        itineraryDetails.querySelector(".itinerary-end-date").textContent = itinerary.endDate;

        // Attached detailed itinerary details link, to redirect user to detailed itinerary page when user selects an itinerary
        itineraryDetails.querySelector(".itinerary-details").href = `/itinerary/view/${itinerary.itineraryId}`;

        // Attached edit button to this itinerary id
        const editBtn = itineraryDetails.querySelector(".edit-itinerary-btn");
        editBtn.addEventListener('click', async function (event) {
            event.preventDefault();

            try {
                // Call GET by itineraryId method to retrieve this selected itinerary record
                const response = await fetch(`/api/itinerary/${itinerary.itineraryId}?userId=1`);

                // Check response received
                if (response.ok) {
                    // Successfully retrieved itinerary records
                    const data = await response.json();

                    // Populate edit modal fields with current value
                    document.getElementById('itineraryId').value = data.itineraryId;
                    document.getElementById('itineraryName').value = data.itineraryName;
                    document.getElementById('itineraryStartDate').value = data.startDate;
                    document.getElementById('itineraryEndDate').value = data.endDate;

                    document.getElementById('itineraryModalErrorAlert').classList.add('d-none');

                    // Show the pop up edit modal
                    $('#itineraryModal').modal('show');
                } else {
                    // When unable to retrieve itinerary records successfully
                    alert("Failed to load itinerary details for editing.");
                }
            } catch (err) {
                // Other unexpected errors
                console.error("Error fetching itinerary for edit:", err);
            }
        });

        // Attached delete button linked to this itinerary id
        const deleteBtn = itineraryDetails.querySelector(".delete-itinerary-btn");
        deleteBtn.addEventListener('click', function (event) {
            event.preventDefault();
            currentDeleteId = itinerary.itineraryId;

            // Edit modal text
            document.getElementById('confirmDeleteModalTitle').innerText = "Delete Itinerary";
            document.getElementById('confirmDeleteModalBody').innerHTML =
                `Are you sure you want to delete <strong>"${itinerary.itineraryName}"</strong>? This cannot be undone.`;

            // Show the pop up delete modal
            $('#confirmDeleteModal').modal('show');
        });

        // Add the populated card details with div block containing all itinerary for display in frontend
        allItineraries.appendChild(itineraryDetails);
    });
}

/**
 * Delete itinerary confirmation
 */
let currentDeleteId = null;

document.getElementById('confirmDeleteModalBtn').addEventListener('click', async function () {
    if (!currentDeleteId) return;

    try {
        // Call DELETE by itineraryId method to delete this specific itinerary record
        const response = await fetch(`/api/itinerary/${currentDeleteId}?userId=${1}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            const successMessage = await response.text();

            // Delete the itinerary card from the frontend
            const itineraryCard = document.querySelector(`[data-card-id="${currentDeleteId}"]`);
            if (itineraryCard) {
                itineraryCard.remove();
            }

            // Close delete confirmation modal
            $('#confirmDeleteModal').modal('hide');

            // Show the success alert banner
            const alertBox = document.getElementById('successAlert');
            document.getElementById('successAlertMessage').textContent = successMessage;
            alertBox.classList.remove('d-none');
        } else {
            // When unable to find the corresponding card display in frontend
            alert("Failed to delete itinerary.");
        }
    } catch (error) {
        console.error("Delete error:", error);
    } finally {
        currentDeleteId = null;
    }
});