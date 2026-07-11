# 📄 ফাইল: apps/web-chat/script.ts

**প্রকার:** .ts  
**সাইজ:** 10,897 বাইট  
**আপডেট:** 2026-07-11T14:23:58.704790

---

## কোড

```ts
import DOMPurify from 'dompurify';
import { AppConfig } from './env';
import { errorBus } from './error-bus';

// --- Type Definitions ---
interface WsPayload {
    text: string;
    image_base64?: string;
}

// --- Global State ---
const abortController = new AbortController();
let ws: WebSocket | null = null;
let isGenerating = false;
let currentImageBase64: string | null = null;
let reconnectAttempts = 0;
let reconnectTimeoutId: number | undefined;

// --- Cleanup on Unload ---
window.addEventListener("unload", () => cleanup("Window unloading"), { once: true });

// --- DOM Elements ---
// Strict non-null assertions or runtime checks are mandatory
const chatHistory = document.getElementById('chatHistory') as HTMLDivElement | null;
const chatInput = document.getElementById('chatInput') as HTMLInputElement | null;
const btnSend = document.getElementById('btnSend') as HTMLButtonElement | null;
const imageUpload = document.getElementById('imageUpload') as HTMLInputElement | null;
const btnAttach = document.getElementById('btnAttach') as HTMLButtonElement | null;
const imagePreviewContainer = document.getElementById('imagePreviewContainer') as HTMLDivElement | null;
const imagePreview = document.getElementById('imagePreview') as HTMLImageElement | null;
const btnRemoveImage = document.getElementById('btnRemoveImage') as HTMLButtonElement | null;
const btnLaunchWorkspace = document.getElementById('btnLaunchWorkspace') as HTMLButtonElement | null;

// --- Initialization Check ---
// বাংলা মন্তব্য: return type যুক্ত করা হয়েছে (void) - ESLint explicit-function-return-type fix
function validateDOM(): void {
    if (!chatHistory || !chatInput || !btnSend) {
        const error = new Error("Critical DOM elements missing (chatHistory, chatInput, or btnSend)");
        errorBus.report(error, { sourceModule: "script.ts", action: "DOM Initialization" }, "critical");
        throw error; // Fail fast
    }
}
validateDOM();

// --- Event Listeners with Strict Cleanup ---
if (btnAttach && imageUpload) {
    btnAttach.addEventListener('click', () => imageUpload.click(), { signal: abortController.signal });
    
    // Strict typing for file input event
    imageUpload.addEventListener('change', (e: Event) => {
        const target = e.target as HTMLInputElement;
        const file = target.files?.[0];
        if (!file) return;

        // Path Traversal Mitigation: Only accept image types (though FileReader handles it locally)
        if (!file.type.startsWith('image/')) {
            errorBus.report(new Error(`Invalid file type attempted: ${file.type}`), { sourceModule: "script.ts", action: "File Selection" }, "warning");
            alert("Only image files are allowed.");
            target.value = ''; // Reset
            return;
        }

        const reader = new FileReader();
        reader.onload = (event: ProgressEvent<FileReader>) => {
            if (typeof event.target?.result === 'string') {
                currentImageBase64 = event.target.result;
                if (imagePreview) imagePreview.src = currentImageBase64;
                if (imagePreviewContainer) imagePreviewContainer.style.display = 'block';
            }
        };
        reader.onerror = (_error) => {
            errorBus.report(new Error("File reading failed"), { sourceModule: "script.ts", action: "File Reader" }, "error");
        };
        reader.readAsDataURL(file);
    }, { signal: abortController.signal });
}

if (btnRemoveImage) {
    btnRemoveImage.addEventListener('click', clearImageAttachment, { signal: abortController.signal });
}

if (btnSend) {
    btnSend.addEventListener('click', handleSend, { signal: abortController.signal });
}

if (chatInput) {
    chatInput.addEventListener('keypress', (e: KeyboardEvent) => {
        if (e.key === 'Enter') handleSend();
    }, { signal: abortController.signal });
}

if (btnLaunchWorkspace) {
    btnLaunchWorkspace.addEventListener('click', () => {
        document.body.setAttribute('data-auth', 'logged-in');
        // Initialize WebSocket connection when entering workspace
        connectWebSocket();
    }, { signal: abortController.signal });
}

// --- Functions ---
// বাংলা মন্তব্য: return type যুক্ত করা হয়েছে (void) - ESLint explicit-function-return-type fix
function clearImageAttachment(): void {
    currentImageBase64 = null;
    if (imageUpload) imageUpload.value = '';
    if (imagePreviewContainer) imagePreviewContainer.style.display = 'none';
    if (imagePreview) imagePreview.src = '';
}

// বাংলা মন্তব্য: return type যুক্ত করা হয়েছে (number) - ESLint explicit-function-return-type fix
function calculateBackoff(): number {
    const delay = AppConfig.ws.initialReconnectDelayMs * Math.pow(2, reconnectAttempts);
    return Math.min(delay, AppConfig.ws.maxReconnectDelayMs);
}

// বাংলা মন্তব্য: return type যুক্ত করা হয়েছে (void) - ESLint explicit-function-return-type fix
function connectWebSocket(): void {
    // If we're already connecting or open, do nothing
    if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
        return;
    }

    try {
        const token = localStorage.getItem(AppConfig.jwtStorageKey);
        // Using URL object for safe query param construction (prevents injection)
        const urlObj = new URL(AppConfig.wsUrl);
        if (token) {
            urlObj.searchParams.append("token", token);
        }
        
        ws = new WebSocket(urlObj.toString());
        
        ws.onopen = () => {
            console.info('🟢 [WS] Connected to Neural Engine');
            reconnectAttempts = 0; // Reset counter on successful connection
            addMessage('assistant', 'সিস্টেম কানেক্টেড! আমি SupremeAI এজেন্ট, কীভাবে সাহায্য করতে পারি?');
        };

        ws.onmessage = (event: MessageEvent<string>) => {
            const data = event.data;
            if (data === '[DONE]') {
                isGenerating = false;
                return;
            }

            const lastMessage = chatHistory?.lastElementChild;
            if (lastMessage && lastMessage.classList.contains('msg-assistant')) {
                // We use innerHTML but strictly sanitize it first using DOMPurify
                lastMessage.innerHTML += DOMPurify.sanitize(data);
            } else {
                addMessage('assistant', data);
            }
            if (chatHistory) chatHistory.scrollTop = chatHistory.scrollHeight;
        };

        ws.onclose = (event: CloseEvent) => {
            ws = null;
            if (abortController.signal.aborted) {
                console.info('⚪ [WS] Closed gracefully due to app unmount.');
                return;
            }
            
            if (reconnectAttempts >= AppConfig.ws.maxReconnectAttempts) {
                errorBus.report(
                    new Error(`WebSocket failed to reconnect after ${AppConfig.ws.maxReconnectAttempts} attempts.`),
                    { sourceModule: "script.ts", action: "WS Reconnection" },
                    "critical"
                );
                addMessage('assistant', '⚠️ সার্ভারের সাথে সংযোগ বিচ্ছিন্ন। দয়া করে পেজটি রিলোড করুন।');
                return;
            }

            const delay = calculateBackoff();
            console.warn(`🔴 [WS] Disconnected (Code: ${event.code}). Reconnecting in ${delay}ms... (Attempt ${reconnectAttempts + 1})`);
            
            clearTimeout(reconnectTimeoutId);
            reconnectTimeoutId = window.setTimeout(() => {
                reconnectAttempts++;
                connectWebSocket();
            }, delay);
        };

        ws.onerror = (event: Event) => {
            // Log WS errors to the event bus
            errorBus.report(
                new Error("WebSocket encountered an error."),
                { sourceModule: "script.ts", action: "WS Communication", rawEvent: event },
                "warning"
            );
        };

    } catch (error: unknown) {
        errorBus.report(error, { sourceModule: "script.ts", action: "WS Initialization" }, "error");
    }
}

// বাংলা মন্তব্য: return type যুক্ত করা হয়েছে (void) - ESLint explicit-function-return-type fix
function addMessage(role: "user" | "assistant", text: string): void {
    if (!chatHistory) return;
    const div = document.createElement('div');
    // We use predefined CSS classes for styling (removed inline styles)
    div.className = `msg msg-${role}`;
    div.innerHTML = DOMPurify.sanitize(text);
    
    chatHistory.appendChild(div);
    chatHistory.scrollTop = chatHistory.scrollHeight;
}

// বাংলা মন্তব্য: return type যুক্ত করা হয়েছে (void) - ESLint explicit-function-return-type fix
function handleSend(): void {
    if (!chatInput) return;
    const text = chatInput.value.trim();
    
    if ((!text && !currentImageBase64) || isGenerating || !ws || ws.readyState !== WebSocket.OPEN) {
        return;
    }

    const displayMessage = text ? text : '[📸 Image Attached]';
    addMessage('user', displayMessage);
    chatInput.value = '';
    isGenerating = true;

    // Strictly typed payload
    const payload: WsPayload = { text };
    if (currentImageBase64) {
        payload.image_base64 = currentImageBase64;
    }

    try {
        ws.send(JSON.stringify(payload));
        addMessage('assistant', ''); // Placeholder for streaming response
    } catch (error: unknown) {
        isGenerating = false;
        errorBus.report(error, { sourceModule: "script.ts", action: "Sending WS Message" }, "error");
        addMessage('assistant', '⚠️ মেসেজ পাঠাতে সমস্যা হয়েছে।');
    }
    
    clearImageAttachment();
}

/**
 * Encapsulated Execution Guards (Anti-Leak Rules)
 * Ensures everything is cleaned up if the module unloads
 */
// বাংলা মন্তব্য: return type যুক্ত করা হয়েছে (void) - ESLint explicit-function-return-type fix
function cleanup(reason: string): void {
    console.info(`🧹 Executing strict cleanup. Reason: ${reason}`);
    abortController.abort(); // Unbinds all DOM event listeners
    clearTimeout(reconnectTimeoutId);
    if (ws) {
        // 1000 = Normal Closure
        ws.close(1000, "Client unmounting");
        ws = null;
    }
}

// Initial setup
if (chatHistory) {
    chatHistory.style.display = 'flex';
    chatHistory.style.flexDirection = 'column';
}
connectWebSocket();
```