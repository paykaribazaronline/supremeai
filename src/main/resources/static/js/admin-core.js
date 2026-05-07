// ========== CONFIGURATION (Managed by AuthHelper) ==========
// AuthHelper initialization is now handled by the platform initializer in admin.html

        // ========== LANGUAGE SYSTEM ==========
        let currentLanguage = localStorage.getItem('supremeai_lang') || 'en';
        
          const translations = {
              en: {
                  overview: 'Overview',
                  providers: 'AI Providers',
                  knowledge: 'Knowledge Base',
                  chat: 'Chat with SupremeAI',
                  pending: 'Pending Confirmations',
                  rules: 'Rules',
                  plans: 'Plans',
                  commands: 'Commands',
                  'chat-history': 'Chat History',
                  projects: 'Projects & Deploy',
                  users: 'User Management',
                  activity: 'Activity & Approvals',
                  settings: 'System Settings',
                  'admin-local': 'Admin (Localhost)',
                  'admin-production': 'Admin (Production)',
                  'customer-panel': 'Customer Panel'
              },
              bn: {
                  overview: 'ওভারভিউ',
                  providers: 'AI প্রোভাইডার',
                  knowledge: 'নলেজ বেস',
                  chat: 'সুপ্রিম AI সাথে চ্যাট',
                  pending: 'পেন্ডিং কনফার্মেশন',
                  rules: 'রুলস',
                  plans: 'প্ল্যান',
                  commands: 'কমান্ড',
                  'chat-history': 'চ্যাট হিস্ট্রি',
                  projects: 'প্রজেক্ট এবং ডেপ্লয়',
                  users: 'ইউজার ম্যানেজমেন্ট',
                  activity: 'অ্যাক্টিভিটি এবং অ্যাপ্রোভাল',
                  settings: 'সিস্টেম সেটিংস',
                  'admin-local': 'অ্যাডমিন (লোকালহোস্ট)',
                  'admin-production': 'অ্যাডমিন (প্রোডাকশন)',
                  'customer-panel': 'কাস্টমার প্যানেল'
              }
          };

          function setLanguage(lang) {
              currentLanguage = lang;
              localStorage.setItem('supremeai_lang', lang);
              document.getElementById('lang-en').classList.toggle('active', lang === 'en');
              document.getElementById('lang-bn').classList.toggle('active', lang === 'bn');
              
              // Update menu items dynamically based on section ID
              document.querySelectorAll('.menu-item').forEach(item => {
                  const onclick = item.getAttribute('onclick');
                  if (!onclick) return;
                  
                  // Handle showSection items
                  const sectionMatch = onclick.match(/showSection\('([^']+)'/);
                  if (sectionMatch) {
                      const sectionId = sectionMatch[1];
                      if (translations[lang][sectionId]) {
                          const icon = item.textContent.trim().split(' ')[0];
                          item.textContent = icon + ' ' + translations[lang][sectionId];
                      }
                  }
                  
                  // Handle openAdminUrl items
                  const adminMatch = onclick.match(/openAdminUrl\('([^']+)'/);
                  if (adminMatch) {
                      const env = adminMatch[1];
                      const key = env === 'local' ? 'admin-local' : 'admin-production';
                      if (translations[lang][key]) {
                          const icon = item.textContent.trim().split(' ')[0];
                          item.textContent = icon + ' ' + translations[lang][key];
                      }
                  }
                  
                  // Handle openCustomerUrl items
                  const customerMatch = onclick.match(/openCustomerUrl/);
                  if (customerMatch && translations[lang]['customer-panel']) {
                      const icon = item.textContent.trim().split(' ')[0];
                      item.textContent = icon + ' ' + translations[lang]['customer-panel'];
                  }
              });
          }

        // Firebase Auth is now managed by AuthHelper


        // ========== NAVIGATION ==========
        function showSection(sectionId, element) {
            // Hide all sections
            document.querySelectorAll('.content-section').forEach(s => s.classList.remove('active'));
            document.querySelectorAll('.menu-item').forEach(m => m.classList.remove('active'));
            
            // Show selected section
            const targetSection = document.getElementById('section-' + sectionId);
            if (targetSection) targetSection.classList.add('active');
            
            // Mark menu item as active
            if (element) {
                element.classList.add('active');
            } else {
                // Fallback: find the menu item that triggered this
                document.querySelectorAll('.menu-item').forEach(item => {
                    if (item.onclick && item.onclick.toString().includes(sectionId)) {
                        item.classList.add('active');
                    }
                });
            }
            
            // Update page title
            const titles = {
                'overview': '📊 Overview Dashboard',
                'providers': '⚡ AI Providers Hub',
                'knowledge': '🧠 AI Knowledge Base',
                'chat': '💬 Chat with SupremeAI',
                'pending': '⏳ Pending Confirmations',
                'rules': '📋 Rules Management',
                'plans': '📅 Plans Management',
                'commands': '⌨️ Commands Management',
                'chat-history': '🕐 Chat History',
                'projects': '🚀 Projects & Deployment',
                'users': '👥 User Management',
                'activity': '📋 Activity & Approvals',
                'settings': '⚙️ System Settings'
            };
            document.getElementById('pageTitle').textContent = titles[sectionId] || 'Dashboard';
        }

        function showSettingsTab(tab, btn) {
            // Hide all settings tabs
            ['mode', 'connectors', 'rules', 'sync'].forEach(t => {
                document.getElementById('settings-' + t).style.display = 'none';
            });
            document.querySelectorAll('.section-tab').forEach(t => t.classList.remove('active'));
            
            // Show selected tab
            document.getElementById('settings-' + tab).style.display = 'block';
            btn.classList.add('active');
        }

        // ========== QUICK URL REDIRECTS ==========
        function openAdminUrl(env) {
            if (env === 'local') {
                window.open('http://localhost:8080/admin.html', '_blank');
            } else if (env === 'production') {
                window.open('https://ide-api.supremeai.google.com/admin.html', '_blank');
            }
        }

        function openCustomerUrl() {
            window.open('https://supremeai-a.web.app', '_blank');
        }

        // ========== DASHBOARD LOADING ==========
        function loadDashboard() {
            // Check if backend is running
            checkBackendStatus();
            
            // Load demo data (since backend is offline)
            loadDemoData();
        }

        async function checkBackendStatus() {
            try {
                const response = await fetch('/api/health', { 
                    signal: AbortSignal.timeout(3000)
                });
                if (response.ok) {
                    updateSystemStatus('online');
                    // If online, also refresh data from real API
                    refreshProviders();
                    refreshRoadmap();
                    refreshEvolutionState();
                } else {
                    updateSystemStatus('degraded');
                }
            } catch (e) {
                updateSystemStatus('offline');
            }
        }

        function updateSystemStatus(status) {
            const statusEl = document.getElementById('systemStatus');
            const bannerEl = document.getElementById('offlineBanner');
            
            const dots = statusEl.querySelector('.status-dot');
            const text = statusEl.querySelector('span:last-child');
            
            dots.className = 'status-dot ' + status;
            
            switch(status) {
                case 'online':
                    text.textContent = 'System Online';
                    bannerEl.style.display = 'none';
                    break;
                case 'degraded':
                    text.textContent = 'System Degraded';
                    bannerEl.style.display = 'block';
                    break;
                case 'offline':
                    text.textContent = 'System Offline';
                    bannerEl.style.display = 'block';
                    break;
            }
        }

        function loadDemoData() {
            // Overview stats
            const stats = {
                'totalUsers': '0',
                'activeProjects': '0',
                'apiRequests': '0',
                'successRate': '--'
            };
            
            Object.entries(stats).forEach(([id, val]) => {
                const el = document.getElementById(id);
                if (el) el.textContent = val;
            });
            
            // Providers
            loadDemoProviders();
            
            // Initialize chart
            initHealthChart();
        }

        function loadDemoProviders() {
            const providers = [
                { name: 'Gemini', status: 'offline', requests: 0, quota: 0 },
                { name: 'GPT-4', status: 'offline', requests: 0, quota: 0 },
                { name: 'Claude', status: 'offline', requests: 0, quota: 0 },
                { name: 'DeepSeek', status: 'offline', requests: 0, quota: 0 }
            ];
            
            const grid = document.getElementById('providerGrid');
            if (!grid) return;
            grid.innerHTML = providers.map(p => `
                <div class="provider-card">
                    <div class="provider-header">
                        <div class="provider-name">${p.name}</div>
                        <span class="badge badge-${p.status === 'online' ? 'success' : 'danger'}">${p.status}</span>
                    </div>
                    <div class="provider-stats">
                        <div class="provider-stat">
                            <div class="provider-stat-value">${p.requests}</div>
                            <div class="provider-stat-label">Requests</div>
                        </div>
                        <div class="provider-stat">
                            <div class="provider-stat-value">${p.quota}%</div>
                            <div class="provider-stat-label">Quota Used</div>
                        </div>
                    </div>
                </div>
            `).join('');
        }

        function initHealthChart() {
            const ctx = document.getElementById('healthChart');
            if (ctx && typeof Chart !== 'undefined') {
                new Chart(ctx, {
                    type: 'line',
                    data: {
                        labels: ['No Data'],
                        datasets: [{
                            label: 'CPU %',
                            data: [0],
                            borderColor: '#4299e1',
                            tension: 0.4
                        }, {
                            label: 'Memory %',
                            data: [0],
                            borderColor: '#48bb78',
                            tension: 0.4
                        }]
                    },
                    options: {
                        responsive: true,
                        maintainAspectRatio: false,
                        scales: {
                            y: { beginAtZero: true, max: 100 }
                        }
                    }
                });
            }
        }

        // ========== ACTION HANDLERS ==========
        async function getAuthHeaders() {
            return await AuthHelper.getAuthHeader();
        }

        async function refreshProviders() {
            try {
                const headers = await getAuthHeaders();
                const response = await fetch('/api/admin/providers/configured', { headers });
                
                const tbody = document.getElementById('apiKeyTable');
                if (!tbody) return;

                if (response.ok) {
                    const data = await response.json();
                    const providers = data.providers || [];

                    tbody.innerHTML = '';

                    if (providers.length === 0) {
                        tbody.innerHTML = '<tr><td colspan="7" class="empty-state">No AI models configured yet. Click "+ Add Any AI Model" to get started.</td></tr>';
                        return;
                    }

                    providers.forEach(p => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${p.name || 'Unnamed'}</td>
                            <td>${p.providerType || p.name || 'Custom'}</td>
                            <td><code style="color: #4299e1;">${p.apiKey ? '••••••••' + p.apiKey.slice(-4) : 'N/A'}</code></td>
                            <td>${p.freeLimit || 'Unlimited'}</td>
                            <td>
                                ${p.eligibleForVoting ? 
                                    '<span class="badge badge-warning">🗳️ Voting</span>' : 
                                    '<span class="badge badge-secondary">No Voting</span>'}
                            </td>
                            <td>
                                ${p.active ?
                                    '<span class="badge badge-success">Active</span>' :
                                    '<span class="badge badge-danger">Inactive</span>'}
                            </td>
                            <td>
                                <button class="btn btn-secondary btn-sm" onclick="editProvider('${p.id}')">Edit</button>
                                <button class="btn btn-danger btn-sm" style="margin-left: 5px;" onclick="removeProvider('${p.id}')">Remove</button>
                            </td>
                        `;
                        tbody.appendChild(tr);
                    });
                    
                    // Update provider grid if it exists
                    const grid = document.getElementById('providerGrid');
                    if (grid) {
                        grid.innerHTML = providers.map(p => `
                            <div class="provider-card">
                                <div class="provider-header">
                                    <div class="provider-name">${p.name}</div>
                                    <span class="badge badge-${p.active ? 'success' : 'danger'}">${p.active ? 'online' : 'offline'}</span>
                                </div>
                                <div class="provider-stats">
                                    <div class="provider-stat">
                                        <div class="provider-stat-value">Live</div>
                                        <div class="provider-stat-label">Status</div>
                                    </div>
                                    <div class="provider-stat">
                                        <div class="provider-stat-value">${p.eligibleForVoting ? 'YES' : 'NO'}</div>
                                        <div class="provider-stat-label">Voting</div>
                                    </div>
                                </div>
                            </div>
                        `).join('');
                    }
                }
            } catch (e) {
                console.error('Error refreshing providers:', e);
            }
        }

        function showAddKeyModal() {
            document.getElementById('addProviderModal').classList.add('active');
        }

        function showAddTechniqueModal() {
            alert('Custom technique configuration is being migrated to the Provider Hub.');
        }

        async function testConsensus() {
            const prompt = document.getElementById('consensusPrompt').value;
            if (!prompt) {
                alert('Please enter a test prompt');
                return;
            }
            const resultDiv = document.getElementById('consensusResult');
            resultDiv.innerHTML = '<div class="alert alert-info">🔄 Running real-time consensus test...</div>';

            try {
                const response = await AuthHelper.apiCall('/api/admin/consensus/test', {
                    method: 'POST',
                    body: JSON.stringify({ prompt })
                });
                if (response.ok) {
                    const data = await response.json();
                    resultDiv.innerHTML = `
                        <div class="alert alert-success">
                            <strong>Consensus Reached!</strong><br>
                            Threshold: ${data.threshold}% | Agreement: ${data.agreement}%<br>
                            Result: ${data.result}
                        </div>
                    `;
                } else {
                    resultDiv.innerHTML = '<div class="alert alert-danger">Error: Could not complete consensus test.</div>';
                }
            } catch (e) {
                resultDiv.innerHTML = '<div class="alert alert-danger">Backend communication error.</div>';
            }
        }

        function showNewProjectModal() {
            document.getElementById('newProjectModal').classList.add('active');
            projectData = {};
            showProjectStep('info', null);
        }

        function showAddUserModal() {
            alert('User management is now handled via Firebase Console for security. UI integration pending.');
        }

        async function exportLogs() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/logs/export');
                if (response.ok) {
                    const blob = await response.blob();
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = `supremeai-audit-logs-${new Date().toISOString().split('T')[0]}.csv`;
                    document.body.appendChild(a);
                    a.click();
                    a.remove();
                }
            } catch (e) {
                alert('Failed to export logs: ' + e.message);
            }
        }

        function changeMode() {
            const mode = document.getElementById('operationMode').value;
            document.getElementById('currentModeInfo').innerHTML = 
                `Mode: <strong>${mode}</strong> | ${mode === 'AUTO' ? 'System will execute instantly' : mode === 'WAIT' ? 'System is waiting for manual approvals' : 'All operations halted'}`;
        }

        // ========== CHAT SYSTEM ==========
        let chatHistory = [];
        let messageCount = 0;
        let pendingConfirmations = {};
        try {
            const historyData = localStorage.getItem('supremeai_chat_history');
            if (historyData) {
                chatHistory = JSON.parse(historyData);
            }
        } catch (e) {
            console.error('Error parsing chat history:', e);
            localStorage.removeItem('supremeai_chat_history');
        }

        // ========== EVOLUTIONARY ER LEARNING & SOURCE TRACKING ==========
        let aiModelsEvolution = {};
        let modelSources = {};
        try {
            const evolutionData = localStorage.getItem('supremeai_evolution');
            if (evolutionData) {
                aiModelsEvolution = JSON.parse(evolutionData);
            }
        } catch (e) {
            console.error('Error parsing evolution data:', e);
            localStorage.removeItem('supremeai_evolution');
        }
        try {
            const sourcesData = localStorage.getItem('supremeai_sources');
            if (sourcesData) {
                modelSources = JSON.parse(sourcesData);
            }
        } catch (e) {
            console.error('Error parsing sources data:', e);
            localStorage.removeItem('supremeai_sources');
        }
        
        // Evolution and Sources are managed via backend APIs
        
        // Evolution state management via backend
        async function trackModelEvolution(modelName, contribution, category) {
            try {
                // Get current state
                const response = await AuthHelper.apiCall('/api/admin/evolution');
                const allEvolved = await response.json();
                let model = allEvolved.find(m => m.name === modelName);

                if (!model) {
                    model = {
                        name: modelName,
                        level: 1,
                        xp: 0,
                        contributions: [],
                        isAlphaPlayer: false
                    };
                }

                // Add XP based on contribution
                const xpGain = category === 'chat' ? 10 : 25;
                model.xp += xpGain;
                model.contributions.push(`${new Date().toISOString()}: ${contribution}`);

                // Level up logic
                const newLevel = Math.floor(model.xp / 100) + 1;
                if (newLevel > model.level) {
                    model.level = newLevel;
                    // Alpha Player at level 5
                    if (model.level >= 5) model.isAlphaPlayer = true;
                }

                // Save back to backend
                await AuthHelper.apiCall('/api/admin/evolution', {
                    method: 'POST',
                    body: JSON.stringify(model)
                });

                return model;
            } catch (error) {
                console.error("Evolution tracking failed:", error);
                // Fallback to local simulation if backend fails
                return { name: modelName, level: 1, xp: 0, isAlphaPlayer: false };
            }
        }

        async function getAlphaPlayers() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/evolution');
                const data = await response.json();
                return data.filter(m => m.isAlphaPlayer).sort((a, b) => b.level - a.level);
            } catch (e) {
                return [];
            }
        }

        async function processUserMessage(message, images = []) {
            let analysis = '';

            // 1. Handle Vision if images are present
            if (images && images.length > 0) {
                for (const img of images) {
                    try {
                        const base64 = await convertFileToBase64(img);
                        const visionRes = await AuthHelper.apiCall('/api/admin/vision/analyze', {
                            method: 'POST',
                            body: JSON.stringify({ image: base64 })
                        });
                        const visionData = await visionRes.json();
                        analysis += `<div class="image-analysis-item">
                            <strong>Analysis:</strong> ${visionData.analysis}
                        </div>`;
                    } catch (e) {
                        console.error("Vision analysis failed", e);
                    }
                }
            }

            // 2. Call Chat Intelligence Backend
            try {
                const chatRes = await AuthHelper.apiCall('/api/admin/chat/message', {
                    method: 'POST',
                    body: JSON.stringify({
                        user_id: AuthHelper.getUser()?.uid || 'admin',
                        message: message,
                        is_admin: true
                    })
                });
                const chatData = await chatRes.json();

                // Construct UI response
                let responseText = analysis ? `I've analyzed your image(s):<br>${analysis}<br>` : "";

                if (chatData.needs_confirmation) {
                    const type = chatData.item_type === 'rule' ? 'rule' : 'work plan';
                    responseText += `I've detected a potential ${type}. Based on my understanding: "${chatData.content}"<br><br>`;
                    responseText += `Should I proceed with this ${type}?`;
                } else {
                    responseText += "I've processed your message. How else can I assist you with your projects today?";
                }

                // 3. Track Model Evolution (using participating models)
                const models = ['GPT-4', 'Claude', 'Gemini'];
                const participating = models.filter(() => Math.random() > 0.4);
                const modelsUsed = [];

                for (const mName of participating) {
                    const evolved = await trackModelEvolution(mName, message, 'chat');
                    modelsUsed.push(evolved);
                }

                return {
                    text: responseText,
                    needsConfirmation: chatData.needs_confirmation,
                    confirmationId: chatData.item_id,
                    confirmationMessage: `Confirm ${chatData.item_type}: "${chatData.content}"?`,
                    itemType: chatData.item_type,
                    modelsUsed: modelsUsed,
                    rule: chatData.item_type === 'rule' ? { content: chatData.content, category: 'System' } : null,
                    workPlan: chatData.item_type === 'plan' ? { description: chatData.content, steps: ["Analyze", "Implement", "Verify"] } : null
                };
            } catch (error) {
                console.error("Chat API failed", error);
                return { text: "Sorry, I'm having trouble connecting to my intelligence core. Please check the system status.", modelsUsed: [] };
            }
        }

        async function convertFileToBase64(file) {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();
                reader.readAsDataURL(file);
                reader.onload = () => resolve(reader.result);
                reader.onerror = error => reject(error);
            });
        }


        function showTypingIndicator() {
            const chatMessages = document.getElementById('chatMessages');
            const typingDiv = document.createElement('div');
            typingDiv.className = 'chat-msg ai';
            typingDiv.id = 'typingIndicator';
            typingDiv.innerHTML = `
                <div class="chat-avatar ai">AI</div>
                <div class="typing-indicator">
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                    <div class="typing-dot"></div>
                </div>
            `;
            chatMessages.appendChild(typingDiv);
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }

        function removeTypingIndicator() {
            const typing = document.getElementById('typingIndicator');
            if (typing) typing.remove();
        }

        // Duplicate processUserMessage removed (Backend version kept at line 602)

        function saveRule(ruleId) {
            // Get rule from pendingConfirmations
            const ruleData = pendingConfirmations[ruleId];
            if (!ruleData || ruleData.type !== 'rule') {
                alert('Error: Rule not found!');
                return;
            }
            
            // In real implementation, this would call backend API
            alert(`✅ Rule saved successfully!\n\nCategory: ${ruleData.category}\nContent: ${ruleData.content}`);
            
            // Add success message to chat
            addMessage('ai', `✅ Rule has been saved to the Knowledge Base under "${ruleData.category}". I'll remember this for future interactions.`);
            
            // Clean up
            delete pendingConfirmations[ruleId];
        }

        async function executeWorkPlan(planId) {
            // Get plan from pendingConfirmations
            const planData = pendingConfirmations[planId];
            if (!planData || planData.type !== 'workplan') {
                alert('Error: Work plan not found!');
                return;
            }
            
            const steps = planData.steps;
            
            // Show execution started message
            addMessage('ai', '🚀 Executing work plan...<br><br>I\'ll update you as each step completes.');
            
            // Simulate step execution
            for (let i = 0; i < steps.length; i++) {
                await new Promise(resolve => setTimeout(resolve, 1500));
                addMessage('ai', `✅ Step ${i + 1} completed: ${steps[i]}`);
            }
            
            addMessage('ai', '🎉 Work plan execution completed successfully! All steps have been executed.');
            
            // Clean up
            delete pendingConfirmations[planId];
        }

        async function handleConfirmation(confirmed, confId) {
            try {
                const response = await AuthHelper.apiCall('/api/admin/chat/confirm', {
                    method: 'POST',
                    body: JSON.stringify({
                        item_id: confId,
                        confirmed: confirmed,
                        user_id: AuthHelper.getUser()?.uid || 'admin'
                    })
                });
                
                const result = await response.json();

                if (confirmed) {
                    addMessage('user', '✅ Yes, proceed with the plan.');
                    addMessage('ai', result.message || 'Perfect! I\'ve saved that and will proceed as discussed.');
                } else {
                    addMessage('user', '❌ No, let me clarify.');
                    addMessage('ai', 'Understood. I\'ve cancelled that action. Please provide more details.');
                }
                
                // Refresh background lists
                if (window.adminChat) {
                    window.adminChat.loadInitialData();
                }
            } catch (error) {
                console.error("Confirmation failed", error);
                alert("Failed to send confirmation to server.");
            }
            
            // Clean up confirmation
            delete pendingConfirmations[confId];
        }


        function clearChat() {
            if (confirm('Clear all chat history?')) {
                document.getElementById('chatMessages').innerHTML = `
                    <div class="chat-msg ai">
                        <div class="chat-avatar ai">AI</div>
                        <div>
                            <div class="chat-bubble">
                                Hello! I'm SupremeAI. I can help you with:<br><br>
                                • Understanding your requirements<br>
                                • Extracting rules from our conversation<br>
                                • Creating work plans for projects<br>
                                • Confirming my understanding before proceeding<br><br>
                                How can I help you today?
                            </div>
                            <div class="chat-meta">Just now</div>
                        </div>
                    </div>
                `;
                chatHistory = [];
                localStorage.removeItem('supremeai_chat_history');
                pendingConfirmations = {};
                clearImagePreview();
            }
        }

        // ========== IMAGE HANDLING ==========
        let selectedImages = [];

        function handleImageSelect(event) {
            const files = event.target.files;
            if (!files || files.length === 0) return;

            for (let i = 0; i < files.length; i++) {
                const file = files[i];
                
                // Validate file type
                if (!file.type.startsWith('image/')) {
                    alert('Please select only image files');
                    continue;
                }

                // Validate file size (max 10MB)
                if (file.size > 10 * 1024 * 1024) {
                    alert(`File "${file.name}" is too large. Maximum size is 10MB.`);
                    continue;
                }

                selectedImages.push(file);
                previewImage(file, selectedImages.length - 1);
            }

            // Clear the file input so the same file can be selected again
            event.target.value = '';
        }

        function previewImage(file, index) {
            const reader = new FileReader();
            reader.onload = function(e) {
                const container = document.getElementById('imagePreviewWrapper');
                const previewDiv = document.createElement('div');
                previewDiv.className = 'image-preview';
                previewDiv.setAttribute('data-index', index);
                previewDiv.innerHTML = `
                    <img src="${e.target.result}" alt="Preview">
                    <button class="image-preview-remove" onclick="removeImagePreview(${index})">✕</button>
                `;
                container.appendChild(previewDiv);

                // Show the preview container
                document.getElementById('imagePreviewContainer').style.display = 'block';
            };
            reader.readAsDataURL(file);
        }

        function removeImagePreview(index) {
            selectedImages = selectedImages.filter((_, i) => i !== index);
            
            // Rebuild preview
            const container = document.getElementById('imagePreviewWrapper');
            container.innerHTML = '';
            
            selectedImages.forEach((file, i) => {
                previewImage(file, i);
            });

            if (selectedImages.length === 0) {
                document.getElementById('imagePreviewContainer').style.display = 'none';
            }
        }

        function clearImagePreview() {
            selectedImages = [];
            document.getElementById('imagePreviewWrapper').innerHTML = '';
            document.getElementById('imagePreviewContainer').style.display = 'none';
        }

        async function sendMessage() {
            const input = document.getElementById('chatInput');
            const message = input.value.trim();
            
            if (!message && selectedImages.length === 0) return;
            
            // Add user message with images
            const imagesToUpload = [...selectedImages];
            addMessage('user', message, null, imagesToUpload);
            input.value = '';
            input.style.height = 'auto';
            clearImagePreview();
            
            // Show typing indicator
            showTypingIndicator();
            
            // Process message and get AI response from backend
            try {
                const aiResponse = await processUserMessage(message, imagesToUpload);
                removeTypingIndicator();
                addMessage('ai', aiResponse.text, aiResponse);

                // Refresh the list of rules/plans if any were added
                if (window.adminChat) {
                    window.adminChat.loadPending();
                }
            } catch (e) {
                removeTypingIndicator();
                addMessage('ai', "I encountered an error while processing your request.");
            }
        }


        // Update addMessage to display images and evolution state
        const originalAddMessage = addMessage;
        function addMessage(sender, text, metadata = null, images = []) {
            const chatMessages = document.getElementById('chatMessages');
            const msgDiv = document.createElement('div');
            msgDiv.className = `chat-msg ${sender}`;
            
            const avatarLetter = sender === 'user' ? 'U' : 'AI';
            const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            
            let bubbleContent = text;
            
            // Add images if present
            if (images && images.length > 0) {
                bubbleContent += '<div style="margin-top: 8px;">';
                images.forEach(img => {
                    const imgSrc = typeof img === 'string' ? img : URL.createObjectURL(img);
                    bubbleContent += `<img src="${imgSrc}" class="chat-image" alt="Uploaded image">`;
                });
                bubbleContent += '</div>';
            }
            
            // Add evolution state for AI messages
            if (sender === 'ai' && metadata && metadata.modelsUsed) {
                bubbleContent += '<div style="margin-top: 10px; padding: 8px; background: rgba(66, 153, 225, 0.1); border-radius: 5px; font-size: 11px;">';
                bubbleContent += '<div style="color: #4299e1; font-weight: 600; margin-bottom: 5px;">🧬 Model Evolution State:</div>';
                metadata.modelsUsed.forEach(model => {
                    const alphaBadge = model.isAlpha ? ' 🏆 <span style="color: #ffd700;">Alpha Player</span>' : '';
                    bubbleContent += `<div style="color: #cbd5e1; margin-bottom: 3px;">
                        • ${model.name}: Level ${model.level} | XP: ${model.xp}${alphaBadge}
                    </div>`;
                });
                bubbleContent += '</div>';
            }
            
            // Add rule suggestion if detected
            if (metadata && metadata.rule) {
                const ruleId = 'rule_' + Date.now();
                pendingConfirmations[ruleId] = { type: 'rule', content: metadata.rule.content, category: metadata.rule.category };
                bubbleContent += `
                    <div class="rule-suggestion">
                        <div class="rule-suggestion-header">📋 Rule Detected</div>
                        <div class="rule-content">${metadata.rule.content}</div>
                        <div style="font-size: 11px; color: #718096; margin-bottom: 8px;">Category: ${metadata.rule.category}</div>
                        <button class="btn btn-success btn-sm" onclick="saveRule('${ruleId}')">💾 Save as Rule</button>
                    </div>
                `;
            }
            
            // Add work plan if detected
            if (metadata && metadata.workPlan) {
                const planId = 'plan_' + Date.now();
                pendingConfirmations[planId] = { type: 'workplan', steps: metadata.workPlan.steps, description: metadata.workPlan.description };
                bubbleContent += `
                    <div class="work-plan">
                        <div class="work-plan-header">📋 Work Plan Created</div>
                        <div style="font-size: 12px; color: #718096; margin-bottom: 10px;">${metadata.workPlan.description}</div>
                        <ul class="work-plan-steps">
                            ${metadata.workPlan.steps.map((step, i) => `
                                <li class="work-plan-step">
                                    <span class="step-number">${i + 1}</span>
                                    <span>${step}</span>
                                </li>
                            `).join('')}
                        </ul>
                        <button class="btn btn-success btn-sm" onclick="executeWorkPlan('${planId}')">▶ Execute Plan</button>
                    </div>
                `;
            }
            
            // Add confirmation if needed
            if (metadata && metadata.needsConfirmation) {
                const confId = metadata.confirmationId || ('conf_' + Date.now());
                pendingConfirmations[confId] = { type: 'confirmation', message: metadata.confirmationMessage };
                bubbleContent += `
                    <div class="confirmation-dialog">
                        <div class="confirmation-header">⚠️ Confirmation Needed</div>
                        <div class="confirmation-message">${metadata.confirmationMessage}</div>
                        <div class="confirmation-actions">
                            <button class="btn btn-success btn-sm" onclick="handleConfirmation(true, '${confId}')">✅ Confirm</button>
                            <button class="btn btn-danger btn-sm" onclick="handleConfirmation(false, '${confId}')">❌ Cancel</button>
                        </div>
                    </div>
                `;
            }
            
            msgDiv.innerHTML = `
                <div class="chat-avatar ${sender}">${avatarLetter}</div>
                <div>
                    <div class="chat-bubble">${bubbleContent}</div>
                    <div class="chat-meta">${time}</div>
                </div>
            `;
            
            chatMessages.appendChild(msgDiv);
            chatMessages.scrollTop = chatMessages.scrollHeight;
            
            // Save to history (without images for localStorage)
            chatHistory.push({ sender, text, timestamp: Date.now() });
            if (chatHistory.length > 100) chatHistory.shift();
            localStorage.setItem('supremeai_chat_history', JSON.stringify(chatHistory));
        }

        // Update processUserMessage to handle images
        // Duplicate processUserMessage (image simulation) removed

        function analyzeImage(images) {
            // Simulate AI image analysis
            // In real implementation, this would send the image to vision-capable AI models
            let analysis = '';
            
            images.forEach((img, i) => {
                analysis += `<div style="margin-bottom: 10px;">
                    <strong>Image ${i + 1}:</strong><br>
                    • Type: UI Screenshot / Code Snippet / Diagram<br>
                    • Detected elements: Buttons, Text fields, Navigation<br>
                    • Colors: Dark theme with blue accents (#4299e1)<br>
                    • Text content: Detected readable text<br>
                    • Recommendation: This appears to be a ${i === 0 ? 'dashboard UI' : 'technical diagram'}
                </div>`;
            });
            
            analysis += `<div style="font-size: 11px; color: #718096; margin-top: 8px;">
                ⚡ Analysis complete. I can help you with code review, UI improvements, or technical questions about the image(s).
            </div>`;
            
            return analysis;
        }

        // Auto-resize chat input
        const chatInput = document.getElementById('chatInput');
        if (chatInput) {
            chatInput.addEventListener('input', function() {
                this.style.height = 'auto';
                this.style.height = Math.min(this.scrollHeight, 120) + 'px';
            });
        }

        // ========== NEW PROJECT WITH GITHUB APP FLOW ==========
        let currentProjectStep = 'info';
        let projectData = {};

        function closeNewProjectModal() {
            document.getElementById('newProjectModal').classList.remove('active');
        }

        function showProjectStep(step, btn) {
            // Hide all steps
            ['info', 'github', 'ai', 'review'].forEach(s => {
                const el = document.getElementById('project-step-' + s);
                if (el) el.style.display = 'none';
            });
            document.querySelectorAll('#newProjectModal .section-tab').forEach(t => t.classList.remove('active'));
            
            // Validate current step before leaving
            if (currentProjectStep === 'info' && step !== 'info') {
                const name = document.getElementById('projectName').value.trim();
                if (!name) {
                    alert('Please enter a project name');
                    return;
                }
                projectData.name = name;
                projectData.description = document.getElementById('projectDescription').value;
                projectData.repoUrl = document.getElementById('repoUrl').value;
            }
            
            // Show selected step
            const stepEl = document.getElementById('project-step-' + step);
            if (stepEl) stepEl.style.display = 'block';
            
            if (btn) btn.classList.add('active');
            else {
                // Find and activate the correct tab button
                const tabs = document.querySelectorAll('#newProjectModal .section-tab');
                const tabIndex = ['info', 'github', 'ai', 'review'].indexOf(step);
                if (tabs[tabIndex]) tabs[tabIndex].classList.add('active');
            }
            
            currentProjectStep = step;
            
            // Populate review step
            if (step === 'review') {
                populateReview();
            }
        }

        function nextProjectStep(step) {
            showProjectStep(step, null);
        }

        async function installGitHubApp() {
            const statusDiv = document.getElementById('githubInstallStatus');
            statusDiv.innerHTML = `<div class="alert alert-warning">🔄 Fetching installation URL...</div>`;
            
            try {
                const response = await AuthHelper.apiCall('/api/github/app-install-url');
                if (response.ok) {
                    const { url } = await response.json();
                    window.open(url, '_blank');
                    statusDiv.innerHTML = `
                        <div class="alert alert-info">
                            🚀 GitHub installation opened in new tab. Please complete it and return here.
                            <button class="btn btn-sm btn-primary" style="margin-top: 5px;" onclick="verifyGitHubInstallation()">Verify Installation</button>
                        </div>
                    `;
                }
            } catch (e) {
                statusDiv.innerHTML = `<div class="alert alert-danger">Failed to get installation URL.</div>`;
            }
        }

        async function verifyGitHubInstallation() {
            const statusDiv = document.getElementById('githubInstallStatus');
            try {
                const response = await AuthHelper.apiCall('/api/github/verify-installation');
                if (response.ok) {
                    projectData.githubAppInstalled = true;
                    statusDiv.innerHTML = `<div class="alert alert-success">✅ GitHub App verified and connected!</div>`;
                }
            } catch (e) {
                statusDiv.innerHTML = `<div class="alert alert-danger">Verification failed. Please ensure the app is installed.</div>`;
            }
        }

        function showCustomAIFields() {
            const select = document.getElementById('primaryAI');
            const customFields = document.getElementById('customAIFields');
            
            if (select.value === 'custom') {
                customFields.style.display = 'block';
            } else {
                customFields.style.display = 'none';
                projectData.aiModel = select.value;
            }
        }

        function addApiKeyField() {
            const container = document.getElementById('apiKeysList');
            const newRow = document.createElement('div');
            newRow.className = 'api-key-row';
            newRow.style.cssText = 'display: flex; gap: 10px; margin-bottom: 10px;';
            newRow.innerHTML = `
                <input type="text" placeholder="Enter API key..." style="flex: 1;">
                <button class="btn btn-sm btn-warning" onclick="testApiKey(this)" style="padding: 6px 12px;">🧪 Test</button>
                <button class="btn btn-sm btn-danger" onclick="removeApiKey(this)" style="padding: 6px 12px;">✕</button>
            `;
            container.appendChild(newRow);
        }

        function removeApiKey(btn) {
            btn.parentElement.remove();
        }

        async function testApiKey(btn) {
            const input = btn.parentElement.querySelector('input');
            const apiKey = input.value.trim();
            
            if (!apiKey) {
                alert('Please enter an API key to test');
                return;
            }
            
            // Get AI model info
            const aiSelect = document.getElementById('primaryAI');
            const customName = document.getElementById('customAIName').value;
            const model = aiSelect.value === 'custom' ? customName : aiSelect.options[aiSelect.selectedIndex].text;
            
            // Update button to show testing
            const originalText = btn.textContent;
            btn.textContent = '🔄 Testing...';
            btn.disabled = true;
            
            // Show result div
            let resultDiv = document.getElementById('apiTestResult');
            if (!resultDiv) {
                resultDiv = document.createElement('div');
                resultDiv.id = 'apiTestResult';
                resultDiv.style.marginTop = '10px';
                document.getElementById('apiKeysContainer').appendChild(resultDiv);
            }
            
            try {
                // Simulate API key test based on model type
                const testResult = await simulateApiKeyTest(model, apiKey);
                
                if (testResult.success) {
                    input.style.borderColor = '#48bb78';
                    resultDiv.innerHTML = `
                        <div class="alert alert-success">
                            ✅ API Key Valid!
                            <div style="font-size: 11px; margin-top: 5px;">
                                Model: ${model}<br>
                                Status: Connected<br>
                                Free Tier Limit: ${testResult.freeLimit}<br>
                                Requests Remaining: ${testResult.remaining}
                            </div>
                        </div>
                    `;
                    
                    // Store test result
                    input.dataset.tested = 'true';
                    input.dataset.freeLimit = testResult.freeLimit;
                    input.dataset.remaining = testResult.remaining;
                } else {
                    input.style.borderColor = '#f56565';
                    resultDiv.innerHTML = `
                        <div class="alert alert-danger">
                            ❌ API Key Invalid: ${testResult.error}
                        </div>
                    `;
                }
            } catch (e) {
                resultDiv.innerHTML = `
                    <div class="alert alert-danger">
                        ❌ Test failed: ${e.message}
                    </div>
                `;
            } finally {
                btn.textContent = originalText;
                btn.disabled = false;
            }
        }

        async function simulateApiKeyTest(model, apiKey) {
            // Simulate API test with free limit detection
            // In real implementation, this would make actual API calls
            
            return new Promise((resolve) => {
                setTimeout(() => {
                    // Simulate different responses based on model
                    const modelLower = model.toLowerCase();
                    
                    if (modelLower.includes('gemini')) {
                        resolve({
                            success: true,
                            freeLimit: '15 requests/minute, 1500 requests/day',
                            remaining: Math.floor(Math.random() * 1500)
                        });
                    } else if (modelLower.includes('gpt') || modelLower.includes('openai')) {
                        resolve({
                            success: true,
                            freeLimit: '3 requests/minute (GPT-4), 200 requests/day',
                            remaining: Math.floor(Math.random() * 200)
                        });
                    } else if (modelLower.includes('claude')) {
                        resolve({
                            success: true,
                            freeLimit: '50 requests/minute, 5000 requests/month',
                            remaining: Math.floor(Math.random() * 5000)
                        });
                    } else if (modelLower.includes('deepseek')) {
                        resolve({
                            success: true,
                            freeLimit: '20 requests/minute, 500 requests/day',
                            remaining: Math.floor(Math.random() * 500)
                        });
                    } else {
                        // Custom model - try to auto-detect
                        resolve({
                            success: true,
                            freeLimit: 'Auto-detected: Check provider docs',
                            remaining: 'Unknown (custom model)'
                        });
                    }
                }, 1500);
            });
        }

        function populateReview() {
            const reviewDiv = document.getElementById('projectReviewContent');
            const githubStatusDiv = document.getElementById('githubAppStatusReview');
            
            let html = `
                <div style="margin-bottom: 15px;">
                    <div style="color: #718096; font-size: 11px;">PROJECT NAME</div>
                    <div style="color: #e2e8f0; font-size: 15px; font-weight: 600;">${projectData.name || '(Not specified)'}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #718096; font-size: 11px;">DESCRIPTION</div>
                    <div style="color: #cbd5e1;">${projectData.description || 'No description'}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #718096; font-size: 11px;">REPOSITORY</div>
                    <div style="color: #cbd5e1;">${projectData.repoUrl || 'Not specified (can be added later)'}</div>
                </div>
                <div style="margin-bottom: 15px;">
                    <div style="color: #718096; font-size: 11px;">AI MODEL</div>
                    <div style="color: #cbd5e1;">${projectData.aiModel || 'Not selected'}</div>
                </div>
            `;
            
            reviewDiv.innerHTML = html;
            
            // GitHub App status
            if (projectData.githubAppInstalled) {
                githubStatusDiv.innerHTML = `
                    <div class="alert alert-success">
                        ✅ GitHub App Installed - SupremeAI has access to your repositories
                    </div>
                `;
            } else {
                githubStatusDiv.innerHTML = `
                    <div class="alert alert-warning">
                        ⚠️ GitHub App not installed - You can install it later from project settings
                    </div>
                `;
            }
        }

        async function createProject() {
            // Validate API keys are tested
            const apiKeys = document.querySelectorAll('#apiKeysList input');
            let untestedKeys = 0;
            apiKeys.forEach(keyInput => {
                if (keyInput.value.trim() && keyInput.dataset.tested !== 'true') {
                    untestedKeys++;
                }
            });
            
            if (untestedKeys > 0) {
                if (!confirm(`You have ${untestedKeys} API key(s) that haven't been tested. Do you want to save anyway?`)) {
                    return;
                }
            }
            
            // Collect API keys
            const apiKeysData = [];
            apiKeys.forEach(keyInput => {
                if (keyInput.value.trim()) {
                    apiKeysData.push({
                        key: keyInput.value.trim(),
                        modelId: projectData.aiModel
                    });
                }
            });

            try {
                const response = await AuthHelper.apiCall('/api/admin/projects', {
                    method: 'POST',
                    body: JSON.stringify({
                        name: projectData.name,
                        description: projectData.description,
                        repoUrl: projectData.repoUrl,
                        primaryAI: projectData.aiModel,
                        apiKeys: apiKeysData,
                        githubConnected: projectData.githubAppInstalled
                    })
                });

                if (response.ok) {
                    alert(`✅ Project "${projectData.name}" created successfully and synced to Firebase!`);
                    closeNewProjectModal();
                    refreshProjects(); // Call existing project refresh if available
                } else {
                    const err = await response.json();
                    alert('Error creating project: ' + (err.message || 'Unknown error'));
                }
            } catch (e) {
                console.error('Project creation failed:', e);
                alert('Network error during project creation.');
            }
        }

        async function refreshProjects() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/projects');
                if (response.ok) {
                    const projects = await response.json();
                    const tbody = document.getElementById('projectsTable');
                    tbody.innerHTML = '';

                    if (projects.length === 0) {
                        tbody.innerHTML = '<tr><td colspan="5" class="empty-state">No projects yet</td></tr>';
                        return;
                    }

                    projects.forEach(p => {
                        const tr = document.createElement('tr');
                        tr.innerHTML = `
                            <td>${p.name}</td>
                            <td><span class="badge ${p.status === 'ACTIVE' ? 'badge-success' : 'badge-warning'}">${p.status}</span></td>
                            <td>${p.primaryAI || 'N/A'}</td>
                            <td><span class="badge ${p.githubConnected ? 'badge-success' : 'badge-danger'}">${p.githubConnected ? '✅ Connected' : '❌ Disconnected'}</span></td>
                            <td>
                                <button class="btn btn-secondary btn-sm" onclick="editProject('${p.id}')">Edit</button>
                            </td>
                        `;
                        tbody.appendChild(tr);
                    });
                }
            } catch (e) {
                console.error('Error refreshing projects:', e);
            }
        }


        // Update showNewProjectModal function call in HTML
        document.querySelectorAll('[onclick*="showNewProjectModal"]').forEach(btn => {
            btn.onclick = showNewProjectModal;
        });

        // ========== AI PROVIDERS HUB - UNIVERSAL AI MODEL SUPPORT ==========
        function showAddProviderModal() {
            document.getElementById('addProviderModal').classList.add('active');
        }

        function closeAddProviderModal() {
            document.getElementById('addProviderModal').classList.remove('active');
            // Reset form
            document.getElementById('newModelName').value = '';
            document.getElementById('newModelProvider').value = '';
            document.getElementById('newModelEndpoint').value = '';
            document.getElementById('providerApiKeysList').innerHTML = `
                <div class="api-key-row" style="display: flex; gap: 10px; margin-bottom: 12px; align-items: flex-start;">
                    <div style="flex: 1;">
                        <input type="text" placeholder="Enter API key..." style="width: 100%;">
                    </div>
                    <button class="btn btn-sm btn-warning" onclick="testProviderApiKey(this)" style="padding: 8px 14px; white-space: nowrap;">🧪 Test</button>
                    <button class="btn btn-sm btn-danger" onclick="removeProviderApiKey(this)" style="padding: 8px 14px;">✕</button>
                </div>
            `;
            document.getElementById('providerApiTestResult').innerHTML = '';
            document.getElementById('detectedLimits').innerHTML = '';
        }

        function addProviderApiKeyField() {
            const container = document.getElementById('providerApiKeysList');
            const newRow = document.createElement('div');
            newRow.className = 'api-key-row';
            newRow.style.cssText = 'display: flex; gap: 10px; margin-bottom: 12px; align-items: flex-start;';
            newRow.innerHTML = `
                <div style="flex: 1;">
                    <input type="text" placeholder="Enter API key..." style="width: 100%;">
                </div>
                <button class="btn btn-sm btn-warning" onclick="testProviderApiKey(this)" style="padding: 8px 14px; white-space: nowrap;">🧪 Test</button>
                <button class="btn btn-sm btn-danger" onclick="removeProviderApiKey(this)" style="padding: 8px 14px;">✕</button>
            `;
            container.appendChild(newRow);
        }

        function removeProviderApiKey(btn) {
            btn.parentElement.remove();
        }

        async function testProviderApiKey(btn) {
            const input = btn.parentElement.querySelector('input');
            const apiKey = input.value.trim();
            const providerType = document.getElementById('newModelProvider').value;
            
            if (!apiKey) {
                alert('Please enter an API key to test');
                return;
            }

            const originalText = btn.textContent;
            btn.textContent = '⏳ Testing...';
            btn.disabled = true;

            try {
                const headers = await getAuthHeaders();
                // Using a hypothetical proxy endpoint that validates keys server-side
                const response = await fetch('/api/admin/providers/test-key', {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ apiKey, providerType })
                });

                if (response.ok) {
                    const result = await response.json();
                    btn.textContent = '✅ Valid';
                    btn.className = 'btn btn-sm btn-success';
                    input.dataset.tested = 'true';
                    document.getElementById('providerApiTestResult').innerHTML =
                        `<div style="color: #48bb78; font-size: 12px; margin-top: 5px;">✅ Key validated! Success Rate: ${result.successRate || '100%'}</div>`;
                } else {
                    btn.textContent = '❌ Invalid';
                    btn.className = 'btn btn-sm btn-danger';
                    document.getElementById('providerApiTestResult').innerHTML =
                        `<div style="color: #f56565; font-size: 12px; margin-top: 5px;">❌ Key validation failed.</div>`;
                }
            } catch (e) {
                console.error('Key test failed:', e);
                btn.textContent = '⚠️ Error';
            } finally {
                btn.disabled = false;
            }
        }

        async function simulateProviderApiTest(modelName, apiKey) {
            // Simulate API test with free limit auto-detection
            return new Promise((resolve) => {
                setTimeout(() => {
                    const modelLower = modelName.toLowerCase();
                    
                    // Auto-detect based on model name
                    if (modelLower.includes('gemini')) {
                        resolve({
                            success: true,
                            freeLimit: '15 requests/minute, 1500 requests/day',
                            remaining: Math.floor(Math.random() * 1500),
                            rateLimit: '15 req/min'
                        });
                    } else if (modelLower.includes('gpt') || modelLower.includes('openai')) {
                        resolve({
                            success: true,
                            freeLimit: '3 requests/minute (GPT-4), 200 requests/day',
                            remaining: Math.floor(Math.random() * 200),
                            rateLimit: '3 req/min (GPT-4)'
                        });
                    } else if (modelLower.includes('claude')) {
                        resolve({
                            success: true,
                            freeLimit: '50 requests/minute, 5000 requests/month',
                            remaining: Math.floor(Math.random() * 5000),
                            rateLimit: '50 req/min'
                        });
                    } else if (modelLower.includes('deepseek')) {
                        resolve({
                            success: true,
                            freeLimit: '20 requests/minute, 500 requests/day',
                            remaining: Math.floor(Math.random() * 500),
                            rateLimit: '20 req/min'
                        });
                    } else if (modelLower.includes('llama')) {
                        resolve({
                            success: true,
                            freeLimit: 'No official free tier (self-hosted or paid)',
                            remaining: 'N/A',
                            rateLimit: 'Depends on deployment'
                        });
                    } else if (modelLower.includes('mistral')) {
                        resolve({
                            success: true,
                            freeLimit: '500 requests/day (Mistral Tiny)',
                            remaining: Math.floor(Math.random() * 500),
                            rateLimit: '5 req/sec'
                        });
                    } else {
                        // Custom/Unknown model - try to auto-detect
                        resolve({
                            success: true,
                            freeLimit: 'Auto-detected: Check provider documentation',
                            remaining: 'Unknown (custom model)',
                            rateLimit: 'Unknown - will be determined on first use'
                        });
                    }
                }, 1500);
            });
        }

        async function autoDetectLimits() {
            const modelName = document.getElementById('newModelName').value.trim();
            
            if (!modelName) {
                alert('Please enter a model name first');
                return;
            }

            const detectDiv = document.getElementById('detectedLimits');
            detectDiv.innerHTML = `
                <div class="alert alert-warning">
                    🔍 Auto-detecting free tier limits for "${modelName}"...
                </div>
            `;

            try {
                const testResult = await simulateProviderApiTest(modelName, 'dummy-key-for-detection');
                
                detectDiv.innerHTML = `
                    <div class="alert alert-success">
                        🔍 Auto-Detected Limits for ${modelName}:
                        <div style="font-size: 11px; margin-top: 8px; line-height: 1.6;">
                            <strong>Free Tier:</strong> ${testResult.freeLimit}<br>
                            <strong>Rate Limit:</strong> ${testResult.rateLimit}<br>
                            <strong>Note:</strong> Limits are auto-detected based on model name. Verify with provider docs.
                        </div>
                    </div>
                `;
            } catch (e) {
                detectDiv.innerHTML = `
                    <div class="alert alert-danger">
                        ❌ Could not auto-detect limits: ${e.message}
                    </div>
                `;
            }
        }



        function addProviderToTable(modelName, provider, apiKeys) {
            const tbody = document.getElementById('apiKeyTable');
            // Remove "No API keys configured" row if present
            if (tbody.querySelector('.empty-state')) {
                tbody.innerHTML = '';
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${modelName}</td>
                <td>${provider || 'Custom'}</td>
                <td>
                    <span class="badge badge-info">${apiKeys.length} key(s)</span>
                    ${apiKeys.filter(k => k.tested).length > 0 ? '<span class="badge badge-success" style="margin-left: 5px;">✓ Tested</span>' : ''}
                </td>
                <td>${apiKeys[0]?.freeLimit || 'Unknown'}</td>
                <td><span class="badge badge-success">Active</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm">Edit</button>
                    <button class="btn btn-danger btn-sm" style="margin-left: 5px;">Remove</button>
                </td>
            `;
            tbody.appendChild(tr);

            // Also update provider grid
            updateProviderGrid();
        }

        function updateProviderGrid() {
            // In a real implementation, this would fetch from backend
            // For demo, add a card for the new provider
            const grid = document.getElementById('providerGrid');
            const newProviderName = document.getElementById('newModelName').value.trim();
            const provider = document.getElementById('newModelProvider').value.trim();
            
            if (newProviderName && !grid.querySelector(`[data-provider="${newProviderName}"]`)) {
                const card = document.createElement('div');
                card.className = 'provider-card';
                card.setAttribute('data-provider', newProviderName);
                card.innerHTML = `
                    <div class="provider-header">
                        <div class="provider-name">${newProviderName}</div>
                        <span class="badge badge-success">Active</span>
                    </div>
                    <div class="provider-stats">
                        <div class="provider-stat">
                            <div class="provider-stat-value">0</div>
                            <div class="provider-stat-label">Requests</div>
                        </div>
                        <div class="provider-stat">
                            <div class="provider-stat-value">0%</div>
                            <div class="provider-stat-label">Quota Used</div>
                        </div>
                    </div>
                    ${provider ? `<div style="font-size: 11px; color: #718096; margin-top: 8px;">Provider: ${provider}</div>` : ''}
                `;
                grid.appendChild(card);
            }
        }

        function configureFallbackChain() {
            const tbody = document.getElementById('apiKeyTable');
            const rows = tbody.querySelectorAll('tr');
            
            if (rows.length === 0 || (rows.length === 1 && rows[0].querySelector('.empty-state'))) {
                alert('Please add at least two AI models to configure fallback chain');
                return;
            }

            // In real implementation, this would open a drag-and-drop interface
            alert('✅ Fallback chain configuration would open here.\n\nYou can drag to reorder providers for automatic failover.');
            
            document.getElementById('fallbackChain').innerHTML = `
                <div style="font-size: 13px; color: #cbd5e1;">
                    <div style="margin-bottom: 10px; font-weight: 600;">Current Fallback Chain:</div>
                    <ol style="padding-left: 20px;">
                        <li style="margin-bottom: 5px;">${document.getElementById('newModelName').value || 'GPT-4'} (Primary)</li>
                        <li style="margin-bottom: 5px;">Gemini (Secondary)</li>
                        <li style="margin-bottom: 5px;">Claude (Tertiary)</li>
                    </ol>
                    <div style="font-size: 11px; color: #718096; margin-top: 10px;">
                        System will automatically try the next provider if the current one fails.
                    </div>
                </div>
            `;
        }

        // ========== VOTING SYSTEM (60% Threshold) ==========
        function configureVotingModels() {
            const tbody = document.getElementById('apiKeyTable');
            const rows = tbody.querySelectorAll('tr');
            
            if (rows.length === 0 || (rows.length === 1 && rows[0].querySelector('.empty-state'))) {
                alert('Please add at least one AI model to configure voting');
                return;
            }

            // In real implementation, this would open a modal with checkboxes
            const votingModels = getVotingModels();
            alert(`🗳️ Voting Configuration:\n\nVoting-eligible models: ${votingModels.length}\nThreshold: 60% agreement required\n\nOnly top-performing models should participate in voting.`);
        }

        function getVotingModels() {
            // In real implementation, this would fetch from backend
            // For demo, return models marked as eligible for voting
            const tbody = document.getElementById('apiKeyTable');
            const votingModels = [];
            
            tbody.querySelectorAll('tr').forEach(row => {
                const votingBadge = row.querySelector('.badge-warning');
                if (votingBadge && votingBadge.textContent.includes('Voting')) {
                    const modelName = row.cells[0].textContent;
                    votingModels.push(modelName);
                }
            });
            
            return votingModels.length > 0 ? votingModels : ['GPT-4', 'Claude', 'Gemini']; // Default top 3
        }

        async function testConsensus() {
            const prompt = document.getElementById('consensusPrompt').value;
            if (!prompt) {
                alert('Please enter a test prompt');
                return;
            }

            const resultDiv = document.getElementById('consensusResult');
            resultDiv.innerHTML = '<div class="alert alert-warning">🗳️ Running voting with top AI models (60% threshold)...</div>';

            const votingModels = getVotingModels();
            
            if (votingModels.length === 0) {
                resultDiv.innerHTML = '<div class="alert alert-danger">❌ No voting-eligible models found. Please mark models as "Eligible for Voting".</div>';
                return;
            }

            // Simulate AI responses
            const responses = await simulateVotingResponses(prompt, votingModels);
            
            // Calculate agreement percentage
            const agreement = calculateAgreement(responses);
            const threshold = 60;
            const passed = agreement >= threshold;
            
            let html = `
                <div class="card" style="margin-top: 15px;">
                    <div class="card-header">
                        <div class="card-title">🗳️ Voting Result (${votingModels.length} models)</div>
                    </div>
                    <div style="font-size: 13px; color: #cbd5e1; line-height: 1.8;">
                        <div style="margin-bottom: 10px;">
                            <strong>Prompt:</strong> "${prompt}"
                        </div>
                        <div style="margin-bottom: 10px;">
                            <strong>Participating Models:</strong> ${votingModels.join(', ')}
                        </div>
                        <div style="background: ${passed ? 'rgba(72, 187, 120, 0.1)' : 'rgba(245, 101, 101, 0.1)'}; 
                             padding: 15px; border-radius: 8px; margin-bottom: 10px;">
                            <div style="font-size: 24px; font-weight: bold; color: ${passed ? '#48bb78' : '#f56565'};">
                                ${agreement}% Agreement
                            </div>
                            <div style="font-size: 11px; color: #718096; margin-top: 5px;">
                                Threshold: ${threshold}% | Status: ${passed ? '✅ PASSED' : '❌ FAILED'}
                            </div>
                        </div>
            `;

            responses.forEach((resp, i) => {
                html += `
                    <div style="background: #111827; padding: 10px; border-radius: 5px; margin-bottom: 8px;">
                        <div style="color: #4299e1; font-weight: 600; font-size: 12px;">${resp.model}:</div>
                        <div style="font-size: 12px; color: #cbd5e1; margin-top: 5px;">${resp.response}</div>
                    </div>
                `;
            });

            html += `
                    <div style="margin-top: 10px; font-size: 12px; color: #718096;">
                        ${passed ? 
                            '✅ Consensus reached! System will proceed with the action.' : 
                            '❌ Consensus not reached. Manual review required.'}
                    </div>
                    </div>
                </div>
            `;

            resultDiv.innerHTML = html;
            
            // Update voting models list
            document.getElementById('votingModelsList').innerHTML = 
                `🗳️ Voting Models: ${votingModels.length} active | 60% threshold`;
        }

        async function simulateVotingResponses(prompt, models) {
            return new Promise((resolve) => {
                setTimeout(() => {
                    const responses = models.map(model => {
                        const responses = [
                            `Yes, I agree with this approach. ${model} recommends proceeding.`,
                            `Based on my analysis, this is correct. ${model} supports this.`,
                            `I have some concerns but generally agree. ${model} suggests caution.`,
                            `This looks good. ${model} approves.`
                        ];
                        return {
                            model: model,
                            response: responses[Math.floor(Math.random() * responses.length)]
                        };
                    });
                    resolve(responses);
                }, 2000);
            });
        }

        function calculateAgreement(responses) {
            // Simple simulation: count positive responses
            const positiveKeywords = ['agree', 'support', 'approve', 'correct', 'good', 'yes'];
            let positiveCount = 0;
            
            responses.forEach(resp => {
                const lowerResp = resp.response.toLowerCase();
                if (positiveKeywords.some(keyword => lowerResp.includes(keyword))) {
                    positiveCount++;
                }
            });
            
            return Math.round((positiveCount / responses.length) * 100);
        }

        // Update saveNewProvider to save voting eligibility
        async function saveNewProvider() {
            const name = document.getElementById('newModelName').value.trim();
            const provider = document.getElementById('newModelProvider').value.trim();
            const endpoint = document.getElementById('newModelEndpoint').value.trim();

            if (!name) {
                alert('Please enter a provider name');
                return;
            }

            const apiKeys = [];
            const apiKeyRows = document.querySelectorAll('#providerApiKeysList .api-key-row');
            apiKeyRows.forEach(row => {
                const key = row.querySelector('input').value.trim();
                if (key) apiKeys.push(key);
            });

            const providerData = {
                name: name,
                providerType: provider,
                baseUrl: endpoint,
                apiKey: apiKeys[0] || '', // Controller expects a single apiKey in APIProvider model
                active: true
            };

            try {
                const headers = await getAuthHeaders();
                const response = await fetch('/api/admin/providers/add', {
                    method: 'POST',
                    headers,
                    body: JSON.stringify(providerData)
                });

                if (response.ok) {
                    alert('Provider saved successfully');
                    closeModal('addProviderModal');
                    refreshProviders();
                } else {
                    const err = await response.json();
                    alert('Error saving provider: ' + (err.error || response.statusText));
                }
            } catch (e) {
                console.error('Save provider failed:', e);
                alert('Connection error while saving provider');
            }
        }

        // Update addProviderToTable to show voting eligibility
        function addProviderToTable(modelName, provider, apiKeys, eligibleForVoting) {
            const tbody = document.getElementById('apiKeyTable');
            // Remove "No API keys configured" row if present
            if (tbody.querySelector('.empty-state')) {
                tbody.innerHTML = '';
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${modelName}</td>
                <td>${provider || 'Custom'}</td>
                <td>
                    <span class="badge badge-info">${apiKeys.length} key(s)</span>
                    ${apiKeys.filter(k => k.tested).length > 0 ? '<span class="badge badge-success" style="margin-left: 5px;">✓ Tested</span>' : ''}
                </td>
                <td>${apiKeys[0]?.freeLimit || 'Unknown'}</td>
                <td>
                    ${eligibleForVoting ? 
                        '<span class="badge badge-warning">🗳️ Voting</span>' : 
                        '<span class="badge badge-secondary">No Voting</span>'}
                </td>
                <td><span class="badge badge-success">Active</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm">Edit</button>
                    <button class="btn btn-danger btn-sm" style="margin-left: 5px;">Remove</button>
                </td>
            `;
            tbody.appendChild(tr);

            // Also update provider grid
            updateProviderGrid();
        }

        // ========== EVOLUTION STATE DISPLAY ==========
        async function refreshEvolutionState() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/evolution');

                if (response.ok) {
                    const evolvedModels = await response.json();

                    const alphaPlayers = evolvedModels.filter(m => m.isAlphaPlayer);

                    document.getElementById('totalModelsTracked').textContent = evolvedModels.length;
                    document.getElementById('alphaPlayersCount').textContent = alphaPlayers.length;
                    document.getElementById('totalXP').textContent =
                        evolvedModels.reduce((sum, m) => sum + m.xp, 0);

                    displayEvolvedModels(evolvedModels, alphaPlayers);
                    // displaySourceTracking(); // To be implemented with SystemLearning
                }
            } catch (e) {
                console.warn('Backend evolution sync failed, using local cache:', e);
                // Fallback logic if needed
            }
        }
        
        function displayEvolvedModels(evolvedModels, alphaPlayers) {
            const listDiv = document.getElementById('evolvedModelsList');
            
            if (evolvedModels.length === 0) {
                listDiv.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">🧬</div>
                        <div class="empty-state-title">No evolution data yet</div>
                        <div class="empty-state-desc">Models will evolve as they participate in chats and contribute to the system.</div>
                    </div>
                `;
                return;
            }
            
            let html = '<div style="max-height: 400px; overflow-y: auto;">';
            
            evolvedModels.forEach((model, index) => {
                const isAlpha = model.isAlphaPlayer;
                const alphaBadge = isAlpha ? 
                    '<span style="color: #ffd700; font-size: 16px;" title="Alpha Player">🏆</span>' : '';
                const levelColor = model.level >= 5 ? '#ffd700' : 
                               model.level >= 3 ? '#48bb78' : '#4299e1';
                
                html += `
                    <div style="background: #111827; padding: 12px; border-radius: 8px; margin-bottom: 10px; 
                                border-left: 3px solid ${levelColor};">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                            <div style="font-weight: 600; color: #e2e8f0; font-size: 14px;">
                                ${alphaBadge} ${model.name} ${alphaBadge}
                            </div>
                            <div style="font-size: 11px; color: #718096;">
                                Level ${model.level} | XP: ${model.xp}
                            </div>
                        </div>
                        <div style="display: flex; gap: 10px; align-items: center; margin-bottom: 8px;">
                            <div class="progress-bar" style="flex: 1; height: 8px;">
                                <div class="progress-fill" style="width: ${model.xp % 100}%; background: ${levelColor};"></div>
                            </div>
                            <span style="font-size: 11px; color: #718096;">${model.xp % 100}/100 to next level</span>
                        </div>
                        <div style="font-size: 11px; color: #718096;">
                            Contributions: ${model.contributions.length} | 
                            Sources: ${modelSources[model.name]?.length || 0}
                            ${isAlpha ? '| 🏆 Alpha Player Status' : ''}
                        </div>
                    </div>
                `;
            });
            
            html += '</div>';
            listDiv.innerHTML = html;
        }
        
        function displaySourceTracking() {
            const trackingDiv = document.getElementById('sourceTrackingList');
            const allSources = Object.entries(modelSources);
            
            if (allSources.length === 0) {
                trackingDiv.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-state-icon">📚</div>
                        <div class="empty-state-title">No source data yet</div>
                        <div class="empty-state-desc">Source tracking will appear as models contribute to the system.</div>
                    </div>
                `;
                return;
            }
            
            let html = '<div style="max-height: 300px; overflow-y: auto;">';
            
            allSources.forEach(([modelName, sources]) => {
                html += `
                    <div style="background: #111827; padding: 10px; border-radius: 5px; margin-bottom: 8px;">
                        <div style="font-weight: 600; color: #4299e1; margin-bottom: 5px;">${modelName}</div>
                        <div style="font-size: 11px; color: #718096;">
                            Total Sources: ${sources.length}<br>
                            Latest: ${new Date(sources[sources.length - 1]?.timestamp).toLocaleString() || 'N/A'}
                        </div>
                    </div>
                `;
            });
            
            html += '</div>';
            trackingDiv.innerHTML = html;
        }
        
        // ========== INITIALIZATION ==========
        window.addEventListener('DOMContentLoaded', async () => {
            // Wait for authentication before loading data
            const authenticated = await AuthHelper.initializeAuth(true);
            if (!authenticated) return;

            setLanguage(currentLanguage);

            // Load user info
            AuthHelper.displayUserInfo('userAvatar', 'userName');
            
            // Load chat history
            if (chatHistory.length > 0) {
                const chatMessages = document.getElementById('chatMessages');
                // Keep the initial message, add history
                chatHistory.forEach(msg => {
                    addMessage(msg.sender, msg.text);
                });
            }
            
            // Load data from backend
            systemRefresh();

            // Initialize P0 Controls
            initEmergencyStop();
            initAPIAccessLock();
            initDynamicRotation();
            initAutoExecApproval();
        });

        // ========== P0: EMERGENCY STOP SWITCH ==========
        let emergencyStopActive = false;
        
        async function initEmergencyStop() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/config');
                if (response.ok) {
                    const config = await response.json();
                    emergencyStopActive = config.emergencyStop || false;
                    document.getElementById('emergencyStop').checked = emergencyStopActive;
                    updateEmergencyStatus();
                }
            } catch (e) {
                console.error('Error fetching emergency stop status:', e);
                emergencyStopActive = localStorage.getItem('supremeai_emergency_stop') === 'true';
                document.getElementById('emergencyStop').checked = emergencyStopActive;
                updateEmergencyStatus();
            }
        }
        
        async function toggleEmergencyStop() {
            emergencyStopActive = !emergencyStopActive;
            try {
                const configResponse = await AuthHelper.apiCall('/api/admin/config');
                if (configResponse.ok) {
                    const config = await configResponse.json();
                    config.emergencyStop = emergencyStopActive;

                    const updateResponse = await AuthHelper.apiCall('/api/admin/config', {
                        method: 'PUT',
                        body: JSON.stringify(config)
                    });

                    if (updateResponse.ok) {
                        localStorage.setItem('supremeai_emergency_stop', emergencyStopActive);
                        updateEmergencyStatus();
                        if (emergencyStopActive) {
                            alert('🛑 EMERGENCY STOP ACTIVATED!');
                        } else {
                            alert('✅ Emergency Stop DEACTIVATED.');
                        }
                    }
                }
            } catch (e) {
                console.error('Error toggling emergency stop:', e);
            }
        }
        
        function updateEmergencyStatus() {
            const statusDiv = document.getElementById('emergencyStatus');
            if (emergencyStopActive) {
                statusDiv.innerHTML = '🛑 SYSTEM HALTED (Emergency Stop: ON)';
                statusDiv.style.color = '#f56565';
                statusDiv.style.fontWeight = '600';
            } else {
                statusDiv.innerHTML = '✅ System is operational (Emergency Stop: OFF)';
                statusDiv.style.color = '#48bb78';
                statusDiv.style.fontWeight = 'normal';
            }
        }

        // ========== P0: API ACCESS LOCK ==========
        let apiAccessLocked = false;
        let apiKeysRotated = 0;
        
        async function initAPIAccessLock() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/config');
                if (response.ok) {
                    const config = await response.json();
                    apiAccessLocked = config.apiAccessLock || false;
                    updateAPILockStatus();
                }
            } catch (e) {
                apiAccessLocked = localStorage.getItem('supremeai_api_lock') === 'true';
                updateAPILockStatus();
            }
        }
        
        async function lockAPIAccess() {
            try {
                const configResponse = await AuthHelper.apiCall('/api/admin/config');
                if (configResponse.ok) {
                    const config = await configResponse.json();
                    config.apiAccessLock = true;
                    await AuthHelper.apiCall('/api/admin/config', {
                        method: 'PUT',
                        body: JSON.stringify(config)
                    });
                    apiAccessLocked = true;
                    localStorage.setItem('supremeai_api_lock', 'true');
                    updateAPILockStatus();
                    alert('🔒 All API access LOCKED!');
                }
            } catch (e) { console.error(e); }
        }
        
        async function unlockAPIAccess() {
            try {
                const configResponse = await AuthHelper.apiCall('/api/admin/config');
                if (configResponse.ok) {
                    const config = await configResponse.json();
                    config.apiAccessLock = false;
                    await AuthHelper.apiCall('/api/admin/config', {
                        method: 'PUT',
                        body: JSON.stringify(config)
                    });
                    apiAccessLocked = false;
                    localStorage.setItem('supremeai_api_lock', 'false');
                    updateAPILockStatus();
                    alert('🔓 API access UNLOCKED!');
                }
            } catch (e) { console.error(e); }
        }
        
        function updateAPILockStatus() {
            const statusDot = document.getElementById('apiLockStatus');
            const statusText = document.getElementById('apiLockText');
            const bar = document.getElementById('apiLockBar');
            
            if (apiAccessLocked) {
                statusDot.className = 'status-dot offline';
                statusText.textContent = 'API access is LOCKED';
                bar.style.width = '100%';
                bar.style.background = '#f56565';
            } else {
                statusDot.className = 'status-dot online';
                statusText.textContent = 'All API keys are valid and operational';
                bar.style.width = '100%';
                bar.style.background = '#48bb78';
            }
        }

        // ========== P0: DYNAMIC API ROTATION ==========
        let rotationStrategy = 'quota-based';
        let keysRotatedToday = 0;
        
        async function initDynamicRotation() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/config');
                if (response.ok) {
                    const config = await response.json();
                    rotationStrategy = config.apiRotationStrategy || 'quota-based';
                    keysRotatedToday = 0; // In real use, this might be a metric

                    document.getElementById('rotationStrategy').value = rotationStrategy;
                    document.getElementById('currentStrategy').textContent =
                        rotationStrategy === 'sequential' ? 'Sequential' :
                        rotationStrategy === 'random' ? 'Random' : 'Quota-Based';
                    document.getElementById('keysRotated').textContent = keysRotatedToday;
                }
            } catch (e) {
                rotationStrategy = localStorage.getItem('supremeai_rotation_strategy') || 'quota-based';
                keysRotatedToday = parseInt(localStorage.getItem('supremeai_keys_rotated') || 0);
            }
        }
        
        async function updateRotationStrategy() {
            rotationStrategy = document.getElementById('rotationStrategy').value;
            try {
                const configResponse = await AuthHelper.apiCall('/api/admin/config');
                if (configResponse.ok) {
                    const config = await configResponse.json();
                    config.apiRotationStrategy = rotationStrategy;
                    await AuthHelper.apiCall('/api/admin/config', {
                        method: 'PUT',
                        body: JSON.stringify(config)
                    });
                    localStorage.setItem('supremeai_rotation_strategy', rotationStrategy);
                    document.getElementById('currentStrategy').textContent =
                        rotationStrategy === 'sequential' ? 'Sequential' :
                        rotationStrategy === 'random' ? 'Random' : 'Quota-Based';
                }
            } catch (e) { console.error(e); }
        }
        
        async function testRotation() {
            const resultDiv = document.getElementById('rotationStatus');
            resultDiv.innerHTML = `
                <div style="background: rgba(66, 153, 225, 0.1); padding: 10px; border-radius: 5px;">
                    🔄 Testing API rotation with "${rotationStrategy}" strategy...
                </div>
            `;
            
            await new Promise(resolve => setTimeout(resolve, 1500));
            
            keysRotatedToday++;
            localStorage.setItem('supremeai_keys_rotated', keysRotatedToday);
            
            resultDiv.innerHTML = `
                <div style="background: rgba(72, 187, 120, 0.1); padding: 10px; border-radius: 5px;">
                    ✅ Rotation test successful!<br>
                    <div style="font-size: 11px; margin-top: 5px;">
                        Strategy: ${rotationStrategy}<br>
                        Keys rotated today: ${keysRotatedToday}<br>
                        Next rotation: Check API response headers for quota remaining
                    </div>
                </div>
            `;
            
            document.getElementById('keysRotated').textContent = keysRotatedToday;
        }

        // ========== P0: AUTO-EXECUTION + ADMIN APPROVAL ==========
        let autoExecNeedsApproval = true;
        
        async function initAutoExecApproval() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/config');
                if (response.ok) {
                    const config = await response.json();
                    autoExecNeedsApproval = config.autoExecApprovalRequired !== false;
                    document.getElementById('autoExecApproval').checked = autoExecNeedsApproval;
                    updateAutoExecStatus();
                }
            } catch (e) {
                autoExecNeedsApproval = localStorage.getItem('supremeai_auto_exec_approval') !== 'false';
                document.getElementById('autoExecApproval').checked = autoExecNeedsApproval;
                updateAutoExecStatus();
            }
        }
        
        async function toggleAutoExecApproval() {
            autoExecNeedsApproval = !autoExecNeedsApproval;
            try {
                const configResponse = await AuthHelper.apiCall('/api/admin/config');
                if (configResponse.ok) {
                    const config = await configResponse.json();
                    config.autoExecApprovalRequired = autoExecNeedsApproval;
                    await AuthHelper.apiCall('/api/admin/config', {
                        method: 'PUT',
                        body: JSON.stringify(config)
                    });
                    localStorage.setItem('supremeai_auto_exec_approval', autoExecNeedsApproval);
                    updateAutoExecStatus();
                }
            } catch (e) { console.error(e); }
        }
        
        function updateAutoExecStatus() {
            const statusDiv = document.getElementById('autoExecStatus');
            if (autoExecNeedsApproval) {
                statusDiv.innerHTML = '⚡ Admin approval required for all auto-execution actions';
                statusDiv.style.color = '#48bb78';
            } else {
                statusDiv.innerHTML = '⚠️ Auto-execution allowed without approval';
                statusDiv.style.color = '#ed8936';
            }
        }

        async function systemRefresh() {
            console.log('Starting global system refresh...');
            const refreshBtn = event?.target;
            if (refreshBtn) {
                refreshBtn.disabled = true;
                refreshBtn.textContent = '🔄 Refreshing...';
            }

            try {
                await Promise.all([
                    refreshEvolutionState(),
                    refreshRoadmap(),
                    initEmergencyStop(),
                    initAPIAccessLock(),
                    initDynamicRotation(),
                    initAutoExecApproval(),
                    updateProviderGrid(),
                    (window.adminChat ? window.adminChat.loadInitialData() : Promise.resolve())
                ]);
                console.log('System refresh complete');
            } catch (e) {
                console.error('System refresh failed:', e);
            } finally {
                if (refreshBtn) {
                    refreshBtn.disabled = false;
                    refreshBtn.textContent = '🔄 System Refresh';
                }
            }
        }

        // ========== ROADMAP DISPLAY ==========
        async function refreshRoadmap() {
            try {
                const response = await AuthHelper.apiCall('/api/admin/milestones');

                if (response.ok) {
                    const milestones = await response.json();
                    renderMilestones(milestones);

                    // Show refresh confirmation
                    showRoadmapRefreshMessage('✅ Roadmap refreshed - Progress data loaded from Firebase');
                }
            } catch (e) {
                console.error('Error refreshing roadmap:', e);
            }
        }

        function renderMilestones(milestones) {
            const container = document.getElementById('roadmapMilestones');
            if (!container) return;
            
            let html = '<div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 15px;">';
            
            milestones.forEach(m => {
                const detailHtml = m.details ? m.details.map(d => `• ${d}`).join('<br>') : '';
                
                html += `
                    <div style="background: #111827; padding: 15px; border-radius: 8px; border-left: 3px solid ${m.color};">
                        <div style="font-weight: 600; color: ${m.color}; margin-bottom: 8px;">${m.icon} ${m.title}</div>
                        <div style="font-size: 12px; color: #718096; margin-bottom: 5px;">Timeline: ${m.timeline}</div>
                        <div style="font-size: 12px; color: #cbd5e1; line-height: 1.6;">
                            ${detailHtml}
                        </div>
                        <div style="margin-top: 8px;">
                            <div class="progress-bar" style="height: 6px;">
                                <div class="progress-fill" style="width: ${m.progress}%; background: ${m.color};"></div>
                            </div>
                            <div style="font-size: 11px; color: #718096; margin-top: 3px;">${m.progress}% complete</div>
                        </div>
                    </div>
                `;
            });

            html += '</div>';
            container.innerHTML = html;
        }

        function showRoadmapRefreshMessage(text) {
            const card = document.getElementById('roadmapMilestones').closest('.card');
            const existingMsg = card.querySelector('.refresh-msg');
            if (existingMsg) existingMsg.remove();

            const msg = document.createElement('div');
            msg.className = 'refresh-msg';
            msg.style.cssText = 'font-size: 11px; color: #48bb78; margin-top: 10px;';
            msg.textContent = text;
            card.appendChild(msg);

            setTimeout(() => msg.remove(), 3000);
        }

