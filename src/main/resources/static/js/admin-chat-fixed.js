// Admin Chat Management JavaScript - Fixed for Spring Boot
// Extends admin-dashboard.js

class AdminChatManager {
    constructor() {
        this.baseUrl = '/api/admin/chat-items';
        this.pendingItems = [];
        this.allItems = {
            rules: [],
            plans: [],
            commands: [],
            history: []
        };
        this.init();
    }

    init() {
        this.setupEventListeners();
        // Only load if authenticated
        if (this.isAuthenticated()) {
            this.loadInitialData();
            this.startAutoRefresh();
        } else {
            // Wait for auth (poll)
            const checkAuth = setInterval(() => {
                if (this.isAuthenticated()) {
                    clearInterval(checkAuth);
                    this.loadInitialData();
                    this.startAutoRefresh();
                }
            }, 1000);
        }
    }

    isAuthenticated() {
        // Check Firebase token exists
        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key && key.startsWith('firebase:authUser:')) {
                try {
                    const data = JSON.parse(localStorage.getItem(key));
                    return !! (data.stsTokenManager?.accessToken || data.accessToken);
                } catch (e) {}
            }
        }
        return false;
    }

    getAuthToken() {
        for (let i = 0; i < localStorage.length; i++) {
            const key = localStorage.key(i);
            if (key && key.startsWith('firebase:authUser:')) {
                try {
                    const data = JSON.parse(localStorage.getItem(key));
                    return data.stsTokenManager?.accessToken || data.accessToken;
                } catch (e) {}
            }
        }
        return '';
    }

    setupEventListeners() {
        // Refresh button
        document.getElementById('refresh-btn')?.addEventListener('click', () => {
            this.loadInitialData();
        });

        // Search filters
        document.getElementById('pending-search')?.addEventListener('input', (e) => {
            this.filterPending(e.target.value);
        });

        document.getElementById('rules-search')?.addEventListener('input', (e) => {
            this.filterList('rules', e.target.value);
        });

        document.getElementById('plans-search')?.addEventListener('input', (e) => {
            this.filterList('plans', e.target.value);
        });

        document.getElementById('commands-search')?.addEventListener('input', (e) => {
            this.filterList('commands', e.target.value);
        });

        // Toggle switches
        document.getElementById('active-rules-only')?.addEventListener('change', () => {
            this.loadRules();
        });
        document.getElementById('active-plans-only')?.addEventListener('change', () => {
            this.loadPlans();
        });
        document.getElementById('active-commands-only')?.addEventListener('change', () => {
            this.loadCommands();
        });

        // History filter
        document.getElementById('history-filter')?.addEventListener('change', (e) => {
            this.loadChatHistory(e.target.value);
        });

        // Bulk actions
        document.getElementById('confirm-all-btn')?.addEventListener('click', () => {
            this.bulkConfirm(true);
        });
        document.getElementById('reject-all-btn')?.addEventListener('click', () => {
            this.bulkConfirm(false);
        });

        // Modal buttons
        document.getElementById('confirm-btn')?.addEventListener('click', () => {
            this.confirmCurrentItem(true);
        });
        document.getElementById('reject-btn')?.addEventListener('click', () => {
            this.confirmCurrentItem(false);
        });
    }

    async loadInitialData() {
        await Promise.all([
            this.loadPending(),
            this.loadRules(),
            this.loadPlans(),
            this.loadCommands(),
            this.loadChatHistory('all'),
            this.updateStats()
        ]);
    }

    startAutoRefresh() {
        setInterval(() => {
            this.loadPending();
            this.updateStats();
        }, 10000); // 10s for backend efficiency
    }

    // API Calls
    async fetchAPI(endpoint) {
        const token = this.getAuthToken();
        const response = await fetch(`${this.baseUrl}${endpoint}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });
        if (!response.ok) throw new Error('API request failed');
        return response.json();
    }

    async postAPI(endpoint, body) {
        const token = this.getAuthToken();
        const response = await fetch(`${this.baseUrl}${endpoint}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(body)
        });
        if (!response.ok) throw new Error('API request failed');
        return response.json();
    }

    // Data Loading
    async loadPending() {
        try {
            const data = await this.fetchAPI('/pending');
            // Handle both array response and wrapped response
            this.pendingItems = Array.isArray(data) ? data : (data.items || []);
            this.renderPending(this.pendingItems);
            this.updatePendingBadge(this.pendingItems.length);
        } catch (error) {
            console.error('Failed to load pending:', error);
        }
    }

    async loadRules() {
        try {
            const data = await this.fetchAPI('/rules');
            this.allItems.rules = Array.isArray(data) ? data : (data.rules || []);
            this.renderList('rules', this.allItems.rules);
        } catch (error) {
            console.error('Failed to load rules:', error);
        }
    }

    async loadPlans() {
        try {
            const data = await this.fetchAPI('/plans');
            this.allItems.plans = Array.isArray(data) ? data : (data.plans || []);
            this.renderList('plans', this.allItems.plans);
        } catch (error) {
            console.error('Failed to load plans:', error);
        }
    }

    async loadCommands() {
        try {
            const data = await this.fetchAPI('/commands');
            this.allItems.commands = Array.isArray(data) ? data : (data.commands || []);
            this.renderList('commands', this.allItems.commands);
        } catch (error) {
            console.error('Failed to load commands:', error);
        }
    }

    async loadChatHistory(filter = 'all') {
        try {
            const data = await this.fetchAPI(`/history?limit=100`);
            let history = Array.isArray(data) ? data : (data.chat_history || []);
            if (filter === 'user') {
                history = history.filter(h => !h.is_admin);
            } else if (filter === 'admin') {
                history = history.filter(h => h.is_admin);
            }
            this.allItems.history = history;
            this.renderChatHistory(history);
        } catch (error) {
            console.error('Failed to load chat history:', error);
        }
    }

    async updateStats() {
        try {
            document.getElementById('total-chats').textContent = this.allItems.history.length;
            document.getElementById('total-rules').textContent = this.allItems.rules.length;
            document.getElementById('total-plans').textContent = this.allItems.plans.length;
            document.getElementById('total-commands').textContent = this.allItems.commands.length;
        } catch (error) {
            console.error('Failed to update stats:', error);
        }
    }

    // Rendering
    renderPending(items) {
        const container = document.getElementById('pending-list-content');
        if (!container) return;

        if (items.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-check-circle"></i><p>All caught up! No pending confirmations.</p></div>';
            return;
        }

        container.innerHTML = items.map(item => this.renderPendingItem(item)).join('');
        this.attachPendingListeners();
    }

    renderPendingItem(item) {
        const confidencePercent = Math.round((item.confidence || 0) * 100);
        const id = item.id || item.item_id;

        return `
        <div class="list-group-item pending-highlight" data-item-id="${id}">
            <div class="d-flex justify-content-between align-items-start">
                <div>
                    <span class="type-badge type-${item.itemType || item.item_type}">${item.itemType || item.item_type}</span>
                    <small class="text-muted ms-2">${new Date().toLocaleTimeString()}</small>
                </div>
                <div class="confidence-bar" style="width: 100px;">
                    <div class="confidence-fill" style="width: ${confidencePercent}%"></div>
                </div>
            </div>
            <p class="mt-2 mb-1">${item.content}</p>
            <small class="text-muted">Confidence: ${confidencePercent}%</small>
            <div class="action-buttons">
                <button class="action-btn approve-btn" data-item-id="${id}" data-confirmed="true">
                    <i class="fas fa-check me-1"></i> Approve
                </button>
                <button class="action-btn reject-btn" data-item-id="${id}" data-confirmed="false">
                    <i class="fas fa-times me-1"></i> Reject
                </button>
            </div>
        </div>
        `;
    }

    renderList(type, items) {
        const containerId = `${type}-list-content`;
        const container = document.getElementById(containerId);
        if (!container) return;

        if (items.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-inbox"></i><p>No items found</p></div>';
            return;
        }

        container.innerHTML = items.map(item => this.renderListItem(item, type)).join('');
    }

    renderListItem(item, type) {
        const confidencePercent = Math.round((item.confidence || 0) * 100);
        const createdDate = item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'N/A';

        return `
        <div class="list-group-item">
            <div class="d-flex justify-content-between align-items-start">
                <div>
                    <span class="type-badge type-${type}">${type}</span>
                    <small class="text-muted ms-2">${createdDate}</small>
                </div>
                <div class="confidence-bar" style="width: 80px;">
                    <div class="confidence-fill" style="width: ${confidencePercent}%"></div>
                </div>
            </div>
            <p class="mt-2 mb-0">${item.content}</p>
            <small class="text-muted">Confidence: ${confidencePercent}% | By: ${item.createdBy || 'system'}</small>
        </div>
        `;
    }

    renderChatHistory(history) {
        const container = document.getElementById('chat-history-list-content');
        if (!container) return;

        if (history.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-comments"></i><p>No chat history</p></div>';
            return;
        }

        container.innerHTML = history.map(item => `
            <div class="list-group-item">
                <div class="d-flex justify-content-between">
                    <span class="badge bg-${item.isAdmin ? 'warning' : 'primary'}">${item.isAdmin ? 'Admin' : 'User'}</span>
                    <small class="text-muted">${new Date(item.timestamp).toLocaleString()}</small>
                </div>
                <p class="mt-2 mb-0">${item.message}</p>
                <small class="text-muted">User: ${item.userId || 'unknown'}</small>
            </div>
        `).join('');
    }

    // Event Handlers
    attachPendingListeners() {
        document.querySelectorAll('.approve-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const itemId = btn.getAttribute('data-item-id');
                await this.confirmItem(itemId, true);
            });
        });

        document.querySelectorAll('.reject-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const itemId = btn.getAttribute('data-item-id');
                await this.confirmItem(itemId, false);
            });
        });
    }

    async confirmItem(itemId, confirmed) {
        try {
            const response = await this.postAPI('/confirm', {
                itemId: itemId,
                confirmed: confirmed,
                userId: 'admin'
            });

            this.showNotification(`Item ${confirmed ? 'approved' : 'rejected'}`, 'success');
            await this.loadInitialData();
        } catch (error) {
            this.showNotification(`Error: ${error.message}`, 'danger');
        }
    }

    async bulkConfirm(confirmed) {
        const itemIds = this.pendingItems.map(item => item.id || item.item_id);
        for (const itemId of itemIds) {
            await this.confirmItem(itemId, confirmed);
        }
    }

    filterPending(query) {
        const filtered = this.pendingItems.filter(item =>
            item.content.toLowerCase().includes(query.toLowerCase())
        );
        this.renderPending(filtered);
    }

    filterList(type, query) {
        const filtered = this.allItems[type].filter(item =>
            item.content.toLowerCase().includes(query.toLowerCase())
        );
        this.renderList(type, filtered);
    }

    updatePendingBadge(count) {
        const badge = document.getElementById('pending-count-sidebar');
        if (badge) {
            badge.textContent = count;
            badge.style.display = count > 0 ? 'flex' : 'none';
        }
    }

    showNotification(message, type = 'info') {
        // Simple alert for now, can be enhanced
        console.log(`[${type}] ${message}`);
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    window.adminChat = new AdminChatManager();
});
