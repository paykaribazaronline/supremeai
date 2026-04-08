// 🧠 SupremeAI Project Analyzer - Frontend JavaScript

const API_BASE_URL = 'http://localhost:8080/api/project-analysis';

// Colors for languages
const LANG_COLORS = {
    'Java': '#b07219',
    'Kotlin': '#A97BFF',
    'Python': '#3572A5',
    'JavaScript': '#f1e05a',
    'TypeScript': '#2b7489',
    'HTML': '#e34c26',
    'CSS': '#563d7c',
    'JSON': '#292929',
    'YAML': '#cb171e',
    'Markdown': '#083fa1',
    'Other': '#999999'
};

/**
 * 🔍 Analyze full project
 */
async function analyzeProject() {
    const path = document.getElementById('projectPath').value;
    if (!path) {
        alert('Please enter a project path');
        return;
    }
    
    showLoading(true);
    
    try {
        const response = await fetch(`${API_BASE_URL}/analyze`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ projectPath: path })
        });
        
        const data = await response.json();
        
        if (data.success) {
            displayResults(data);
        } else {
            alert('Error: ' + data.error);
        }
    } catch (error) {
        alert('Failed to analyze: ' + error.message);
    } finally {
        showLoading(false);
    }
}

/**
 * 🏥 Quick health check
 */
async function quickHealth() {
    const path = document.getElementById('projectPath').value;
    if (!path) {
        alert('Please enter a project path');
        return;
    }
    
    showLoading(true);
    
    try {
        const response = await fetch(`${API_BASE_URL}/health`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ projectPath: path })
        });
        
        const data = await response.json();
        
        if (data.success) {
            displayHealth(data);
        } else {
            alert('Error: ' + data.error);
        }
    } catch (error) {
        alert('Failed to check health: ' + error.message);
    } finally {
        showLoading(false);
    }
}

/**
 * 📊 Display full results
 */
function displayResults(data) {
    document.getElementById('results').classList.remove('hidden');
    
    // Health Score
    const score = data.healthScore;
    const scoreCircle = document.getElementById('scoreCircle');
    const scoreValue = document.getElementById('scoreValue');
    const healthStatus = document.getElementById('healthStatus');
    
    scoreValue.textContent = score;
    healthStatus.textContent = data.overallHealth;
    
    scoreCircle.className = 'score-circle ' + getScoreClass(score);
    
    // Stats
    document.getElementById('totalFiles').textContent = data.totalFiles;
    document.getElementById('totalLines').textContent = data.totalLinesOfCode.toLocaleString();
    document.getElementById('projectType').textContent = data.projectType;
    document.getElementById('issuesCount').textContent = data.issuesCount;
    
    // Languages
    displayLanguages(data.languageStats);
    
    // Summary
    document.getElementById('summaryText').textContent = data.summary;
    
    // Note: Issues and suggestions would need additional endpoints
    // For now, showing placeholder
    document.getElementById('issuesList').innerHTML = 
        `<div class="issue-item severity-medium">
            <strong>${data.issuesCount} issues found</strong>
            <p>Detailed issue list available in backend</p>
        </div>`;
    
    document.getElementById('suggestionsList').innerHTML = 
        `<div class="suggestion-item priority-high">
            <strong>${data.suggestionsCount} suggestions available</strong>
            <p>Use /api/project-analysis/suggestions endpoint for details</p>
        </div>`;
}

/**
 * 🏥 Display health only
 */
function displayHealth(data) {
    document.getElementById('results').classList.remove('hidden');
    
    const score = data.healthScore;
    document.getElementById('scoreValue').textContent = score;
    document.getElementById('healthStatus').textContent = data.overallHealth;
    document.getElementById('scoreCircle').className = 'score-circle ' + getScoreClass(score);
    document.getElementById('issuesCount').textContent = data.issuesCount;
}

/**
 * 🎨 Get score color class
 */
function getScoreClass(score) {
    if (score >= 90) return 'score-excellent';
    if (score >= 75) return 'score-good';
    if (score >= 50) return 'score-fair';
    return 'score-poor';
}

/**
 * 📊 Display language statistics
 */
function displayLanguages(langStats) {
    const container = document.getElementById('languageStats');
    container.innerHTML = '';
    
    if (!langStats || Object.keys(langStats).length === 0) {
        container.innerHTML = '<p style="color: #888;">No language data</p>';
        return;
    }
    
    const total = Object.values(langStats).reduce((a, b) => a + b, 0);
    
    // Sort by lines
    const sorted = Object.entries(langStats).sort((a, b) => b[1] - a[1]);
    
    sorted.forEach(([lang, lines]) => {
        const percent = ((lines / total) * 100).toFixed(1);
        const color = LANG_COLORS[lang] || LANG_COLORS['Other'];
        
        const bar = document.createElement('div');
        bar.className = 'lang-bar';
        bar.innerHTML = `
            <div class="lang-name">${lang}</div>
            <div class="lang-progress">
                <div class="lang-fill" style="width: ${percent}%; background: ${color};"></div>
            </div>
            <div class="lang-percent">${percent}%</div>
        `;
        container.appendChild(bar);
    });
}

/**
 * ⏳ Show/hide loading
 */
function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'block' : 'none';
    document.getElementById('analyzeBtn').disabled = show;
    document.getElementById('healthBtn').disabled = show;
}

// Auto-load on Enter key
document.getElementById('projectPath')?.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') analyzeProject();
});
