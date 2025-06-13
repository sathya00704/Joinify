// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// API Helper Class
class ApiService {
    constructor() {
        this.baseURL = API_BASE_URL;
    }

    // Get JWT token from localStorage
    getToken() {
        return localStorage.getItem('jwt_token');
    }

    // Set JWT token in localStorage
    setToken(token) {
        localStorage.setItem('jwt_token', token);
    }

    // Remove JWT token from localStorage
    removeToken() {
        localStorage.removeItem('jwt_token');
    }

    // Get authorization headers
    getAuthHeaders() {
        const token = this.getToken();
        return token ? { 'Authorization': `Bearer ${token}` } : {};
    }

    // Enhanced Generic API request method with better error handling
    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
        console.log('Making API request to:', url);

        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...this.getAuthHeaders(),
                ...options.headers
            },
            ...options
        };

        try {
            const response = await fetch(url, config);
            console.log('API Response status:', response.status);

            // Handle non-JSON responses (like for boolean endpoints)
            const contentType = response.headers.get('content-type');
            let data;

            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            console.log('API Response data:', data);

            if (!response.ok) {
                const errorMessage = typeof data === 'object' ? data.message : data;
                throw new Error(errorMessage || `HTTP ${response.status}: ${response.statusText}`);
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);

            // Enhanced error handling for better debugging
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                throw new Error('Network error: Cannot connect to backend server. Please ensure Spring Boot is running on port 8080.');
            }

            throw error;
        }
    }

    // Auth API methods
    async register(userData) {
        return this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify(userData)
        });
    }

    async login(credentials) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify(credentials)
        });
    }

    async checkUsername(username) {
        return this.request(`/auth/check-username/${username}`);
    }

    async checkEmail(email) {
        return this.request(`/auth/check-email/${email}`);
    }

    // Events API methods
    async getEvents() {
        return this.request('/events');
    }

    async getUpcomingEvents() {
        return this.request('/events/upcoming');
    }

    async getPastEvents() {
        return this.request('/events/past');
    }

    async getAvailableEvents() {
        return this.request('/events/available');
    }

    async getEventById(id) {
        return this.request(`/events/${id}`);
    }

    async createEvent(eventData) {
        return this.request('/events', {
            method: 'POST',
            body: JSON.stringify(eventData)
        });
    }

    async updateEvent(id, eventData) {
        return this.request(`/events/${id}`, {
            method: 'PUT',
            body: JSON.stringify(eventData)
        });
    }

    async deleteEvent(id) {
        return this.request(`/events/${id}`, {
            method: 'DELETE'
        });
    }

    async searchEvents(keyword) {
        return this.request(`/events/search/title?keyword=${encodeURIComponent(keyword)}`);
    }

    async searchEventsByLocation(location) {
        return this.request(`/events/search/location?location=${encodeURIComponent(location)}`);
    }

    async getEventsByDateRange(startDate, endDate) {
        return this.request(`/events/date-range?startDate=${startDate}&endDate=${endDate}`);
    }

    // Organizer event methods
    async getMyEvents() {
        return this.request('/events/my-events');
    }

    async getMyUpcomingEvents() {
        return this.request('/events/my-events/upcoming');
    }

    async getMyPastEvents() {
        return this.request('/events/my-events/past');
    }

    async getEventsByOrganizer(organizerId) {
        return this.request(`/events/organizer/${organizerId}`);
    }

    async getEventCapacity(eventId) {
        return this.request(`/events/${eventId}/capacity`);
    }

    // Users API methods
    async getUserStats() {
        return this.request('/users/stats');
    }

    async getCurrentUser() {
        return this.request('/users/profile');
    }

    async updateProfile(userData) {
        return this.request('/users/profile', {
            method: 'PUT',
            body: JSON.stringify(userData)
        });
    }

    async changePassword(newPassword) {
        return this.request('/users/change-password', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `newPassword=${encodeURIComponent(newPassword)}`
        });
    }

    async getUserById(id) {
        return this.request(`/users/${id}`);
    }

    async getAllUsers() {
        return this.request('/users');
    }

    async getAllOrganizers() {
        return this.request('/users/organizers');
    }

    async getAllAttendees() {
        return this.request('/users/attendees');
    }

    async getUsersByRole(role) {
        return this.request(`/users/role/${role}`);
    }

    async deleteUser(id) {
        return this.request(`/users/${id}`, {
            method: 'DELETE'
        });
    }

    async deleteCurrentUser() {
        return this.request('/users/profile', {
            method: 'DELETE'
        });
    }

    // RSVP API methods
    async createRSVP(eventId) {
        return this.request(`/rsvp/event/${eventId}`, {
            method: 'POST'
        });
    }

    async cancelRSVP(eventId) {
        return this.request(`/rsvp/event/${eventId}`, {
            method: 'DELETE'
        });
    }

    async updateRSVPStatus(eventId, status) {
        return this.request(`/rsvp/event/${eventId}/status?status=${status}`, {
            method: 'PUT'
        });
    }

    async getRSVPStatus(eventId) {
        return this.request(`/rsvp/event/${eventId}/status`);
    }

    async checkRSVP(eventId) {
        return this.request(`/rsvp/event/${eventId}/check`);
    }

    async getEventRSVPs(eventId) {
        return this.request(`/rsvp/event/${eventId}`);
    }

    async getEventAttendees(eventId) {
        return this.request(`/rsvp/event/${eventId}/attendees`);
    }

    async getPendingRSVPs(eventId) {
        return this.request(`/rsvp/event/${eventId}/pending`);
    }

    async getMyRSVPs() {
        return this.request('/rsvp/my-rsvps');
    }

    async getMyUpcomingRSVPs() {
        return this.request('/rsvp/my-rsvps/upcoming');
    }

    async getMyPastRSVPs() {
        return this.request('/rsvp/my-rsvps/past');
    }

    async getRSVPCount(eventId) {
        return this.request(`/rsvp/event/${eventId}/count`);
    }

    async confirmPendingRSVPs(eventId) {
        return this.request(`/rsvp/event/${eventId}/confirm-pending`, {
            method: 'POST'
        });
    }

    // Test connectivity method
    async testConnection() {
        try {
            const response = await this.request('/users/stats');
            console.log('✅ Backend connection successful');
            return { success: true, data: response };
        } catch (error) {
            console.error('❌ Backend connection failed:', error.message);
            return { success: false, error: error.message };
        }
    }
}

// Create global API instance
const api = new ApiService();

// Test backend connection on load
document.addEventListener('DOMContentLoaded', async () => {
    const connectionTest = await api.testConnection();
    if (!connectionTest.success) {
        console.warn('Backend connection failed. Some features may not work.');
        if (typeof showToast === 'function') {
            showToast('Cannot connect to backend server', 'warning');
        }
    }
});
