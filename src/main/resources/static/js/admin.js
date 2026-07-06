// Sidebar toggle for mobile
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('sidebarOverlay');
    if (!sidebar || !overlay) return;
    sidebar.classList.toggle('open');
    overlay.classList.toggle('active');
    document.body.style.overflow = sidebar.classList.contains('open') ? 'hidden' : '';
}

// Close sidebar when a nav link is clicked on mobile
function initializeAdminPage() {
    document.querySelectorAll('.sidebar ul li a').forEach(function (link) {
        link.addEventListener('click', function () {
            const sidebar = document.getElementById('sidebar');
            if (sidebar && sidebar.classList.contains('open')) {
                toggleSidebar();
            }
        });
    });

    // Make topbar bell icon clickable and load custom dropdown menu
    const topbarBell = document.querySelector('.topbar-icon');
    if (topbarBell) {
        topbarBell.style.position = 'relative';
        topbarBell.style.cursor = 'pointer';

        // Remove static orange dot if exists
        const staticDot = topbarBell.querySelector('.notif-dot');
        if (staticDot) staticDot.remove();

        // Create and append dynamic badge count
        const badge = document.createElement('span');
        badge.className = 'notif-badge';
        badge.style.display = 'none';
        topbarBell.appendChild(badge);

        // Create and append dynamic dropdown list
        const dropdown = document.createElement('div');
        dropdown.className = 'notif-dropdown';
        dropdown.innerHTML = `
            <div class="notif-header">
                <div class="notif-header-container">
                    <span>Pending Requests</span>
                    <button class="notif-read-all-btn">Read All</button>
                </div>
            </div>
            <div class="notif-list">
                <div class="notif-empty">Loading...</div>
            </div>
        `;
        topbarBell.appendChild(dropdown);

        // Toggle dropdown on click
        topbarBell.addEventListener('click', function (e) {
            if (e.target.closest('.notif-dropdown')) return; // ignore clicks inside dropdown content
            e.stopPropagation();
            dropdown.classList.toggle('show');
            if (dropdown.classList.contains('show')) {
                fetchPendingRequests(dropdown, badge);
            }
        });

        // Add event listener to Read All button
        const readAllBtn = dropdown.querySelector('.notif-read-all-btn');
        if (readAllBtn) {
            readAllBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                fetch('/api/admin/pending-requests/read-all', { method: 'POST' })
                    .then(res => res.json())
                    .then(() => {
                        fetchPendingRequests(dropdown, badge);
                    })
                    .catch(err => console.error("Error marking all read:", err));
            });
        }

        // Close dropdown when clicking outside
        document.addEventListener('click', function () {
            dropdown.classList.remove('show');
        });

        // Initial fetch to show correct badge count
        updateBadgeOnly(badge);
    }

    // Start WebSocket
    initAdminWebSocket();
}

// Fetch helper functions
function updateBadgeOnly(badge) {
    fetch('/api/admin/pending-requests')
        .then(res => res.json())
        .then(data => {
            const unreadCount = data.filter(req => !req.adminRead).length;
            if (unreadCount > 0) {
                badge.textContent = unreadCount;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        })
        .catch(err => console.error("Error updating notification badge:", err));
}

function fetchPendingRequests(dropdown, badge) {
    const listContainer = dropdown.querySelector('.notif-list');
    listContainer.innerHTML = '<div class="notif-empty">Loading...</div>';

    fetch('/api/admin/pending-requests')
        .then(res => res.json())
        .then(data => {
            const unreadCount = data.filter(req => !req.adminRead).length;
            if (unreadCount > 0) {
                badge.textContent = unreadCount;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }

            if (data && data.length > 0) {
                listContainer.innerHTML = '';
                data.forEach(req => {
                    const item = document.createElement('div');
                    item.className = 'notif-item' + (req.adminRead ? ' read' : '');
                    
                    const textDiv = document.createElement('div');
                    textDiv.className = 'notif-text';
                    textDiv.innerHTML = `<strong>${req.memberName}</strong> (${req.memberEmail}) requested <strong>"${req.bookTitle}"</strong>`;
                    item.appendChild(textDiv);
                    
                    const dateDiv = document.createElement('div');
                    dateDiv.className = 'notif-date';
                    dateDiv.textContent = req.requestDate;
                    item.appendChild(dateDiv);

                    // Action buttons
                    const actionsDiv = document.createElement('div');
                    actionsDiv.className = 'notif-actions';

                    if (!req.adminRead) {
                        const checkBtn = document.createElement('i');
                        checkBtn.className = 'fa-solid fa-check notif-action-btn read-btn';
                        checkBtn.title = 'Mark as read';
                        checkBtn.addEventListener('click', (e) => {
                            e.stopPropagation();
                            fetch('/api/admin/pending-requests/read/' + req.id, { method: 'POST' })
                                .then(res => res.json())
                                .then(() => fetchPendingRequests(dropdown, badge));
                        });
                        actionsDiv.appendChild(checkBtn);
                    }

                    const deleteBtn = document.createElement('i');
                    deleteBtn.className = 'fa-solid fa-trash notif-action-btn';
                    deleteBtn.title = 'Dismiss';
                    deleteBtn.addEventListener('click', (e) => {
                        e.stopPropagation();
                        fetch('/api/admin/pending-requests/delete/' + req.id, { method: 'POST' })
                            .then(res => res.json())
                            .then(() => fetchPendingRequests(dropdown, badge));
                    });
                    actionsDiv.appendChild(deleteBtn);
                    item.appendChild(actionsDiv);

                    item.addEventListener('click', () => {
                        fetch('/api/admin/pending-requests/read/' + req.id, { method: 'POST' })
                            .then(() => {
                                window.location.href = '/issue-books';
                            })
                            .catch(() => {
                                window.location.href = '/issue-books';
                            });
                    });
                    listContainer.appendChild(item);
                });
            } else {
                listContainer.innerHTML = '<div class="notif-empty">No pending borrow requests.</div>';
            }
        })
        .catch(err => {
            console.error("Error fetching pending requests:", err);
            listContainer.innerHTML = '<div class="notif-empty">Failed to load requests.</div>';
        });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeAdminPage);
} else {
    initializeAdminPage();
}

// Load SockJS & STOMP from local files dynamically
function loadWebSocketClient(callback) {
    if (window.SockJS && window.Stomp) {
        callback();
        return;
    }
    const sockScript = document.createElement('script');
    sockScript.src = "/js/sockjs.min.js";
    sockScript.onload = () => {
        const stompScript = document.createElement('script');
        stompScript.src = "/js/stomp.min.js";
        stompScript.onload = callback;
        document.head.appendChild(stompScript);
    };
    sockScript.onerror = () => {
        console.error("Failed to load local sockjs.min.js");
    };
    document.head.appendChild(sockScript);
}

// Toast Notifications System
function showToast(message, type = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast-notification ${type}`;
    toast.style.cursor = 'pointer';
    
    let icon = '<i class="fa-solid fa-circle-info"></i>';
    if (type === 'success') icon = '<i class="fa-solid fa-circle-check"></i>';
    if (type === 'error') icon = '<i class="fa-solid fa-circle-exclamation"></i>';
    if (type === 'warning') icon = '<i class="fa-solid fa-triangle-exclamation"></i>';
    if (type === 'request') icon = '<i class="fa-solid fa-bell"></i>';

    toast.innerHTML = `${icon} <span>${message}</span>`;
    container.appendChild(toast);

    // Make toast clickable to redirect to appropriate page
    toast.addEventListener('click', () => {
        if (type === 'request') {
            window.location.href = '/issue-books';
        } else if (type === 'return') {
            window.location.href = '/return-books';
        } else {
            window.location.href = '/issue-books';
        }
    });

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.35s ease-in forwards';
        setTimeout(() => toast.remove(), 350);
    }, 5000);
}

// Admin WebSocket Connection
function initAdminWebSocket() {
    loadWebSocketClient(() => {
        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        stompClient.debug = console.log; // Enable console logs for debugging connection state
        stompClient.connect({}, function (frame) {
            console.log("Admin WebSocket connected successfully: " + frame);
            stompClient.subscribe('/topic/admin', function (messageOutput) {
                const data = JSON.parse(messageOutput.body);
                if (data && data.message) {
                    showToast(data.message, data.type || 'info');
                    
                    // Update dropdown and badge dynamically in real-time
                    const badge = document.querySelector('.notif-badge');
                    const dropdown = document.querySelector('.notif-dropdown');
                    if (badge) {
                        updateBadgeOnly(badge);
                        if (dropdown && dropdown.classList.contains('show')) {
                            fetchPendingRequests(dropdown, badge);
                        }
                    }
                    
                    // Refresh data automatically if admin is currently viewing queues
                    if (window.location.pathname.includes('/admin') || window.location.pathname.includes('/issue-books')) {
                        setTimeout(() => {
                            window.location.reload();
                        }, 2500);
                    }
                }
            });
        }, function (error) {
            console.error("Admin WebSocket connection error: ", error);
        });
    });
}
