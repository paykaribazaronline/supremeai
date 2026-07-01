import DOMPurify from 'dompurify';

// WebSocket Setup
const isProd = window.location.hostname !== '127.0.0.1' && window.location.hostname !== 'localhost';
const PROTOCOL = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
const HOST = isProd ? window.location.host : '127.0.0.1:8000';
const WS_URL = `${PROTOCOL}${HOST}/ws/chat`;

let ws: WebSocket | null = null;
let isGenerating = false;
let currentImageBase64: string | null = null;

// DOM Elements
const chatHistory = document.getElementById('chatHistory') as HTMLDivElement;
const chatInput = document.getElementById('chatInput') as HTMLInputElement;
const btnSend = document.getElementById('btnSend') as HTMLButtonElement;

const imageUpload = document.getElementById('imageUpload') as HTMLInputElement;
const btnAttach = document.getElementById('btnAttach') as HTMLButtonElement;
const imagePreviewContainer = document.getElementById('imagePreviewContainer') as HTMLDivElement;
const imagePreview = document.getElementById('imagePreview') as HTMLImageElement;
const btnRemoveImage = document.getElementById('btnRemoveImage') as HTMLButtonElement;

// Attachment Logic
if (btnAttach) btnAttach.addEventListener('click', () => imageUpload?.click());

if (imageUpload) {
    imageUpload.addEventListener('change', function(e: any) {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = function(event: any) {
            currentImageBase64 = event.target.result;
            if (imagePreview) imagePreview.src = currentImageBase64 || '';
            if (imagePreviewContainer) imagePreviewContainer.style.display = 'block';
        };
        reader.readAsDataURL(file);
    });
}

if (btnRemoveImage) btnRemoveImage.addEventListener('click', clearImageAttachment);

function clearImageAttachment() {
    currentImageBase64 = null;
    if (imageUpload) imageUpload.value = '';
    if (imagePreviewContainer) imagePreviewContainer.style.display = 'none';
    if (imagePreview) imagePreview.src = '';
}

function connectWebSocket() {
    // Append JWT token as query param for WebSocket auth
    const token = localStorage.getItem('jwt_token');
    const urlWithAuth = token ? `${WS_URL}?token=${token}` : WS_URL;
    ws = new WebSocket(urlWithAuth);

    ws.onopen = () => {
        console.log('🟢 [WS] Connected to Neural Engine');
        addMessage('assistant', 'সিস্টেম কানেক্টেড! আমি SupremeAI এজেন্ট, কীভাবে সাহায্য করতে পারি?');
    };

    ws.onmessage = (event) => {
        const data = event.data;
        if (data === '[DONE]') {
            isGenerating = false;
            return;
        }

        const lastMessage = chatHistory?.lastElementChild;
        if (lastMessage && lastMessage.classList.contains('msg-assistant')) {
            lastMessage.innerHTML += DOMPurify.sanitize(data);
        } else {
            addMessage('assistant', data);
        }
        if (chatHistory) chatHistory.scrollTop = chatHistory.scrollHeight;
    };

    ws.onclose = () => {
        console.log('🔴 [WS] Disconnected. Reconnecting in 3s...');
        setTimeout(connectWebSocket, 3000);
    };
}

function addMessage(role: string, text: string) {
    if (!chatHistory) return;
    const div = document.createElement('div');
    div.className = `msg msg-${role}`;
    div.innerHTML = DOMPurify.sanitize(text);
    
    div.style.padding = '10px 14px';
    div.style.borderRadius = '12px';
    div.style.marginBottom = '8px';
    div.style.maxWidth = '80%';
    div.style.color = 'white';
    div.style.alignSelf = role === 'user' ? 'flex-end' : 'flex-start';
    div.style.background = role === 'user' ? '#3B82F6' : '#1F2937';
    
    chatHistory.appendChild(div);
    chatHistory.scrollTop = chatHistory.scrollHeight;
}

function handleSend() {
    const text = chatInput ? chatInput.value.trim() : '';
    
    if ((!text && !currentImageBase64) || isGenerating || !ws || ws.readyState !== WebSocket.OPEN) return;

    const displayMessage = text ? text : '[📸 Image Attached]';
    addMessage('user', displayMessage);
    if (chatInput) chatInput.value = '';
    isGenerating = true;

    const payload: any = { text: text };
    if (currentImageBase64) {
        payload.image_base64 = currentImageBase64;
    }

    ws.send(JSON.stringify(payload));
    addMessage('assistant', ''); 
    clearImageAttachment();
}

if (btnSend) btnSend.addEventListener('click', handleSend);
if (chatInput) {
    chatInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleSend();
    });
}

if (chatHistory) {
    chatHistory.style.display = 'flex';
    chatHistory.style.flexDirection = 'column';
}

connectWebSocket();
