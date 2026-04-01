# SupremeAI - Admin-Only Authentication System

## ✅ What's Implemented

A complete **admin-only authentication system** with:

- ✅ Login page (`login.html`)
- ✅ JWT token-based authentication
- ✅ Admin user registration (admin creates other admins)
- ✅ Secure password hashing (BCrypt)
- ✅ Token refresh mechanism (7-day expiration)
- ✅ Firebase integration for user storage
- ✅ REST API endpoints
- ✅ Browser auto-protection (redirects to login if not authenticated)
- ✅ Works on **localhost and web host** automatically

---

## 🚀 Quick Start - 5 Minutes

### Step 1: Create First Admin User (One-time)

```bash
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "admin",
  "email": "admin@supremeai.com",
  "password": "supremeai123"
}
```

**Response:**

```json
{
  "status": "success",
  "message": "Admin user registered successfully",
  "user": {
    "id": "admin",
    "username": "admin",
    "email": "admin@supremeai.com"
  }
}
```

### Step 2: Login at Login Page

Open browser:

- **Localhost:** http://localhost:8001/login.html
- **Web Host:** https://your-domain.com/login.html

Enter credentials:

```
Username: admin
Password: supremeai123
```

### Step 3: Access Admin Dashboard

After login, you're redirected to:

- **Localhost:** http://localhost:8001/admin
- **Web Host:** https://your-domain.com/admin

---

## 📋 API Endpoints

### Authentication Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login (get JWT token) |
| POST | `/api/auth/register` | Register new admin |
| POST | `/api/auth/refresh` | Refresh expired token |
| GET | `/api/auth/me` | Get current user info |
| POST | `/api/auth/change-password` | Change password |
| GET | `/api/auth/users` | List all admins |
| POST | `/api/auth/users/{userId}/disable` | Disable admin account |

### All Other Endpoints

All other endpoints (`/api/...`) require:

```
Authorization: Bearer <JWT_TOKEN>
```

---

## 🔐 Login & Token Flow

```
User Browser                    SupremeAI Server
    |                                  |
    |--1. POST /api/auth/login-------->|
    |   (username, password)           |
    |                                  |
    |<-----2. JWT Token + Refresh------|
    |       Token expires in 24h       |
    |                                  |
    |--3. GET /api/v1/data/...-------->|
    |   Authorization: Bearer JWT      |
    |                                  |
    |<-----4. Data (200 OK)------------|
    |                                  |
    |                      [Token Expires]
    |                                  |
    |--5. POST /api/auth/refresh------>|
    |   (refresh_token)                |
    |                                  |
    |<-----6. New JWT Token------------|
    |       Valid for 24 more hours    |
    |                                  |
```

### Token Expiration

- **Access Token (JWT):** 24 hours
- **Refresh Token:** 7 days
- **Auto-refresh:** Browser automatically refreshes when token expires

---

## 📁 File Structure

```
SupremeAI/
├── login.html                          # Login page
├── admin/index.html                    # Admin dashboard
├── src/main/java/org/example/
│   ├── model/
│   │   ├── User.java                   # Admin user model
│   │   └── AuthToken.java              # JWT token response
│   ├── service/
│   │   └── AuthenticationService.java  # JWT + user logic
│   ├── controller/
│   │   └── AuthenticationController.java # /api/auth endpoints
│   ├── filter/
│   │   └── AuthenticationFilter.java   # JWT validation filter
│   └── Application.java                # Spring Boot app
└── src/main/resources/static/js/
    └── auth-helper.js                  # Frontend token management
```

---

## 💻 Usage Examples

### JavaScript (Frontend)

```javascript
// Get current user
const user = AuthHelper.getUser();
console.log(user.username, user.email);

// Check if authenticated
if (AuthHelper.isAuthenticated()) {
    console.log("User is logged in");
}

// Make API call with auto-token handling
const response = await AuthHelper.apiCall('/api/v1/data/github/owner/repo');
const data = await response.json();

// Logout
AuthHelper.logout();  // Clears tokens and redirects to /login.html
```

### cURL (Command Line)

```bash
# Step 1: Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"supremeai123"}'

# Response includes: token, refreshToken, user

# Step 2: Use token in API calls
curl -X GET http://localhost:8080/api/v1/data/health \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# Step 3: Refresh token (when expires)
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

---

## 🏗️ Register New Admin (Admin Creates Admin)

An existing admin can register new admins:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -d '{
    "username": "newadmin",
    "email": "newadmin@supremeai.com",
    "password": "secure_password_123"
  }'
```

---

## 🔄 Auto-Refresh Mechanism

The `auth-helper.js` file automatically:

1. **Checks token status** on every API call
2. **Refreshes token** if expired (using refresh token)
3. **Retries failed request** with new token
4. **Redirects to login** if refresh fails

No manual handling needed!

---

## 🌍 Localhost vs Web Host

### Localhost (Development)

```
Login:      http://localhost:8001/login.html
Admin:      http://localhost:8001/admin
Monitoring: http://localhost:8000
API:        http://localhost:8080/api/...
```

### Web Host (Production)

```
Login:      https://your-domain.com/login.html
Admin:      https://your-domain.com/admin
API:        https://your-domain.com/api/...
```

**Same authentication works on both!** Token is stored in `localStorage`.

---

## 🔒 Security Features

| Feature | Details |
|---------|---------|
| **Password Hashing** | BCrypt (salted + hashed) |
| **JWT Tokens** | HS256 algorithm, signed with server key |
| **Token Expiration** | 24h access, 7d refresh (auto-expiration) |
| **HTTPS** | Use in production (auto-upgrade from HTTP) |
| **Firebase** | User data encrypted at rest |
| **Public Endpoints** | Only `/login`, `/register`, `/refresh`, `/health` |
| **Private Endpoints** | All require valid JWT token |

---

## 🛠️ Environment Variables (Optional)

```bash
# JWT signing key (change in production)
export JWT_SECRET="your-secret-key-32-chars-minimum"

# Firebase config
export FIREBASE_SERVICE_ACCOUNT_JSON='{...json...}'
```

---

## 📊 Database Structure (Firebase)

### Users Collection

```
users/
├── admin/
│   ├── username: "admin"
│   ├── email: "admin@supremeai.com"
│   ├── passwordHash: "bcrypt_hash"
│   ├── active: true
│   ├── createdAt: 1711612800000
│   └── lastLogin: 1711699200000
└── newadmin/
    └── (same structure)
```

---

## ❓ Troubleshooting

### "401 Unauthorized"

- Token expired → Click "Logout" and login again
- Invalid token → Clear `localStorage` and refresh page
- Missing header → Ensure `Authorization: Bearer TOKEN` format

### "User not found"

- Username is case-sensitive
- Check spelling in database
- Register new user if needed

### Token Not Refreshing

- Refresh token expired → Must login again
- Check network tab for `/api/auth/refresh` response
- Clear `localStorage` if corrupted

### Can't Access Admin Dashboard

- Must be logged in first (redirects to `/login.html`)
- Check browser console for errors
- Verify Firebase is initialized

---

## 🎯 Next Steps

1. **Set initial admin credentials** securely
2. **Register additional admin accounts** as needed
3. **Change JWT_SECRET** in production
4. **Enable HTTPS** on web host
5. **Monitor admin_logs** for suspicious activity
6. **Rotate passwords quarterly**

---

## 📞 Support

For issues with authentication:

1. Check browser DevTools → Console for errors
2. Check server logs for `/api/auth/...` calls
3. Verify Firebase connection
4. Test with cURL commands above

---

**System Version:** SupremeAI v1.0  
**Last Updated:** March 27, 2026  
**Authentication:** JWT + BCrypt + Firebase
