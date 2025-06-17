// Attendee Dashboard JavaScript
class AttendeeDashboard {
    constructor() {
        this.currentUser = null;
        this.myRSVPs = [];
        this.allEvents = [];
        this.searchTimeout = null;
        this.init();
    }

    async init() {
        try {
            await this.checkAuthentication();
            await this.loadDashboardData();
            this.setupEventListeners();
            this.showSection('overview');
        } catch (error) {
            showToast('Failed to load dashboard', 'error');
        }
    }

    async checkAuthentication() {
        try {
            await authManager.checkAuthStatus();
            if (!authManager.isLoggedIn() || authManager.getCurrentUserRole() !== 'ATTENDEE') {
                window.location.href = 'index.html';
                return;
            }
            this.currentUser = authManager.currentUser;
            if (this.currentUser && this.currentUser.username) {
                document.getElementById('user-name').textContent = this.currentUser.username;
            }
        } catch (error) {
            showToast('Authentication failed', 'error');
        }
    }

    async loadDashboardData() {
        showLoading();
        try {
            // Load user RSVPs and available events in parallel
            const [myRSVPs, allEvents] = await Promise.all([
                api.getMyRSVPs().catch(err => {
                    return [];
                }),
                api.getUpcomingEvents().catch(err => {
                    return [];
                })
            ]);

            this.myRSVPs = myRSVPs || [];
            this.allEvents = allEvents || [];

            // Update all dashboard sections
            this.updateStats();
            this.loadUpcomingEvents();
            this.loadDiscoverEvents();
            this.loadMyEvents();
            this.loadEventHistory();

        } catch (error) {
            showToast('Failed to load dashboard data: ' + error.message, 'error');
        } finally {
            hideLoading();
        }
    }

    updateStats() {
        try {
            const now = new Date();

            // Calculate upcoming confirmed RSVPs
            const upcomingRSVPs = this.myRSVPs.filter(rsvp => {
                if (!rsvp || !rsvp.event || !rsvp.event.dateTime) return false;
                try {
                    return new Date(rsvp.event.dateTime) > now && rsvp.status === 'CONFIRMED';
                } catch (error) {
                    return false;
                }
            });

            // Calculate past confirmed RSVPs
            const pastRSVPs = this.myRSVPs.filter(rsvp => {
                if (!rsvp || !rsvp.event || !rsvp.event.dateTime) return false;
                try {
                    return new Date(rsvp.event.dateTime) < now && rsvp.status === 'CONFIRMED';
                } catch (error) {
                    return false;
                }
            });

            // Calculate available events (not already RSVP'd)
            const rsvpEventIds = this.myRSVPs
                .filter(rsvp => rsvp.event && rsvp.event.id)
                .map(rsvp => rsvp.event.id);

            const availableEvents = this.allEvents.filter(event =>
                event && event.id && !rsvpEventIds.includes(event.id)
            );

            // Update stats display
            document.getElementById('upcoming-rsvps').textContent = upcomingRSVPs.length;
            document.getElementById('total-rsvps').textContent = this.myRSVPs.length;
            document.getElementById('events-attended').textContent = pastRSVPs.length;
            document.getElementById('available-events').textContent = availableEvents.length;

        } catch (error) {
            // Handle stats calculation errors silently
        }
    }

    loadUpcomingEvents() {
        try {
            const now = new Date();

            // Filter and sort upcoming confirmed events
            const upcomingEvents = this.myRSVPs.filter(rsvp => {
                if (!rsvp || !rsvp.event || !rsvp.event.dateTime) return false;
                try {
                    return new Date(rsvp.event.dateTime) > now && rsvp.status === 'CONFIRMED';
                } catch (error) {
                    return false;
                }
            }).sort((a, b) => new Date(a.event.dateTime) - new Date(b.event.dateTime));

            const container = document.getElementById('upcoming-events-list');
            if (!container) return;

            if (upcomingEvents.length === 0) {
                container.innerHTML = '<p class="no-data">No upcoming events</p>';
                return;
            }

            // Show only the next upcoming event with link to view all
            const nextEvent = upcomingEvents[0];
            container.innerHTML = `
                <div class="event-list-item">
                    <div class="event-list-info">
                        <h3>${nextEvent.event.title}</h3>
                        <div class="event-list-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(nextEvent.event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${nextEvent.event.location}</span>
                            <span class="event-status confirmed">Confirmed</span>
                        </div>
                    </div>
                    <div class="event-list-actions">
                        <button class="btn btn-sm btn-secondary" onclick="attendeeDashboard.viewEventDetails(${nextEvent.event.id})">
                            View Details
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="attendeeDashboard.confirmCancelRSVP(${nextEvent.event.id}, '${nextEvent.event.title.replace(/'/g, "\\'")}')">
                            Cancel RSVP
                        </button>
                    </div>
                </div>
                ${upcomingEvents.length > 1 ?
                    `<div class="more-events-info">
                        <p style="text-align: center; margin-top: 15px; color: #666;">
                            <i class="fas fa-info-circle"></i>
                            You have ${upcomingEvents.length - 1} more upcoming event${upcomingEvents.length > 2 ? 's' : ''}.
                            <a href="#" onclick="showSection('my-events')" style="color: #007bff; text-decoration: none;">View all</a>
                        </p>
                    </div>` : ''
                }
            `;
        } catch (error) {
            // Handle upcoming events loading errors silently
        }
    }

    async loadDiscoverEvents() {
        try {
            const container = document.getElementById('discover-events-grid');
            if (!container) return;

            if (!this.allEvents || this.allEvents.length === 0) {
                container.innerHTML = '<p class="no-data">No events available</p>';
                return;
            }

            const now = new Date();
            const rsvpEventIds = this.myRSVPs
                .filter(rsvp => rsvp.event && rsvp.event.id)
                .map(rsvp => rsvp.event.id);

            // Filter for future events not already RSVP'd
            const availableEvents = this.allEvents.filter(event => {
                if (!event || !event.id || !event.dateTime) return false;
                const isFuture = new Date(event.dateTime) > now;
                const notRSVPd = !rsvpEventIds.includes(event.id);
                return isFuture && notRSVPd;
            });

            const sortedEvents = availableEvents.sort((a, b) =>
                new Date(a.dateTime) - new Date(b.dateTime)
            );

            if (sortedEvents.length === 0) {
                container.innerHTML = '<p class="no-data">No upcoming events available</p>';
                return;
            }

            // Get RSVP counts for each event
            const eventsWithCounts = await Promise.all(
                sortedEvents.map(async (event) => {
                    try {
                        const rsvpCount = await api.getRSVPCount(event.id);
                        return {
                            ...event,
                            currentRSVPs: rsvpCount.confirmed || 0,
                            isAtCapacity: rsvpCount.confirmed >= event.maxCapacity
                        };
                    } catch (error) {
                        return {
                            ...event,
                            currentRSVPs: 0,
                            isAtCapacity: false
                        };
                    }
                })
            );

            // Render event cards
            container.innerHTML = eventsWithCounts.map(event => {
                // Determine event status and availability
                let statusClass, statusText, showStatus = true;

                if (event.registrationStatus === 'CLOSED') {
                    statusClass = 'registration-closed';
                    statusText = 'Registration Closed';
                } else if (event.isAtCapacity) {
                    statusClass = 'not-available';
                    statusText = 'Not Available';
                } else {
                    showStatus = false; // Hide status when registration is open and not at capacity
                }

                // Determine RSVP button state
                const rsvpButton = event.isAtCapacity ?
                    `<button class="btn btn-sm btn-secondary" disabled>Full</button>` :
                    event.registrationStatus === 'CLOSED' ?
                    `<button class="btn btn-sm btn-secondary" disabled>Registration Closed</button>` :
                    `<button class="btn btn-sm btn-primary" onclick="attendeeDashboard.confirmRSVPToEvent(${event.id}, '${event.title.replace(/'/g, "\\'")}')">RSVP</button>`;

                const eventImage = renderEventImage(event);
                const eventFee = renderEventFee(event.fee);
                const statusDisplay = showStatus ? `<div class="event-status ${statusClass}">${statusText}</div>` : '';

                return `
                    <div class="event-card">
                        ${eventImage}
                        <div class="event-card-header">
                            <div class="event-card-title">${event.title}</div>
                            <div class="event-card-meta">
                                <span><i class="fas fa-calendar"></i> ${this.formatDateTime(event.dateTime)}</span>
                                <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                                <span><i class="fas fa-users"></i> ${event.currentRSVPs}/${event.maxCapacity} capacity</span>
                                ${eventFee}
                            </div>
                            <div class="event-card-description">${event.description || 'No description available'}</div>
                        </div>
                        <div class="event-card-footer">
                            ${statusDisplay}
                            <div class="event-actions">
                                <button class="btn btn-sm btn-secondary" onclick="attendeeDashboard.viewEventDetails(${event.id})">
                                    Details
                                </button>
                                ${rsvpButton}
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        } catch (error) {
            // Handle discover events loading errors silently
        }
    }

    loadMyEvents() {
        try {
            const now = new Date();

            // Filter for upcoming events (all statuses)
            const upcomingEvents = this.myRSVPs.filter(rsvp => {
                if (!rsvp || !rsvp.event || !rsvp.event.dateTime) return false;
                try {
                    const eventDate = new Date(rsvp.event.dateTime);
                    return eventDate > now;
                } catch (error) {
                    return false;
                }
            });

            const sortedUpcomingEvents = upcomingEvents.sort((a, b) =>
                new Date(a.event.dateTime) - new Date(b.event.dateTime)
            );

            const container = document.getElementById('my-events-list');
            if (!container) return;

            if (sortedUpcomingEvents.length === 0) {
                container.innerHTML = '<p class="no-data">No upcoming events</p>';
                return;
            }

            // Render upcoming events with appropriate actions
            container.innerHTML = sortedUpcomingEvents.map(rsvp => `
                <div class="event-list-item">
                    <div class="event-list-info">
                        <h3>${rsvp.event.title}</h3>
                        <div class="event-list-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(rsvp.event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${rsvp.event.location}</span>
                            <span class="event-status ${rsvp.status.toLowerCase()}">${rsvp.status}</span>
                        </div>
                    </div>
                    <div class="event-list-actions">
                        <button class="btn btn-sm btn-secondary" onclick="attendeeDashboard.viewEventDetails(${rsvp.event.id})">
                            View Details
                        </button>
                        ${rsvp.status === 'CONFIRMED' ?
                            `<button class="btn btn-sm btn-danger" onclick="attendeeDashboard.confirmCancelRSVP(${rsvp.event.id}, '${rsvp.event.title.replace(/'/g, "\\'")}')">Cancel RSVP</button>` :
                            `<button class="btn btn-sm btn-success" onclick="attendeeDashboard.confirmRSVP(${rsvp.event.id})">Confirm</button>`
                        }
                    </div>
                </div>
            `).join('');

        } catch (error) {
            const container = document.getElementById('my-events-list');
            if (container) {
                container.innerHTML = '<p class="error-message">Error loading your events</p>';
            }
        }
    }

    loadEventHistory() {
        try {
            const now = new Date();

            // Filter for past completed events
            const pastEvents = this.myRSVPs.filter(rsvp => {
                if (!rsvp || !rsvp.event || !rsvp.event.dateTime) return false;
                try {
                    const eventDate = new Date(rsvp.event.dateTime);
                    const isPast = eventDate < now;
                    const isCompleted = rsvp.status === 'CONFIRMED' || rsvp.status === 'ATTENDED';
                    return isPast && isCompleted;
                } catch (error) {
                    return false;
                }
            });

            const sortedPastEvents = pastEvents.sort((a, b) =>
                new Date(b.event.dateTime) - new Date(a.event.dateTime)
            );

            const container = document.getElementById('history-list');
            if (!container) return;

            if (sortedPastEvents.length === 0) {
                container.innerHTML = '<p class="no-data">No past events</p>';
                return;
            }

            // Render past events
            container.innerHTML = sortedPastEvents.map(rsvp => `
                <div class="event-list-item">
                    <div class="event-list-info">
                        <h3>${rsvp.event.title}</h3>
                        <div class="event-list-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(rsvp.event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${rsvp.event.location}</span>
                            <span class="event-status past">Completed</span>
                        </div>
                    </div>
                    <div class="event-list-actions">
                        <button class="btn btn-sm btn-secondary" onclick="attendeeDashboard.viewEventDetails(${rsvp.event.id})">
                            View Details
                        </button>
                    </div>
                </div>
            `).join('');

        } catch (error) {
            // Handle event history loading errors silently
        }
    }

    showSection(sectionName) {
        try {
            // Hide all sections and remove active states
            document.querySelectorAll('.content-section').forEach(section => {
                section.classList.remove('active');
            });
            document.querySelectorAll('.menu-item').forEach(item => {
                item.classList.remove('active');
            });

            // Show target section and set active menu item
            const targetSection = document.getElementById(sectionName + '-section');
            if (targetSection) {
                targetSection.classList.add('active');
            }

            const menuItem = document.querySelector(`[onclick="showSection('${sectionName}')"]`);
            if (menuItem) {
                menuItem.classList.add('active');
            }
        } catch (error) {
            // Handle section switching errors silently
        }
    }

    async confirmRSVPToEvent(eventId, eventTitle) {
        const confirmed = confirm(`Are you sure you want to RSVP to "${eventTitle}"?\n\nThis will confirm your attendance at the event.`);
        if (confirmed) {
            await this.rsvpToEvent(eventId);
        }
    }

    async confirmCancelRSVP(eventId, eventTitle) {
        const confirmed = confirm(`Are you sure you want to cancel your RSVP for "${eventTitle}"?\n\nFeel free to RSVP later if you change your mind, subject to availability.`);
        if (confirmed) {
            await this.cancelRSVP(eventId);
        }
    }

    async rsvpToEvent(eventId) {
        try {
            showLoading();
            await api.createRSVP(eventId);
            showToast('RSVP successful!', 'success');
            await this.loadDashboardData();
        } catch (error) {
            showToast(error.message || 'RSVP failed', 'error');
        } finally {
            hideLoading();
        }
    }

    async cancelRSVP(eventId) {
        try {
            showLoading();
            await api.cancelRSVP(eventId);
            showToast('RSVP cancelled successfully', 'success');
            await this.loadDashboardData();
        } catch (error) {
            showToast(error.message || 'Failed to cancel RSVP', 'error');
        } finally {
            hideLoading();
        }
    }

    async confirmRSVP(eventId) {
        try {
            showLoading();
            await api.updateRSVPStatus(eventId, 'CONFIRMED');
            showToast('RSVP confirmed!', 'success');
            await this.loadDashboardData();
        } catch (error) {
            showToast(error.message || 'Failed to confirm RSVP', 'error');
        } finally {
            hideLoading();
        }
    }

    async viewEventDetails(eventId) {
        try {
            // Load event details and RSVP count
            const event = await api.getEventById(eventId);
            const rsvpCount = await api.getRSVPCount(eventId);

            // Prepare event image display
            const eventImage = event.imageUrl
                ? `<div class="modal-event-image">
                     <img src="${event.imageUrl}"
                          alt="${event.title}"
                          onclick="zoomImage('${event.imageUrl}', '${event.title.replace(/'/g, "\\'")}')">
                     <div class="image-zoom-hint">
                         <i class="fas fa-search-plus"></i> Click to zoom
                     </div>
                   </div>`
                : '';

            // Prepare fee and payment information
            const eventFee = event.fee && event.fee > 0
                ? `<p><strong>Fee:</strong> Rs. ${parseFloat(event.fee).toFixed(2)}</p>`
                : '<p><strong>Fee:</strong> Free</p>';

            const paymentInfo = event.fee && event.fee > 0
                ? '<p><strong>Payment:</strong> Payment details will be provided directly by organizer</p>'
                : '';

            // Update modal content
            document.getElementById('modal-event-title').textContent = event.title;
            document.getElementById('modal-event-content').innerHTML = `
                <div class="event-details scrollable-content">
                    ${eventImage}
                    <div class="event-info">
                        <p><strong>Description:</strong> ${event.description || 'No description available'}</p>
                        <p><strong>Date & Time:</strong> ${this.formatDateTime(event.dateTime)}</p>
                        <p><strong>Location:</strong> ${event.location}</p>
                        <p><strong>Capacity:</strong> ${event.maxCapacity}</p>
                        <p><strong>Current RSVPs:</strong> ${rsvpCount.confirmed || 0}</p>
                        <p><strong>Available Spots:</strong> ${event.maxCapacity - (rsvpCount.confirmed || 0)}</p>
                        ${eventFee}
                        ${paymentInfo}
                        <p><strong>Organizer:</strong> ${event.organizer?.username || 'Unknown'}</p>
                    </div>
                </div>
            `;

            showModal('event-modal');
        } catch (error) {
            showToast('Failed to load event details', 'error');
        }
    }

    async searchEvents() {
        const keyword = document.getElementById('discover-search').value.trim();
        if (!keyword) {
            this.loadDiscoverEvents();
            return;
        }

        try {
            showLoading();
            const searchResults = await api.searchEvents(keyword);

            // Filter for upcoming events only
            const now = new Date();
            const upcomingSearchResults = searchResults.filter(event => {
                if (!event || !event.dateTime) return false;
                try {
                    return new Date(event.dateTime) > now;
                } catch (error) {
                    return false;
                }
            });

            await this.renderFilteredDiscoverEvents(upcomingSearchResults);
        } catch (error) {
            showToast('Search failed', 'error');
        } finally {
            hideLoading();
        }
    }

    async renderFilteredDiscoverEvents(events) {
        const container = document.getElementById('discover-events-grid');
        if (!container) return;

        if (!events || events.length === 0) {
            container.innerHTML = '<p class="no-data">No upcoming events found</p>';
            return;
        }

        const now = new Date();
        const upcomingEvents = events.filter(event => {
            if (!event || !event.dateTime) return false;
            try {
                return new Date(event.dateTime) > now;
            } catch (error) {
                return false;
            }
        });

        // Filter out events already RSVP'd
        const rsvpEventIds = this.myRSVPs
            .filter(rsvp => rsvp.event && rsvp.event.id)
            .map(rsvp => rsvp.event.id);

        const availableEvents = upcomingEvents.filter(event =>
            event && event.id && !rsvpEventIds.includes(event.id)
        );

        if (availableEvents.length === 0) {
            container.innerHTML = '<p class="no-data">No upcoming events available</p>';
            return;
        }

        // Get RSVP counts for filtered events
        const eventsWithCounts = await Promise.all(
            availableEvents.map(async (event) => {
                try {
                    const rsvpCount = await api.getRSVPCount(event.id);
                    return {
                        ...event,
                        currentRSVPs: rsvpCount.confirmed || 0,
                        isAtCapacity: rsvpCount.confirmed >= event.maxCapacity
                    };
                } catch (error) {
                    return {
                        ...event,
                        currentRSVPs: 0,
                        isAtCapacity: false
                    };
                }
            })
        );

        // Render filtered events
        container.innerHTML = eventsWithCounts.map(event => {
            // Determine event status
            let statusClass, statusText, showStatus = true;

            if (event.registrationStatus === 'CLOSED') {
                statusClass = 'registration-closed';
                statusText = 'Registration Closed';
            } else if (event.isAtCapacity) {
                statusClass = 'not-available';
                statusText = 'Not Available';
            } else {
                showStatus = false;
            }

            const rsvpButton = event.isAtCapacity
                ? `<button class="btn btn-sm btn-secondary" disabled>Full</button>`
                : event.registrationStatus === 'CLOSED'
                ? `<button class="btn btn-sm btn-secondary" disabled>Registration Closed</button>`
                : `<button class="btn btn-sm btn-primary" onclick="attendeeDashboard.confirmRSVPToEvent(${event.id}, '${event.title.replace(/'/g, "\\'")}')">RSVP</button>`;

            const eventImage = renderEventImage(event);
            const eventFee = renderEventFee(event.fee);
            const statusDisplay = showStatus ? `<div class="event-status ${statusClass}">${statusText}</div>` : '';

            return `
                <div class="event-card">
                    ${eventImage}
                    <div class="event-card-header">
                        <div class="event-card-title">${event.title}</div>
                        <div class="event-card-meta">
                            <span><i class="fas fa-calendar"></i> ${this.formatDateTime(event.dateTime)}</span>
                            <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                            <span><i class="fas fa-users"></i> ${event.currentRSVPs}/${event.maxCapacity} capacity</span>
                            ${eventFee}
                        </div>
                        <div class="event-card-description">${event.description || 'No description available'}</div>
                    </div>
                    <div class="event-card-footer">
                        ${statusDisplay}
                        <div class="event-actions">
                            <button class="btn btn-sm btn-secondary" onclick="attendeeDashboard.viewEventDetails(${event.id})">
                                Details
                            </button>
                            ${rsvpButton}
                        </div>
                    </div>
                </div>
            `;
        }).join('');
    }

    formatDateTime(dateTimeString) {
        if (!dateTimeString) return 'Date TBD';

        try {
            const date = new Date(dateTimeString);
            if (isNaN(date.getTime())) return 'Invalid Date';

            return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
        } catch (error) {
            return 'Date Error';
        }
    }

    setupEventListeners() {
        const searchInput = document.getElementById('discover-search');
        if (searchInput) {
            // Real-time search with debouncing
            searchInput.addEventListener('input', (e) => {
                clearTimeout(this.searchTimeout);
                this.searchTimeout = setTimeout(() => {
                    this.searchEvents();
                }, 300); // 300ms delay to avoid too many API calls
            });

            // Enter key search
            searchInput.addEventListener('keypress', (e) => {
                if (e.key === 'Enter') {
                    clearTimeout(this.searchTimeout);
                    this.searchEvents();
                }
            });
        }
    }
}

// Global functions for HTML onclick handlers
function showSection(sectionName) {
    if (window.attendeeDashboard) {
        window.attendeeDashboard.showSection(sectionName);
    }
}

function searchEvents() {
    if (window.attendeeDashboard) {
        window.attendeeDashboard.searchEvents();
    }
}

function editProfile() {
    showToast('Contact Administrator at support@joinify.com', 'info');
}

function logout() {
    authManager.logout();
    window.location.href = 'index.html';
}

function closeModal(modalId) {
    hideModal(modalId);
}

// Enhanced image display function
function renderEventImage(event) {
    if (!event.imageUrl) {
        return '';
    }

    return `
        <div class="event-image-container">
            <img src="${event.imageUrl}"
                 alt="${event.title}"
                 class="event-image"
                 onclick="zoomImage('${event.imageUrl}', '${event.title.replace(/'/g, "\\'")}')">
            <div class="zoom-overlay">
                <i class="fas fa-search-plus"></i>
                <span>Click to zoom</span>
            </div>
        </div>
    `;
}

// Image zoom functionality
function zoomImage(imageUrl, title) {
    const modal = document.createElement('div');
    modal.className = 'image-zoom-modal';
    modal.innerHTML = `
        <div class="image-zoom-overlay" onclick="closeImageZoom()">
            <div class="image-zoom-container">
                <div class="image-zoom-header">
                    <h3>${title}</h3>
                    <button class="zoom-close" onclick="closeImageZoom()">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="image-zoom-content">
                    <img src="${imageUrl}"
                         alt="${title}"
                         class="zoomed-image"
                         onload="this.style.opacity='1'"
                         onerror="this.src=''; this.alt='Image not available';">
                </div>
            </div>
        </div>
    `;

    document.body.appendChild(modal);
    modal.style.display = 'flex';
    document.body.style.overflow = 'hidden'; // Prevent body scroll
}

function closeImageZoom() {
    const modal = document.querySelector('.image-zoom-modal');
    if (modal) {
        modal.remove();
        document.body.style.overflow = 'auto';
    }
}

// Enhanced fee display function
function renderEventFee(fee) {
    if (!fee || fee === 0) {
        return '<span class="event-fee free">Free</span>';
    }

    const formattedFee = parseFloat(fee).toFixed(2);
    return `<span class="event-fee paid">Rs. ${formattedFee}</span>`;
}

// Initialize dashboard when page loads
document.addEventListener('DOMContentLoaded', () => {
    window.attendeeDashboard = new AttendeeDashboard();
});

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('event-modal');
    if (event.target === modal) {
        hideModal('event-modal');
    }
}
