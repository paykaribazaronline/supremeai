
// সকেট কানেকশন সেটআপ
const socket = io();

// গ্লোবাল ভেরিয়েবল
let selectedImage = null;

// ডকুমেন্ট লোড হলে
document.addEventListener('DOMContentLoaded', function() {
    // চ্যাট মেসেজ পাঠানোর ইভেন্ট হ্যান্ডলার
    const messageInput = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-btn');

    // এন্টার কী চাপলে মেসেজ পাঠানো
    messageInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendMessage();
        }
    });

    // পাঠান বাটনে ক্লিক করলে মেসেজ পাঠানো
    sendBtn.addEventListener('click', function() {
        sendMessage();
    });

    // ইমেজ আপলোড বাটন
    const imageUploadBtn = document.getElementById('image-upload-btn');
    const imageInput = document.getElementById('image-input');

    imageUploadBtn.addEventListener('click', function() {
        imageInput.click();
    });

    // ইমেজ সিলেক্ট করলে প্রিভিউ দেখানো
    imageInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = function(e) {
                selectedImage = e.target.result;
                document.getElementById('preview-img').src = selectedImage;
                document.getElementById('image-preview').style.display = 'block';
            };
            reader.readAsDataURL(file);
        }
    });

    // ইমেজ রিমুভ বাটন
    const removeImageBtn = document.getElementById('remove-image-btn');
    removeImageBtn.addEventListener('click', function() {
        selectedImage = null;
        document.getElementById('image-input').value = '';
        document.getElementById('image-preview').style.display = 'none';
    });

    // পেন্ডিং কনফার্মেশন বাটন
    const pendingBtn = document.getElementById('pending-btn');
    pendingBtn.addEventListener('click', function() {
        getPendingConfirmations();
    });

    // মোডাল বন্ধ করার বাটন
    const closeModal = document.getElementById('close-modal');
    closeModal.addEventListener('click', function() {
        document.getElementById('confirmation-modal').style.display = 'none';
    });

    // কনফার্ম বাটন
    const confirmBtn = document.getElementById('confirm-btn');
    confirmBtn.addEventListener('click', function() {
        confirmItem(true);
    });

    // প্রত্যাখ্যান বাটন
    const rejectBtn = document.getElementById('reject-btn');
    rejectBtn.addEventListener('click', function() {
        confirmItem(false);
    });

    // ট্যাব পরিবর্তন ইভেন্ট
    const tabButtons = document.querySelectorAll('[data-bs-toggle="tab"]');
    tabButtons.forEach(button => {
        button.addEventListener('shown.bs.tab', function(e) {
            const targetId = e.target.getAttribute('data-bs-target');

            if (targetId === '#rules') {
                loadRules();
            } else if (targetId === '#plans') {
                loadPlans();
            } else if (targetId === '#commands') {
                loadCommands();
            }
        });
    });

    // অ্যাকটিভ রুলস ফিল্টার
    const activeRulesOnly = document.getElementById('active-rules-only');
    activeRulesOnly.addEventListener('change', function() {
        loadRules();
    });

    // অ্যাকটিভ প্ল্যান ফিল্টার
    const activePlansOnly = document.getElementById('active-plans-only');
    activePlansOnly.addEventListener('change', function() {
        loadPlans();
    });

    // অ্যাকটিভ কমান্ড ফিল্টার
    const activeCommandsOnly = document.getElementById('active-commands-only');
    activeCommandsOnly.addEventListener('change', function() {
        loadCommands();
    });

    // প্ল্যান অ্যানালিসিস বাটন
    const analyzePlanBtn = document.getElementById('analyze-plan-btn');
    analyzePlanBtn.addEventListener('click', function() {
        analyzePlan();
    });

    // প্রাথমিক ডাটা লোড
    updatePendingCount();

    // সকেট ইভেন্ট হ্যান্ডলার
    setupSocketEvents();
});

// মেসেজ পাঠানোর ফাংশন
function sendMessage() {
    const messageInput = document.getElementById('message-input');
    const message = messageInput.value.trim();

    if (!message && !selectedImage) return;

    // ব্যবহারকারীর মেসেজ দেখানো
    if (message) {
        addMessageToChat(message, 'user');
    }

    // ইমেজ থাকলে ইমেজ আপলোড করা
    if (selectedImage) {
        uploadImageAndSend(selectedImage);
        selectedImage = null;
        document.getElementById('image-input').value = '';
        document.getElementById('image-preview').style.display = 'none';
    } else {
        // মেসেজ সার্ভারে পাঠানো
        socket.emit('chat_message', {
            message: message,
            is_admin: false
        });
    }

    // ইনপুট ফিল্ড ক্লিয়ার করা
    messageInput.value = '';
}

// ইমেজ আপলোড করে মেসেজ পাঠানোর ফাংশন
function uploadImageAndSend(base64Image) {
    fetch('/api/image/upload-base64', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            image: base64Image
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            // ইমেজ সফলভাবে আপলোড হয়েছে
            addImageToChat(data.urls.medium, 'user');

            // সার্ভারে নোটিফিকেশন পাঠানো
            socket.emit('chat_message', {
                message: 'একটি ইমেজ পাঠানো হয়েছে',
                is_admin: false,
                has_image: true,
                image_url: data.urls.medium
            });
        } else {
            // এরর মেসেজ দেখানো
            addMessageToChat(data.message || 'ইমেজ আপলোডে সমস্যা হয়েছে', 'system', true);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        addMessageToChat('ইমেজ আপলোডে সমস্যা হয়েছে', 'system', true);
    });
}

// চ্যাটে ইমেজ যোগ করার ফাংশন
function addImageToChat(imageUrl, sender) {
    const chatMessages = document.getElementById('chat-messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;

    const imageElement = document.createElement('img');
    imageElement.src = imageUrl;
    imageElement.className = 'img-fluid rounded';
    imageElement.style.maxWidth = '100%';
    imageElement.style.maxHeight = '300px';

    const messageTime = document.createElement('div');
    messageTime.className = 'message-time';
    messageTime.textContent = 'এখন';

    messageDiv.appendChild(imageElement);
    messageDiv.appendChild(messageTime);
    chatMessages.appendChild(messageDiv);

    // স্ক্রল নিচে নিয়ে যাওয়া
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// চ্যাটে মেসেজ যোগ করার ফাংশন
function addMessageToChat(message, sender, isSystem = false) {
    const chatMessages = document.getElementById('chat-messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${sender}-message`;

    const messageContent = document.createElement('div');
    messageContent.className = 'message-content';
    messageContent.textContent = message;

    const messageTime = document.createElement('div');
    messageTime.className = 'message-time';
    messageTime.textContent = isSystem ? 'আজ' : 'এখন';

    messageDiv.appendChild(messageContent);
    messageDiv.appendChild(messageTime);
    chatMessages.appendChild(messageDiv);

    // স্ক্রল নিচে নিয়ে যাওয়া
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// সকেট ইভেন্ট সেটআপ করা
function setupSocketEvents() {
    // চ্যাট রেসপন্স হ্যান্ডলার
    socket.on('chat_response', function(data) {
        // সিস্টেম মেসেজ দেখানো
        addMessageToChat(data.reason, 'system', true);

        // যদি কনফার্মেশন প্রয়োজন হয়
        if (data.needs_confirmation) {
            showConfirmationModal(data);
            updatePendingCount();
        }
    });

    // কনফার্মেশন রেসপন্স হ্যান্ডলার
    socket.on('confirmation_response', function(data) {
        if (data.success) {
            // মেসেজ দেখানো
            addMessageToChat(data.message, 'system', true);

            // পেন্ডিং কাউন্ট আপডেট করা
            updatePendingCount();
        } else {
            // এরর মেসেজ দেখানো
            addMessageToChat(data.message, 'system', true);
        }
    });

    // পেন্ডিং আইটেম হ্যান্ডলার
    socket.on('pending_items', function(data) {
        if (data.items && data.items.length > 0) {
            showPendingItems(data.items);
        } else {
            alert('কোন পেন্ডিং কনফার্মেশন নেই');
        }
    });

    // এরর হ্যান্ডলার
    socket.on('error', function(data) {
        addMessageToChat(data.message, 'system', true);
    });
}

// কনফার্মেশন মোডাল দেখানোর ফাংশন
function showConfirmationModal(data) {
    const modal = document.getElementById('confirmation-modal');
    const itemTypeBadge = document.getElementById('item-type-badge');
    const itemContent = document.getElementById('item-content');
    const confidenceValue = document.getElementById('confidence-value');
    const confidenceFill = document.getElementById('confidence-fill');
    const confirmBtn = document.getElementById('confirm-btn');
    const rejectBtn = document.getElementById('reject-btn');

    // আইটেম টাইপ ব্যাজ সেট করা
    itemTypeBadge.textContent = data.item_type;
    itemTypeBadge.className = `type-badge type-${data.item_type}`;

    // আইটেম কন্টেন্ট সেট করা
    itemContent.textContent = data.content;

    // কনফিডেন্স স্কোর সেট করা
    const confidencePercent = Math.round(data.confidence * 100);
    confidenceValue.textContent = `${confidencePercent}%`;
    confidenceFill.style.width = `${confidencePercent}%`;

    // কনফার্ম বাটনে আইটেম আইডি সেট করা
    confirmBtn.dataset.itemId = data.item_id;
    rejectBtn.dataset.itemId = data.item_id;

    // মোডাল দেখানো
    modal.style.display = 'flex';
}

// আইটেম কনফার্ম বা প্রত্যাখ্যান করার ফাংশন
function confirmItem(confirmed) {
    const confirmBtn = document.getElementById('confirm-btn');
    const rejectBtn = document.getElementById('reject-btn');
    const itemId = confirmed ? confirmBtn.dataset.itemId : rejectBtn.dataset.itemId;

    if (!itemId) return;

    // আইটেম কনফার্ম করার জন্য সার্ভারে পাঠানো
    socket.emit('confirm_item', {
        item_id: itemId,
        confirmed: confirmed
    });

    // মোডাল বন্ধ করা
    document.getElementById('confirmation-modal').style.display = 'none';
}

// পেন্ডিং কনফার্মেশনের তালিকা পাওয়ার ফাংশন
function getPendingConfirmations() {
    socket.emit('get_pending');
}

// পেন্ডিং আইটেম দেখানোর ফাংশন
function showPendingItems(items) {
    const pendingItemsDiv = document.getElementById('pending-items');
    pendingItemsDiv.innerHTML = '';

    if (items.length === 0) {
        pendingItemsDiv.innerHTML = '<div class="text-center py-3">কোন পেন্ডিং কনফার্মেশন নেই</div>';
        return;
    }

    items.forEach(item => {
        const itemDiv = document.createElement('div');
        itemDiv.className = 'pending-item';

        const itemHeader = document.createElement('div');
        itemHeader.className = 'pending-item-header';

        const itemType = document.createElement('span');
        itemType.className = `type-badge type-${item.item_type}`;
        itemType.textContent = item.item_type;

        const itemTime = document.createElement('span');
        itemTime.className = 'text-muted small';
        itemTime.textContent = 'পেন্ডিং';

        itemHeader.appendChild(itemType);
        itemHeader.appendChild(itemTime);

        const itemContent = document.createElement('div');
        itemContent.className = 'item-content';
        itemContent.textContent = item.content;

        const itemActions = document.createElement('div');
        itemActions.className = 'item-actions mt-2';

        const confirmBtn = document.createElement('button');
        confirmBtn.className = 'btn btn-sm btn-success me-2';
        confirmBtn.innerHTML = '<i class="fas fa-check"></i> কনফার্ম';
        confirmBtn.onclick = function() {
            confirmItemById(item.item_id, true);
        };

        const rejectBtn = document.createElement('button');
        rejectBtn.className = 'btn btn-sm btn-danger';
        rejectBtn.innerHTML = '<i class="fas fa-times"></i> প্রত্যাখ্যান';
        rejectBtn.onclick = function() {
            confirmItemById(item.item_id, false);
        };

        itemActions.appendChild(confirmBtn);
        itemActions.appendChild(rejectBtn);

        itemDiv.appendChild(itemHeader);
        itemDiv.appendChild(itemContent);
        itemDiv.appendChild(itemActions);

        pendingItemsDiv.appendChild(itemDiv);
    });
}

// আইটেম আইডি দিয়ে কনফার্ম করার ফাংশন
function confirmItemById(itemId, confirmed) {
    socket.emit('confirm_item', {
        item_id: itemId,
        confirmed: confirmed
    });

    // পেন্ডিং কাউন্ট আপডেট করা
    updatePendingCount();

    // পেন্ডিং আইটেম লোড করা
    getPendingConfirmations();
}

// পেন্ডিং কাউন্ট আপডেট করার ফাংশন
function updatePendingCount() {
    fetch('/api/pending')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const pendingCount = document.getElementById('pending-count');
                pendingCount.textContent = data.items.length;

                if (data.items.length > 0) {
                    pendingCount.classList.remove('d-none');
                } else {
                    pendingCount.classList.add('d-none');
                }
            }
        })
        .catch(error => {
            console.error('Error fetching pending items:', error);
        });
}

// রুলস লোড করার ফাংশন
function loadRules() {
    const activeOnly = document.getElementById('active-rules-only').checked;
    const rulesList = document.getElementById('rules-list');

    fetch(`/api/rules?active_only=${activeOnly}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayRules(data.rules);
            }
        })
        .catch(error => {
            console.error('Error loading rules:', error);
            rulesList.innerHTML = '<div class="alert alert-danger">রুলস লোড করতে সমস্যা হয়েছে</div>';
        });
}

// রুলস ডিসপ্লে করার ফাংশন
function displayRules(rules) {
    const rulesList = document.getElementById('rules-list');
    rulesList.innerHTML = '';

    if (rules.length === 0) {
        rulesList.innerHTML = '<div class="text-center py-3">কোন রুল নেই</div>';
        return;
    }

    rules.forEach(rule => {
        const ruleDiv = document.createElement('div');
        ruleDiv.className = 'card mb-2';

        const ruleBody = document.createElement('div');
        ruleBody.className = 'card-body';

        const ruleContent = document.createElement('p');
        ruleContent.className = 'mb-0';
        ruleContent.textContent = rule.content;

        const ruleMeta = document.createElement('div');
        ruleMeta.className = 'text-muted small mt-2';
        ruleMeta.textContent = `তৈরি: ${new Date(rule.created_at).toLocaleDateString('bn-BD')}`;

        ruleBody.appendChild(ruleContent);
        ruleBody.appendChild(ruleMeta);
        ruleDiv.appendChild(ruleBody);

        rulesList.appendChild(ruleDiv);
    });
}

// প্ল্যান লোড করার ফাংশন
function loadPlans() {
    const activeOnly = document.getElementById('active-plans-only').checked;
    const plansList = document.getElementById('plans-list');

    fetch(`/api/plans?active_only=${activeOnly}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayPlans(data.plans);
            }
        })
        .catch(error => {
            console.error('Error loading plans:', error);
            plansList.innerHTML = '<div class="alert alert-danger">প্ল্যান লোড করতে সমস্যা হয়েছে</div>';
        });
}

// প্ল্যান ডিসপ্লে করার ফাংশন
function displayPlans(plans) {
    const plansList = document.getElementById('plans-list');
    plansList.innerHTML = '';

    if (plans.length === 0) {
        plansList.innerHTML = '<div class="text-center py-3">কোন প্ল্যান নেই</div>';
        return;
    }

    plans.forEach(plan => {
        const planDiv = document.createElement('div');
        planDiv.className = 'card mb-2';

        const planBody = document.createElement('div');
        planBody.className = 'card-body';

        const planContent = document.createElement('p');
        planContent.className = 'mb-0';
        planContent.textContent = plan.content;

        const planMeta = document.createElement('div');
        planMeta.className = 'text-muted small mt-2';
        planMeta.textContent = `তৈরি: ${new Date(plan.created_at).toLocaleDateString('bn-BD')}`;

        planBody.appendChild(planContent);
        planBody.appendChild(planMeta);
        planDiv.appendChild(planBody);

        plansList.appendChild(planDiv);
    });
}

// কমান্ড লোড করার ফাংশন
function loadCommands() {
    const activeOnly = document.getElementById('active-commands-only').checked;
    const commandsList = document.getElementById('commands-list');

    fetch(`/api/commands?active_only=${activeOnly}`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayCommands(data.commands);
            }
        })
        .catch(error => {
            console.error('Error loading commands:', error);
            commandsList.innerHTML = '<div class="alert alert-danger">কমান্ড লোড করতে সমস্যা হয়েছে</div>';
        });
}

// কমান্ড ডিসপ্লে করার ফাংশন
function displayCommands(commands) {
    const commandsList = document.getElementById('commands-list');
    commandsList.innerHTML = '';

    if (commands.length === 0) {
        commandsList.innerHTML = '<div class="text-center py-3">কোন কমান্ড নেই</div>';
        return;
    }

    commands.forEach(command => {
        const commandDiv = document.createElement('div');
        commandDiv.className = 'card mb-2';

        const commandBody = document.createElement('div');
        commandBody.className = 'card-body';

        const commandContent = document.createElement('p');
        commandContent.className = 'mb-0';
        commandContent.textContent = command.content;

        const commandMeta = document.createElement('div');
        commandMeta.className = 'text-muted small mt-2';
        commandMeta.textContent = `তৈরি: ${new Date(command.created_at).toLocaleDateString('bn-BD')}`;

        commandBody.appendChild(commandContent);
        commandBody.appendChild(commandMeta);
        commandDiv.appendChild(commandBody);

        commandsList.appendChild(commandDiv);
    });
}

// প্ল্যান অ্যানালিসিস করার ফাংশন
function analyzePlan() {
    const planInput = document.getElementById('plan-input');
    const plan = planInput.value.trim();

    if (!plan) {
        alert('অনুগ্রহ করে একটি প্ল্যান লিখুন');
        return;
    }

    // লোডিং দেখানো
    const analyzeBtn = document.getElementById('analyze-plan-btn');
    const originalBtnText = analyzeBtn.innerHTML;
    analyzeBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> বিশ্লেষণ হচ্ছে...';
    analyzeBtn.disabled = true;

    // প্ল্যান অ্যানালিসিস API কল করা
    fetch('/api/plan/analyze', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            plan: plan
        })
    })
    .then(response => response.json())
    .then(data => {
        // বাটন রিসেট করা
        analyzeBtn.innerHTML = originalBtnText;
        analyzeBtn.disabled = false;

        if (data.success) {
            // অ্যানালিসিস রেজাল্ট দেখানো
            displayAnalysisResults(data.compatibility_report, data.future_state);
        } else {
            alert('প্ল্যান বিশ্লেষণে সমস্যা হয়েছে: ' + data.message);
        }
    })
    .catch(error => {
        // বাটন রিসেট করা
        analyzeBtn.innerHTML = originalBtnText;
        analyzeBtn.disabled = false;

        console.error('Error:', error);
        alert('প্ল্যান বিশ্লেষণে সমস্যা হয়েছে');
    });
}

// অ্যানালিসিস রেজাল্ট দেখানোর ফাংশন
function displayAnalysisResults(compatibilityReport, futureState) {
    // রেজাল্ট সেকশন দেখানো
    document.getElementById('analysis-results').style.display = 'block';

    // সামঞ্জস্যতা স্কোর দেখানো
    const scorePercent = Math.round(compatibilityReport.score * 100);
    document.getElementById('compatibility-score').textContent = `${scorePercent}%`;
    document.getElementById('compatibility-bar').style.width = `${scorePercent}%`;

    // সামঞ্জস্যতা বার কালার সেট করা
    const compatibilityBar = document.getElementById('compatibility-bar');
    if (scorePercent >= 70) {
        compatibilityBar.className = 'progress-bar bg-success';
    } else if (scorePercent >= 40) {
        compatibilityBar.className = 'progress-bar bg-warning';
    } else {
        compatibilityBar.className = 'progress-bar bg-danger';
    }

    // সামঞ্জস্যতা মেসেজ দেখানো
    const compatibilityMessage = document.getElementById('compatibility-message');
    compatibilityMessage.textContent = compatibilityReport.message;

    if (compatibilityReport.compatible) {
        compatibilityMessage.className = 'alert alert-success';
    } else {
        compatibilityMessage.className = 'alert alert-danger';
    }

    // বিরোধ দেখানো
    const conflictsSection = document.getElementById('conflicts-section');
    const conflictsList = document.getElementById('conflicts-list');
    conflictsList.innerHTML = '';

    if (compatibilityReport.conflicts && compatibilityReport.conflicts.length > 0) {
        conflictsSection.style.display = 'block';

        compatibilityReport.conflicts.forEach(conflict => {
            const conflictItem = document.createElement('div');
            conflictItem.className = 'list-group-item';

            const conflictType = document.createElement('strong');
            conflictType.textContent = conflict.conflict_type + ': ';

            const conflictDesc = document.createElement('span');
            conflictDesc.textContent = conflict.conflict_description;

            conflictItem.appendChild(conflictType);
            conflictItem.appendChild(conflictDesc);

            conflictsList.appendChild(conflictItem);
        });
    } else {
        conflictsSection.style.display = 'none';
    }

    // সুপারিশ দেখানো
    const recommendationsList = document.getElementById('recommendations-list');
    recommendationsList.innerHTML = '';

    if (compatibilityReport.recommendations && compatibilityReport.recommendations.length > 0) {
        compatibilityReport.recommendations.forEach(recommendation => {
            const recommendationItem = document.createElement('li');
            recommendationItem.className = 'list-group-item';
            recommendationItem.textContent = recommendation;

            recommendationsList.appendChild(recommendationItem);
        });
    }

    // ভবিষ্যত পূর্বাভাস দেখানো
    document.getElementById('timeline-prediction').textContent = futureState.timeline || 'নির্দিষ্ট সময়সূচী নেই';

    // অ্যাকশন আইটেম দেখানো
    const actionItemsList = document.getElementById('action-items-list');
    actionItemsList.innerHTML = '';

    if (futureState.action_items && futureState.action_items.length > 0) {
        futureState.action_items.forEach(action => {
            const actionItem = document.createElement('li');
            actionItem.className = 'list-group-item';
            actionItem.textContent = action;

            actionItemsList.appendChild(actionItem);
        });
    } else {
        const noActionItem = document.createElement('li');
        noActionItem.className = 'list-group-item text-muted';
        noActionItem.textContent = 'কোন নির্দিষ্ট কাজ চিহ্নিত করা হয়নি';
        actionItemsList.appendChild(noActionItem);
    }

    // সম্ভাব্য ফলাফল দেখানো
    const outcomesList = document.getElementById('outcomes-list');
    outcomesList.innerHTML = '';

    if (futureState.predicted_outcomes && futureState.predicted_outcomes.length > 0) {
        futureState.predicted_outcomes.forEach(outcome => {
            const outcomeItem = document.createElement('li');
            outcomeItem.className = 'list-group-item';
            outcomeItem.textContent = outcome;

            outcomesList.appendChild(outcomeItem);
        });
    } else {
        const noOutcomeItem = document.createElement('li');
        noOutcomeItem.className = 'list-group-item text-muted';
        noOutcomeItem.textContent = 'কোন নির্দিষ্ট ফলাফল প্রেডিক্ট করা যায়নি';
        outcomesList.appendChild(noOutcomeItem);
    }

    // রিস্ক অ্যাসেসমেন্ট দেখানো
    const riskAssessment = document.getElementById('risk-assessment');
    riskAssessment.textContent = futureState.risk_assessment || 'কোন উল্লেখযোগ্য রিস্ক চিহ্নিত করা হয়নি';

    // রিস্ক লেভেল অনুযায়ী কালার সেট করা
    if (futureState.risk_level === 'high') {
        riskAssessment.className = 'alert alert-danger';
    } else if (futureState.risk_level === 'medium') {
        riskAssessment.className = 'alert alert-warning';
    } else {
        riskAssessment.className = 'alert alert-success';
    }

    // বাস্তবায়ন সুপারিশ দেখানো
    const implementationSuggestionsList = document.getElementById('implementation-suggestions-list');
    implementationSuggestionsList.innerHTML = '';

    if (futureState.implementation_suggestions && futureState.implementation_suggestions.length > 0) {
        futureState.implementation_suggestions.forEach(suggestion => {
            const suggestionItem = document.createElement('li');
            suggestionItem.className = 'list-group-item';
            suggestionItem.textContent = suggestion;

            implementationSuggestionsList.appendChild(suggestionItem);
        });
    } else {
        const noSuggestionItem = document.createElement('li');
        noSuggestionItem.className = 'list-group-item text-muted';
        noSuggestionItem.textContent = 'কোন নির্দিষ্ট বাস্তবায়ন সুপারিশ নেই';
        implementationSuggestionsList.appendChild(noSuggestionItem);
    }
}
