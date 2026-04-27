class DashboardCharts {
    constructor() {
        this.charts = {};
        this.initCharts();
        this.connectWebSocket();
    }

    initCharts() {
        if (typeof Chart !== 'undefined') {
            this.charts.agentPerf = new Chart(document.getElementById('agentChart'), {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'X-Builder',
                        data: [],
                        borderColor: '#4CAF50'
                    }, {
                        label: 'Z-Architect',
                        data: [],
                        borderColor: '#2196F3'
                    }]
                },
                options: {
                    responsive: true,
                    animation: { duration: 0 }
                }
            });
        }

        if (typeof Gauge !== 'undefined') {
            this.charts.cpuGauge = new Gauge(document.getElementById('cpuGauge'), {
                max: 100,
                value: 0,
                label: 'CPU %'
            });
        }
    }

    connectWebSocket() {
        // Use SockJS for better compatibility
        this.socket = new SockJS('/ws');
        this.stompClient = Stomp.over(this.socket);
        
        // Get Firebase token
        const token = localStorage.getItem('firebase:authUser:AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8');
        const headers = token ? { Authorization: `Bearer ${JSON.parse(token).stsTokenManager.accessToken}` } : {};
        
        this.stompClient.connect(headers, (frame) => {
            console.log('Connected to WebSocket:', frame);
            // Update backend status alert
            const alertEl = document.getElementById('backendStatusAlert');
            if (alertEl) {
                alertEl.className = 'alert alert-success';
                alertEl.querySelector('span').textContent = '✅ Backend online - Live alerts active';
            }
            // Subscribe to dashboard updates
            this.stompClient.subscribe('/topic/dashboard', (message) => {
                const data = JSON.parse(message.body);
                this.handleUpdate(data);
            });
            // Subscribe to alerts
            this.stompClient.subscribe('/topic/alerts', (message) => {
                const data = JSON.parse(message.body);
                this.showAlert(data);
            });
        }, (error) => {
            console.error('STOMP connection error:', error);
            setTimeout(() => this.connectWebSocket(), 3000);
        });
    }

    handleUpdate(data) {
        switch (data.type) {
            case 'AGENT_UPDATE':
                this.updateAgentStatus(data);
                break;
            case 'METRIC_UPDATE':
                this.updateMetric(data);
                break;
            case 'ALERT':
                this.showAlert(data);
                break;
            case 'INITIAL_DATA':
                console.log('Received initial data');
                break;
            case 'dashboard_update':
                this.updateDashboard(data.stats);
                break;
        }
    }

    updateDashboard(stats) {
        if (!stats) return;
        
        // Update Total Users
        const totalUsersEl = document.getElementById('totalUsers');
        if (totalUsersEl) totalUsersEl.textContent = stats.totalUsers || '0';
        
        // Update Active Projects
        const activeProjectsEl = document.getElementById('activeProjects');
        if (activeProjectsEl) activeProjectsEl.textContent = stats.runningProjects || '0';
        
        // Update Success Rate
        const successRateEl = document.getElementById('successRate');
        if (successRateEl) successRateEl.textContent = stats.successRate ? stats.successRate + '%' : '--';
        
        console.log('Dashboard updated with stats:', stats);
    }

    updateAgentStatus(data) {
        console.log('Agent update:', data);
    }

    updateMetric(data) {
        const { metric, value, threshold } = data;

        if (metric === 'cpu' && this.charts.cpuGauge) {
            this.charts.cpuGauge.setValue(value);
            if (value > threshold) {
                this.triggerAlert('CPU Overload!', 'critical');
            }
        }
    }

    showAlert(data) {
        console.warn('Alert:', data);
        const alertDiv = document.createElement('div');
        alertDiv.className = 'alert alert-warning alert-dismissible';
        alertDiv.innerHTML = data.message + '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>';
        document.getElementById('alerts-container')?.appendChild(alertDiv);
    }

    triggerAlert(message, level) {
        this.showAlert({ message, level });
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // Always connect WebSocket for live dashboard data
    new DashboardCharts();
});
