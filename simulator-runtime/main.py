"""
Simulator Runtime Service
Serves generated apps with device-specific emulation on Cloud Run.
"""

import os
import logging
from typing import Optional

import firebase_admin
from firebase_admin import credentials, firestore
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize Firebase
if os.path.exists('/etc/secrets/firebase-service-account.json'):
    cred = credentials.Certificate('/etc/secrets/firebase-service-account.json')
    firebase_admin.initialize_app(cred)
else:
    try:
        firebase_admin.initialize_app()
    except Exception as e:
        logger.warning(f"Firebase initialization failed: {e}")

db = firestore.client() if firebase_admin._apps else None

app = FastAPI(
    title="Simulator Runtime",
    description="Serves generated apps with device emulation",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Device profiles (viewport dimensions in CSS pixels, DPR)
DEVICE_PROFILES = {
    "PIXEL_6": {"width": 412, "height": 915, "dpr": 2.6, "userAgent": "Mozilla/5.0 (Linux; Android 13; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"},
    "IPHONE_14": {"width": 390, "height": 844, "dpr": 3.0, "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"},
    "SAMSUNG_S22": {"width": 360, "height": 800, "dpr": 3.5, "userAgent": "Mozilla/5.0 (Linux; Android 13; SM-S906B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"},
    "IPAD_PRO": {"width": 1024, "height": 1366, "dpr": 2.0, "userAgent": "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"},
    "DESKTOP_1920": {"width": 1920, "height": 1080, "dpr": 1.0, "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"},
    "DESKTOP_1366": {"width": 1366, "height": 768, "dpr": 1.0, "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"},
}

def get_device_profile(device_type: str) -> dict:
    profile = DEVICE_PROFILES.get(device_type.upper())
    if not profile:
        logger.warning(f"Unknown device type: {device_type}, falling back to PIXEL_6")
        profile = DEVICE_PROFILES["PIXEL_6"]
    return profile

def inject_device_emulation(html: str, device_profile: dict) -> str:
    """
    Inject viewport meta, device-specific JS, and user-agent hints.
    This is a simple server-side injection; for full emulation, consider headless browser.
    """
    # Ensure viewport meta tag exists
    viewport_meta = f'<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">'
    
    if '<meta name="viewport"' not in html and '<meta content="viewport"' not in html:
        # Insert into <head>
        head_close = html.find('</head>')
        if head_close != -1:
            html = html[:head_close] + viewport_meta + html[head_close:]
        else:
            # Prepend to document
            html = viewport_meta + html
    
    # Inject a small script to help with touch events and screen dimensions
    injection_script = f"""
    <script>
      // Device emulation hints for client-side
      (function() {{
        const DEVICE = {{
          width: {device_profile["width"]},
          height: {device_profile["height"]},
          dpr: {device_profile["dpr"]},
          userAgent: "{device_profile["userAgent"]}"
        }};
        // Expose to console for debugging
        console.log("[Simulator] Running in device mode:", DEVICE);
        // Override navigator.userAgent (note: read-only in some browsers)
        // But we can set a custom property
        window.__SIMULATOR_DEVICE__ = DEVICE;
        // Adjust CSS for device pixel ratio
        document.documentElement.style.setProperty('--device-dpr', DEVICE.dpr);
      }})();
    </script>
    """
    
    # Insert before </body>
    body_close = html.rfind('</body>')
    if body_close != -1:
        html = html[:body_close] + injection_script + html[body_close:]
    else:
        html = html + injection_script
    
    return html

@app.get("/health")
async def health():
    status = {
        "status": "UP",
        "service": "simulator-runtime",
        "firestore": "connected" if db else "disconnected"
    }
    return status

@app.get("/")
async def serve_simulator():
    app_id = os.getenv("APP_ID")
    device_type = os.getenv("DEVICE_TYPE", "PIXEL_6")
    simulator_mode = os.getenv("SIMULATOR_MODE", "preview")
    
    if not app_id:
        raise HTTPException(status_code=500, detail="APP_ID environment variable not set")
    
    logger.info(f"Serving simulator for app={app_id} device={device_type} mode={simulator_mode}")
    
    if not db:
        # Fallback placeholder if Firestore unavailable
        placeholder = f"""
        <!DOCTYPE html><html><head><title>Simulator - {app_id}</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
        <body style="font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f0f0f0;">
        <div style="text-align: center; padding: 20px; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
            <h2>Simulator: {device_type}</h2>
            <p>Generated App ID: {app_id}</p>
            <p>Mode: {simulator_mode}</p>
            <p><em>(Firestore unavailable - showing placeholder)</em></p>
        </div></body></html>
        """
        return HTMLResponse(content=placeholder)
    
    # Fetch generated app from Firestore
    try:
        doc_ref = db.collection('generated_apps').document(app_id)
        doc = doc_ref.get()
        if not doc.exists:
            logger.error(f"Generated app not found: {app_id}")
            raise HTTPException(status_code=404, detail=f"App {app_id} not found")
        
        data = doc.to_dict()
        html = data.get('htmlContent', '')
        
        if not html:
            logger.warning(f"No htmlContent for app {app_id}, using placeholder")
            html = f"<html><body><h2>App {app_id} has no content yet.</h2></body></html>"
        
        # Apply device emulation
        device_profile = get_device_profile(device_type)
        transformed_html = inject_device_emulation(html, device_profile)
        
        # Update app status if needed (optional)
        doc_ref.update({"status": "DEPLOYED", "updatedAt": firestore.SERVER_TIMESTAMP})
        
        return HTMLResponse(content=transformed_html)
        
    except Exception as e:
        logger.error(f"Error serving simulator: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Failed to load app: {e}")

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8080))
    uvicorn.run(app, host="0.0.0.0", port=port)
