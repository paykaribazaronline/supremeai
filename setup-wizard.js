const http = require('http');
const fs = require('fs');
const path = require('path');

const html = `
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SupremeAI - Premium Setup Wizard</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&family=Space+Grotesk:wght@400;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-color: #030712;
            --panel-bg: rgba(17, 24, 39, 0.75);
            --border-color: rgba(255, 255, 255, 0.08);
            --text-primary: #f9fafb;
            --text-secondary: #9ca3af;
            --primary-glow: #3b82f6;
            --secondary-glow: #8b5cf6;
            --accent-color: #10b981;
        }

        * {
            box-sizing: border-box;
            margin: 0;
            padding: 0;
        }

        body {
            font-family: 'Outfit', sans-serif;
            background-color: var(--bg-color);
            background-image: 
                radial-gradient(circle at 10% 20%, rgba(59, 130, 246, 0.1) 0%, transparent 40%),
                radial-gradient(circle at 90% 80%, rgba(139, 92, 246, 0.1) 0%, transparent 40%);
            color: var(--text-primary);
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            overflow-x: hidden;
        }

        .background-grid {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-image: linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
                              linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
            background-size: 40px 40px;
            z-index: -1;
            pointer-events: none;
        }

        .container {
            background: var(--panel-bg);
            backdrop-filter: blur(16px);
            -webkit-backdrop-filter: blur(16px);
            border: 1px solid var(--border-color);
            padding: 48px;
            border-radius: 24px;
            box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.8),
                        0 0 40px rgba(59, 130, 246, 0.05);
            text-align: center;
            max-width: 800px;
            width: 90%;
            animation: fadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) forwards;
            position: relative;
        }

        .container::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 4px;
            background: linear-gradient(90deg, #3b82f6, #8b5cf6, #10b981);
            border-radius: 24px 24px 0 0;
        }

        h1 {
            font-family: 'Space Grotesk', sans-serif;
            font-size: 36px;
            font-weight: 700;
            letter-spacing: -0.03em;
            background: linear-gradient(135deg, #60a5fa 0%, #a78bfa 50%, #34d399 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            margin-bottom: 12px;
        }

        .subtitle {
            color: var(--text-secondary);
            font-size: 16px;
            line-height: 1.6;
            margin-bottom: 40px;
            max-width: 600px;
            margin-left: auto;
            margin-right: auto;
        }

        .modes-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 28px;
            margin-bottom: 30px;
        }

        @media (max-width: 640px) {
            .modes-grid {
                grid-template-columns: 1fr;
            }
        }

        .card {
            background: rgba(255, 255, 255, 0.02);
            border: 1px solid var(--border-color);
            border-radius: 16px;
            padding: 32px 24px;
            transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
            cursor: pointer;
            position: relative;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            align-items: center;
        }

        .card:hover {
            transform: translateY(-5px);
            background: rgba(255, 255, 255, 0.04);
            border-color: rgba(255, 255, 255, 0.15);
        }

        .card.prod:hover {
            box-shadow: 0 10px 30px -10px rgba(59, 130, 246, 0.3),
                        0 0 15px rgba(59, 130, 246, 0.1);
            border-color: #3b82f6;
        }

        .card.sandbox:hover {
            box-shadow: 0 10px 30px -10px rgba(139, 92, 246, 0.3),
                        0 0 15px rgba(139, 92, 246, 0.1);
            border-color: #8b5cf6;
        }

        .card-icon {
            font-size: 40px;
            margin-bottom: 20px;
            filter: drop-shadow(0 4px 8px rgba(0,0,0,0.3));
        }

        .card-title {
            font-family: 'Space Grotesk', sans-serif;
            font-size: 20px;
            font-weight: 700;
            margin-bottom: 10px;
        }

        .card-desc {
            color: var(--text-secondary);
            font-size: 14px;
            line-height: 1.5;
            margin-bottom: 24px;
            flex-grow: 1;
        }

        .dropzone {
            width: 100%;
            border: 2px dashed rgba(255, 255, 255, 0.15);
            border-radius: 12px;
            padding: 24px 16px;
            cursor: pointer;
            transition: all 0.3s;
            background: rgba(0, 0, 0, 0.2);
            text-align: center;
        }

        .dropzone:hover, .dropzone.dragover {
            border-color: #3b82f6;
            background: rgba(59, 130, 246, 0.05);
        }

        .dropzone p {
            font-size: 13px;
            color: var(--text-secondary);
        }

        .dropzone strong {
            display: block;
            font-size: 14px;
            margin-bottom: 4px;
            color: #60a5fa;
        }

        .btn {
            background: linear-gradient(135deg, #8b5cf6 0%, #6d28d9 100%);
            color: #ffffff;
            border: none;
            padding: 14px 28px;
            border-radius: 12px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
            box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
            width: 100%;
        }

        .btn:hover {
            transform: scale(1.02);
            box-shadow: 0 6px 20px rgba(139, 92, 246, 0.5);
            background: linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%);
        }

        input[type="file"] {
            display: none;
        }

        #status-container {
            margin-top: 30px;
            padding: 20px;
            border-radius: 12px;
            background: rgba(16, 185, 129, 0.1);
            border: 1px solid rgba(16, 185, 129, 0.2);
            display: none;
            animation: slideUp 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
        }

        .status-title {
            color: var(--accent-color);
            font-family: 'Space Grotesk', sans-serif;
            font-weight: 700;
            font-size: 18px;
            margin-bottom: 6px;
        }

        .status-desc {
            color: var(--text-secondary);
            font-size: 14px;
        }

        .pulse-dot {
            width: 8px;
            height: 8px;
            background-color: var(--accent-color);
            border-radius: 50%;
            display: inline-block;
            margin-right: 8px;
            box-shadow: 0 0 10px var(--accent-color);
            animation: pulse 1.5s infinite;
        }

        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }

        @keyframes slideUp {
            from { opacity: 0; transform: translateY(10px); }
            to { opacity: 1; transform: translateY(0); }
        }

        @keyframes pulse {
            0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
            70% { transform: scale(1); box-shadow: 0 0 0 8px rgba(16, 185, 129, 0); }
            100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
        }
    </style>
</head>
<body>
    <div class="background-grid"></div>
    <div class="container">
        <h1>SupremeAI Control Hub</h1>
        <p class="subtitle">Welcome to the advanced onboarding and control deck. Select your environment mode below to configure the autonomous orchestration suite.</p>
        
        <div class="modes-grid">
            <!-- Production Mode -->
            <div class="card prod">
                <div class="card-icon">⚡</div>
                <div class="card-title">Production Mode</div>
                <div class="card-desc">Deploy your app with full Cloud capabilities. Requires Google Cloud Service Account credentials to unlock Firestore, Cloud Logging and persistent state management.</div>
                
                <div class="dropzone" id="dropzone">
                    <strong>Drop service-account.json</strong>
                    <p>or click to browse local files</p>
                    <input type="file" id="fileInput" accept=".json" />
                </div>
            </div>

            <!-- Sandbox Mode -->
            <div class="card sandbox" id="sandboxCard">
                <div class="card-icon">🧪</div>
                <div class="card-title">Sandbox / Demo Mode</div>
                <div class="card-desc">Instant offline exploration of SupremeAI using a local H2 relational database, memory cache, and mock cloud providers. No cloud billing or API keys required.</div>
                
                <button class="btn" id="sandboxBtn">Launch Sandbox Mode</button>
            </div>
        </div>

        <div id="status-container">
            <div class="status-title"><span class="pulse-dot"></span><span id="status-text">Configuration Locked!</span></div>
            <div class="status-desc" id="status-desc">Initializing orchestrator... This wizard will self-terminate in a few seconds.</div>
        </div>
    </div>

    <script>
        const dropzone = document.getElementById('dropzone');
        const fileInput = document.getElementById('fileInput');
        const sandboxBtn = document.getElementById('sandboxBtn');
        const statusContainer = document.getElementById('status-container');
        const statusText = document.getElementById('status-text');
        const statusDesc = document.getElementById('status-desc');
        const modesGrid = document.querySelector('.modes-grid');

        // Prod path: file upload
        dropzone.addEventListener('click', () => fileInput.click());

        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });

        dropzone.addEventListener('dragleave', () => {
            dropzone.classList.remove('dragover');
        });

        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            if (e.dataTransfer.files.length) {
                handleFile(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length) {
                handleFile(e.target.files[0]);
            }
        });

        function handleFile(file) {
            if (!file.name.endsWith('.json')) {
                alert('Please upload a valid JSON credentials file.');
                return;
            }
            
            if (file.size > 5 * 1024 * 1024) {
                alert('File size exceeds the 5MB limit.');
                return;
            }
            
            const reader = new FileReader();
            reader.onload = (e) => {
                try {
                    const credentials = JSON.parse(e.target.result);
                    if (!credentials.project_id || !credentials.private_key || !credentials.client_email) {
                        alert('Invalid GCP credentials file. Missing project_id, private_key, or client_email.');
                        return;
                    }
                    submitConfig('/upload', e.target.result, 'Production Credentials Loaded');
                } catch (err) {
                    alert('Invalid JSON format. Please upload a valid JSON file.');
                }
            };
            reader.readAsText(file);
        }

        // Sandbox path: mock start
        sandboxBtn.addEventListener('click', () => {
            submitConfig('/sandbox', JSON.stringify({ mode: 'sandbox' }), 'Sandbox Mode Activated');
        });

        function submitConfig(endpoint, payload, successMessage) {
            fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: payload
            }).then(response => {
                if (response.ok) {
                    modesGrid.style.display = 'none';
                    statusContainer.style.display = 'block';
                    statusText.innerText = successMessage;
                    statusDesc.innerText = 'Orchestrator online. Launching local application gateway...';
                    setTimeout(() => {
                        window.location.reload();
                    }, 2500);
                } else {
                    alert('Configuration submission failed.');
                }
            }).catch(err => {
                alert('Connection error: ' + err);
            });
        }
    </script>
</body>
</html>
`;

const server = http.createServer((req, res) => {
    // CORS headers
    const headers = {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'OPTIONS, POST, GET',
        'Access-Control-Allow-Headers': 'Content-Type',
    };

    if (req.method === 'OPTIONS') {
        res.writeHead(204, headers);
        res.end();
        return;
    }

    if (req.method === 'POST' && req.url === '/upload') {
        let body = '';
        let tooLarge = false;
        req.on('data', chunk => {
            body += chunk.toString();
            if (body.length > 5 * 1024 * 1024) {
                tooLarge = true;
                res.writeHead(413, headers);
                res.end('Payload Too Large - Limit is 5MB');
                req.destroy();
            }
        });
        req.on('end', () => {
            if (tooLarge) return;
            try {
                // Verify it's valid JSON and contains required keys
                const credentials = JSON.parse(body);
                if (!credentials.project_id || !credentials.private_key || !credentials.client_email) {
                    res.writeHead(400, headers);
                    res.end('Bad Request - Missing project_id, private_key, or client_email');
                    return;
                }
                fs.writeFileSync('service-account.json', body);
                
                // Keep firestore enabled for prod mode
                res.writeHead(200, { ...headers, 'Content-Type': 'text/plain' });
                res.end('OK');
                console.log('Production service-account.json credentials received via Setup Wizard.');
                process.exit(0);
            } catch (e) {
                res.writeHead(400, headers);
                res.end('Invalid JSON');
            }
        });
    } else if (req.method === 'POST' && req.url === '/sandbox') {
        // Handle Sandbox path
        try {
            // Write a fully formed Mock service account JSON to satisfy GCP sdk parsing
            const mockServiceAccount = {
                type: "service_account",
                project_id: "supremeai-sandbox-mock",
                private_key_id: "sandbox_mock_key_id_123456",
                private_key: "-----BEGIN PRIVATE KEY-----\nMIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDJvEa1yq5Vj6lR\n-----END PRIVATE KEY-----\n",
                client_email: "sandbox@supremeai-sandbox-mock.iam.gserviceaccount.com",
                client_id: "99999999999999999999",
                auth_uri: "https://accounts.google.com/o/oauth2/auth",
                token_uri: "https://oauth2.googleapis.com/token",
                auth_provider_x509_cert_url: "https://www.googleapis.com/oauth2/v1/certs",
                client_x509_cert_url: "https://www.googleapis.com/robot/v1/metadata/x509/sandbox%40supremeai-sandbox-mock.iam.gserviceaccount.com"
            };

            fs.writeFileSync('service-account.json', JSON.stringify(mockServiceAccount, null, 2));
            
            // Also we can write/update properties to explicitly enable H2 standalone mode
            // or we can allow firestore initialization to fail gracefully as designed.
            res.writeHead(200, { ...headers, 'Content-Type': 'text/plain' });
            res.end('OK');
            console.log('Sandbox Mode requested. Generated sandbox service-account.json key.');
            process.exit(0);
        } catch (e) {
            res.writeHead(500, headers);
            res.end('Failed to initialize sandbox environment.');
        }
    } else if (req.method === 'GET' && req.url === '/') {
        res.writeHead(200, { ...headers, 'Content-Type': 'text/html' });
        res.end(html);
    } else {
        res.writeHead(404, headers);
        res.end();
    }
});

server.listen(8080, '0.0.0.0', () => {
    console.log('Premium Setup Wizard running at http://localhost:8080/');
});
