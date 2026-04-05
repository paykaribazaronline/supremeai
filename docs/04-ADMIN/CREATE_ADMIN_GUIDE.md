# How to Create Admin User for SupremeAI

## ⚠️ IMPORTANT: One-Time Setup Required

The system needs an initial admin user before you can login. Follow these steps:

### Option 1: Use the Setup Endpoint (RECOMMENDED)

1. Start the application:

```bash
./gradlew bootRun
```

2. Call the one-time setup endpoint:

```bash
curl -X POST http://localhost:8080/api/auth/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer SUPREMEAI_SETUP_TOKEN" \
  -d '{
    "username": "supremeai",
    "email": "supremeai@admin.com",
    "password": "${SUPREMEAI_ADMIN_PASSWORD}"
  }'
```

**Setup Token**: Defined in `src/main/resources/application.properties`

```properties
supremeai.setup.token=your_setup_token_here
supremeai.default.admin.username=supremeai
supremeai.default.admin.email=supremeai@admin.com
```

### Option 2: Firebase Console (MANUAL)

1. Go to Firebase Console: https://console.firebase.google.com
2. Select project: **supremeai-a**
3. Navigate to **Authentication** → **Users**
4. Click **Add user**
5. Enter:
   - Email: `supremeai@admin.com`

- Password: use your secure admin password from `SUPREMEAI_ADMIN_PASSWORD`

6. Click **Add user**

### Option 3: Backend Database Direct Insert

Contact DBA or use database admin tool to insert:

```sql
INSERT INTO users (username, email, password_hash, role, created_at)
VALUES (
  'supremeai',
  'supremeai@admin.com',
  HASH('<ADMIN_PASSWORD_FROM_ENV>'),
  'ADMIN',
  NOW()
);
```

## After Admin Creation

1. Go to: https://supremeai-a.web.app/admin/#/login
2. Login with:
   - Username: `supremeai`

- Password: value from `SUPREMEAI_ADMIN_PASSWORD`

3. Check **Remember me** to persist login
4. Click **Sign in**

## Troubleshooting

### "Remember me" not working?

- Check browser localStorage is enabled
- Check browser console for errors (F12)
- Try incognito/private mode

### Still can't login?

- Verify admin user exists in Firebase
- Check backend logs: `./gradlew bootRun 2>&1 | grep -i auth`
- Verify JWT token is being generated
- Check network response in browser DevTools

### Error: "Danger" sign shown

- This typically means authentication error
- Check browser console for exact error message
- Verify credentials are correct
