# SupremeAI Admin Authentication - Quick Setup

## 🎯 What You Just Got

A complete **admin-only login system** that:

- ✅ Protects both localhost (8001) and web host
- ✅ Uses JWT tokens (secure, industry standard)
- ✅ Auto-refreshes tokens (stays logged in for 7 days)
- ✅ Works offline after login
- ✅ Stores user data in Firebase
- ✅ Professional login page included

---

## ⚡ 3-Step Startup

### Step 1: Build the Project

```bash
cd c:\Users\Nazifa\supremeai
.\gradlew build
```

### Step 2: Run the Application

```bash
.\gradlew run
```

Server starts:

- Admin Dashboard: http://localhost:8001
- Monitoring: http://localhost:8000
- API Backend: http://localhost:8080

### Step 3: Create First Admin

Open a terminal/PowerShell and run:

```PowerShell
$body = @{
    username = "admin"
    email = "admin@supremeai.com"
    password = "supremeai123"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/auth/register" `
    -Method Post `
    -Headers @{"Content-Type"="application/json"} `
    -Body $body
```

**Or use cURL:**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"email\":\"admin@supremeai.com\",\"password\":\"supremeai123\"}"
```

---

## 🚀 Login & Access Admin Dashboard

1. Open: **http://localhost:8001/login.html**
2. Enter:
   - Username: `admin`
   - Password: `supremeai123`
3. Click **Sign In**
4. Redirected to admin dashboard automatically

---

## 📁 Key Files Created

| File | Purpose |
|------|---------|
| `login.html` | Login page (responsive, professional) |
| `AuthenticationService.java` | Handles JWT & passwords |
| `AuthenticationController.java` | REST endpoints for auth |
| `auth-helper.js` | Browser token management |
| `AUTHENTICATION_GUIDE.md` | Complete documentation |

---

## 🔐 How It Works

```
User enters credentials
        ↓
Server validates password (BCrypt)
        ↓
Server creates JWT token (24h lifetime)
        ↓
Browser stores token in localStorage
        ↓
All API calls include: Authorization: Bearer <TOKEN>
        ↓
Token auto-refreshes when needed (7d refresh token)
        ↓
User stays logged in for 7 days even after browser close
```

---

## 📊 Architecture

```
┌─────────────────────────────────────────┐
│ Browser (localhost:8001 / web host)    │
│ ├── login.html (public)                 │
│ ├── admin/index.html (protected)        │
│ ├── dashboard/index.html (protected)    │
│ └── auth-helper.js (token management)   │
└──────────────┬──────────────────────────┘
               │ HTTP/HTTPS
┌──────────────▼──────────────────────────┐
│ Spring Boot Backend (8080)               │
│ ├── /api/auth/login                      │
│ ├── /api/auth/register                   │
│ ├── /api/auth/refresh                    │
│ ├── /api/auth/me                         │
│ ├── /api/auth/users (list all admins)   │
│ └── [ALL OTHER ENDPOINTS] (need JWT)     │
└──────────────┬──────────────────────────┘
               │ Firebase REST
┌──────────────▼──────────────────────────┐
│ Firebase Realtime Database               │
│ └── users/ (stores admin accounts)       │
└──────────────────────────────────────────┘
```

---

## 🎮 Testing the System

### Test 1: Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"supremeai123"}'
```

Response (save the `token`):

```json
{
  "status": "success",
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "type": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": "admin",
    "username": "admin",
    "email": "admin@supremeai.com"
  }
}
```

### Test 2: Use Token in Protected Endpoint

```bash
curl -X GET http://localhost:8080/api/auth/me \
  -H "Authorization: Bearer <PASTE_TOKEN_HERE>"
```

### Test 3: Create Another Admin

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -d '{
    "username": "admin2",
    "email": "admin2@supremeai.com",
    "password": "secure123"
  }'
```

---

## 🌍 Localhost vs Web Host

### Localhost (Development)

```
Login Page:    http://localhost:8001/login.html
Admin Panel:   http://localhost:8001/admin/
Monitoring:    http://localhost:8000/
API:           http://localhost:8080/api/...
```

### Web Host (Production)

```
Login Page:    https://your-domain.com/login.html
Admin Panel:   https://your-domain.com/admin/
API:           https://your-domain.com/api/...
```

**Same code, same authentication. Zero configuration needed!**

---

## 🔧 Configuration

### Optional: Change Demo Password

Edit and rebuild:

```bash
./gradlew run  # Uses default "supremeai123"
```

### Optional: Change JWT Secret

Set environment variable before running:

```bash
export JWT_SECRET="your-32-char-minimum-secret-key"
.\gradlew run
```

### Optional: Firebase Config

Set before running:

```bash
export FIREBASE_SERVICE_ACCOUNT_JSON='{...your json...}'
.\gradlew run
```

---

## ✅ Verification Checklist

- [ ] Server running on 8080
- [ ] Can visit http://localhost:8001/login.html
- [ ] Can login with admin/supremeai123
- [ ] Redirected to admin dashboard after login
- [ ] Can see user name in top-right corner
- [ ] "Logout" button works
- [ ] All dashboard features accessible
- [ ] API calls work with JWT token
- [ ] Token auto-refreshes on expiration

---

## ❌ Common Issues

| Issue | Solution |
|-------|----------|
| 401 Unauthorized | Token expired → logout and login again |
| Can't see login page | Is server running? Check port 8001 |
| "User not found" | Username or password wrong, or user doesn't exist |
| API returns 401 | Ensure Authorization header: `Bearer <TOKEN>` |
| Can't register second admin | First admin must be logged in with JWT token |

---

## 📚 Documentation

For complete details, see:

- **[AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md)** - Full authentication system docs
- **[README.md](README.md)** - Project overview
- **[ADMIN_OPERATIONS_GUIDE.md](ADMIN_OPERATIONS_GUIDE.md)** - Admin operations manual

---

## 🚀 Ready to Use

Your SupremeAI system is now:

1. ✅ Protected by login authentication
2. ✅ Admin-controlled features
3. ✅ Works on localhost AND web host
4. ✅ Enterprise-grade security

Next: Add your AI providers and start creating projects!

---

**Version:** 1.0  
**Date:** March 27, 2026  
**For:** SupremeAI Admin System
