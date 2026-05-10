// admin/dashboard.js - Dynamic API Integration
class SupremeDashboard {
    constructor() {
        // Use relative URL - works for both localhost and production
        this.apiBase = window.location.origin + '/api';
        this.token = localStorage.getItem('jwt_token');
        this.refreshInterval = null;
    }
    
    async init() {
        if (!this.token) {
            this.showLogin();
            return;
        }
        
        await this.loadAllData();
        this.startRealtimeUpdates();
    }
    
    async loadAllData() {
        try {
            const [agents, providers, projects, alerts, metrics, reasoning, knowledge] = await Promise.all([
                this.fetch('/agents/status'),
                this.fetch('/providers/status'),
                this.fetch('/projects/summary'),
                this.fetch('/alerts/active'),
                this.fetch('/metrics/current'),
                this.fetch('/ai/reasoning/recent'),
                this.fetch('/ai/knowledge/all')
            ]);
            
            this.renderAgents(agents);
            this.renderProviders(providers);
            this.renderProjects(projects);
            this.renderAlerts(alerts);
            this.renderMetrics(metrics);
            this.renderReasoningLogs(reasoning);
            this.renderKnowledgeBase(knowledge);
            
        } catch (error) {
            if (error.status === 401) {
                this.showLogin();
            }
        }
    }

    renderReasoningLogs(logs) {
        const container = document.getElementById('reasoning-logs');
        if (!container) return;
        container.innerHTML = logs.map(l => `
            <div class="log-entry">
                <span class="timestamp">${new Date(l.timestamp).toLocaleTimeString()}</span>
                <strong>${l.decision}</strong>: ${l.reason}
                <div class="actions">
                    <button class="approve-btn" onclick="dashboard.handleAction('${l.id}', 'APPROVE')">Approve</button>
                    <button class="reject-btn" onclick="dashboard.handleAction('${l.id}', 'REJECT')">Reject</button>
                </div>
            </div>
        `).join('');
    }

    renderKnowledgeBase(entries) {
        const container = document.getElementById('knowledge-base');
        if (!container) return;
        container.innerHTML = entries.map(e => `
            <div class="knowledge-card">
                <h5>${e.topic}</h5>
                <p>Pattern: <code>${e.pattern}</code></p>
                <small>Learned from: ${e.sourceProvider}</small>
            </div>
        `).join('');
    }

    async handleAction(logId, action) {
        console.log(`Action ${action} for log ${logId}`);
        await fetch(`${this.apiBase}/ai/action`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ logId, action })
        });
        this.loadAllData();
    }
    
    async fetch(endpoint) {
        const response = await fetch(`${this.apiBase}${endpoint}`, {
            headers: {
                'Authorization': `Bearer ${this.token}`,
                'Content-Type': 'application/json'
            }
        });
        
        if (response.status === 401) {
            throw { status: 401 };
        }
        
        return response.json();
    }
    
    startRealtimeUpdates() {
        // Try WebSocket first
        try {
            const ws = new WebSocket(this.apiBase.replace('https', 'wss').replace('http', 'ws').replace('/api', '/ws'));
            ws.onmessage = (e) => this.handleUpdate(JSON.parse(e.data));
        } catch {
            // Fallback: Poll every 5 seconds
            this.refreshInterval = setInterval(() => this.loadAllData(), 5000);
        }
    }
    
    handleUpdate(data) {
        console.log('Realtime update received:', data);
        // Handle specific updates here
        this.loadAllData(); // For now, just reload all
    }

    renderAgents(agents) {
        const container = document.getElementById('agent-list');
        if (!container) return;
        container.innerHTML = agents.map(a => `
            <div class="agent-card">
                <h4>${a.name}</h4>
                <p>Status: ${a.status}</p>
            </div>
        `).join('');
    }

    renderProviders(providers) {
        const container = document.getElementById('provider-coverage');
        if (!container) return;
        container.innerHTML = providers.map(p => `
            <div class="provider-card ${p.configured ? 'active' : 'missing'}">
                <h4>${p.name}</h4>
                <span class="status">${p.configured ? '✅ Ready' : '❌ Missing Key'}</span>
                ${!p.configured ? `<button onclick="dashboard.configureProvider('${p.id}')">Configure</button>` : ''}
            </div>
        `).join('');
    }

    renderProjects(projects) {
        const container = document.getElementById('project-summary');
        if (!container) return;
        // Basic rendering logic
    }

    renderAlerts(alerts) {
        const container = document.getElementById('active-alerts');
        if (!container) return;
        // Basic rendering logic
    }

    renderMetrics(metrics) {
        const container = document.getElementById('current-metrics');
        if (!container) return;
        // Basic rendering logic
    }
    
    showLogin() {
        const mainContent = document.getElementById('main-content');
        if (!mainContent) return;
        mainContent.innerHTML = `
            <div class="login-prompt">
                <h2>Please Login</h2>
                <input type="text" id="username" placeholder="Username">
                <input type="password" id="password" placeholder="Password">
                <button onclick="dashboard.login()">Login</button>
            </div>
        `;
    }
    
    async login() {
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        
        try {
            const response = await fetch(`${this.apiBase}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });
            
            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('jwt_token', data.token);
                this.token = data.token;
                location.reload();
            } else {
                alert('Login failed');
            }
        } catch (error) {
            console.error('Login error:', error);
        }
    }

    configureProvider(providerId) {
        console.log('Configuring provider:', providerId);
        // Add configuration UI or redirect
    }
}

const dashboard = new SupremeDashboard();
document.addEventListener('DOMContentLoaded', () => dashboard.init());
