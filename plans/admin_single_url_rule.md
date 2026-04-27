# Admin Panel URL Rule - Customer & Admin

## Overview

URL structure for different user types:

- **Customer**: 1 URL to remember/save
- **Admin**: 2 URLs to remember/save (localhost + production backend)

## URL Structure

### Customer (1 URL only)

```
https://supremeai-a.web.app
```

Customer saves ONLY this single URL. All customer features accessible from here.

### Admin (2 URLs to save)

#### URL 1: Local Development

```
http://localhost:8080/admin.html
```

#### URL 2: Production Backend (Cloud Run)

```
https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html
```

Admin saves BOTH URLs:

- Use localhost URL for local development
- Use Cloud Run URL for production backend access

## URL Access Mapping

All other URLs are accessible FROM these 2 admin URLs:

| Feature | Access Via Admin URL |
|---------|---------------------|
| Customer Panel | `https://supremeai-a.web.app` |
| Admin Dashboard (Local) | `http://localhost:8080/admin.html` |
| Admin Dashboard (Prod) | `https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html` |
| API Endpoints (Local) | `http://localhost:8080/api/*` |
| API Endpoints (Prod) | `https://supremeai-lhlwyikwlq-uc.a.run.app/api/*` |
| Firebase Hosting | `https://supremeai-a.web.app` (rewrites `/api/**` to Cloud Run) |

## Implementation Rules

### 1. No Hard-Coded URLs in Code

All API calls must use:

- **Relative paths** (e.g., `/api/chat/message`, `/actuator/health`)
- **Environment variables** for configuration
- **No hard-coded** `localhost:8080` or `supremeai-lhlwyikwlq-uc.a.run.app`

### 2. Firebase Hosting Rewrites (firebase.json)

```json
{
  "hosting": {
    "public": "build/web",
    "rewrites": [
      {
        "source": "/api/**",
        "run": {
          "serviceId": "supremeai",
          "region": "us-central1"
        }
      },
      {
        "source": "**",
        "destination": "/index.html"
      }
    ]
  }
}
```

This allows `https://supremeai-a.web.app/api/*` to forward to Cloud Run backend.

### 3. Backend URL Configuration

#### Local Development

- Backend runs on `http://localhost:8080`
- Frontend uses relative URLs automatically

#### Production

- Cloud Run: `https://supremeai-lhlwyikwlq-uc.a.run.app`
- Firebase rewrites route `/api/**` to Cloud Run

### 4. Code Examples

#### ✅ Correct (Relative URLs)

```javascript
fetch('/api/chat/message')  // Works on both localhost and production
fetch('/actuator/health')    // Works everywhere
```

#### ❌ Wrong (Hard-coded URLs)

```javascript
fetch('http://localhost:8080/api/chat/message')  // Breaks on production
fetch('https://supremeai-lhlwyikwlq-uc.a.run.app/api/health')  // Breaks on localhost
```

## Files That Need URL Fixes

All files with hard-coded URLs must be updated to use relative paths or env vars:

1. **Flutter App** (`supremeai/lib/`)
   - `api_service.dart` - remove hard-coded baseUrl
   - `settings_provider.dart` - use env variable
   - `orchestration_provider.dart` - use relative URLs

2. **VS Code Extension** (`vscode-extension/`)
   - `src/extension.ts` - use configuration, not hard-coded
   - `package.json` - remove default hard-coded URL

3. **IntelliJ Plugin** (`supremeai-intellij-plugin/`)
   - `SupremeAIToolWindowFactory.kt` - use settings, not hard-coded
   - `SupremeAISettings.kt` - allow user configuration

4. **Firebase Functions** (`functions/`)
   - `system-health.js` - use relative or env var
   - `index.js` - use env variable for backend URL

5. **Admin Dashboard** (`public/`, `src/main/resources/static/`)
   - `admin-dashboard.html` - use relative URLs
   - `admin.html` - already uses relative URLs ✅

6. **Documentation** (`.md` files)
   - Update all hard-coded URLs to reference the 2 admin URLs rule

## Security Requirements

1. Authentication required for all `/admin` routes
2. Role-based access control (Customer vs Admin)
3. Same security rules for localhost and production
4. Secure token-based authentication

## Testing Checklist

- [ ] Customer accesses only `https://supremeai-a.web.app`
- [ ] Admin can use both localhost and Cloud Run URLs
- [ ] No hard-coded URLs in codebase
- [ ] All API calls use relative paths
- [ ] Firebase rewrites work correctly
- [ ] Local and production behave consistently
- [ ] All sub-routes function correctly

## Admin Panel Quick Links

The admin panel (`admin.html`) includes **QUICK LINKS** menu section with buttons:

- **🖥️ Admin (Localhost)** → Opens `http://localhost:8080/admin.html` in new tab
- **☁️ Admin (Production)** → Opens `https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html` in new tab
- **🏪 Customer Panel** → Opens `https://supremeai-a.web.app` in new tab

## Deployment Notes

1. **Local**: Run backend on `localhost:8080`, access via `http://localhost:8080/admin.html`
2. **Production Backend**: Deployed to Cloud Run, access via `https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html`
3. **Customer Frontend**: Deployed to Firebase, access via `https://supremeai-a.web.app`
4. All URLs route to same features, just different entry points
