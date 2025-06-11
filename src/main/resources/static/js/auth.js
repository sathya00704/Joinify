// Authentication management
class AuthManager {
    constructor() {
        this.currentUser = null;
        this.isAuthenticated = false;
    }

    // Check if user is logged in
    isLoggedIn() {
        return !!api.getToken();
    }

    // Get current user role
    // Get current user role
    getCurrentUserRole() {
        // First try to get from stored user info
        if (this.currentUser && this.currentUser.role) {
            console.log('Role from stored user:', this.currentUser.role);
            return this.currentUser.role;
        }

        // Fallback to JWT token decoding
        const token = api.getToken();
        if (!token) return null;

        try {
            // Decode JWT token to get user info
            const payload = JSON.parse(atob(token.split('.')[1]));
            console.log('JWT Payload:', payload); // Debug log

            // Check different possible role field names from Spring Boot JWT
            const role = payload.role ||
                        payload.authorities?.[0]?.authority?.replace('ROLE_', '') ||
                        payload.scope ||
                        payload.sub ||
                        null;

            console.log('Role from JWT:', role);
            return role;
        } catch (error) {
            console.error('Error decoding token:', error);
            return null;
        }
    }


    // Login user
    async login(credentials) {
        try {
            const response = await api.login(credentials);

            if (response.token) {
                api.setToken(response.token);
                this.isAuthenticated = true;

                // Store user info from login response
                this.currentUser = {
                    username: response.username,
                    email: response.email,
                    role: response.role // Make sure this matches your backend response
                };

                console.log('Login response:', response); // Debug log
                console.log('User role:', response.role); // Debug log

                // Update UI
                this.updateUIForLoggedInUser();

                return { success: true, user: this.currentUser };
            } else {
                throw new Error(response.message || 'Login failed');
            }
        } catch (error) {
            console.error('Login error:', error);
            throw error;
        }
    }


    // Register user
    async register(userData) {
        try {
            const response = await api.register(userData);

            if (response.success) {
                return { success: true, message: response.message };
            } else {
                throw new Error(response.message || 'Registration failed');
            }
        } catch (error) {
            console.error('Registration error:', error);
            throw error;
        }
    }

    // Logout user
    logout() {
        api.removeToken();
        this.isAuthenticated = false;
        this.currentUser = null;
        this.updateUIForLoggedOutUser();
    }

    // Update navigation for logged in user
    updateUIForLoggedInUser() {
        const navAuth = document.querySelector('.nav-auth');
        if (navAuth) {
            const role = this.getCurrentUserRole();
            console.log('Updating UI for role:', role);

            let dashboardText = 'Dashboard';
            let dashboardUrl = 'index.html';

            if (role === 'ORGANIZER') {
                dashboardText = 'Organizer Dashboard';
                dashboardUrl = 'dashboard-organizer.html';
            } else if (role === 'ATTENDEE') {
                dashboardText = 'My Events';
                dashboardUrl = 'dashboard-attendee.html';
            }

            navAuth.innerHTML = `
                <button class="btn btn-outline" onclick="window.location.href='${dashboardUrl}'">
                    ${dashboardText}
                </button>
                <button class="btn btn-primary" onclick="authManager.logout()">
                    Logout
                </button>
            `;
        }
    }

    // Update navigation for logged out user
    updateUIForLoggedOutUser() {
        const navAuth = document.querySelector('.nav-auth');
        if (navAuth) {
            navAuth.innerHTML = `
                <button class="btn btn-outline" onclick="showLogin()">Login</button>
                <button class="btn btn-primary" onclick="showRegister()">Sign Up</button>
            `;
        }
    }

    // Check authentication status on page load
    async checkAuthStatus() {
        if (this.isLoggedIn()) {
            try {
                // Verify token is still valid by making an API call
                const user = await api.getCurrentUser();
                this.isAuthenticated = true;

                // Store user info including role
                this.currentUser = {
                    username: user.username,
                    email: user.email,
                    role: user.role
                };

                console.log('Auth check - stored user:', this.currentUser);
                this.updateUIForLoggedInUser();
            } catch (error) {
                // Token is invalid, remove it
                console.error('Token validation failed:', error);
                this.logout();
            }
        }
    }


    // Validate form inputs
    validateLoginForm(credentials) {
        const errors = [];

        if (!credentials.username || credentials.username.trim().length < 3) {
            errors.push('Username must be at least 3 characters long');
        }

        if (!credentials.password || credentials.password.length < 6) {
            errors.push('Password must be at least 6 characters long');
        }

        return errors;
    }

    validateRegistrationForm(userData) {
        const errors = [];

        if (!userData.username || userData.username.trim().length < 3) {
            errors.push('Username must be at least 3 characters long');
        }

        if (!userData.email || !this.isValidEmail(userData.email)) {
            errors.push('Please enter a valid email address');
        }

        if (!userData.password || userData.password.length < 8) {
            errors.push('Password must be at least 8 characters long');
        }

        if (!this.isStrongPassword(userData.password)) {
            errors.push('Password must contain uppercase, lowercase, number, and special character');
        }

        if (!userData.role) {
            errors.push('Please select a role');
        }

        return errors;
    }

    isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    isStrongPassword(password) {
        const strongPasswordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!*()]).{8,}$/;
        return strongPasswordRegex.test(password);
    }

    // Add this method for debugging
    debugUserInfo() {
        console.log('=== AUTH DEBUG INFO ===');
        console.log('Is logged in:', this.isLoggedIn());
        console.log('Current user:', this.currentUser);
        console.log('Token exists:', !!api.getToken());
        console.log('Detected role:', this.getCurrentUserRole());
        console.log('=====================');
    }

}

// Create global auth manager instance
const authManager = new AuthManager();
