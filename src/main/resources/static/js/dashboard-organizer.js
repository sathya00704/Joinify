// Organizer Dashboard JavaScript
class OrganizerDashboard {
    constructor() {
        this.currentUser = null;
        this.myEvents = [];
        this.allAttendees = [];
        this.charts = {};
        this.init();
    }

    async init() {
        try {
            await this.checkAuthentication();
            await this.loadDashboardData();
            this.setupEventListeners();
            this.showSection('overview');
        } catch (error) {
            //console.error('Dashboard initialization failed:', error);
            showToast('Failed to load dashboard', 'error');
        }
    }

    async checkAuthentication() {
        await authManager.checkAuthStatus();
        if (!authManager.isLoggedIn() || authManager.getCurrentUserRole() !== 'ORGANIZER') {
            window.location.href = 'index.html';
            return;
        }
        this.currentUser = authManager.currentUser;
        document.getElementById('user-name').textContent = this.currentUser.username;
    }

    async loadDashboardData() {
        showLoading();
        try {
            const [myEvents, userStats] = await Promise.all([
                api.getMyEvents(),
                api.getUserStats()
            ]);

            this.myEvents = myEvents || [];

            await this.updateStats();
            this.loadRecentEvents();
            this.loadMyEvents();
            this.loadAttendeeEventSelect();
            this.loadReminderEventSelect();
            this.initializeCharts();

            // NEW: Check for capacity alerts
            await this.checkCapacityAlerts();
        } catch (error) {
            //console.error('Error loading dashboard data:', error);
            showToast('Failed to load dashboard data', 'error');
        } finally {
            hideLoading();
        }
    }

    async updateStats() {
        const now = new Date();
        const upcomingEvents = this.myEvents.filter(event => new Date(event.dateTime) > now);

        // Get total attendees across all events
        let totalAttendees = 0;
        let totalCapacity = 0;

        for (const event of this.myEvents) {
            try {
                const rsvpCount = await api.request(`/rsvp/event/${event.id}/count`);
                totalAttendees += rsvpCount.confirmed || 0;
                totalCapacity += event.maxCapacity;
            } catch (error) {
                //console.error('Error getting RSVP count for event:', event.id);
            }
        }

        const avgAttendance = totalCapacity > 0 ? Math.round((totalAttendees / totalCapacity) * 100) : 0;

        document.getElementById('total-events').textContent = this.myEvents.length;
        document.getElementById('upcoming-events').textContent = upcomingEvents.length;
        document.getElementById('total-attendees').textContent = totalAttendees;
        document.getElementById('avg-attendance').textContent = avgAttendance + '%';
    }

    loadRecentEvents() {
        const now = new Date();

        // Filter and sort upcoming events
        const upcomingEvents = this.myEvents
            .filter(event => new Date(event.dateTime) > now)
            .sort((a, b) => new Date(a.dateTime) - new Date(b.dateTime));

        const tbody = document.getElementById('recent-events-tbody');

        if (upcomingEvents.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No upcoming events</td></tr>';
            return;
        }

        // Show only the FIRST upcoming event
        const nextEvent = upcomingEvents[0];
        const eventDate = new Date(nextEvent.dateTime);
        const status = 'upcoming';

        tbody.innerHTML = `
            <tr>
                <td>${nextEvent.title}</td>
                <td>${this.formatDateTime(nextEvent.dateTime)}</td>
                <td id="attendees-${nextEvent.id}">Loading...</td>
                <td><span class="event-status ${status}">${status}</span></td>
                <td>
                    <button class="btn btn-sm btn-secondary" onclick="organizerDashboard.viewEventDetails(${nextEvent.id})">
                        View
                    </button>
                    <button class="btn btn-sm btn-primary" onclick="organizerDashboard.editEvent(${nextEvent.id})">
                        Edit
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="organizerDashboard.deleteEvent(${nextEvent.id})">
                        Delete
                    </button>
                </td>
            </tr>
            ${upcomingEvents.length > 1 ?
                `<tr>
                    <td colspan="5" style="text-align: center; padding: 15px; color: #666; font-style: italic;">
                        <i class="fas fa-info-circle"></i>
                        You have ${upcomingEvents.length - 1} more upcoming event${upcomingEvents.length > 2 ? 's' : ''}.
                        <a href="#" onclick="showSection('events')" style="color: #007bff; text-decoration: none;">View all events</a>
                    </td>
                </tr>` : ''
            }
            <tr id="capacity-alert-${nextEvent.id}" style="display: none;">
                <td colspan="5" class="capacity-alert-row">
                    <!-- Capacity alert will be inserted here -->
                </td>
            </tr>
        `;

        // Load attendee count and capacity alert for the next event
        this.loadAttendeeCountWithAlert(nextEvent);
    }

    // Enhanced method to load attendee count with capacity alerts
    async loadAttendeeCountWithAlert(event) {
        try {
            const rsvpCount = await api.request(`/rsvp/event/${event.id}/count`);
            const currentRSVPs = rsvpCount.confirmed || 0;
            const maxCapacity = event.maxCapacity;
            const availableSpots = maxCapacity - currentRSVPs;

            const cell = document.getElementById(`attendees-${event.id}`);
            if (cell) {
                cell.innerHTML = `${currentRSVPs}/${maxCapacity}`;

                // Color coding
                if (currentRSVPs >= maxCapacity) {
                    cell.style.color = '#dc3545';
                    cell.style.fontWeight = 'bold';
                } else if (currentRSVPs >= maxCapacity * 0.8) {
                    cell.style.color = '#ffc107';
                    cell.style.fontWeight = 'bold';
                }
            }

            // Show capacity alert if needed
            const alertRow = document.getElementById(`capacity-alert-${event.id}`);
            if (alertRow) {
                if (currentRSVPs >= maxCapacity) {
                    alertRow.style.display = 'table-row';
                    alertRow.innerHTML = `
                        <td colspan="5" class="capacity-alert-row seats-filled">
                            <div class="alert alert-danger">
                                <i class="fas fa-exclamation-triangle"></i>
                                <strong>SEATS FILLED!</strong> Your event "${event.title}" has reached maximum capacity (${maxCapacity} attendees).
                                <button class="btn btn-sm btn-warning ml-2" onclick="organizerDashboard.closeRegistration(${event.id})">
                                    Close Registration
                                </button>
                            </div>
                        </td>
                    `;
                } else if (currentRSVPs >= maxCapacity * 0.9) {
                    alertRow.style.display = 'table-row';
                    alertRow.innerHTML = `
                        <td colspan="5" class="capacity-alert-row almost-full">
                            <div class="alert alert-warning">
                                <i class="fas fa-exclamation-circle"></i>
                                <strong>Almost Full!</strong> Only ${availableSpots} seat${availableSpots !== 1 ? 's' : ''} remaining for "${event.title}".
                            </div>
                        </td>
                    `;
                }
            }

        } catch (error) {
            //console.error('Error loading attendee count:', error);
            const cell = document.getElementById(`attendees-${event.id}`);
            if (cell) {
                cell.textContent = `0/${event.maxCapacity}`;
            }
        }
    }


    // Helper method to load attendee count
    async loadAttendeeCount(event) {
        try {
            const rsvpCount = await api.request(`/rsvp/event/${event.id}/count`);
            const cell = document.getElementById(`attendees-${event.id}`);
            if (cell) {
                cell.textContent = `${rsvpCount.confirmed || 0}/${event.maxCapacity}`;
            }
        } catch (error) {
            //console.error('Error loading attendee count:', error);
            const cell = document.getElementById(`attendees-${event.id}`);
            if (cell) {
                cell.textContent = `0/${event.maxCapacity}`;
            }
        }
    }

    loadMyEvents() {
        const container = document.getElementById('events-grid');
        if (this.myEvents.length === 0) {
            container.innerHTML = '<p class="no-data">No events created yet</p>';
            return;
        }

        container.innerHTML = this.myEvents.map(event => {
            const now = new Date();
            const eventDate = new Date(event.dateTime);
            const status = eventDate > now ? 'upcoming' : 'past';

            // Render event image if available
            const eventImage = event.imageUrl ?
                `<div class="event-image-container">
                    <img src="${event.imageUrl}" alt="${event.title}" class="event-image"
                         onerror="this.style.display='none';">
                </div>` : '';

            // Render event fee
            const eventFee = event.fee && event.fee > 0 ?
                `<span class="event-fee">Rs. ${parseFloat(event.fee).toFixed(2)}</span>` :
                '<span class="event-fee free">Free</span>';

            // Registration status badge
            const registrationStatus = event.registrationStatus || 'OPEN';
            const statusBadge = this.getRegistrationStatusBadge(registrationStatus);

            // NEW: Capacity status indicator
            const capacityStatus = this.getCapacityStatusBadge(event);

            // Registration control buttons (only for upcoming events)
            const registrationControls = eventDate > now ? this.getRegistrationControlButtons(event.id, registrationStatus) : '';

            return `
                <div class="event-card">
                    ${eventImage}
                    <div class="event-card-header">
                        <div class="event-card-title">${event.title}</div>
                        <div class="event-card-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                            <span><i class="fas fa-users"></i> <span id="capacity-${event.id}">Loading...</span>/${event.maxCapacity} capacity</span>
                            ${eventFee}
                            ${statusBadge}
                            ${capacityStatus}
                        </div>
                        <div class="event-card-description">${event.description || 'No description available'}</div>
                    </div>
                    <div class="event-card-footer">
                        <div class="event-status ${status}">${status}</div>
                        <div class="event-actions">
                            <button class="btn btn-sm btn-secondary" onclick="organizerDashboard.viewEventDetails(${event.id})">
                                Details
                            </button>
                            <button class="btn btn-sm btn-success" onclick="organizerDashboard.downloadAttendeesCSV(${event.id}, '${event.title.replace(/'/g, "\\'")}')">
                                <i class="fas fa-download"></i> Export
                            </button>
                            ${registrationControls}
                            <button class="btn btn-sm btn-primary" onclick="organizerDashboard.editEvent(${event.id})">
                                Edit
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="organizerDashboard.deleteEvent(${event.id})">
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        // Load capacity data for each event
        this.loadCapacityData();
    }

    // NEW: Method to get capacity status badge
    getCapacityStatusBadge(event) {
        // This will be populated after loading capacity data
        return `<span class="capacity-status" id="capacity-status-${event.id}"></span>`;
    }

    // NEW: Load capacity data for all events
    async loadCapacityData() {
        for (const event of this.myEvents) {
            try {
                const rsvpCount = await api.getRSVPCount(event.id);
                const currentRSVPs = rsvpCount.confirmed || 0;
                const maxCapacity = event.maxCapacity;
                const availableSpots = maxCapacity - currentRSVPs;

                // Update capacity display
                const capacityElement = document.getElementById(`capacity-${event.id}`);
                if (capacityElement) {
                    capacityElement.textContent = currentRSVPs;

                    // Add color coding based on capacity
                    if (currentRSVPs >= maxCapacity) {
                        capacityElement.style.color = '#dc3545'; // Red for full
                        capacityElement.style.fontWeight = 'bold';
                    } else if (currentRSVPs >= maxCapacity * 0.8) {
                        capacityElement.style.color = '#ffc107'; // Yellow for nearly full
                        capacityElement.style.fontWeight = 'bold';
                    } else {
                        capacityElement.style.color = '#28a745'; // Green for available
                    }
                }

                // Update capacity status badge
                const statusElement = document.getElementById(`capacity-status-${event.id}`);
                if (statusElement) {
                    if (currentRSVPs >= maxCapacity) {
                        statusElement.innerHTML = `
                            <span class="capacity-full">
                                <i class="fas fa-exclamation-triangle"></i> SEATS FILLED
                            </span>`;
                    } else if (currentRSVPs >= maxCapacity * 0.9) {
                        statusElement.innerHTML = `
                            <span class="capacity-almost-full">
                                <i class="fas fa-exclamation-circle"></i> ${availableSpots} SEATS LEFT
                            </span>`;
                    } else if (currentRSVPs >= maxCapacity * 0.8) {
                        statusElement.innerHTML = `
                            <span class="capacity-filling">
                                <i class="fas fa-info-circle"></i> ${availableSpots} SEATS AVAILABLE
                            </span>`;
                    }
                }

            } catch (error) {
                //console.error(`Error loading capacity for event ${event.id}:`, error);
            }
        }
    }


    // Helper method for registration status badge
    getRegistrationStatusBadge(status) {
        const statusConfig = {
            'OPEN': { class: 'registration-open', text: 'RSVPs Open', icon: 'fa-check-circle' },
            'CLOSED': { class: 'registration-closed', text: 'RSVPs Closed', icon: 'fa-times-circle' },
            'SUSPENDED': { class: 'registration-suspended', text: 'RSVPs Suspended', icon: 'fa-pause-circle' }
        };

        const config = statusConfig[status] || statusConfig['OPEN'];
        return `<span class="registration-status ${config.class}">
                    <i class="fas ${config.icon}"></i> ${config.text}
                </span>`;
    }

    // Helper method for registration control buttons
    getRegistrationControlButtons(eventId, status) {
        if (status === 'OPEN') {
            return `<button class="btn btn-sm btn-warning" onclick="organizerDashboard.closeRegistration(${eventId})">
                        <i class="fas fa-lock"></i> Close Registration
                    </button>`;
        } else {
            return `<button class="btn btn-sm btn-success" onclick="organizerDashboard.openRegistration(${eventId})">
                        <i class="fas fa-unlock"></i> Open Registration
                    </button>`;
        }
    }

    // Close registration method
    async closeRegistration(eventId) {
        const confirmed = confirm('Are you sure you want to close registration for this event? Attendees will no longer be able to register.');
        if (!confirmed) return;

        try {
            showLoading();
            const response = await api.request(`/events/${eventId}/close-registration`, {
                method: 'PUT'
            });

            showToast(response.message, 'success');
            await this.loadDashboardData();
        } catch (error) {
            //console.error('Error closing registration:', error);
            showToast(error.message || 'Failed to close registration', 'error');
        } finally {
            hideLoading();
        }
    }

    // Open registration method
    async openRegistration(eventId) {
        const confirmed = confirm('Are you sure you want to open registration for this event?');
        if (!confirmed) return;

        try {
            showLoading();
            const response = await api.request(`/events/${eventId}/open-registration`, {
                method: 'PUT'
            });

            showToast(response.message, 'success');
            await this.loadDashboardData();
        } catch (error) {
            //console.error('Error opening registration:', error);
            showToast(error.message || 'Failed to open registration', 'error');
        } finally {
            hideLoading();
        }
    }


    loadAttendeeEventSelect() {
        const select = document.getElementById('attendee-event-select');
        select.innerHTML = '<option value="">Select an event</option>' +
            this.myEvents.map(event =>
                `<option value="${event.id}">${event.title}</option>`
            ).join('');

        select.addEventListener('change', (e) => {
            if (e.target.value) {
                this.loadEventAttendees(e.target.value);
            } else {
                document.getElementById('attendees-container').innerHTML = '<p class="no-data">Select an event to view attendees</p>';
            }
        });
    }

    async loadEventAttendees(eventId) {
        try {
            showLoading();
            const attendees = await api.getEventAttendees(eventId);
            const container = document.getElementById('attendees-container');

            if (attendees.length === 0) {
                container.innerHTML = '<p class="no-data">No attendees for this event</p>';
                return;
            }

            container.innerHTML = `
                <div class="attendees-list">
                    ${attendees.map(attendee => `
                        <div class="attendee-item">
                            <div class="attendee-info">
                                <h4>${attendee.username}</h4>
                                <p>${attendee.email}</p>
                            </div>
                            <div class="attendee-actions">
                                <button class="btn btn-sm btn-secondary" onclick="organizerDashboard.contactAttendee('${attendee.email}')">
                                    Contact
                                </button>
                            </div>
                        </div>
                    `).join('')}
                </div>
            `;
        } catch (error) {
            //console.error('Error loading attendees:', error);
            showToast('Failed to load attendees', 'error');
        } finally {
            hideLoading();
        }
    }

    // Simplified createEvent method without validation
    async createEvent() {
        const form = document.getElementById('create-event-form');
        const formData = new FormData(form);

        const eventData = {
            title: formData.get('title'),
            description: formData.get('description'),
            dateTime: formData.get('dateTime'),
            location: formData.get('location'),
            maxCapacity: parseInt(formData.get('maxCapacity')),
            imageUrl: formData.get('imageUrl') || null,
            fee: formData.get('fee') ? parseFloat(formData.get('fee')) : 0.00
        };

        try {
            showLoading();
            await api.createEvent(eventData);
            showToast('Event created successfully!', 'success');
            form.reset();
            await this.loadDashboardData();
            this.showSection('events');
        } catch (error) {
            //console.error('Error creating event:', error);
            // The error message from GlobalExceptionHandler will be displayed
            showToast(error.message || 'Failed to create event', 'error');
        } finally {
            hideLoading();
        }
    }


    // Enhanced URL validation for images (removed Google Drive)
    isValidImageUrl(url) {
        // If URL is empty, null, or undefined, it's valid (optional field)
        if (!url || url.trim() === '') {
            return true;
        }

        try {
            const urlObj = new URL(url);

            // Check for valid protocols
            if (!['http:', 'https:'].includes(urlObj.protocol)) {
                return false;
            }

            // Check for image file extensions
            const imageExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp', '.svg', '.tiff', '.ico'];
            const pathname = url.toLowerCase();
            const hasImageExtension = imageExtensions.some(ext => pathname.includes(ext));

            // Check for common image hosting services
            const imageHosts = [
                'imgur.com', 'flickr.com', 'unsplash.com', 'pexels.com', 'pixabay.com',
                'cloudinary.com', 'amazonaws.com', 'googleusercontent.com', 'github.com',
                'githubusercontent.com', 'wikimedia.org', 'wordpress.com'
            ];
            const isImageHost = imageHosts.some(host => url.includes(host));

            return hasImageExtension || isImageHost;

        } catch (error) {
            // If URL constructor throws an error, it's not a valid URL
            return false;
        }
    }


    // Validate fee input
    validateFee(fee) {
        // If fee is empty, null, or undefined, it's valid (defaults to 0.00)
        if (fee === null || fee === undefined || fee === '' || fee.trim() === '') {
            return true;
        }

        const numFee = parseFloat(fee);
        if (isNaN(numFee) || numFee < 0) {
            return false;
        }

        // Check for reasonable maximum (e.g., Rs. 100,000)
        if (numFee > 100000) {
            return false;
        }

        // Check for too many decimal places
        const decimalPlaces = (fee.toString().split('.')[1] || '').length;
        if (decimalPlaces > 2) {
            return false;
        }

        return true;
    }


    // Format fee for display
    formatFee(fee) {
        if (!fee || fee === 0) return 'Free';
        return `Rs. ${parseFloat(fee).toFixed(2)}`;
    }

    async editEvent(eventId) {
        try {
            const event = await api.getEventById(eventId);

            // Populate form with event data
            document.getElementById('event-title').value = event.title;
            document.getElementById('event-description').value = event.description || '';
            document.getElementById('event-location').value = event.location;
            document.getElementById('event-capacity').value = event.maxCapacity;
            document.getElementById('event-image-url').value = event.imageUrl || '';
            document.getElementById('event-fee').value = event.fee || '';

            // FIX: Format datetime correctly for local timezone
            const eventDate = new Date(event.dateTime);

            // Get local timezone offset and adjust
            const timezoneOffset = eventDate.getTimezoneOffset() * 60000;
            const localDate = new Date(eventDate.getTime() - timezoneOffset);
            const formattedDate = localDate.toISOString().slice(0, 16);

            document.getElementById('event-date').value = formattedDate;

            // Change form to edit mode
            const form = document.getElementById('create-event-form');
            form.dataset.editId = eventId;

            // Update form heading
            const formHeading = document.querySelector('#create-event-section .section-header h1');
            if (formHeading) {
                formHeading.textContent = 'Update Event';
            }

            // Update form description
            const formDescription = document.querySelector('#create-event-section .section-header p');
            if (formDescription) {
                formDescription.textContent = 'Update your event details';
            }

            // Update submit button
            form.querySelector('button[type="submit"]').innerHTML = '<i class="fas fa-save"></i> Update Event';

            // Add cancel button for edit mode
            const formActions = document.querySelector('#create-event-section .form-actions');
            if (formActions && !formActions.querySelector('.cancel-edit-btn')) {
                const cancelButton = document.createElement('button');
                cancelButton.type = 'button';
                cancelButton.className = 'btn btn-secondary cancel-edit-btn';
                cancelButton.innerHTML = '<i class="fas fa-times"></i> Cancel Edit';
                cancelButton.onclick = () => {
                    clearForm('create-event-form');
                    this.showSection('events');
                };
                formActions.appendChild(cancelButton);
            }

            this.showSection('create-event');
        } catch (error) {
            //console.error('Error loading event for edit:', error);
            showToast('Failed to load event details', 'error');
        }
    }



    async updateEvent(eventId) {
        const form = document.getElementById('create-event-form');
        const formData = new FormData(form);

        const eventData = {
            title: formData.get('title'),
            description: formData.get('description'),
            dateTime: formData.get('dateTime'),
            location: formData.get('location'),
            maxCapacity: parseInt(formData.get('maxCapacity')),
            imageUrl: formData.get('imageUrl') ? formData.get('imageUrl').trim() : null,
            fee: formData.get('fee') ? parseFloat(formData.get('fee')) : 0.00
        };

        try {
            showLoading();
            await api.updateEvent(eventId, eventData);
            showToast('Event updated successfully!', 'success');

            // Reset form to create mode
            form.reset();
            delete form.dataset.editId;

            // Reset heading and button
            const formHeading = document.querySelector('#create-event-section .section-header h1');
            if (formHeading) {
                formHeading.textContent = 'Create New Event';
            }

            const formDescription = document.querySelector('#create-event-section .section-header p');
            if (formDescription) {
                formDescription.textContent = 'Fill in the details to create a new event';
            }

            form.querySelector('button[type="submit"]').innerHTML = '<i class="fas fa-save"></i> Create Event';

            await this.loadDashboardData();
            this.showSection('events');
        } catch (error) {
            //console.error('Error updating event:', error);
            showToast(error.message || 'Failed to update event', 'error');
        } finally {
            hideLoading();
        }
    }


    async deleteEvent(eventId) {
        if (!confirm('Are you sure you want to delete this event? This action cannot be undone.')) {
            return;
        }

        try {
            showLoading();
            await api.deleteEvent(eventId);
            showToast('Event deleted successfully', 'success');
            await this.loadDashboardData();
        } catch (error) {
            //console.error('Error deleting event:', error);
            showToast(error.message || 'Failed to delete event', 'error');
        } finally {
            hideLoading();
        }
    }

    async viewEventDetails(eventId) {
        try {
            const [event, rsvpCount, attendees] = await Promise.all([
                api.getEventById(eventId),
                api.getRSVPCount(eventId),
                api.getEventAttendees(eventId)
            ]);

            const eventImage = event.imageUrl ?
                `<div class="modal-event-image">
                    <img src="${event.imageUrl}" alt="${event.title}" style="max-width: 100%; height: 200px; object-fit: cover; border-radius: 8px; margin-bottom: 15px;" onerror="this.style.display='none';">
                </div>` : '';

            const eventFee = event.fee && event.fee > 0 ?
                `<p><strong>Fee:</strong> Rs. ${parseFloat(event.fee).toFixed(2)}</p>` :
                '<p><strong>Fee:</strong> Free</p>';

            document.getElementById('modal-event-title').textContent = event.title;
            document.getElementById('modal-event-content').innerHTML = `
                <div class="event-details scrollable-content">
                    ${eventImage}
                    <div class="event-info">
                        <p><strong>Description:</strong> ${event.description || 'No description available'}</p>
                        <p><strong>Date & Time:</strong> ${this.formatDateTime(event.dateTime)}</p>
                        <p><strong>Location:</strong> ${event.location}</p>
                        <p><strong>Capacity:</strong> ${event.maxCapacity}</p>
                        <p><strong>Confirmed RSVPs:</strong> ${rsvpCount.confirmed || 0}</p>
                        <p><strong>Pending RSVPs:</strong> ${rsvpCount.pending || 0}</p>
                        <p><strong>Total RSVPs:</strong> ${rsvpCount.total || 0}</p>
                        <p><strong>Available Spots:</strong> ${event.maxCapacity - (rsvpCount.confirmed || 0)}</p>
                        ${eventFee}

                        <div class="attendees-section">
                            <div class="attendees-header">
                                <h4>Attendees (${attendees.length})</h4>
                                <button class="btn btn-sm btn-success" onclick="organizerDashboard.downloadAttendeesCSV(${eventId}, '${event.title.replace(/'/g, "\\'")}')">
                                    <i class="fas fa-download"></i> Download CSV
                                </button>
                            </div>

                            <div class="attendees-preview">
                                ${attendees.slice(0, 5).map(attendee => `
                                    <div class="attendee-preview">
                                        <span class="attendee-name">${attendee.username}</span>
                                        <span class="attendee-email">${attendee.email}</span>
                                    </div>
                                `).join('')}
                                ${attendees.length > 5 ? `<p class="more-attendees">... and ${attendees.length - 5} more attendees</p>` : ''}
                                ${attendees.length === 0 ? '<p class="no-attendees">No attendees yet</p>' : ''}
                            </div>
                        </div>
                    </div>
                </div>
            `;

            showModal('event-modal');
        } catch (error) {
            //console.error('Failed to load event details:', error);
            showToast('Failed to load event details', 'error');
        }
    }

    // CSV Download functionality
    async downloadAttendeesCSV(eventId, eventTitle) {
        try {
            showLoading();

            // Get detailed attendee data
            const [attendees, event, rsvpCount] = await Promise.all([
                api.getEventAttendees(eventId),
                api.getEventById(eventId),
                api.getRSVPCount(eventId)
            ]);

            if (attendees.length === 0) {
                showToast('No attendees to export', 'warning');
                return;
            }

            // Convert to CSV
            const csvData = this.convertAttendeesToCSV(attendees, event, rsvpCount);

            // Create and download file
            const blob = new Blob([csvData], { type: 'text/csv;charset=utf-8;' });
            const link = document.createElement('a');

            if (link.download !== undefined) {
                const url = URL.createObjectURL(blob);
                link.setAttribute('href', url);
                link.setAttribute('download', `${this.sanitizeFilename(eventTitle)}_attendees_${this.getCurrentDate()}.csv`);
                link.style.visibility = 'hidden';
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);

                showToast(`Attendees list downloaded successfully (${attendees.length} attendees)`, 'success');
            } else {
                showToast('CSV download not supported in this browser', 'error');
            }

        } catch (error) {
            //console.error('Error downloading attendees CSV:', error);
            showToast('Failed to download attendees list', 'error');
        } finally {
            hideLoading();
        }
    }

    // Convert attendees data to CSV format
    convertAttendeesToCSV(attendees, event, rsvpCount) {
        // CSV Headers
        const headers = [
            'Event Title',
            'Event Date',
            'Event Location',
            'Attendee Name',
            'Email Address',
            'Registration Date',
            'RSVP Status',
            'User ID',
            'Contact'
        ];

        // Create CSV content
        let csvContent = headers.map(header => `"${header}"`).join(',') + '\n';

        // Add event summary row
        csvContent += [
            `"${event.title}"`,
            `"${this.formatDateTime(event.dateTime)}"`,
            `"${event.location}"`,
            `"SUMMARY"`,
            `"Total Attendees: ${attendees.length}"`,
            `"Confirmed: ${rsvpCount.confirmed || 0}"`,
            `"Pending: ${rsvpCount.pending || 0}"`,
            `"Capacity: ${event.maxCapacity}"`,
            `"Available: ${event.maxCapacity - (rsvpCount.confirmed || 0)}"`
        ].join(',') + '\n';

        // Add separator row
        csvContent += Array(headers.length).fill('""').join(',') + '\n';

        // Add attendee data
        attendees.forEach(attendee => {
            const row = [
                `"${event.title}"`,
                `"${this.formatDateTime(event.dateTime)}"`,
                `"${event.location}"`,
                `"${attendee.username || 'N/A'}"`,
                `"${attendee.email || 'N/A'}"`,
                `"${attendee.registrationDate ? this.formatDateTime(attendee.registrationDate) : 'N/A'}"`,
                `"${attendee.rsvpStatus || 'CONFIRMED'}"`,
                `"${attendee.id || 'N/A'}"`,
                `"${attendee.email || 'N/A'}"`
            ];
            csvContent += row.join(',') + '\n';
        });

        // Add footer with export info
        csvContent += '\n';
        csvContent += [
            `"Export Date"`,
            `"${new Date().toLocaleString()}"`,
            `"Exported By"`,
            `"${this.currentUser?.username || 'Organizer'}"`,
            `"Total Records"`,
            `"${attendees.length}"`,
            `""`,
            `""`,
            `""`
        ].join(',') + '\n';

        return csvContent;
    }

    // Helper method to sanitize filename
    sanitizeFilename(filename) {
        return filename.replace(/[^a-z0-9]/gi, '_').toLowerCase();
    }

    // Helper method to get current date for filename
    getCurrentDate() {
        const now = new Date();
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        return `${year}${month}${day}`;
    }

    // Show detailed attendees list
    async showAttendeesTable(eventId) {
        try {
            showLoading();
            const [attendees, event] = await Promise.all([
                api.getEventAttendees(eventId),
                api.getEventById(eventId)
            ]);

            const modalContent = `
                <div class="attendees-table-container">
                    <div class="attendees-table-header">
                        <h3>${event.title} - Attendees List</h3>
                        <div class="table-actions">
                            <button class="btn btn-success" onclick="organizerDashboard.downloadAttendeesCSV(${eventId}, '${event.title.replace(/'/g, "\\'")}')">
                                <i class="fas fa-download"></i> Export CSV
                            </button>
                            <button class="btn btn-secondary" onclick="closeModal('attendees-modal')">
                                <i class="fas fa-times"></i> Close
                            </button>
                        </div>
                    </div>

                    <div class="attendees-table-content">
                        ${attendees.length === 0 ?
                            '<p class="no-data">No attendees registered for this event</p>' :
                            `<table class="attendees-table">
                                <thead>
                                    <tr>
                                        <th>Name</th>
                                        <th>Email</th>
                                        <th>Registration Date</th>
                                        <th>Status</th>
                                        <th>Actions</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${attendees.map((attendee, index) => `
                                        <tr>
                                            <td>${attendee.username}</td>
                                            <td>${attendee.email}</td>
                                            <td>${attendee.registrationDate ? this.formatDateTime(attendee.registrationDate) : 'N/A'}</td>
                                            <td><span class="status-badge confirmed">Confirmed</span></td>
                                            <td>
                                                <button class="btn btn-sm btn-secondary" onclick="organizerDashboard.contactAttendee('${attendee.email}')">
                                                    <i class="fas fa-envelope"></i> Contact
                                                </button>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>`
                        }
                    </div>

                    <div class="attendees-table-footer">
                        <p><strong>Total Attendees:</strong> ${attendees.length}</p>
                        <p><strong>Event Capacity:</strong> ${event.maxCapacity}</p>
                        <p><strong>Available Spots:</strong> ${event.maxCapacity - attendees.length}</p>
                    </div>
                </div>
            `;

            // Create modal if it doesn't exist
            let modal = document.getElementById('attendees-modal');
            if (!modal) {
                modal = document.createElement('div');
                modal.id = 'attendees-modal';
                modal.className = 'modal';
                modal.innerHTML = `
                    <div class="modal-content large">
                        <div class="modal-body" id="attendees-modal-content"></div>
                    </div>
                `;
                document.body.appendChild(modal);
            }

            document.getElementById('attendees-modal-content').innerHTML = modalContent;
            showModal('attendees-modal');

        } catch (error) {
            //console.error('Error loading attendees table:', error);
            showToast('Failed to load attendees list', 'error');
        } finally {
            hideLoading();
        }
    }



    contactAttendee(email) {
        window.location.href = `mailto:${email}`;
    }

    setupEventListeners() {
        // Create event form submission
        document.getElementById('create-event-form').addEventListener('submit', (e) => {
            e.preventDefault();
            const form = e.target;

            if (form.dataset.editId) {
                this.updateEvent(form.dataset.editId);
            } else {
                this.createEvent();
            }
        });

        // Filter buttons
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                // Remove active class from siblings
                e.target.parentElement.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
                e.target.classList.add('active');

                const filter = e.target.dataset.filter;
                this.applyEventFilter(filter);
            });
        });

        // Search functionality
        const searchInput = document.getElementById('event-search');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchMyEvents(e.target.value);
            });
        }

        document.getElementById('send-reminder-form').addEventListener('submit', (e) => {
                e.preventDefault();
                this.sendEventReminder();
            });
    }

    applyEventFilter(filter) {
        const now = new Date();
        let filteredEvents = this.myEvents;

        switch (filter) {
            case 'upcoming':
                filteredEvents = this.myEvents.filter(event => new Date(event.dateTime) > now);
                break;
            case 'past':
                filteredEvents = this.myEvents.filter(event => new Date(event.dateTime) < now);
                break;
        }

        this.renderFilteredEvents(filteredEvents);
    }

    searchMyEvents(keyword) {
        if (!keyword.trim()) {
            this.loadMyEvents();
            return;
        }

        const filteredEvents = this.myEvents.filter(event =>
            event.title.toLowerCase().includes(keyword.toLowerCase()) ||
            event.location.toLowerCase().includes(keyword.toLowerCase()) ||
            (event.description && event.description.toLowerCase().includes(keyword.toLowerCase()))
        );

        this.renderFilteredEvents(filteredEvents);
    }

    renderFilteredEvents(events) {
        const container = document.getElementById('events-grid');
        if (events.length === 0) {
            container.innerHTML = '<p class="no-data">No events found</p>';
            return;
        }

        container.innerHTML = events.map(event => {
            const now = new Date();
            const eventDate = new Date(event.dateTime);
            const status = eventDate > now ? 'upcoming' : 'past';

            const eventImage = event.imageUrl ?
                `<div class="event-image-container">
                    <img src="${event.imageUrl}" alt="${event.title}" class="event-image"
                         onerror="this.style.display='none';">
                </div>` : '';

            const eventFee = event.fee && event.fee > 0 ?
                `<span class="event-fee">Rs. ${parseFloat(event.fee).toFixed(2)}</span>` :
                '<span class="event-fee free">Free</span>';

            return `
                <div class="event-card">
                    ${eventImage}
                    <div class="event-card-header">
                        <div class="event-card-title">${event.title}</div>
                        <div class="event-card-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                            <span><i class="fas fa-users"></i> ${event.maxCapacity} capacity</span>
                            ${eventFee}
                        </div>
                        <div class="event-card-description">${event.description || 'No description available'}</div>
                    </div>
                    <div class="event-card-footer">
                        <div class="event-status ${status}">${status}</div>
                        <div class="event-actions">
                            <button class="btn btn-sm btn-secondary" onclick="organizerDashboard.viewEventDetails(${event.id})">
                                Details
                            </button>
                            <button class="btn btn-sm btn-primary" onclick="organizerDashboard.editEvent(${event.id})">
                                Edit
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="organizerDashboard.deleteEvent(${event.id})">
                                Delete
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    }

    initializeCharts() {
        // Initialize Chart.js charts if the library is loaded
        if (typeof Chart !== 'undefined') {
            this.destroyExistingCharts();
            this.recreateChartCanvases();
            this.createAttendanceChart();
            this.createStatusChart();
        }
    }

    recreateChartCanvases() {
        // Recreate attendance chart canvas
        const attendanceContainer = document.getElementById('attendance-chart')?.parentElement;
        if (attendanceContainer) {
            const oldCanvas = document.getElementById('attendance-chart');
            if (oldCanvas) {
                oldCanvas.remove();
            }
            const newCanvas = document.createElement('canvas');
            newCanvas.id = 'attendance-chart';
            newCanvas.width = 400;
            newCanvas.height = 200;
            attendanceContainer.appendChild(newCanvas);
        }

        // Recreate status chart canvas
        const statusContainer = document.getElementById('status-chart')?.parentElement;
        if (statusContainer) {
            const oldCanvas = document.getElementById('status-chart');
            if (oldCanvas) {
                oldCanvas.remove();
            }
            const newCanvas = document.createElement('canvas');
            newCanvas.id = 'status-chart';
            newCanvas.width = 400;
            newCanvas.height = 200;
            statusContainer.appendChild(newCanvas);
        }
    }

    createAttendanceChart() {
        const ctx = document.getElementById('attendance-chart');
        if (!ctx) return;

        // Sample data - replace with real data from your API
        const chartData = {
            labels: this.myEvents.slice(0, 6).map(event => event.title),
            datasets: [{
                label: 'Attendees',
                data: [12, 19, 3, 5, 2, 3], // Replace with real attendance data
                borderColor: 'rgb(99, 102, 241)',
                backgroundColor: 'rgba(99, 102, 241, 0.1)',
                tension: 0.1
            }]
        };

        this.charts.attendance = new Chart(ctx, {
            type: 'line',
            data: chartData,
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    createStatusChart() {
        const ctx = document.getElementById('status-chart');
        if (!ctx) return;

        const now = new Date();
        const upcoming = this.myEvents.filter(event => new Date(event.dateTime) > now).length;
        const past = this.myEvents.filter(event => new Date(event.dateTime) < now).length;

        const chartData = {
            labels: ['Upcoming', 'Past'],
            datasets: [{
                data: [upcoming, past],
                backgroundColor: [
                    'rgba(59, 130, 246, 0.8)',
                    'rgba(107, 114, 128, 0.8)'
                ]
            }]
        };

        this.charts.status = new Chart(ctx, {
            type: 'doughnut',
            data: chartData,
            options: {
                responsive: true
            }
        });
    }

    destroyExistingCharts() {
            // Destroy attendance chart if it exists
            if (this.charts.attendance) {
                this.charts.attendance.destroy();
                this.charts.attendance = null;
            }

            // Destroy status chart if it exists
            if (this.charts.status) {
                this.charts.status.destroy();
                this.charts.status = null;
            }

            // Alternative: Use Chart.js getChart method
            const attendanceChart = Chart.getChart('attendance-chart');
            if (attendanceChart) {
                attendanceChart.destroy();
            }

            const statusChart = Chart.getChart('status-chart');
            if (statusChart) {
                statusChart.destroy();
            }
        }

    formatDateTime(dateTimeString) {
        const date = new Date(dateTimeString);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    }

    showSection(sectionName) {
        // Hide all sections
        document.querySelectorAll('.content-section').forEach(section => {
            section.classList.remove('active');
        });

        // Remove active class from all menu items
        document.querySelectorAll('.menu-item').forEach(item => {
            item.classList.remove('active');
        });

        // Show selected section
        document.getElementById(sectionName + '-section').classList.add('active');

        // Add active class to corresponding menu item
        document.querySelector(`[onclick="showSection('${sectionName}')"]`).classList.add('active');
    }

    // Load events for reminder selection
    loadReminderEventSelect() {
        const select = document.getElementById('reminder-event-select');
        const now = new Date();

        // Filter upcoming events only
        const upcomingEvents = this.myEvents.filter(event =>
            new Date(event.dateTime) > now
        );

        select.innerHTML = '<option value="">Choose an event...</option>' +
            upcomingEvents.map(event =>
                `<option value="${event.id}">${event.title} - ${this.formatDateTime(event.dateTime)}</option>`
            ).join('');

        // Add event listener for attendee count display
        select.addEventListener('change', async (e) => {
            if (e.target.value) {
                await this.displayAttendeeCount(e.target.value);
            } else {
                document.getElementById('attendee-count-display').innerHTML =
                    '<p>Select an event to see attendee count</p>';
            }
        });
    }

    // Display attendee count for selected event
    async displayAttendeeCount(eventId) {
        try {
            const [attendees, rsvpCount] = await Promise.all([
                api.getEventAttendees(eventId),
                api.getRSVPCount(eventId)
            ]);

            const container = document.getElementById('attendee-count-display');
            container.innerHTML = `
                <div class="attendee-count-info">
                    <h4>Attendee Information</h4>
                    <p><strong>Confirmed Attendees:</strong> ${rsvpCount.confirmed || 0}</p>
                    <p><strong>Total RSVPs:</strong> ${rsvpCount.total || 0}</p>
                    <p><strong>Reminder will be sent to:</strong> ${attendees.length} attendees</p>
                </div>
            `;
        } catch (error) {
            //console.error('Error loading attendee count:', error);
            document.getElementById('attendee-count-display').innerHTML =
                '<p class="error">Error loading attendee information</p>';
        }
    }

    // Update your sendEventReminder method
    async sendEventReminder() {
        const form = document.getElementById('send-reminder-form');
        const formData = new FormData(form);

        const eventId = formData.get('eventId');
        const message = formData.get('message');

        if (!eventId) {
            showToast('Please select an event', 'error');
            return;
        }

        const confirmed = confirm('Are you sure you want to send reminder emails to all confirmed attendees?');
        if (!confirmed) return;

        try {
            showLoading();

            // Get token from localStorage
            const token = localStorage.getItem('jwt_token');
            //console.log('Token being sent:', token);

            if (!token) {
                showToast('Authentication required. Please login again.', 'error');
                window.location.href = 'index.html';
                return;
            }

            const response = await fetch(`/api/events/${eventId}/send-reminder`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}` // Ensure Bearer prefix
                },
                body: JSON.stringify({ message: message })
            });

            if (response.ok) {
                const result = await response.text();
                showToast(result, 'success');
                form.reset();
                document.getElementById('attendee-count-display').innerHTML =
                    '<p>Select an event to see attendee count</p>';
            } else if (response.status === 401 || response.status === 403) {
                showToast('Authentication failed. Please login again.', 'error');
                localStorage.removeItem('token');
                window.location.href = 'index.html';
            } else {
                const error = await response.text();
                showToast(error || 'Failed to send reminder', 'error');
            }
        } catch (error) {
            //console.error('Error sending reminder:', error);
            showToast('Failed to send reminder', 'error');
        } finally {
            hideLoading();
        }
    }

    // Add this method to check for capacity alerts across all events
    async checkCapacityAlerts() {
        const alerts = [];

        for (const event of this.myEvents) {
            try {
                const rsvpCount = await api.getRSVPCount(event.id);
                const currentRSVPs = rsvpCount.confirmed || 0;
                const maxCapacity = event.maxCapacity;

                if (currentRSVPs >= maxCapacity) {
                    alerts.push({
                        type: 'danger',
                        message: `🚨 Event "${event.title}" is at full capacity (${currentRSVPs}/${maxCapacity})`,
                        eventId: event.id
                    });
                } else if (currentRSVPs >= maxCapacity * 0.9) {
                    alerts.push({
                        type: 'warning',
                        message: `⚠️ Event "${event.title}" is almost full (${currentRSVPs}/${maxCapacity})`,
                        eventId: event.id
                    });
                }
            } catch (error) {
                //console.error(`Error checking capacity for event ${event.id}:`, error);
            }
        }

        this.displayCapacityAlerts(alerts);
    }

    // Display capacity alerts at the top of dashboard
    displayCapacityAlerts(alerts) {
        const alertContainer = document.getElementById('dashboard-alerts');
        if (!alertContainer) return;

        if (alerts.length === 0) {
            alertContainer.innerHTML = '';
            return;
        }

        alertContainer.innerHTML = alerts.map(alert => `
            <div class="alert alert-${alert.type} alert-dismissible">
                <span>${alert.message}</span>
                <div class="alert-actions">
                    <button class="btn btn-sm btn-outline-secondary" onclick="organizerDashboard.viewEventDetails(${alert.eventId})">
                        View Event
                    </button>
                    <button type="button" class="btn-close" onclick="this.parentElement.parentElement.remove()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
            </div>
        `).join('');
    }

}

// Global functions
function showSection(sectionName) {
    organizerDashboard.showSection(sectionName);
}

function editProfile() {
    showToast('Contact administrator at support@joinify.com', 'info');
}

function logout() {
    authManager.logout();
    window.location.href = 'index.html';
}

function closeModal(modalId) {
    hideModal(modalId);
}

function clearForm(formId) {
    const form = document.getElementById(formId);
    form.reset();
    delete form.dataset.editId;

    // Reset headings and button
    const formHeading = document.querySelector('#create-event-section .section-header h1');
    if (formHeading) {
        formHeading.textContent = 'Create New Event';
    }

    const formDescription = document.querySelector('#create-event-section .section-header p');
    if (formDescription) {
        formDescription.textContent = 'Fill in the details to create a new event';
    }

    form.querySelector('button[type="submit"]').innerHTML = '<i class="fas fa-save"></i> Create Event';

    // Remove cancel button if it exists
    const cancelButton = document.querySelector('.cancel-edit-btn');
    if (cancelButton) {
        cancelButton.remove();
    }
}

// Clear reminder form
function clearReminderForm() {
    document.getElementById('send-reminder-form').reset();
    document.getElementById('attendee-count-display').innerHTML =
        '<p>Select an event to see attendee count</p>';
}

// Initialize dashboard when page loads
let organizerDashboard;
document.addEventListener('DOMContentLoaded', () => {
    organizerDashboard = new OrganizerDashboard();
});

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('event-modal');
    if (event.target === modal) {
        hideModal('event-modal');
    }
}

// Add this debug function to your dashboard
function debugTokenInfo() {
    const token = localStorage.getItem('token');
//    console.log('=== TOKEN DEBUG INFO ===');
//    console.log('Token exists:', !!token);
//    console.log('Token length:', token ? token.length : 0);
//    console.log('Token format valid:', token ? token.split('.').length === 3 : false);
//    console.log('Token preview:', token ? token.substring(0, 50) + '...' : 'No token');

    if (token) {
        try {
            const parts = token.split('.');
            //console.log('Token parts:', parts.length);
            if (parts.length === 3) {
                const payload = JSON.parse(atob(parts[1]));
                //console.log('Token payload:', payload);
                //console.log('Token expires:', new Date(payload.exp * 1000));
            }
        } catch (e) {
            //console.error('Error parsing token:', e);
        }
    }
}