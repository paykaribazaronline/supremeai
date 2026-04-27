# SupremeAI Deployment Guide

## 🌐 Access URLs

### Local Development Server

- **Main App:** http://localhost:5000/
- **Health Check:** http://localhost:5000/health
- **API Status:** http://localhost:5000/api/status
- **Admin Dashboard:** http://localhost:5000/admin-dashboard.html

### Firebase Hosting (GCloud Server)

- **Main App:** https://supremeai.firebaseapp.com/
- **Admin Dashboard:** https://supremeai.firebaseapp.com/admin-dashboard.html
- **Admin Dashboard (Alternate):** https://supremeai.firebaseapp.com/admin

### Firebase Functions

- **Check Server Connections:** https://us-central1-supremeai.cloudfunctions.net/checkServerConnections
- **Get System Health:** https://us-central1-supremeai.cloudfunctions.net/getSystemHealth

## 🚀 Deployment Commands

### Initial Setup

```bash
# Install Firebase CLI (if not installed)
npm install -g firebase-tools

# Login to Firebase
firebase login

# Initialize Firebase (if not done)
firebase init
```

### Deploy Everything

```bash
# Deploy all Firebase resources
firebase deploy
```

### Deploy Specific Services

```bash
# Deploy only Firebase Functions
firebase deploy --only functions

# Deploy only Firebase Hosting
firebase deploy --only hosting

# Deploy specific function
firebase deploy --only functions:checkServerConnections
```

### Local Development

```bash
# Start local server
start-server.bat

# Or manually:
cd smart_chat_system
python run.py
```

## 📊 Admin Dashboard Features

### System Health Monitoring

- CPU Usage
- Memory Usage
- Disk Usage
- System Uptime
- Service Status

### Server Connections

- Firebase Services Status
- Google Cloud Platform Status
- Local Server Status
- Smart Chat System Status
- Response Times

### Auto Update

- Health data updates every 1 minute
- Connection data updates every 1 minute

## 🔧 Configuration

### Server Configuration

Edit `smart_chat_system/config.json` to change:

- Server host
- Server port
- Debug mode
- Health check settings

### Firebase Configuration

Edit `firebase.json` to change:

- Hosting settings
- Function settings
- Rewrite rules
- Headers

## 📝 Notes

1. **First Time Deployment:**
   - Run `deploy.bat` to deploy everything
   - This will deploy Firebase Functions and Hosting

2. **After Deployment:**
   - Access admin dashboard at: https://supremeai.firebaseapp.com/admin-dashboard.html
   - Or at: https://supremeai.firebaseapp.com/admin

3. **Local Development:**
   - Run `start-server.bat` to start local server
   - Access at: http://localhost:5000/

4. **Monitoring:**
   - Admin dashboard auto-updates every minute
   - No manual refresh needed
   - All real-time data from connected servers

## 🔐 Security

- Admin dashboard is protected by Firebase Authentication
- Only authorized users can access
- All API calls are authenticated

## 📞 Support

For issues or questions, contact the development team.
