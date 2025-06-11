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

    // Generic API request method
    async request(endpoint, options = {}) {
        const url = `${this.baseURL}${endpoint}`;
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

            // Handle non-JSON responses (like for boolean endpoints)
            const contentType = response.headers.get('content-type');
            let data;

            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            if (!response.ok) {
                const errorMessage = typeof data === 'object' ? data.message : data;
                throw new Error(errorMessage || `HTTP ${response.status}: ${response.statusText}`);
            }

            return data;
        } catch (error) {
            console.error('API Error:', error);
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

    async getMyEvents() {
        return this.request('/events/my-events');
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

    async getRSVPStatus(eventId) {
        return this.request(`/rsvp/event/${eventId}/status`);
    }

    async getMyRSVPs() {
        return this.request('/rsvp/my-rsvps');
    }

    async getEventAttendees(eventId) {
        return this.request(`/rsvp/event/${eventId}/attendees`);
    }
}

// Create global API instance
const api = new ApiService();
