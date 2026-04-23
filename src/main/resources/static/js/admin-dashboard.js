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
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        this.ws = new WebSocket(protocol + '//' + window.location.host + '/ws/admin');

        this.ws.onopen = () => {
            console.log('WebSocket connected');
        };

        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            this.handleUpdate(data);
        };

        this.ws.onclose = () => {
            console.log('WebSocket disconnected, reconnecting...');
            setTimeout(() => this.connectWebSocket(), 3000);
        };

        this.ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };
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
        }
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
    if (document.getElementById('agentChart')) {
        new DashboardCharts();
    }
});
