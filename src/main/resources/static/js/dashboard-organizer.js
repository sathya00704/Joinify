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
            console.error('Dashboard initialization failed:', error);
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
            // Load organizer's events
            const [myEvents, userStats] = await Promise.all([
                api.getMyEvents(),
                api.getUserStats()
            ]);

            this.myEvents = myEvents || [];

            await this.updateStats();
            this.loadRecentEvents();
            this.loadMyEvents();
            this.loadAttendeeEventSelect();
            this.initializeCharts();
        } catch (error) {
            console.error('Error loading dashboard data:', error);
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
                console.error('Error getting RSVP count for event:', event.id);
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
        `;

        // Load attendee count for the next event
        this.loadAttendeeCount(nextEvent);
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
            console.error('Error loading attendee count:', error);
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

            return `
                <div class="event-card">
                    <div class="event-card-header">
                        <div class="event-card-title">${event.title}</div>
                        <div class="event-card-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                            <span><i class="fas fa-users"></i> ${event.maxCapacity} capacity</span>
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
            console.error('Error loading attendees:', error);
            showToast('Failed to load attendees', 'error');
        } finally {
            hideLoading();
        }
    }

    async createEvent() {
        const form = document.getElementById('create-event-form');
        const formData = new FormData(form);

        const eventData = {
            title: formData.get('title'),
            description: formData.get('description'),
            dateTime: formData.get('dateTime'),
            location: formData.get('location'),
            maxCapacity: parseInt(formData.get('maxCapacity'))
        };

        // Validate required fields
        if (!eventData.title || !eventData.dateTime || !eventData.location || !eventData.maxCapacity) {
            showToast('Please fill in all required fields', 'error');
            return;
        }

        // Validate future date
        if (new Date(eventData.dateTime) <= new Date()) {
            showToast('Event date must be in the future', 'error');
            return;
        }

        try {
            showLoading();
            await api.createEvent(eventData);
            showToast('Event created successfully!', 'success');
            form.reset();
            await this.loadDashboardData();
            this.showSection('events');
        } catch (error) {
            console.error('Error creating event:', error);
            showToast(error.message || 'Failed to create event', 'error');
        } finally {
            hideLoading();
        }
    }

    async editEvent(eventId) {
        try {
            const event = await api.getEventById(eventId);

            // Populate form with event data
            document.getElementById('event-title').value = event.title;
            document.getElementById('event-description').value = event.description || '';
            document.getElementById('event-location').value = event.location;
            document.getElementById('event-capacity').value = event.maxCapacity;

            // Format datetime for input
            const eventDate = new Date(event.dateTime);
            const formattedDate = eventDate.toISOString().slice(0, 16);
            document.getElementById('event-date').value = formattedDate;

            // Change form to edit mode
            const form = document.getElementById('create-event-form');
            form.dataset.editId = eventId;
            form.querySelector('button[type="submit"]').innerHTML = '<i class="fas fa-save"></i> Update Event';

            this.showSection('create-event');
        } catch (error) {
            console.error('Error loading event for edit:', error);
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
            maxCapacity: parseInt(formData.get('maxCapacity'))
        };

        try {
            showLoading();
            await api.updateEvent(eventId, eventData);
            showToast('Event updated successfully!', 'success');

            // Reset form to create mode
            form.reset();
            delete form.dataset.editId;
            form.querySelector('button[type="submit"]').innerHTML = '<i class="fas fa-save"></i> Create Event';

            await this.loadDashboardData();
            this.showSection('events');
        } catch (error) {
            console.error('Error updating event:', error);
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
            console.error('Error deleting event:', error);
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

            document.getElementById('modal-event-title').textContent = event.title;
            document.getElementById('modal-event-content').innerHTML = `
                <div class="event-details">
                    <p><strong>Description:</strong> ${event.description || 'No description available'}</p>
                    <p><strong>Date & Time:</strong> ${this.formatDateTime(event.dateTime)}</p>
                    <p><strong>Location:</strong> ${event.location}</p>
                    <p><strong>Capacity:</strong> ${event.maxCapacity}</p>
                    <p><strong>Confirmed RSVPs:</strong> ${rsvpCount.confirmed || 0}</p>
                    <p><strong>Pending RSVPs:</strong> ${rsvpCount.pending || 0}</p>
                    <p><strong>Total RSVPs:</strong> ${rsvpCount.total || 0}</p>
                    <p><strong>Available Spots:</strong> ${event.maxCapacity - (rsvpCount.confirmed || 0)}</p>
                    <div class="attendees-preview">
                        <h4>Attendees:</h4>
                        ${attendees.slice(0, 5).map(attendee => `
                            <div class="attendee-preview">${attendee.username} (${attendee.email})</div>
                        `).join('')}
                        ${attendees.length > 5 ? `<p>... and ${attendees.length - 5} more</p>` : ''}
                        ${attendees.length === 0 ? '<p>No attendees yet</p>' : ''}
                    </div>
                </div>
            `;

            // Use the showModal function from utils.js
            showModal('event-modal');
        } catch (error) {
            console.error('Failed to load event details:', error);
            showToast('Failed to load event details', 'error');
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
        document.getElementById('event-search').addEventListener('input', (e) => {
            this.searchMyEvents(e.target.value);
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

            return `
                <div class="event-card">
                    <div class="event-card-header">
                        <div class="event-card-title">${event.title}</div>
                        <div class="event-card-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                            <span><i class="fas fa-users"></i> ${event.maxCapacity} capacity</span>
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
            this.createAttendanceChart();
            this.createStatusChart();
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
    document.getElementById(modalId).style.display = 'none';
}

function clearForm(formId) {
    const form = document.getElementById(formId);
    form.reset();
    delete form.dataset.editId;
    form.querySelector('button[type="submit"]').innerHTML = '<i class="fas fa-save"></i> Create Event';
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
        modal.style.display = 'none';
    }
}
