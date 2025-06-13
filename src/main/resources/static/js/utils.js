// Enhanced Utility functions for the Joinify application

// Loading management
function showLoading() {
    const loadingOverlay = document.getElementById('loading-overlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'flex';
    }
}

function hideLoading() {
    const loadingOverlay = document.getElementById('loading-overlay');
    if (loadingOverlay) {
        loadingOverlay.style.display = 'none';
    }
}

// Enhanced Toast notifications with better styling and positioning
function showToast(message, type = 'info', duration = 5000) {
    const toastContainer = document.getElementById('toast-container') || createToastContainer();

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <div class="toast-content">
            <i class="fas fa-${getToastIcon(type)}"></i>
            <span>${message}</span>
            <button class="toast-close" onclick="this.parentElement.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;

    // Add animation class
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(20px)';
    toastContainer.appendChild(toast);

    // Animate in
    setTimeout(() => {
        toast.style.opacity = '1';
        toast.style.transform = 'translateY(0)';
        toast.style.transition = 'all 0.3s ease';
    }, 10);

    // Auto remove after duration
    setTimeout(() => {
        if (toast.parentNode) {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(-20px)';
            setTimeout(() => {
                if (toast.parentNode) {
                    toast.remove();
                }
            }, 300);
        }
    }, duration);
}

function createToastContainer() {
    const container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    container.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        z-index: 10000;
        display: flex;
        flex-direction: column;
        gap: 10px;
    `;
    document.body.appendChild(container);
    return container;
}

function getToastIcon(type) {
    const icons = {
        success: 'check-circle',
        error: 'exclamation-circle',
        warning: 'exclamation-triangle',
        info: 'info-circle'
    };
    return icons[type] || 'info-circle';
}

// Enhanced Date and time formatting for better user experience
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return '';

    const date = new Date(dateTimeString);
    const now = new Date();
    const diffInHours = (date - now) / (1000 * 60 * 60);

    // Show relative time for events within 24 hours
    if (Math.abs(diffInHours) < 24) {
        return date.toLocaleDateString('en-US', {
            weekday: 'short',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }) + (diffInHours > 0 ? ' (Soon!)' : ' (Past)');
    }

    return date.toLocaleDateString('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function formatDate(dateString) {
    if (!dateString) return '';

    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
        month: 'long',
        day: 'numeric',
        year: 'numeric'
    });
}

function formatTime(dateTimeString) {
    if (!dateTimeString) return '';

    const date = new Date(dateTimeString);
    return date.toLocaleTimeString('en-US', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Get relative time (e.g., "2 hours ago", "in 3 days")
function getRelativeTime(dateTimeString) {
    if (!dateTimeString) return '';

    const date = new Date(dateTimeString);
    const now = new Date();
    const diffInSeconds = (date - now) / 1000;
    const diffInMinutes = diffInSeconds / 60;
    const diffInHours = diffInMinutes / 60;
    const diffInDays = diffInHours / 24;

    if (Math.abs(diffInSeconds) < 60) {
        return 'Just now';
    } else if (Math.abs(diffInMinutes) < 60) {
        const minutes = Math.floor(Math.abs(diffInMinutes));
        return diffInMinutes > 0 ? `in ${minutes} minute${minutes !== 1 ? 's' : ''}` : `${minutes} minute${minutes !== 1 ? 's' : ''} ago`;
    } else if (Math.abs(diffInHours) < 24) {
        const hours = Math.floor(Math.abs(diffInHours));
        return diffInHours > 0 ? `in ${hours} hour${hours !== 1 ? 's' : ''}` : `${hours} hour${hours !== 1 ? 's' : ''} ago`;
    } else {
        const days = Math.floor(Math.abs(diffInDays));
        return diffInDays > 0 ? `in ${days} day${days !== 1 ? 's' : ''}` : `${days} day${days !== 1 ? 's' : ''} ago`;
    }
}

// Text utilities
function truncateText(text, maxLength) {
    if (!text) return '';
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
}

function capitalizeFirst(str) {
    if (!str) return '';
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

function capitalizeWords(str) {
    if (!str) return '';
    return str.replace(/\w\S*/g, (txt) =>
        txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase()
    );
}

// Enhanced Modal management with better UX
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.display = 'block';
        document.body.style.overflow = 'hidden';

        // Add fade-in animation
        modal.style.opacity = '0';
        setTimeout(() => {
            modal.style.opacity = '1';
            modal.style.transition = 'opacity 0.3s ease';
        }, 10);
    }
}

function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.style.opacity = '0';
        setTimeout(() => {
            modal.style.display = 'none';
            document.body.style.overflow = 'auto';
        }, 300);
    }
}

function closeModal(modalId) {
    hideModal(modalId);
}

// Enhanced Form utilities
function clearForm(formId) {
    const form = document.getElementById(formId);
    if (form) {
        form.reset();

        // Clear any error messages
        const errorElements = form.querySelectorAll('.error-message, .field-error');
        errorElements.forEach(element => element.remove());

        // Remove error classes from inputs
        const inputs = form.querySelectorAll('.error');
        inputs.forEach(input => input.classList.remove('error'));

        // Reset any edit mode data
        delete form.dataset.editId;

        // Reset submit button text if it was changed
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn && submitBtn.dataset.originalText) {
            submitBtn.innerHTML = submitBtn.dataset.originalText;
        }
    }
}

function getFormData(formId) {
    const form = document.getElementById(formId);
    if (!form) return {};

    const formData = new FormData(form);
    const data = {};

    for (let [key, value] of formData.entries()) {
        // Handle checkboxes and multiple values
        if (data[key]) {
            if (Array.isArray(data[key])) {
                data[key].push(value);
            } else {
                data[key] = [data[key], value];
            }
        } else {
            data[key] = value;
        }
    }

    return data;
}

function showFormErrors(formId, errors) {
    const form = document.getElementById(formId);
    if (!form || !errors.length) return;

    // Clear existing errors
    const existingErrors = form.querySelectorAll('.error-message');
    existingErrors.forEach(error => error.remove());

    // Create error container
    const errorContainer = document.createElement('div');
    errorContainer.className = 'error-message alert alert-danger';
    errorContainer.innerHTML = `
        <div class="error-header">
            <i class="fas fa-exclamation-triangle"></i>
            <strong>Please fix the following errors:</strong>
        </div>
        <ul class="error-list">
            ${errors.map(error => `<li>${error}</li>`).join('')}
        </ul>
    `;

    // Insert at the beginning of the form
    form.insertBefore(errorContainer, form.firstChild);

    // Scroll to error
    errorContainer.scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function validateForm(formId, rules) {
    const form = document.getElementById(formId);
    if (!form) return { isValid: false, errors: ['Form not found'] };

    const data = getFormData(formId);
    const errors = [];

    for (const [field, rule] of Object.entries(rules)) {
        const value = data[field];

        if (rule.required && (!value || value.trim() === '')) {
            errors.push(`${rule.label || field} is required`);
            continue;
        }

        if (value && rule.minLength && value.length < rule.minLength) {
            errors.push(`${rule.label || field} must be at least ${rule.minLength} characters`);
        }

        if (value && rule.maxLength && value.length > rule.maxLength) {
            errors.push(`${rule.label || field} must not exceed ${rule.maxLength} characters`);
        }

        if (value && rule.pattern && !rule.pattern.test(value)) {
            errors.push(rule.message || `${rule.label || field} format is invalid`);
        }

        if (value && rule.custom && !rule.custom(value)) {
            errors.push(rule.message || `${rule.label || field} is invalid`);
        }
    }

    return { isValid: errors.length === 0, errors };
}

// Navigation utilities
function smoothScrollTo(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        });
    }
}

function goToPage(url) {
    window.location.href = url;
}

function goBack() {
    window.history.back();
}

function reloadPage() {
    window.location.reload();
}

// Enhanced Local storage utilities with error handling
function saveToLocalStorage(key, data) {
    try {
        localStorage.setItem(key, JSON.stringify(data));
        return true;
    } catch (error) {
        console.error('Error saving to localStorage:', error);
        showToast('Failed to save data locally', 'warning');
        return false;
    }
}

function getFromLocalStorage(key) {
    try {
        const data = localStorage.getItem(key);
        return data ? JSON.parse(data) : null;
    } catch (error) {
        console.error('Error reading from localStorage:', error);
        return null;
    }
}

function removeFromLocalStorage(key) {
    try {
        localStorage.removeItem(key);
        return true;
    } catch (error) {
        console.error('Error removing from localStorage:', error);
        return false;
    }
}

function clearLocalStorage() {
    try {
        localStorage.clear();
        return true;
    } catch (error) {
        console.error('Error clearing localStorage:', error);
        return false;
    }
}

// Animation utilities
function animateCounter(elementId, targetValue, duration = 2000) {
    const element = document.getElementById(elementId);
    if (!element) return;

    let startValue = 0;
    const increment = targetValue / (duration / 16);
    const startTime = Date.now();

    const timer = setInterval(() => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / duration, 1);

        // Use easing function for smoother animation
        const easeOutQuart = 1 - Math.pow(1 - progress, 4);
        const currentValue = Math.floor(targetValue * easeOutQuart);

        element.textContent = currentValue;

        if (progress >= 1) {
            element.textContent = targetValue;
            clearInterval(timer);
        }
    }, 16);
}

function fadeIn(elementId, duration = 300) {
    const element = document.getElementById(elementId);
    if (!element) return;

    element.style.opacity = '0';
    element.style.display = 'block';

    setTimeout(() => {
        element.style.transition = `opacity ${duration}ms ease`;
        element.style.opacity = '1';
    }, 10);
}

function fadeOut(elementId, duration = 300) {
    const element = document.getElementById(elementId);
    if (!element) return;

    element.style.transition = `opacity ${duration}ms ease`;
    element.style.opacity = '0';

    setTimeout(() => {
        element.style.display = 'none';
    }, duration);
}

// Enhanced Debounce function with immediate option
function debounce(func, wait, immediate = false) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            timeout = null;
            if (!immediate) func(...args);
        };
        const callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func(...args);
    };
}

// Throttle function for performance-sensitive operations
function throttle(func, limit) {
    let inThrottle;
    return function() {
        const args = arguments;
        const context = this;
        if (!inThrottle) {
            func.apply(context, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// Enhanced Validation utilities
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function isValidPhone(phone) {
    const phoneRegex = /^\+?[\d\s\-\(\)]{10,}$/;
    return phoneRegex.test(phone);
}

function isValidURL(url) {
    try {
        new URL(url);
        return true;
    } catch {
        return false;
    }
}

function isValidPassword(password) {
    // At least 8 characters, 1 uppercase, 1 lowercase, 1 number, 1 special character
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=!*()]).{8,}$/;
    return passwordRegex.test(password);
}

function isValidDate(dateString) {
    const date = new Date(dateString);
    return date instanceof Date && !isNaN(date);
}

function isFutureDate(dateString) {
    const date = new Date(dateString);
    return date > new Date();
}

// Enhanced Error handling with better user experience
function handleApiError(error) {
    console.error('API Error:', error);

    // Extract error message from different error formats
    let message = 'An unexpected error occurred.';

    if (typeof error === 'string') {
        message = error;
    } else if (error.message) {
        message = error.message;
    } else if (error.response && error.response.data && error.response.data.message) {
        message = error.response.data.message;
    }

    if (message.includes('401') || message.includes('Unauthorized')) {
        showToast('Session expired. Please login again.', 'warning');
        authManager.logout();
        setTimeout(() => {
            window.location.href = 'index.html';
        }, 2000);
    } else if (message.includes('403') || message.includes('Forbidden')) {
        showToast('You do not have permission to perform this action.', 'error');
    } else if (message.includes('404') || message.includes('Not Found')) {
        showToast('The requested resource was not found.', 'error');
    } else if (message.includes('409') || message.includes('Conflict')) {
        showToast('This action conflicts with existing data.', 'error');
    } else if (message.includes('500') || message.includes('Internal Server Error')) {
        showToast('Server error. Please try again later.', 'error');
    } else {
        showToast(message, 'error');
    }
}

// Copy to clipboard utility
function copyToClipboard(text) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text).then(() => {
            showToast('Copied to clipboard!', 'success', 2000);
        }).catch(err => {
            console.error('Failed to copy: ', err);
            showToast('Failed to copy to clipboard', 'error');
        });
    } else {
        // Fallback for older browsers
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        try {
            document.execCommand('copy');
            showToast('Copied to clipboard!', 'success', 2000);
        } catch (err) {
            showToast('Failed to copy to clipboard', 'error');
        }
        document.body.removeChild(textArea);
    }
}

// Download utilities
function downloadJSON(data, filename) {
    const dataStr = JSON.stringify(data, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    downloadBlob(dataBlob, filename);
}

function downloadCSV(data, filename) {
    const csvContent = arrayToCSV(data);
    const dataBlob = new Blob([csvContent], { type: 'text/csv' });
    downloadBlob(dataBlob, filename);
}

function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
}

function arrayToCSV(data) {
    if (!data.length) return '';

    const headers = Object.keys(data[0]);
    const csvRows = [headers.join(',')];

    for (const row of data) {
        const values = headers.map(header => {
            const escaped = ('' + row[header]).replace(/"/g, '\\"');
            return `"${escaped}"`;
        });
        csvRows.push(values.join(','));
    }

    return csvRows.join('\n');
}

// Utility functions for dashboard-specific needs
function formatEventStatus(event) {
    const now = new Date();
    const eventDate = new Date(event.dateTime);

    if (eventDate > now) {
        return 'upcoming';
    } else {
        return 'past';
    }
}

function formatRSVPStatus(status) {
    const statusMap = {
        'CONFIRMED': 'confirmed',
        'PENDING': 'pending',
        'CANCELLED': 'cancelled'
    };
    return statusMap[status] || status.toLowerCase();
}

function generateEventShareLink(eventId) {
    return `${window.location.origin}/event-details.html?id=${eventId}`;
}

// Initialize common functionality
document.addEventListener('DOMContentLoaded', function() {
    // Close modals when clicking outside
    window.addEventListener('click', function(event) {
        if (event.target.classList.contains('modal')) {
            event.target.style.opacity = '0';
            setTimeout(() => {
                event.target.style.display = 'none';
                document.body.style.overflow = 'auto';
            }, 300);
        }
    });

    // Handle escape key to close modals
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape') {
            const modals = document.querySelectorAll('.modal');
            modals.forEach(modal => {
                if (modal.style.display === 'block') {
                    modal.style.opacity = '0';
                    setTimeout(() => {
                        modal.style.display = 'none';
                        document.body.style.overflow = 'auto';
                    }, 300);
                }
            });
        }
    });

    // Add loading states to buttons
    document.addEventListener('click', function(event) {
        if (event.target.matches('button[data-loading]')) {
            const button = event.target;
            const originalText = button.innerHTML;
            button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Loading...';
            button.disabled = true;

            // Restore button after 3 seconds (fallback)
            setTimeout(() => {
                button.innerHTML = originalText;
                button.disabled = false;
            }, 3000);
        }
    });

    // Auto-hide alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(alert => {
        setTimeout(() => {
            fadeOut(alert.id || alert.className);
        }, 5000);
    });
});

