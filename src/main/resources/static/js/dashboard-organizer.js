// Organizer Dashboard Functionality
let currentEvents = [];
let attendanceChart = null;
let statusChart = null;

document.addEventListener('DOMContentLoaded', function() {
    initializeOrganizerDashboard();
});

async function initializeOrganizerDashboard() {
    try {
        // Check authentication and role
        await authManager.checkAuthStatus();

        if (!authManager.isLoggedIn() || authManager.getCurrentUserRole() !== 'ORGANIZER') {
            window.location.href = 'index.html';
            return;
        }

        // Load user info
        await loadUserInfo();

        // Load dashboard data
        await loadDashboardData();

        // Initialize event listeners
        initializeEventListeners();

    } catch (error) {
        console.error('Error initializing dashboard:', error);
        handleApiError(error);
    }
}

function initializeEventListeners() {
    // Create event form
    const createEventForm = document.getElementById('create-event-form');
    if (createEventForm) {
        createEventForm.addEventListener('submit', handleCreateEvent);
    }

    // Event search
    const eventSearch = document.getElementById('event-search');
    if (eventSearch) {
        eventSearch.addEventListener('input', debounce(filterEvents, 300));
    }

    // Filter buttons
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            // Update active state
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // Filter events
            const filter = this.dataset.filter;
            filterEvents(null, filter);
        });
    });

    // Attendee event select
    const attendeeEventSelect = document.getElementById('attendee-event-select');
    if (attendeeEventSelect) {
        attendeeEventSelect.addEventListener('change', loadEventAttendees);
    }
}

async function loadUserInfo() {
    try {
        const user = await api.getCurrentUser();
        document.getElementById('user-name').textContent = user.username;
    } catch (error) {
        console.error('Error loading user info:', error);
    }
}

async function loadDashboardData() {
    try {
        showLoading();

        // Load all data in parallel
        await Promise.all([
            loadStats(),
            loadRecentEvents(),
            loadMyEvents(),
            loadEventSelectOptions()
        ]);

    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showToast('Error loading dashboard data', 'error');
    } finally {
        hideLoading();
    }
}

async function loadStats() {
    try {
        const [myEvents, userStats] = await Promise.all([
            api.getMyEvents(),
            api.getUserStats()
        ]);

        const now = new Date();
        const upcomingEvents = myEvents.filter(event => new Date(event.dateTime) > now);

        // Calculate total attendees across all events
        let totalAttendees = 0;
        let totalCapacity = 0;

        for (const event of myEvents) {
            const attendeeCount = await api.getEventAttendees(event.id);
            totalAttendees += attendeeCount.length;
            totalCapacity += event.maxCapacity;
        }

        const avgAttendance = totalCapacity > 0 ? Math.round((totalAttendees / totalCapacity) * 100) : 0;

        // Update stats
        animateCounter('total-events', myEvents.length);
        animateCounter('upcoming-events', upcomingEvents.length);
        animateCounter('total-attendees', totalAttendees);
        document.getElementById('avg-attendance').textContent = `${avgAttendance}%`;

    } catch (error) {
        console.error('Error loading stats:', error);
    }
}

async function loadRecentEvents() {
    try {
        const events = await api.getMyEvents();
        const recentEvents = events.slice(0, 5); // Show last 5 events

        const tbody = document.getElementById('recent-events-tbody');
        if (!tbody) return;

        if (recentEvents.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center">No events found</td>
                </tr>
            `;
            return;
        }

        tbody.innerHTML = await Promise.all(recentEvents.map(async event => {
            const attendees = await api.getEventAttendees(event.id);
            const status = getEventStatus(event);

            return `
                <tr>
                    <td>${event.title}</td>
                    <td>${formatDateTime(event.dateTime)}</td>
                    <td>${attendees.length}/${event.maxCapacity}</td>
                    <td><span class="event-status ${status.toLowerCase()}">${status}</span></td>
                    <td>
                        <button class="btn btn-sm btn-primary" onclick="editEvent(${event.id})">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteEvent(${event.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </td>
                </tr>
            `;
        })).then(rows => rows.join(''));

    } catch (error) {
        console.error('Error loading recent events:', error);
    }
}

async function loadMyEvents() {
    try {
        const events = await api.getMyEvents();
        currentEvents = events;
        displayEvents(events);

    } catch (error) {
        console.error('Error loading events:', error);
        displayEventsError();
    }
}

function displayEvents(events) {
    const eventsGrid = document.getElementById('events-grid');
    if (!eventsGrid) return;

    if (events.length === 0) {
        eventsGrid.innerHTML = `
            <div class="no-data">
                <i class="fas fa-calendar-times"></i>
                <h3>No events found</h3>
                <p>Create your first event to get started!</p>
                <button class="btn btn-primary" onclick="showSection('create-event')">
                    <i class="fas fa-plus"></i> Create Event
                </button>
            </div>
        `;
        return;
    }

    eventsGrid.innerHTML = events.map(event => createEventCard(event)).join('');
}

function createEventCard(event) {
    const status = getEventStatus(event);
    const statusClass = status.toLowerCase();

    return `
        <div class="event-card">
            <div class="event-card-header">
                <h3 class="event-card-title">${event.title}</h3>
                <div class="event-card-meta">
                    <span><i class="fas fa-clock"></i> ${formatDateTime(event.dateTime)}</span>
                    <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                    <span><i class="fas fa-users"></i> ${event.maxCapacity} capacity</span>
                </div>
                <p class="event-card-description">${truncateText(event.description || 'No description', 100)}</p>
            </div>
            <div class="event-card-footer">
                <span class="event-status ${statusClass}">${status}</span>
                <div class="event-actions">
                    <button class="btn btn-sm btn-secondary" onclick="viewEventDetails(${event.id})">
                        <i class="fas fa-eye"></i> View
                    </button>
                    <button class="btn btn-sm btn-primary" onclick="editEvent(${event.id})">
                        <i class="fas fa-edit"></i> Edit
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="confirmDeleteEvent(${event.id})">
                        <i class="fas fa-trash"></i> Delete
                    </button>
                </div>
            </div>
        </div>
    `;
}

function getEventStatus(event) {
    const now = new Date();
    const eventDate = new Date(event.dateTime);

    if (eventDate < now) {
        return 'Past';
    } else if (eventDate > now) {
        return 'Upcoming';
    } else {
        return 'Live';
    }
}

async function loadEventSelectOptions() {
    try {
        const events = await api.getMyEvents();
        const select = document.getElementById('attendee-event-select');

        if (!select) return;

        select.innerHTML = '<option value="">Select an event</option>' +
            events.map(event => `<option value="${event.id}">${event.title}</option>`).join('');

    } catch (error) {
        console.error('Error loading event options:', error);
    }
}

async function loadEventAttendees() {
    const eventId = document.getElementById('attendee-event-select').value;
    const container = document.getElementById('attendees-container');

    if (!eventId) {
        container.innerHTML = '<p class="no-data">Select an event to view attendees</p>';
        return;
    }

    try {
        showLoading();
        const attendees = await api.getEventAttendees(eventId);

        if (attendees.length === 0) {
            container.innerHTML = '<p class="no-data">No attendees registered for this event</p>';
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
                        <span class="attendee-role">${attendee.role}</span>
                    </div>
                `).join('')}
            </div>
        `;

    } catch (error) {
        console.error('Error loading attendees:', error);
        container.innerHTML = '<p class="no-data">Error loading attendees</p>';
    } finally {
        hideLoading();
    }
}

async function handleCreateEvent(e) {
    e.preventDefault();

    const formData = getFormData('create-event-form');

    try {
        showLoading();
        const event = await api.createEvent(formData);

        showToast('Event created successfully!', 'success');
        clearForm('create-event-form');

        // Refresh data
        await loadDashboardData();

        // Switch to events section
        showSection('events');

    } catch (error) {
        console.error('Error creating event:', error);
        handleApiError(error);
    } finally {
        hideLoading();
    }
}

function filterEvents(searchTerm = null, filter = 'all') {
    if (searchTerm === null) {
        searchTerm = document.getElementById('event-search')?.value || '';
    }

    let filteredEvents = currentEvents;

    // Apply search filter
    if (searchTerm) {
        filteredEvents = filteredEvents.filter(event =>
            event.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
            event.location.toLowerCase().includes(searchTerm.toLowerCase())
        );
    }

    // Apply status filter
    if (filter !== 'all') {
        const now = new Date();
        filteredEvents = filteredEvents.filter(event => {
            const eventDate = new Date(event.dateTime);
            if (filter === 'upcoming') return eventDate > now;
            if (filter === 'past') return eventDate < now;
            return true;
        });
    }

    displayEvents(filteredEvents);
}

function showSection(sectionName) {
    // Hide all sections
    document.querySelectorAll('.content-section').forEach(section => {
        section.classList.remove('active');
    });

    // Show selected section
    const targetSection = document.getElementById(`${sectionName}-section`);
    if (targetSection) {
        targetSection.classList.add('active');
    }

    // Update menu active state
    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('active');
    });

    const activeMenuItem = document.querySelector(`[onclick="showSection('${sectionName}')"]`);
    if (activeMenuItem) {
        activeMenuItem.classList.add('active');
    }

    // Load section-specific data
    if (sectionName === 'analytics') {
        loadAnalytics();
    }
}

async function loadAnalytics() {
    try {
        const events = await api.getMyEvents();

        // Prepare data for charts
        const attendanceData = await prepareAttendanceData(events);
        const statusData = prepareStatusData(events);

        // Create charts
        createAttendanceChart(attendanceData);
        createStatusChart(statusData);

    } catch (error) {
        console.error('Error loading analytics:', error);
    }
}

async function prepareAttendanceData(events) {
    const data = {
        labels: [],
        datasets: [{
            label: 'Attendees',
            data: [],
            borderColor: 'rgb(99, 102, 241)',
            backgroundColor: 'rgba(99, 102, 241, 0.2)',
            tension: 0.1
        }]
    };

    for (const event of events.slice(0, 10)) { // Last 10 events
        data.labels.push(event.title);
        const attendees = await api.getEventAttendees(event.id);
        data.datasets[0].data.push(attendees.length);
    }

    return data;
}

function prepareStatusData(events) {
    const now = new Date();
    const statusCount = {
        upcoming: 0,
        past: 0,
        live: 0
    };

    events.forEach(event => {
        const eventDate = new Date(event.dateTime);
        if (eventDate > now) {
            statusCount.upcoming++;
        } else {
            statusCount.past++;
        }
    });

    return {
        labels: ['Upcoming', 'Past'],
        datasets: [{
            data: [statusCount.upcoming, statusCount.past],
            backgroundColor: [
                'rgba(16, 185, 129, 0.8)',
                'rgba(107, 114, 128, 0.8)'
            ],
            borderColor: [
                'rgb(16, 185, 129)',
                'rgb(107, 114, 128)'
            ],
            borderWidth: 1
        }]
    };
}

function createAttendanceChart(data) {
    const ctx = document.getElementById('attendance-chart');
    if (!ctx) return;

    if (attendanceChart) {
        attendanceChart.destroy();
    }

    attendanceChart = new Chart(ctx, {
        type: 'line',
        data: data,
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

function createStatusChart(data) {
    const ctx = document.getElementById('status-chart');
    if (!ctx) return;

    if (statusChart) {
        statusChart.destroy();
    }

    statusChart = new Chart(ctx, {
        type: 'doughnut',
        data: data,
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'bottom',
                }
            }
        }
    });
}

// Event management functions
function viewEventDetails(eventId) {
    window.location.href = `event-details.html?id=${eventId}`;
}

function editEvent(eventId) {
    window.location.href = `edit-event.html?id=${eventId}`;
}

function confirmDeleteEvent(eventId) {
    if (confirm('Are you sure you want to delete this event? This action cannot be undone.')) {
        deleteEvent(eventId);
    }
}

async function deleteEvent(eventId) {
    try {
        showLoading();
        await api.deleteEvent(eventId);

        showToast('Event deleted successfully', 'success');

        // Refresh data
        await loadDashboardData();

    } catch (error) {
        console.error('Error deleting event:', error);
        handleApiError(error);
    } finally {
        hideLoading();
    }
}

function editProfile() {
    // Implement profile editing
    showToast('Profile editing coming soon!', 'info');
}

function logout() {
    authManager.logout();
    window.location.href = 'index.html';
}

function displayEventsError() {
    const eventsGrid = document.getElementById('events-grid');
    if (!eventsGrid) return;

    eventsGrid.innerHTML = `
        <div class="no-data">
            <i class="fas fa-exclamation-triangle"></i>
            <h3>Error loading events</h3>
            <p>Please try again later</p>
            <button class="btn btn-primary" onclick="loadMyEvents()">
                <i class="fas fa-refresh"></i> Retry
            </button>
        </div>
    `;
}
