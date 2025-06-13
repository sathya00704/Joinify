document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value;

    try {
        // Use your AuthManager for login
        const result = await authManager.login({ username, password });
        if (result.success) {
            // Redirect based on role
            const role = result.user.role;
            if (role === 'ORGANIZER') {
                window.location.href = 'dashboard-organizer.html';
            } else if (role === 'ATTENDEE') {
                window.location.href = 'dashboard-attendee.html';
            } else {
                document.getElementById('loginError').textContent = 'Unknown user role.';
            }
        }
    } catch (error) {
        document.getElementById('loginError').textContent = error.message || 'Login failed.';
    }
});
