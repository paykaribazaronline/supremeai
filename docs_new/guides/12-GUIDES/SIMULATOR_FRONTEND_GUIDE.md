# Simulator API - Frontend Integration Guide

**Quick reference for frontend developers**  
**Base URL:** `/api/simulator`  
**Auth:** Firebase ID token (Authorization: Bearer {token})

---

## 📋 Endpoint Summary

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| GET | `/profile/{userId}` | Get user profile | User/Admin |
| POST | `/profile/{userId}` | Update profile | User/Admin |
| POST | `/install` | Install app to simulator | User |
| DELETE | `/install/{appId}` | Uninstall app | User |
| GET | `/installed` | List installed apps | User |
| POST | `/session/start` | Launch simulator | User |
| POST | `/session/stop` | Stop running session | User |
| GET | `/session/status` | Get current session | User |
| GET | `/devices` | List device types | User |
| POST | `/device/configure` | Change device config | User |
| GET | `/admin/usage` | All users' usage (Admin) | Admin |
| POST | `/admin/set-quota/{userId}` | Override quota (Admin) | Admin |

---

## 🔄 Data Flow: Install App

```
┌─────────┐     ┌──────────────┐     ┌──────────────┐     ┌────────────┐
│  Select  │────▶│ POST /install│────▶│   Service    │────▶│  Deploy to │
│   App    │     │  {appId}     │     │ validates &  │     │ Cloud Run  │
│ from UI  │     │              │     │  deploys     │     │  Preview   │
└─────────┘     └──────────────┘     └──────────────┘     └────────────┘
       │                                                          │
       │                                                          ▼
       │                                          URL: https://app-abc-sim.run.app
       │                                                          │
       ▼                                                          ▼
┌──────────────┐                                       ┌──────────────┐
│ Show Progress│◀──────────────────────────────────────│  Update DB   │
│  (WebSocket) │          Real-time update             │  Firestore   │
└──────────────┘                                       └──────────────┘
```

---

## 📝 API Details

### 1. Get User Profile

```http
GET /api/simulator/profile/{userId}
Authorization: Bearer {firebaseToken}
```

**Response:**

```json
{
  "userId": "uid_123",
  "installQuota": 5,
  "activeInstalls": 2,
  "installedApps": [
    {
      "appId": "app-abc",
      "appName": "MyShop",
      "version": "1.0.0",
      "previewUrl": "https://app-abc-sim.run.app",
      "installedAt": "2026-04-20T10:30:00Z",
      "launchCount": 3,
      "status": "INSTALLED"
    }
  ],
  "device": {
    "type": "PIXEL_6",
    "osVersion": "Android 14",
    "screenResolution": "1080x2340"
  },
  "lastActiveAt": "2026-04-20T11:00:00Z"
}
```

---

### 2. Install App

```http
POST /api/simulator/install
Authorization: Bearer {firebaseToken}
Content-Type: application/json

{
  "appId": "generated-app-xyz",
  "deviceProfile": "PIXEL_6"
}
```

**Success Response (201):**

```json
{
  "success": true,
  "app": {
    "appId": "app-xyz",
    "appName": "MyGeneratedApp",
    "previewUrl": "https://app-xyz-sim.run.app",
    "installedAt": "2026-04-20T10:35:00Z",
    "status": "INSTALLED"
  },
  "quota": {
    "used": 3,
    "total": 5
  }
}
```

**Error Response (409 - Quota Exceeded):**

```json
{
  "error": "QUOTA_EXCEEDED",
  "message": "Quota exceeded: 5/5 apps installed",
  "quota": { "used": 5, "total": 5 }
}
```

**Real-time Progress (WebSocket):**

```json
{
  "type": "INSTALL_PROGRESS",
  "appId": "app-xyz",
  "percent": 45,
  "stage": "Deploying to Cloud Run..."
}
```

---

### 3. List Installed Apps

```http
GET /api/simulator/installed
Authorization: Bearer {firebaseToken}
```

**Response:**

```json
{
  "installedApps": [
    {
      "appId": "app-123",
      "appName": "WeatherApp",
      "version": "1.2.0",
      "previewUrl": "https://app-123-sim.run.app",
      "installedAt": "2026-04-19T14:22:00Z",
      "launchCount": 5,
      "lastLaunchedAt": "2026-04-20T08:12:00Z",
      "status": "INSTALLED"
    }
  ],
  "quota": { "used": 1, "total": 5 }
}
```

---

### 4. Launch Simulator (Start Session)

```http
POST /api/simulator/session/start?appId={appId}
Authorization: Bearer {firebaseToken}
```

**Response:**

```json
{
  "sessionId": "sess_abc123xyz",
  "websocketUrl": "wss://simulator.run.app/ws/sess_abc123xyz",
  "state": "ACTIVE",
  "startedAt": "2026-04-20T10:40:00Z"
}
```

**Usage:**  
Open `previewUrl` from installed app in new tab, OR connect to `websocketUrl` for embedded simulator iframe.

---

### 5. Stop Session

```http
POST /api/simulator/session/stop
Authorization: Bearer {firebaseToken}
```

**Response:** `204 No Content` on success

---

### 6. Get Available Devices

```http
GET /api/simulator/devices
Authorization: Bearer {firebaseToken}
```

**Response:**

```json
[
  {
    "type": "PIXEL_6",
    "name": "Google Pixel 6",
    "osVersion": "Android 14",
    "screenResolution": "1080x2340",
    "densityDpi": 440
  },
  {
    "type": "IPHONE_15",
    "name": "iPhone 15",
    "osVersion": "iOS 17.4",
    "screenResolution": "1179x2556",
    "densityDpi": 460
  }
]
```

---

### 7. Admin: Get All Usage

```http
GET /api/simulator/admin/usage
Authorization: Bearer {adminToken}
```

**Response:**

```json
[
  {
    "userId": "user_123",
    "activeInstalls": 2,
    "installQuota": 5,
    "lastActiveAt": "2026-04-20T11:00:00Z",
    "installedAppsCount": 2
  }
]
```

---

## 🎨 UI Component Guidelines

### State Management (React)

```typescript
interface SimulatorState {
  profile: UserSimulatorProfile | null;
  installedApps: SimulatorApp[];
  quota: { used: number; total: number };
  activeSession: Session | null;
  loading: boolean;
  error: string | null;
}

const [state, setState] = useState<SimulatorState>({
  profile: null,
  installedApps: [],
  quota: { used: 0, total: 5 },
  activeSession: null,
  loading: false,
  error: null
});
```

### Loading Installed Apps

```typescript
useEffect(() => {
  fetch('/api/simulator/installed', {
    headers: { 'Authorization': `Bearer ${token}` }
  })
  .then(r => r.json())
  .then(data => {
    setState(prev => ({
      ...prev,
      installedApps: data.installedApps,
      quota: data.quota
    }));
  });
}, [token]);
```

### Install Button Flow

```typescript
const handleInstall = async (appId: string) => {
  setState(prev => ({ ...prev, loading: true }));
  
  // Connect WebSocket for progress
  const ws = new WebSocket(`wss://${location.host}/ws/simulator`);
  ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    if (msg.type === 'INSTALL_PROGRESS') {
      setProgress(msg.percent);
    }
  };
  
  try {
    const response = await fetch('/api/simulator/install', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ appId, deviceProfile: 'PIXEL_6' })
    });
    
    if (response.ok) {
      const result = await response.json();
      // Refresh installed apps list
      loadInstalledApps();
      // Show success
      message.success(`Installed! Preview: ${result.app.previewUrl}`);
    } else if (response.status === 409) {
      const err = await response.json();
      message.error(err.message); // Quota exceeded
    }
  } finally {
    setState(prev => ({ ...prev, loading: false }));
    ws.close();
  }
};
```

### Launch Button

```typescript
const handleLaunch = (app: SimulatorApp) => {
  // Option 1: Open in new tab (simplest)
  window.open(app.previewUrl, '_blank', 'width=400,height=800');
  
  // Option 2: Embedded iframe modal
  setState(prev => ({
    ...prev,
    activeSession: { appId: app.appId, url: app.previewUrl }
  }));
};
```

### Quota Badge Component

```typescript
const QuotaBadge: React.FC<{ used: number; total: number }> = ({ used, total }) => {
  const percent = (used / total) * 100;
  const color = percent >= 80 ? 'red' : percent >= 60 ? 'orange' : 'green';
  
  return (
    <Tooltip title={`${used} of ${total} simulator slots used`}>
      <Progress 
        type="circle" 
        percent={percent} 
        size={60}
        strokeColor={color}
        format={() => `${used}/${total}`}
      />
    </Tooltip>
  );
};
```

---

## 🧪 Testing with cURL

### Get Profile

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/simulator/profile/$USER_ID
```

### Install App

```bash
curl -X POST \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"appId":"app-123","deviceProfile":"PIXEL_6"}' \
  http://localhost:8080/api/simulator/install
```

### List Installed

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/simulator/installed
```

---

## 🔍 Debugging

### Check Firestore Data

```bash
# Using gcloud
gcloud firestore documents get simulator_profiles/$USER_ID

# Expected output:
# fields:
#   activeInstalls: { integerValue: "2" }
#   installQuota: { integerValue: "5" }
#   installedApps: { arrayValue: { values: [...] } }
```

### View Cloud Run Preview Services

```bash
gcloud run services list --region us-central1 \
  --filter="metadata.name:sim-*"
```

### Test WebSocket

```javascript
// In browser console
const socket = new WebSocket('wss://localhost:8080/ws/simulator');
socket.onmessage = (event) => console.log(JSON.parse(event.data));
```

---

## 📊 WebSocket Message Types

| Event | Direction | Payload |
|-------|-----------|---------|
| `INSTALL_PROGRESS` | Server → Client | `{type, appId, percent, stage}` |
| `SESSION_STARTED` | Server → Client | `{type, sessionId, websocketUrl}` |
| `SESSION_ENDED` | Server → Client | `{type, sessionId, reason}` |
| `APP_LAUNCHED` | Server → Client | `{type, appId, launchedAt}` |
| `QUOTA_WARNING` | Server → Client | `{type, used, total}` |

---

## ⚡ Performance Tips

1. **Cache profile locally** - Store in React context or Redux to avoid repeated GET calls
2. **Batch requests** - If installing multiple apps, stagger by 500ms to avoid race conditions
3. **WebSocket reconnection** - Auto-reconnect with exponential backoff
4. **Image lazy-load** - App screenshots (future) should be lazy-loaded
5. **Polling fallback** - If WebSocket fails, fall back to polling `/session/status` every 2s

---

## 🚨 Error Handling

| HTTP Code | Meaning | Recovery |
|-----------|---------|----------|
| 401 | Not authenticated | Redirect to login |
| 403 | Not authorized (not user's profile) | Show "Access denied" |
| 404 | App not found | Refresh app list |
| 409 | Quota exceeded / Already installed | Reduce installs or request quota increase |
| 502 | Deployment failed | Retry or contact support |
| 503 | Service unavailable | Show maintenance message, retry |

**Frontend error display:**

```typescript
const errorMessage = (status: number, err: any) => {
  switch (status) {
    case 409: return `Cannot install: ${err.quota.used}/${err.quota.total} apps limit reached`;
    case 502: return "Simulator launch failed. Please try again or contact support.";
    default: return "An unexpected error occurred";
  }
};
```

---

## 🎯 Quick Implementation Checklist

- [ ] Create `api/simulator` service in frontend
- [ ] Add AuthInterceptor to include Bearer token
- [ ] Build `SimulatorPanel` component showing:
  - [ ] Quota progress bar
  - [ ] Grid of installed apps with Launch/Uninstall buttons
  - [ ] "Install to Simulator" button on project cards
  - [ ] Device selector dropdown
  - [ ] Admin panel (if user.isAdmin)
- [ ] Implement WebSocket progress listener
- [ ] Show error toast on quota exceeded
- [ ] Add loading skeleton during install
- [ ] Add confirmation modal for uninstall ("This will remove the app from your simulator")
- [ ] Mobile-responsive layout (responsive grid)

---

## 📚 Related Docs

- **Full spec:** `SIMULATOR_CONTROLLER_PERFECTION_PLAN.md`
- **Dev guide:** `docs_new/guides/12-GUIDES/SIMULATOR_DEVELOPER_QUICKSTART.md`
- **API inventory:** `docs_new/guides/00-START-HERE/API_ENDPOINT_INVENTORY.md` (update when done)
- **User guide:** (to be written) `SIMULATOR_USER_GUIDE.md`

---

**Last Updated:** 2026-04-20  
**Status:** Not yet implemented (stub only)
