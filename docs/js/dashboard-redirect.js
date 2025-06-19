// Dashboard redirect functionality for role-based navigation
document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    // Get form data
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    // Clear any previous error messages
    const errorElement = document.getElementById('loginError');
    if (errorElement) {
        errorElement.textContent = '';
    }

    try {
        // Attempt login using AuthManager
        const result = await authManager.login({ username, password });
        
        if (result.success) {
            // Redirect based on user role
            const role = result.user.role;
            
            if (role === 'ORGANIZER') {
                window.location.href = 'dashboard-organizer.html';
            } else if (role === 'ATTENDEE') {
                window.location.href = 'dashboard-attendee.html';
            } else {
                // Handle unknown role
                if (errorElement) {
                    errorElement.textContent = 'Unknown user role.';
                }
            }
        }
    } catch (error) {
        // Display login error to user
        if (errorElement) {
            errorElement.textContent = error.message || 'Login failed.';
        }
    }
});
