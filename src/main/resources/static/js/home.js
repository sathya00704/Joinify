// Home page functionality
document.addEventListener('DOMContentLoaded', function() {
    initializeHomePage();
});

async function initializeHomePage() {
    try {
        // Check authentication status
        await authManager.checkAuthStatus();

        // Load initial data
        await Promise.all([
            loadUpcomingEvents(),
            loadUserStats()
        ]);

        // Initialize event listeners
        initializeEventListeners();

    } catch (error) {
        console.error('Error initializing home page:', error);
        showToast('Error loading page data', 'error');
    }
}

function initializeEventListeners() {
    // Mobile navigation toggle
    const navToggle = document.getElementById('nav-toggle');
    const navMenu = document.getElementById('nav-menu');

    if (navToggle && navMenu) {
        navToggle.addEventListener('click', () => {
            navMenu.classList.toggle('active');
        });
    }

    // Smooth scrolling for navigation links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('href').substring(1);
            smoothScrollTo(targetId);
        });
    });

    // Form submissions
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');

    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    if (registerForm) {
        registerForm.addEventListener('submit', handleRegister);
    }
}

// Load and display upcoming events
async function loadUpcomingEvents() {
    try {
        showLoading();
        const events = await api.getUpcomingEvents();
        displayEvents(events.slice(0, 3)); // Show only first 3 events
    } catch (error) {
        console.error('Error loading events:', error);
        displayEventsError();
    } finally {
        hideLoading();
    }
}

function displayEvents(events) {
    const eventsGrid = document.getElementById('events-grid');
    if (!eventsGrid) return;

    if (events.length === 0) {
        eventsGrid.innerHTML = `
            <div class="no-events">
                <i class="fas fa-calendar-times"></i>
                <h3>No upcoming events</h3>
                <p>Be the first to create an event!</p>
                <button class="btn btn-primary" onclick="showRegister()">Get Started</button>
            </div>
        `;
        return;
    }

    eventsGrid.innerHTML = events.map(event => createEventCard(event)).join('');
}

function createEventCard(event) {
    return `
        <div class="event-card" onclick="viewEventDetails(${event.id})">
            <div class="event-image">
                <i class="fas fa-calendar-alt"></i>
            </div>
            <div class="event-content">
                <h3 class="event-title">${event.title}</h3>
                <div class="event-meta">
                    <span><i class="fas fa-clock"></i> ${formatDateTime(event.dateTime)}</span>
                    <span><i class="fas fa-map-marker-alt"></i> ${event.location}</span>
                </div>
                <p class="event-description">${truncateText(event.description || 'No description available', 100)}</p>
                <div class="event-footer">
                    <span class="event-capacity">
                        <i class="fas fa-users"></i> ${event.maxCapacity} spots
                    </span>
                    <button class="btn btn-primary btn-sm" onclick="event.stopPropagation(); viewEventDetails(${event.id})">
                        View Details
                    </button>
                </div>
            </div>
        </div>
    `;
}

function displayEventsError() {
    const eventsGrid = document.getElementById('events-grid');
    if (!eventsGrid) return;

    eventsGrid.innerHTML = `
        <div class="error-message">
            <i class="fas fa-exclamation-triangle"></i>
            <h3>Unable to load events</h3>
            <p>Please try again later.</p>
            <button class="btn btn-outline" onclick="loadUpcomingEvents()">Retry</button>
        </div>
    `;
}

// Load and display user statistics
async function loadUserStats() {
    try {
        const stats = await api.getUserStats();
        updateStatsDisplay(stats);
    } catch (error) {
        console.error('Error loading stats:', error);
        // Set default values if API fails
        updateStatsDisplay({
            total: 0,
            organizers: 0,
            attendees: 0
        });
    }
}

function updateStatsDisplay(stats) {
    // Animate counters with staggered timing
    setTimeout(() => animateCounter('total-events', stats.total || 0), 200);
    setTimeout(() => animateCounter('total-users', stats.total || 0), 400);
    setTimeout(() => animateCounter('total-organizers', stats.organizers || 0), 600);
    setTimeout(() => animateCounter('total-attendees', stats.attendees || 0), 800);
}

// Modal functions
function showLogin() {
    clearForm('login-form');
    showModal('login-modal');
}

function showRegister() {
    clearForm('register-form');
    showModal('register-modal');
}

function closeModal(modalId) {
    hideModal(modalId);
}

function switchToRegister() {
    hideModal('login-modal');
    showRegister();
}

function switchToLogin() {
    hideModal('register-modal');
    showLogin();
}

// Authentication handlers with role-based redirection
async function handleLogin(e) {
    e.preventDefault();

    const credentials = getFormData('login-form');

    // Validate form
    const errors = authManager.validateLoginForm(credentials);
    if (errors.length > 0) {
        showFormErrors('login-form', errors);
        return;
    }

    try {
        showLoading();
        const result = await authManager.login(credentials);

        if (result.success) {
            showToast('Login successful!', 'success');
            hideModal('login-modal');

            // Debug the authentication state
            authManager.debugUserInfo();

            const userRole = authManager.getCurrentUserRole();
            console.log('Final role check:', userRole);

            setTimeout(() => {
                if (userRole === 'ORGANIZER') {
                    console.log('Redirecting to organizer dashboard');
                    window.location.href = 'dashboard-organizer.html';
                } else if (userRole === 'ATTENDEE') {
                    console.log('Redirecting to attendee dashboard');
                    window.location.href = 'dashboard-attendee.html';
                } else {
                    console.log('Role not recognized, staying on home page. Role:', userRole);
                    window.location.href = 'index.html';
                }
            }, 1000);
        }
    } catch (error) {
        handleApiError(error);
    } finally {
        hideLoading();
    }
}



async function handleRegister(e) {
    e.preventDefault();

    const userData = getFormData('register-form');

    // Validate form
    const errors = authManager.validateRegistrationForm(userData);
    if (errors.length > 0) {
        showFormErrors('register-form', errors);
        return;
    }

    try {
        showLoading();
        const result = await authManager.register(userData);

        if (result.success) {
            showToast('Registration successful! Please login.', 'success');
            hideModal('register-modal');
            setTimeout(() => {
                showLogin();
            }, 1000);
        }
    } catch (error) {
        handleApiError(error);
    } finally {
        hideLoading();
    }
}

// Navigation functions with role-based redirection
function redirectToDashboard() {
    if (authManager.isLoggedIn()) {
        const userRole = authManager.getCurrentUserRole();

        if (userRole === 'ORGANIZER') {
            window.location.href = 'dashboard-organizer.html';
        } else if (userRole === 'ATTENDEE') {
            window.location.href = 'dashboard-attendee.html';
        } else {
            window.location.href = 'dashboard.html';
        }
    } else {
        showLogin();
    }
}

function viewAllEvents() {
    if (authManager.isLoggedIn()) {
        const userRole = authManager.getCurrentUserRole();
        if (userRole === 'ATTENDEE') {
            window.location.href = 'dashboard-attendee.html#discover';
        } else {
            window.location.href = 'event-list.html';
        }
    } else {
        window.location.href = 'event-list.html';
    }
}

function viewEventDetails(eventId) {
    window.location.href = `event-details.html?id=${eventId}`;
}

// Update navigation for logged in users with role-based links
function updateNavigationForLoggedInUser() {
    const navAuth = document.querySelector('.nav-auth');
    if (!navAuth) return;

    const userRole = authManager.getCurrentUserRole();
    let dashboardText = 'Dashboard';
    let dashboardUrl = 'dashboard.html';

    if (userRole === 'ORGANIZER') {
        dashboardText = 'Organizer Dashboard';
        dashboardUrl = 'dashboard-organizer.html';
    } else if (userRole === 'ATTENDEE') {
        dashboardText = 'My Events';
        dashboardUrl = 'dashboard-attendee.html';
    }

    navAuth.innerHTML = `
        <button class="btn btn-outline" onclick="window.location.href='${dashboardUrl}'">
            ${dashboardText}
        </button>
        <button class="btn btn-primary" onclick="logout()">
            Logout
        </button>
    `;
}

// Enhanced logout function
function logout() {
    authManager.logout();
    showToast('Logged out successfully', 'info');
    setTimeout(() => {
        window.location.reload();
    }, 1000);
}

// Check authentication status and update UI accordingly
async function checkAuthenticationStatus() {
    if (authManager.isLoggedIn()) {
        try {
            // Verify token is still valid
            await authManager.checkAuthStatus();
            updateNavigationForLoggedInUser();
        } catch (error) {
            // Token is invalid, logout user
            authManager.logout();
        }
    }
}

// Initialize authentication check when page loads
document.addEventListener('DOMContentLoaded', function() {
    checkAuthenticationStatus();
});

// Add these script tags to your HTML head section
function loadRequiredScripts() {
    const scripts = [
        'js/api.js',
        'js/auth.js',
        'js/utils.js',
        'js/home.js'
    ];

    scripts.forEach(src => {
        const script = document.createElement('script');
        script.src = src;
        document.head.appendChild(script);
    });
}
