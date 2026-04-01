# 🔐 Secure Setup Guide - SupremeAI Authentication

**Updated:** April 1, 2026  
**Status:** ✅ Secure, One-Time Initialization Only

---

## ⚠️ Security Policy

- **ONE DEFAULT USER ONLY** - একটি মাত্র initial admin user
- **No Public Initialization** - কোনো public endpoint নেই default user তৈরির জন্য
- **Token Protected** - Setup এ environment variable দিয়ে protection
- **Firebase Integration Ready** - নতুন users Firebase auth দিয়ে manage হবে

---

## 🚀 How to Initialize System

### **Option 1: Using Setup API (Recommended)**

#### Step 1: Generate Setup Token

```bash
# Linux/macOS
export SUPREMEAI_SETUP_TOKEN="your-secure-random-token-min-32-chars-long"

# Windows PowerShell
$env:SUPREMEAI_SETUP_TOKEN = "your-secure-random-token-min-32-chars-long"
```

**Generate Strong Token:**
```bash
# Linux/macOS
openssl rand -hex 32

# Windows PowerShell
[System.Security.Cryptography.RNGCryptoServiceProvider]::GetBytes(32) | ForEach-Object {'{0:X2}' -f $_}
```

#### Step 2: Start Backend with Token

```bash
export SUPREMEAI_SETUP_TOKEN="abc123def456..."
./gradlew run
```

#### Step 3: Call Setup Endpoint

```bash
curl -X POST http://localhost:8080/api/auth/setup \
  -H "Content-Type: application/json" \
  -d '{
    "setupToken": "abc123def456...",
    "username": "admin",
    "email": "admin@supremeai.com",
    "password": "YourSecurePassword123!"
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "✅ Initial admin user created successfully",
  "username": "admin",
  "email": "admin@supremeai.com",
  "note": "⚠️ Change password on first login. Other users can now be added through API."
}
```

#### Step 4: ✅ System Initialized

- Admin user created
- Login via: `http://localhost:8001`
- Credentials: `admin` / `YourSecurePassword123!`

---

### **Option 2: Manual Firebase Creation**

If you prefer Firebase console:

1. Go to Firebase Console → Realtime Database
2. Create `users/{adminUserId}` document:

```json
{
  "username": "admin",
  "email": "admin@supremeai.com",
  "passwordHash": "$2a$10$...", // Use /api/auth/hash-password
  "active": true,
  "role": "admin",
  "permissions": [],
  "createdAt": 1711900000000,
  "lastLogin": 0
}
```

---

## 🔒 Adding New Users (After Initial Setup)

**Only existing admin users can add new users:**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN" \
  -d '{
    "username": "newadmin",
    "email": "newadmin@supremeai.com",
    "password": "SecurePassword456!"
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "Admin user registered successfully",
  "user": {
    "id": "user-123",
    "username": "newadmin",
    "email": "newadmin@supremeai.com",
    "role": "admin"
  }
}
```

---

## 🔑 Generating Password Hashes

For manual user creation in Firebase:

```bash
curl -X POST http://localhost:8080/api/auth/hash-password \
  -H "Content-Type: application/json" \
  -d '{"password": "YourSecurePassword123!"}'
```

**Response:**
```json
{
  "status": "success",
  "password": "YourSecurePassword123!",
  "hash": "$2a$10$...",
  "note": "Use this hash as 'passwordHash' field in Firebase users collection",
  "firebase_template": {
    "username": "your_username",
    "email": "user@example.com",
    "passwordHash": "$2a$10$...",
    "active": true,
    "role": "admin",
    "permissions": [],
    "createdAt": 1711900000000,
    "lastLogin": 0
  }
}
```

---

## 📋 Setup Checklist

- [ ] Generate secure setup token
- [ ] Set `SUPREMEAI_SETUP_TOKEN` environment variable
- [ ] Start backend: `./gradlew run`
- [ ] Call `/api/auth/setup` endpoint with token
- [ ] Verify admin user created
- [ ] Log in with admin credentials
- [ ] **Change password immediately!**
- [ ] Add additional users via authenticated `/api/auth/register`

---

## ⚠️ Security Best Practices

1. **Setup Token:**
   - Use strong random 32+ character token
   - Store in environment, never in code
   - Delete after first setup

2. **Initial Admin:**
   - Change password immediately after first login
   - Use strong password (8+ chars, mixed case, numbers, symbols)
   - Enable 2FA when Firebase Auth added

3. **New Users:**
   - Only created by authenticated admin
   - Firebase Realtime Database protection rules enabled
   - Audit logs track all user creation

4. **Production Deployment:**
   - Remove setup tokens from environment after initialization
   - Enable Firebase Security Rules
   - Use environment variables for all secrets
   - Enable HTTPS only
   - Setup audit logging

---

## 🔗 Related Endpoints

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST | `/api/auth/setup` | Setup Token | Initialize first admin |
| POST | `/api/auth/login` | None | User login |
| POST | `/api/auth/register` | Admin JWT | Add new admin user |
| POST | `/api/auth/refresh` | Refresh Token | Renew access token |
| GET | `/api/auth/users` | Admin JWT | List all users |
| POST | `/api/auth/change-password` | User JWT | Change own password |

---

## 📖 Next Steps

1. **Initial Setup:** Follow Option 1 above
2. **Firebase Integration:** Configure Firebase Authentication
3. **User Management:** Add users via authenticated API
4. **Mobile App:** Connect Flutter app with same credentials
5. **Monitoring:** Enable audit logging

---

## 🆘 Troubleshooting

### "Setup not configured"
```
Error: Setup not configured. Contact administrator.
```
**Fix:** Set SUPREMEAI_SETUP_TOKEN environment variable before starting backend

### "System already initialized"
```
Error: System already initialized. Cannot re-run setup.
```
**Normal:** Setup runs only once. This is expected behavior.

### "Invalid setup token"
```
Error: Invalid setup token
```
**Fix:** Ensure token matches environment variable exactly

---

## ✅ Security Verification

After setup:
- ✅ Single admin user created
- ✅ No public initialization endpoints
- ✅ Token-protected setup
- ✅ New users require admin auth
- ✅ Passwords hashed with BCrypt
- ✅ Ready for Firebase integration

