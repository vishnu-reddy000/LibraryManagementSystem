/* ═══════════════════════════════════════
   USER SYSTEM — Interactive JS
   ═══════════════════════════════════════ */

// ── Sidebar toggle (mobile) ──
function toggleSidebar() {
    const sidebar = document.getElementById('dbSidebar');
    const overlay = document.getElementById('dbOverlay');
    const hamburger = document.getElementById('dbHamburger');
    if (!sidebar || !overlay) return;
    
    const isOpen = sidebar.classList.toggle('open');
    overlay.classList.toggle('active', isOpen);
    if (hamburger) {
        hamburger.setAttribute('aria-expanded', isOpen);
    }
    document.body.style.overflow = isOpen ? 'hidden' : '';
}

// ── Sidebar size toggle (desktop full-size vs logos only) ──
function toggleSidebarSize(event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    const sidebar = document.getElementById('dbSidebar');
    if (!sidebar) return;
    const isExpanded = sidebar.classList.toggle('expanded');
    localStorage.setItem('sidebarExpanded', isExpanded ? 'true' : 'false');
}

function initializeUserPage() {
    // Restore sidebar state from localStorage immediately
    const sidebar = document.getElementById('dbSidebar');
    if (sidebar) {
        const isExpanded = localStorage.getItem('sidebarExpanded') === 'true';
        sidebar.classList.toggle('expanded', isExpanded);
    }

    // Close sidebar when clicking any navigation link on mobile
    document.querySelectorAll('.db-sidebar .db-nav-item').forEach(function (link) {
        link.addEventListener('click', function () {
            const sidebar = document.getElementById('dbSidebar');
            if (sidebar && sidebar.classList.contains('open')) {
                toggleSidebar();
            }
        });
    });

    // Fetch user details and start WebSocket connection
    fetch('/user/current-email')
        .then(response => response.json())
        .then(data => {
            if (data && data.email) {
                initUserWebSocket(data.email);
            }
        })
        .catch(err => console.error("Could not fetch user email for WS initialization: ", err));
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initializeUserPage);
} else {
    initializeUserPage();
}

// Dynamically load SockJS & STOMP scripts if not already present
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
    
    let icon = '<i class="fa-solid fa-circle-info"></i>';
    if (type === 'success') icon = '<i class="fa-solid fa-circle-check"></i>';
    if (type === 'error') icon = '<i class="fa-solid fa-circle-exclamation"></i>';
    if (type === 'warning') icon = '<i class="fa-solid fa-triangle-exclamation"></i>';

    toast.innerHTML = `${icon} <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideOut 0.35s ease-in forwards';
        setTimeout(() => toast.remove(), 350);
    }, 5000);
}

// User WebSocket Connection
function initUserWebSocket(userEmail) {
    if (!userEmail) return;
    loadWebSocketClient(() => {
        const socket = new SockJS('/ws');
        const stompClient = Stomp.over(socket);
        stompClient.debug = console.log; // Console logging for connection debugging
        stompClient.connect({}, function (frame) {
            console.log("User WebSocket connected successfully: " + frame);
            stompClient.subscribe('/topic/notifications-' + userEmail, function (messageOutput) {
                const data = JSON.parse(messageOutput.body);
                if (data && data.message) {
                    showToast(data.message, data.type || 'info');
                    
                    // 1. Update the notification badge counts in topbar/mobile bars
                    const badgeElements = document.querySelectorAll('.db-notif-badge');
                    badgeElements.forEach(badge => {
                        let count = parseInt(badge.textContent) || 0;
                        badge.textContent = count + 1;
                        badge.style.display = 'flex';
                    });

                    // 2. If currently viewing the notification list page, dynamically prepend the alert card
                    const notifContent = document.querySelector('.db-content .content');
                    if (window.location.pathname.includes('/user/notifications') && notifContent) {
                        const emptyState = notifContent.querySelector('.empty-state');
                        if (emptyState) emptyState.remove();

                        let listContainer = notifContent.querySelector('div');
                        if (!listContainer) {
                            listContainer = document.createElement('div');
                            notifContent.appendChild(listContainer);
                        }

                        const card = document.createElement('div');
                        card.className = `notif-card ${data.type || 'info'}`;
                        card.style.cssText = "display: flex; justify-content: space-between; align-items: center; width: 100%;";
                        
                        card.innerHTML = `
                            <span>${data.message}</span>
                            <form action="" method="post" style="margin: 0; display: inline-block;" onsubmit="event.preventDefault(); this.closest('.notif-card').remove();">
                                <button type="submit" style="background: none; border: none; color: inherit; opacity: 0.5; cursor: pointer; font-size: 16px; display: flex; align-items: center; padding: 4px;" aria-label="Dismiss">
                                    <i class="fa-solid fa-xmark"></i>
                                </button>
                            </form>
                        `;
                        listContainer.insertBefore(card, listContainer.firstChild);
                    }
                }
            });
        }, function (error) {
            console.error("User WebSocket connection error: ", error);
        });
    });
}

// Close sidebar on outside clicks
document.addEventListener('click', function (e) {
    const sidebar = document.getElementById('dbSidebar');
    const hamburger = document.getElementById('dbHamburger');
    if (!sidebar || !sidebar.classList.contains('open')) return;
    if (!sidebar.contains(e.target) && (!hamburger || !hamburger.contains(e.target))) {
        toggleSidebar();
    }
});

// Close sidebar on Escape key press
document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape') {
        const sidebar = document.getElementById('dbSidebar');
        if (sidebar && sidebar.classList.contains('open')) {
            toggleSidebar();
        }
    }
});
