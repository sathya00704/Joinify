// Attendee Dashboard Functionality
let availableEvents = [];
let myRSVPs = [];

document.addEventListener('DOMContentLoaded', function() {
    initializeAttendeeDashboard();
});

async function initializeAttendeeDashboard() {
    try {
        // Check authentication and role
        await authManager.checkAuthStatus();

        if (!authManager.isLoggedIn() || authManager.getCurrentUserRole() !== 'ATTENDEE') {
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
    // Search functionality
    const discoverSearch = document.getElementById('discover-search');
    if (discoverSearch) {
        discoverSearch.addEventListener('input', debounce(searchEvents, 300));
        discoverSearch.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchEvents();
            }
        });
    }

    // Filter buttons for discover section
    document.querySelectorAll('#discover-section .filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            // Update active state
            document.querySelectorAll('#discover-section .filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // Filter events
            const filter = this.dataset.filter;
            filterDiscoverEvents(filter);
        });
    });

    // Filter buttons for my events section
    document.querySelectorAll('#my-events-section .filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            // Update active state
            document.querySelectorAll('#my-events-section .filter-btn').forEach(b => b.classList.remove('active'));
            this.classList.add('active');

            // Filter my events
            const filter = this.dataset.filter;
            filterMyEvents(filter);
        });
    });
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
            loadUpcomingRSVPs(),
            loadAvailableEvents(),
            loadMyRSVPs(),
            loadEventHistory()
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
        const [rsvps, allEvents] = await Promise.all([
            api.getMyRSVPs(),
            api.getUpcomingEvents()
        ]);

        const now = new Date();
        const upcomingRSVPs = rsvps.filter(rsvp => new Date(rsvp.event.dateTime) > now);
        const pastRSVPs = rsvps.filter(rsvp => new Date(rsvp.event.dateTime) < now);
        const availableEvents = allEvents.filter(event => {
            // Check if user hasn't RSVP'd and event has capacity
            const hasRSVPd = rsvps.some(rsvp => rsvp.event.id === event.id);
            return !hasRSVPd && new Date(event.dateTime) > now;
        });

        // Update stats with animation
        animateCounter('upcoming-rsvps', upcomingRSVPs.length);
        animateCounter('total-rsvps', rsvps.length);
        animateCounter('events-attended', pastRSVPs.length);
        animateCounter('available-events', availableEvents.length);

    } catch (error) {
        console.error('Error loading stats:', error);
        // Set default values
        animateCounter('upcoming-rsvps', 0);
        animateCounter('total-rsvps', 0);
        animateCounter('events-attended', 0);
        animateCounter('available-events', 0);
    }
}

async function loadUpcomingRSVPs() {
    try {
        const rsvps = await api.getUpcomingRSVPsForUser(authManager.currentUser?.id);
        const upcomingList = document.getElementById('upcoming-events-list');

        if (!upcomingList) return;

        if (rsvps.length === 0) {
            upcomingList.innerHTML = `
                <div class="no-data">
                    <i class="fas fa-calendar-times"></i>
                    <h3>No upcoming events</h3>
                    <p>Discover and join exciting events!</p>
                    <button class="btn btn-primary" onclick="showSection('discover')">
                        <i class="fas fa-search"></i> Discover Events
                    </button>
                </div>
            `;
            return;
        }

        upcomingList.innerHTML = rsvps.slice(0, 3).map(rsvp => createUpcomingEventItem(rsvp)).join('');

    } catch (error) {
        console.error('Error loading upcoming RSVPs:', error);
        const upcomingList = document.getElementById('upcoming-events-list');
        if (upcomingList) {
            upcomingList.innerHTML = '<div class="no-data">Error loading upcoming events</div>';
        }
    }
}

function createUpcomingEventItem(rsvp) {
    const event = rsvp.event;
    const timeUntil = getTimeUntilEvent(event.dateTime);

    return `
        <div class="event-list-item">
            <div class="event-list-info">
                <h3>${event.title}</h3>
                <div class="event-list-meta">
                    <span><i class="fas fa-clock"></i> ${formatDateTime(event.dateTime)}</span>
                    <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                    <span><i class="fas fa-hourglass-half"></i> ${timeUntil}</span>
                </div>
            </div>
            <div class="event-list-actions">
                <button class="btn btn-sm btn-primary" onclick="viewEventDetails(${event.id})">
                    <i class="fas fa-eye"></i> View
                </button>
                <button class="btn btn-sm btn-danger" onclick="confirmCancelRSVP(${event.id})">
                    <i class="fas fa-times"></i> Cancel
                </button>
            </div>
        </div>
    `;
}

async function loadAvailableEvents() {
    try {
        const [allEvents, myRSVPs] = await Promise.all([
            api.getUpcomingEvents(),
            api.getMyRSVPs()
        ]);

        // Filter out events user has already RSVP'd to
        const myEventIds = myRSVPs.map(rsvp => rsvp.event.id);
        availableEvents = allEvents.filter(event => !myEventIds.includes(event.id));

        displayDiscoverEvents(availableEvents);

    } catch (error) {
        console.error('Error loading available events:', error);
        displayDiscoverEventsError();
    }
}

function displayDiscoverEvents(events) {
    const eventsGrid = document.getElementById('discover-events-grid');
    if (!eventsGrid) return;

    if (events.length === 0) {
        eventsGrid.innerHTML = `
            <div class="no-data">
                <i class="fas fa-search"></i>
                <h3>No events found</h3>
                <p>Try adjusting your search or filters</p>
            </div>
        `;
        return;
    }

    eventsGrid.innerHTML = events.map(event => createDiscoverEventCard(event)).join('');
}

function createDiscoverEventCard(event) {
    const now = new Date();
    const eventDate = new Date(event.dateTime);
    const isUpcoming = eventDate > now;

    return `
        <div class="event-card">
            <div class="event-card-header">
                <h3 class="event-card-title">${event.title}</h3>
                <div class="event-card-meta">
                    <span><i class="fas fa-clock"></i> ${formatDateTime(event.dateTime)}</span>
                    <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                    <span><i class="fas fa-users"></i> ${event.maxCapacity} capacity</span>
                </div>
                <p class="event-card-description">${truncateText(event.description || 'No description available', 100)}</p>
            </div>
            <div class="event-card-footer">
                <div class="event-capacity">
                    <span class="capacity-text">Available spots</span>
                    <div class="capacity-bar">
                        <div class="capacity-fill" style="width: 70%"></div>
                    </div>
                </div>
                <div class="event-actions">
                    <button class="btn btn-sm btn-secondary" onclick="viewEventDetails(${event.id})">
                        <i class="fas fa-eye"></i> Details
                    </button>
                    ${isUpcoming ? `
                        <button class="btn btn-sm btn-success" onclick="joinEvent(${event.id})">
                            <i class="fas fa-plus"></i> Join
                        </button>
                    ` : `
                        <button class="btn btn-sm btn-secondary" disabled>
                            <i class="fas fa-clock"></i> Past Event
                        </button>
                    `}
                </div>
            </div>
        </div>
    `;
}

async function loadMyRSVPs() {
    try {
        const rsvps = await api.getMyRSVPs();
        myRSVPs = rsvps;
        displayMyEvents(rsvps);

    } catch (error) {
        console.error('Error loading my RSVPs:', error);
        displayMyEventsError();
    }
}

function displayMyEvents(rsvps) {
    const eventsList = document.getElementById('my-events-list');
    if (!eventsList) return;

    if (rsvps.length === 0) {
        eventsList.innerHTML = `
            <div class="no-data">
                <i class="fas fa-calendar-times"></i>
                <h3>No events joined yet</h3>
                <p>Start discovering and joining events!</p>
                <button class="btn btn-primary" onclick="showSection('discover')">
                    <i class="fas fa-search"></i> Discover Events
                </button>
            </div>
        `;
        return;
    }

    eventsList.innerHTML = rsvps.map(rsvp => createMyEventItem(rsvp)).join('');
}

function createMyEventItem(rsvp) {
    const event = rsvp.event;
    const now = new Date();
    const eventDate = new Date(event.dateTime);
    const isUpcoming = eventDate > now;
    const status = rsvp.status || 'CONFIRMED';

    return `
        <div class="event-list-item">
            <div class="event-list-info">
                <h3>${event.title}</h3>
                <div class="event-list-meta">
                    <span><i class="fas fa-clock"></i> ${formatDateTime(event.dateTime)}</span>
                    <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                    <span><i class="fas fa-check-circle"></i> ${status}</span>
                    ${isUpcoming ? `<span class="event-status upcoming">Upcoming</span>` : `<span class="event-status past">Past</span>`}
                </div>
            </div>
            <div class="event-list-actions">
                <button class="btn btn-sm btn-primary" onclick="viewEventDetails(${event.id})">
                    <i class="fas fa-eye"></i> View
                </button>
                ${isUpcoming ? `
                    <button class="btn btn-sm btn-danger" onclick="confirmCancelRSVP(${event.id})">
                        <i class="fas fa-times"></i> Cancel RSVP
                    </button>
                ` : ''}
            </div>
        </div>
    `;
}

async function loadEventHistory() {
    try {
        const rsvps = await api.getPastRSVPsForUser(authManager.currentUser?.id);
        displayEventHistory(rsvps);

    } catch (error) {
        console.error('Error loading event history:', error);
        displayEventHistoryError();
    }
}

function displayEventHistory(rsvps) {
    const historyList = document.getElementById('history-list');
    if (!historyList) return;

    if (rsvps.length === 0) {
        historyList.innerHTML = `
            <div class="no-data">
                <i class="fas fa-history"></i>
                <h3>No event history</h3>
                <p>Your attended events will appear here</p>
            </div>
        `;
        return;
    }

    historyList.innerHTML = rsvps.map(rsvp => createHistoryEventItem(rsvp)).join('');
}

function createHistoryEventItem(rsvp) {
    const event = rsvp.event;

    return `
        <div class="event-list-item">
            <div class="event-list-info">
                <h3>${event.title}</h3>
                <div class="event-list-meta">
                    <span><i class="fas fa-clock"></i> ${formatDateTime(event.dateTime)}</span>
                    <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                    <span><i class="fas fa-check"></i> Attended</span>
                </div>
            </div>
            <div class="event-list-actions">
                <button class="btn btn-sm btn-primary" onclick="viewEventDetails(${event.id})">
                    <i class="fas fa-eye"></i> View Details
                </button>
            </div>
        </div>
    `;
}

// Search and filter functions
function searchEvents() {
    const searchTerm = document.getElementById('discover-search').value.toLowerCase();

    if (!searchTerm.trim()) {
        displayDiscoverEvents(availableEvents);
        return;
    }

    const filteredEvents = availableEvents.filter(event =>
        event.title.toLowerCase().includes(searchTerm) ||
        event.location.toLowerCase().includes(searchTerm) ||
        (event.description && event.description.toLowerCase().includes(searchTerm))
    );

    displayDiscoverEvents(filteredEvents);
}

function filterDiscoverEvents(filter) {
    let filteredEvents = availableEvents;
    const now = new Date();

    switch (filter) {
        case 'upcoming':
            filteredEvents = availableEvents.filter(event => new Date(event.dateTime) > now);
            break;
        case 'available':
            // Filter events that still have capacity (this would need capacity info from API)
            filteredEvents = availableEvents.filter(event => new Date(event.dateTime) > now);
            break;
        case 'all':
        default:
            filteredEvents = availableEvents;
            break;
    }

    displayDiscoverEvents(filteredEvents);
}

function filterMyEvents(filter) {
    let filteredRSVPs = myRSVPs;
    const now = new Date();

    switch (filter) {
        case 'upcoming':
            filteredRSVPs = myRSVPs.filter(rsvp => new Date(rsvp.event.dateTime) > now);
            break;
        case 'today':
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const tomorrow = new Date(today);
            tomorrow.setDate(tomorrow.getDate() + 1);

            filteredRSVPs = myRSVPs.filter(rsvp => {
                const eventDate = new Date(rsvp.event.dateTime);
                return eventDate >= today && eventDate < tomorrow;
            });
            break;
        case 'all':
        default:
            filteredRSVPs = myRSVPs;
            break;
    }

    displayMyEvents(filteredRSVPs);
}

// Event interaction functions
async function joinEvent(eventId) {
    try {
        showLoading();
        await api.createRSVP(eventId);

        showToast('Successfully joined the event!', 'success');

        // Refresh data
        await loadDashboardData();

    } catch (error) {
        console.error('Error joining event:', error);
        handleApiError(error);
    } finally {
        hideLoading();
    }
}

function confirmCancelRSVP(eventId) {
    if (confirm('Are you sure you want to cancel your RSVP for this event?')) {
        cancelRSVP(eventId);
    }
}

async function cancelRSVP(eventId) {
    try {
        showLoading();
        await api.cancelRSVP(eventId);

        showToast('RSVP cancelled successfully', 'info');

        // Refresh data
        await loadDashboardData();

    } catch (error) {
        console.error('Error cancelling RSVP:', error);
        handleApiError(error);
    } finally {
        hideLoading();
    }
}

function viewEventDetails(eventId) {
    // Show event details in modal
    showEventDetailsModal(eventId);
}

async function showEventDetailsModal(eventId) {
    try {
        showLoading();
        const event = await api.getEventById(eventId);

        const modalContent = document.getElementById('modal-event-content');
        const modalTitle = document.getElementById('modal-event-title');

        modalTitle.textContent = event.title;
        modalContent.innerHTML = createEventDetailsContent(event);

        showModal('event-modal');

    } catch (error) {
        console.error('Error loading event details:', error);
        showToast('Error loading event details', 'error');
    } finally {
        hideLoading();
    }
}

function createEventDetailsContent(event) {
    const now = new Date();
    const eventDate = new Date(event.dateTime);
    const isUpcoming = eventDate > now;

    return `
        <div class="event-details">
            <div class="event-detail-section">
                <h4><i class="fas fa-info-circle"></i> Event Information</h4>
                <p><strong>Date & Time:</strong> ${formatDateTime(event.dateTime)}</p>
                <p><strong>Location:</strong> ${event.location}</p>
                <p><strong>Capacity:</strong> ${event.maxCapacity} attendees</p>
                <p><strong>Organizer:</strong> ${event.organizer?.username || 'Unknown'}</p>
            </div>

            ${event.description ? `
                <div class="event-detail-section">
                    <h4><i class="fas fa-align-left"></i> Description</h4>
                    <p>${event.description}</p>
                </div>
            ` : ''}

            <div class="event-detail-actions">
                ${isUpcoming ? `
                    <button class="btn btn-success" onclick="joinEvent(${event.id}); closeModal('event-modal');">
                        <i class="fas fa-plus"></i> Join Event
                    </button>
                ` : `
                    <span class="event-status past">This event has ended</span>
                `}
            </div>
        </div>
    `;
}

// Navigation functions
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
}

// Utility functions
function getTimeUntilEvent(eventDateTime) {
    const now = new Date();
    const eventDate = new Date(eventDateTime);
    const diffTime = eventDate - now;

    if (diffTime < 0) return 'Event has passed';

    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    const diffHours = Math.ceil(diffTime / (1000 * 60 * 60));

    if (diffDays > 1) {
        return `${diffDays} days`;
    } else if (diffHours > 1) {
        return `${diffHours} hours`;
    } else {
        return 'Starting soon';
    }
}

function editProfile() {
    showToast('Profile editing coming soon!', 'info');
}

function logout() {
    authManager.logout();
    window.location.href = 'index.html';
}

// Error display functions
function displayDiscoverEventsError() {
    const eventsGrid = document.getElementById('discover-events-grid');
    if (!eventsGrid) return;

    eventsGrid.innerHTML = `
        <div class="no-data">
            <i class="fas fa-exclamation-triangle"></i>
            <h3>Error loading events</h3>
            <p>Please try again later</p>
            <button class="btn btn-primary" onclick="loadAvailableEvents()">
                <i class="fas fa-refresh"></i> Retry
            </button>
        </div>
    `;
}

function displayMyEventsError() {
    const eventsList = document.getElementById('my-events-list');
    if (!eventsList) return;

    eventsList.innerHTML = `
        <div class="no-data">
            <i class="fas fa-exclamation-triangle"></i>
            <h3>Error loading your events</h3>
            <p>Please try again later</p>
            <button class="btn btn-primary" onclick="loadMyRSVPs()">
                <i class="fas fa-refresh"></i> Retry
            </button>
        </div>
    `;
}

function displayEventHistoryError() {
    const historyList = document.getElementById('history-list');
    if (!historyList) return;

    historyList.innerHTML = `
        <div class="no-data">
            <i class="fas fa-exclamation-triangle"></i>
            <h3>Error loading event history</h3>
            <p>Please try again later</p>
            <button class="btn btn-primary" onclick="loadEventHistory()">
                <i class="fas fa-refresh"></i> Retry
            </button>
        </div>
    `;
}
