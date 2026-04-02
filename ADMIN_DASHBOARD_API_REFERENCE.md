# Admin Control Dashboard - REST API Reference

**Dashboard Location:** `/admin-control-dashboard.html`  
**Base URL:** `http://localhost:8080`  
**Authentication:** Token in `Authorization: Bearer {token}` header  

---

## 📡 Endpoints Used by Dashboard

### **System Control Endpoints**

#### **1. Get Current Status**

```
GET /api/admin/control
```

**Purpose:** Fetch current system state  
**Response:**

```json
{
  "status": "success",
  "data": {
    "permissionMode": "WAIT",
    "isRunning": true,
    "canCommit": false,
    "lastUpdated": 1712074200000,
    "currentAdmin": "admin@supremeai.com"
  }
}
```

**Used by Dashboard:**

- Updates system status display
- Shows current mode
- Indicates if running
- Shows last update time

---

#### **2. Change Permission Mode**

```
POST /api/admin/control/mode
Content-Type: application/json

{
  "mode": "AUTO|WAIT|FORCE_STOP",
  "description": "Why changing mode"
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Permission mode changed to AUTO",
  "mode": "AUTO",
  "isRunning": true,
  "canCommit": true,
  "changedBy": "admin@supremeai.com",
  "timestamp": 1712074200000
}
```

**Used by Dashboard:**

- When clicking the mode buttons (⚡ AUTO, ⏸️ WAIT, 🛑 STOP)
- Provides feedback on mode change
- Updates audit trail

---

#### **3. Force Stop System**

```
POST /api/admin/control/stop
Content-Type: application/json

{
  "reason": "Why stopping"
}
```

**Response:**

```json
{
  "status": "success",
  "message": "🛑 System FORCE STOPPED",
  "isRunning": false,
  "reason": "Database connection lost",
  "stoppedBy": "admin@supremeai.com",
  "timestamp": 1712074200000
}
```

**Used by Dashboard:**

- 🛑 FORCE STOP button
- Stops all operations
- Logs reason in audit trail

---

#### **4. Resume Operations**

```
POST /api/admin/control/resume
Content-Type: application/json

{
  "mode": "AUTO|WAIT"
}
```

**Response:**

```json
{
  "status": "success",
  "message": "✅ System resumed with mode: AUTO",
  "mode": "AUTO",
  "isRunning": true,
  "resumedBy": "admin@supremeai.com",
  "timestamp": 1712074200000
}
```

**Used by Dashboard:**

- ✅ RESUME OPERATIONS button
- Brings system back online
- Sets initial mode

---

### **Pending Actions Endpoints**

#### **5. Get All Pending Actions**

```
GET /api/admin/control/pending
```

**Response:**

```json
{
  "status": "success",
  "data": [
    {
      "id": "action-12345",
      "actionType": "GIT_COMMIT",
      "description": "Commit changes for bug fix",
      "status": "PENDING",
      "createdAt": 1712074200000,
      "requestedBy": "admin@supremeai.com"
    }
  ]
}
```

**Used by Dashboard:**

- Populates "Pending Actions" section
- Shows all actions awaiting approval
- Auto-refreshes every 10 seconds

---

#### **6. Approve Pending Action**

```
POST /api/admin/control/pending/{actionId}/approve
Content-Type: application/json

{
  "approved": true
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Action approved and executed",
  "actionId": "action-12345",
  "executionResult": {
    "success": true,
    "output": "Changes committed successfully"
  }
}
```

**Used by Dashboard:**

- ✅ APPROVE button on pending actions
- Executes the approved action
- Removes from pending list

---

#### **7. Reject Pending Action**

```
POST /api/admin/control/pending/{actionId}/reject
Content-Type: application/json

{
  "approved": false
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Action rejected",
  "actionId": "action-12345"
}
```

**Used by Dashboard:**

- ❌ REJECT button on pending actions
- Cancels the action
- Doesn't execute it

---

#### **8. Get Action History (Audit Trail)**

```
GET /api/admin/control/history
```

**Response:**

```json
{
  "status": "success",
  "data": [
    {
      "timestamp": 1712074200000,
      "admin": "admin@supremeai.com",
      "action": "SET_MODE",
      "description": "Changed mode to WAIT",
      "status": "SUCCESS",
      "details": {
        "previousMode": "AUTO",
        "newMode": "WAIT"
      }
    }
  ]
}
```

**Used by Dashboard:**

- Populates Audit Trail table
- Shows last 20 actions
- Auto-updates every 15 seconds

---

### **Git Operations Endpoints**

#### **9. Make Git Commit**

```
POST /api/git/commit
Content-Type: application/json

{
  "message": "Commit message",
  "author": "admin"
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Changes committed",
  "hash": "abc123def456",
  "files": ["file1.java", "file2.java"]
}
```

**Behavior:**

- In AUTO mode: Commits immediately
- In WAIT mode: Creates pending action
- In STOP mode: Returns error

**Used by Dashboard:**

- 💾 COMMIT CHANGES button
- Commits staged files
- Respects current mode

---

#### **10. Push to Remote**

```
POST /api/git/push
Content-Type: application/json

{
  "branch": "main"
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Pushed to remote",
  "branch": "main",
  "commits": 1
}
```

**Behavior:**

- In AUTO mode: Pushes immediately
- In WAIT mode: Creates pending action
- In STOP mode: Returns error

**Used by Dashboard:**

- 📤 PUSH TO REMOTE button
- Pushes commits to remote
- Respects current mode

---

## 🔄 Complete Workflow Example

### **User Action: Switch to WAIT Mode and Commit**

```
1. GET /api/admin/control
   ↓ (Get current status)
   ← Returns: mode=AUTO, isRunning=true

2. POST /api/admin/control/mode
   Body: { "mode": "WAIT", "description": "Reviewing changes" }
   ↓ (Change mode)
   ← Returns: mode=WAIT (button turns orange)

3. Commit message entered

4. POST /api/git/commit
   Body: { "message": "Bug fix", "author": "admin" }
   ↓ (Because mode=WAIT, creates pending action instead of committing)
   ← Returns: status=PENDING, actionId=action-123

5. GET /api/admin/control/pending
   ↓ (Fetch pending actions)
   ← Returns: action-123 in pending list

6. User clicks ✅ APPROVE

7. POST /api/admin/control/pending/action-123/approve
   ↓ (Approve and execute)
   ← Returns: Commit executed successfully

8. GET /api/admin/control/history
   ↓ (Refresh audit trail)
   ← Returns: commit action logged
```

---

## 🔐 Authentication

All endpoints require:

```
Authorization: Bearer {access_token}
```

Token obtained from login:

```
POST /api/auth/login
Body: { "username": "admin@supremeai.com", "password": "..." }
Response: { "accessToken": "...", "refreshToken": "..." }
```

Token expires after **24 hours**.  
Refresh token valid for **7 days**.

---

## 🎯 Error Responses

All errors follow this format:

```json
{
  "status": "error",
  "message": "Error description",
  "code": "ERROR_CODE"
}
```

**Common Errors:**

- `401 UNAUTHORIZED` - Token missing/invalid
- `403 FORBIDDEN` - Not an admin
- `400 BAD_REQUEST` - Invalid parameters
- `500 INTERNAL_SERVER_ERROR` - Server error

---

## 📊 Status Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | Success | Proceed normally |
| 400 | Bad request | Check parameters |
| 401 | Unauthorized | Login again |
| 403 | Forbidden | Not admin user |
| 500 | Server error | Check server logs |

---

## 🔄 Auto-Refresh Cycle

Dashboard automatically calls these on schedule:

```
Every 10 seconds:
  → GET /api/admin/control (status update)

Every 10 seconds:
  → GET /api/admin/control/pending (pending actions)

Every 15 seconds:
  → GET /api/admin/control/history (audit trail)
```

---

## 💾 Request Examples

### **cURL Examples**

**Get Status:**

```bash
curl -X GET http://localhost:8080/api/admin/control \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Change Mode to WAIT:**

```bash
curl -X POST http://localhost:8080/api/admin/control/mode \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "WAIT",
    "description": "Reviewing major changes"
  }'
```

**Commit Changes:**

```bash
curl -X POST http://localhost:8080/api/git/commit \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Fix: resolve database connection issue",
    "author": "admin"
  }'
```

---

## 🧪 Testing

### **Test WAIT Mode Workflow**

```bash
# 1. Change to WAIT mode
curl -X POST http://localhost:8080/api/admin/control/mode \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mode":"WAIT","description":"test"}'

# 2. Commit (should create pending action)
curl -X POST http://localhost:8080/api/git/commit \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"test","author":"admin"}'

# 3. Get pending actions
curl -X GET http://localhost:8080/api/admin/control/pending \
  -H "Authorization: Bearer TOKEN"

# 4. Approve the action
curl -X POST http://localhost:8080/api/admin/control/pending/ACTION_ID/approve \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"approved":true}'
```

---

## 📝 Notes

- All timestamps are in milliseconds (Unix timestamp * 1000)
- API rate limited to 1000 requests/minute per admin
- Responses are JSON only
- Request/response bodies are always JSON
- Empty arrays `[]` indicate no results (not an error)

---

**Dashboard uses all these endpoints automatically - no manual API calls needed unless customizing!**
