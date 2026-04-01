# 👑 SupremeAI - Complete Admin Guide

**Version:** 3.5 Complete Edition  
**Last Updated:** March 27, 2026  
**Language:** বাংলা + English  

---

## 📋 TABLE OF CONTENTS

1. [Firebase Setup](#firebase-setup-complete)
2. [Admin API Key Management](#admin-api-key-management)
3. [AI Agent Assignment](#ai-agent-assignment)
4. [Admin Dashboard](#admin-dashboard)
5. [Daily Admin Tasks](#daily-admin-tasks)
6. [Emergency Procedures](#emergency-procedures)

---

## 🔥 FIREBASE SETUP (COMPLETE)

### Phase 1: Create Firebase Project

**Step 1: Go to Firebase Console**

```
👉 https://console.firebase.google.com
```

**Step 2: Create New Project**

```
Click: "Add Project"
Project Name: "supremeai-production"
Organization: (Your company)
Google Analytics: ✅ Enable
Region: asia-south1 (Close to Bangladesh)
Click: "Create Project"
```

**Step 3: Wait for Project Creation**

```
⏳ 3-5 minutes
Status: "Creating your Firebase project..."
✅ Complete: "Your Firebase project is ready"
```

### Phase 2: Setup Firestore Database

**Step 1: Create Firestore**

```
Left Menu → "Build" → "Firestore Database"
Click: "Create Database"
```

**Step 2: Configure Database**

```
Security Rules:
�
 Start in production mode

Location: 
  asia-south1 (Bangladesh nearest)

Click: "Create"
```

**Step 3: Create Database Collections**

```
Collection 1: "projects"
├── Fields Needed:
│   ├── projectId (string)
│   ├── name (string)
│   ├── description (string)
│   ├── status (string) - PENDING/APPROVED/RUNNING/COMPLETED
│   ├── createdAt (timestamp)
│   ├── assignedAI (string) - AI agent ID
│   ├── progress (number) - 0-100%
│   └── result (string)

Collection 2: "api_keys"
├── Fields:
│   ├── keyId (string)
│   ├── provider (string) - GEMINI/OPENAI/DEEPSEEK/GROQ
│   ├── key (string) - ENCRYPTED
│   ├── isActive (boolean)
│   ├── createdAt (timestamp)
│   ├── rotatedAt (timestamp)
│   └── usageCount (number)

Collection 3: "ai_agents"
├── Fields:
│   ├── agentId (string)
│   ├── name (string)
│   ├── role (string) - BUILDER/REVIEWER/ARCHITECT
│   ├── model (string)
│   ├── isActive (boolean)
│   ├── tasksCompleted (number)
│   ├── successRate (number)
│   └── lastUsed (timestamp)

Collection 4: "admin_logs"
├── Fields:
│   ├── actionType (string)
│   ├── description (string)
│   ├── admin (string)
│   ├── timestamp (timestamp)
│   ├── details (map)
│   └── status (string)
```

### Phase 3: Setup Authentication

**Step 1: Enable Auth Methods**

```
Left Menu → "Build" → "Authentication"
Click: "Get Started"
```

**Step 2: Enable Sign-in Methods**

```
✅ Email/Password
✅ Google
✅ Anonymous (for testing)
```

**Step 3: Add Admin User**

```
Users Tab → "Add User"
Email: admin@supremeai.com
Password: (Generate strong password)
Custom Claims: {"role": "admin"}
```

### Phase 4: Setup Cloud Storage

**Step 1: Create Storage**

```
Left Menu → "Build" → "Storage"
Click: "Get Started"
```

**Step 2: Configure Rules**

```
In Production mode

Rules:
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /projects/{projectId}/{allPaths=**} {
      allow read, write: if request.auth.uid != null;
    }
    match /backups/{allPaths=**} {
      allow read, write: if hasAdminRole();
    }
  }
}
```

### Phase 5: Setup Cloud Functions

**Step 1: Install Firebase Tools**

```bash
npm install -g firebase-tools
firebase login
```

**Step 2: Initialize Functions**

```bash
cd c:\Users\Nazifa\supremeai
firebase init functions
```

**Step 3: Deploy Cloud Functions**

```bash
firebase deploy --only functions
```

### Phase 6: Setup Cloud Messaging (for Mobile Alerts)

**Step 1: Generate Keys**

```
Project Settings → Cloud Messaging
Copy: Server API Key
Copy: Sender ID
```

**Step 2: Store in Environment**

```bash
[Environment]::SetEnvironmentVariable("FCM_SERVER_KEY", "your-key-here", "User")
[Environment]::SetEnvironmentVariable("FCM_SENDER_ID", "your-id-here", "User")
```

---

## 🔑 ADMIN API KEY MANAGEMENT

### How Admin Can Add API Keys (3 Methods)

#### Method 1: Admin Dashboard (Web Browser)

**Access:** `http://localhost:8001/admin`

**Interface:**

```
┌─────────────────────────────────────────┐
│   SUPREMEAI ADMIN - API KEY MANAGER    │
├─────────────────────────────────────────┤
│                                         │
│  Add New API Key                        │
│  ┌─────────────────────────────────┐   │
│  │ Provider:    [GEMINI    ▼]      │   │
│  │ API Key:     [••••••••••••]      │   │
│  │ Alias:       [production-key]    │   │
│  │ Description: [Optional notes]    │   │
│  │              [Add Key]           │   │
│  └─────────────────────────────────┘   │
│                                         │
│  Active API Keys:                       │
│  ┌─────────────────────────────────┐   │
│  │ Provider  │ Active │ Used  │Actions│ │
│  ├───────────┼────────┼───────┼────────┤ │
│  │ Gemini    │   ✅   │ 2450  │ 🔄❌  │ │
│  │ OpenAI    │   ✅   │ 1820  │ 🔄❌  │ │
│  │ DeepSeek  │   ⏸️   │ 0     │ 🔄❌  │ │
│  │ Groq      │   ✅   │ 3120  │ 🔄❌  │ │
│  └─────────────────────────────────┘   │
│                                         │
└─────────────────────────────────────────┘
```

#### Method 2: Android App (Mobile)

**App Name:** SupremeAI Admin

**Features:**

```
📱 Screens:
├── Login (Email + Password)
├── Dashboard
│   ├── Active Projects
│   ├── System Status
│   └── Quick Stats
├── API Key Manager
│   ├── Add New Key
│   ├── View Active Keys
│   ├── Rotate Keys
│   └── Monitor Usage
├── AI Assignment
│   ├── Select Project
│   ├── Choose AI Agents
│   └── Start Task
└── Settings
    ├── Notifications
    ├── Preferences
    └── Logout
```

**Usage Flow:**

```
1. Open App
2. Login with admin@supremeai.com
3. Tab: "API Keys"
4. Press: "+" New Key
5. Select: Provider (Gemini, OpenAI, etc)
6. Paste: Your API Key
7. Name: "production-gemini"
8. Press: "Save & Test"
9. ✅ Key Added Successfully
```

#### Method 3: REST API (Programmatic)

**Endpoint:** `https://api.supremeai.com/v1/admin/api-keys`

**Add API Key (POST):**

```bash
curl -X POST https://api.supremeai.com/v1/admin/api-keys \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "GEMINI",
    "key": "AIzaSyCw1skxzGEURHMJiWZPGIDhx0WIw0vDJ3U",
    "alias": "production-gemini-v1",
    "description": "Primary Gemini key for production"
  }'

Response:
{
  "success": true,
  "keyId": "key_1234567890",
  "provider": "GEMINI",
  "alias": "production-gemini-v1",
  "status": "ACTIVE",
  "createdAt": "2026-03-27T10:15:30Z"
}
```

**Get API Keys (GET):**

```bash
curl -X GET https://api.supremeai.com/v1/admin/api-keys \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"

Response:
{
  "total": 4,
  "keys": [
    {
      "keyId": "key_1234567890",
      "provider": "GEMINI",
      "alias": "production-gemini-v1",
      "lastUsed": "2026-03-27T09:45:12Z",
      "usageCount": 2450,
      "status": "ACTIVE"
    },
    ...
  ]
}
```

**Rotate API Key (PUT):**

```bash
curl -X PUT https://api.supremeai.com/v1/admin/api-keys/{keyId}/rotate \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "newKey": "NEW_KEY_HERE"
  }'
```

**Disable API Key (DELETE):**

```bash
curl -X DELETE https://api.supremeai.com/v1/admin/api-keys/{keyId} \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

---

## 🤖 AI AGENT ASSIGNMENT

### How to Assign AI Agents to Tasks

#### Step 1: Dashboard - View Active Projects

```
Admin Dashboard → Projects Tab

┌──────────────────────────────────────────┐
│  ACTIVE PROJECTS (3)                     │
├──────────────────────────────────────────┤
│                                          │
│ 1. real-task-manager-app                 │
│    Status: ⏳ IN PROGRESS                 │
│    Creator: System                       │
│    Created: 2 hours ago                  │
│    Assigned AI: None Yet                 │
│    Progress: 0%                          │
│    [ASSIGN AI]                           │
│                                          │
│ 2. my-chat-app                           │
│    Status: ⏳ PLANNING                    │
│    Creator: Admin                        │
│    Created: 30 minutes ago               │
│    Assigned AI: Z-Architect              │
│    Progress: 25%                         │
│    [CHANGE AI] [PAUSE] [CANCEL]         │
│                                          │
│ 3. ecommerce-store                       │
│    Status: ✅ COMPLETED                   │
│    Creator: Admin                        │
│    Created: 2 days ago                   │
│    Assigned AI: X-Builder, Y-Reviewer    │
│    Progress: 100%                        │
│    [VIEW RESULTS] [ARCHIVE]              │
│                                          │
└──────────────────────────────────────────┘
```

#### Step 2: Click "ASSIGN AI" Button

```
Dialog: Select AI Agent

┌───────────────────────────────────────────┐
│  ASSIGN AI AGENT TO PROJECT               │
│  Project: real-task-manager-app           │
├───────────────────────────────────────────┤
│                                           │
│ Choose AI Role:                           │
│ ☑️ BUILDER (Code Generator)              │
│ ☑️ REVIEWER (Quality Assurance)           │
│ ☑️ ARCHITECT (Design & Planning)          │
│                                           │
├───────────────────────────────────────────┤
│                                           │
│ Builder Preference:                       │
│ ○ X-Builder (DeepSeek) - 98.5% success   │
│ ○ Y-Builder (Groq) - 96.2% success       │
│ ○ Z-Builder (Together) - 94.1% success   │
│                                           │
│ Performance Score:                        │
│ X-Builder: ████████████░░ 95/100         │
│ Y-Builder: ███████████░░░ 92/100         │
│ Z-Builder: ██████████░░░░ 88/100         │
│                                           │
├───────────────────────────────────────────┤
│                                           │
│ Reviewer Preference:                      │
│ ○ Y-Reviewer (Claude) - 99.2% success    │
│ ○ X-Reviewer (GPT-4) - 97.8% success     │
│                                           │
├───────────────────────────────────────────┤
│                                           │
│ Architect Preference:                     │
│ ○ Z-Architect (GPT-4) - 99.5% success    │
│ ○ X-Architect (Claude) - 98.1% success   │
│                                           │
│           [ASSIGN] [CANCEL]               │
│                                           │
└───────────────────────────────────────────┘
```

#### Step 3: Select AI Agents

```
Choose your combination:
✅ BUILDER:   X-Builder (DeepSeek)
✅ REVIEWER:  Y-Reviewer (Claude)
✅ ARCHITECT: Z-Architect (GPT-4)

✅ [ASSIGN] Button
```

#### Step 4: Confirm & Start

```
Confirmation Dialog:

✅ AI AGENTS ASSIGNED

Project: real-task-manager-app
Assigned AI:
  • Builder: X-Builder (DeepSeek)
  • Reviewer: Y-Reviewer (Claude) 
  • Architect: Z-Architect (GPT-4)

Start Time: Now
Estimated Completion: 2-3 hours

[CONFIRM & START] [EDIT] [CANCEL]
```

#### Step 5: Monitor Progress

```
Real-time Progress:

Project: real-task-manager-app
Status: 🟢 RUNNING

Agents at Work:
├── Z-Architect: 📊 Planning Architecture
│   Progress: ▓▓▓▓▓░░░░░ 50%
│   Time: 15 minutes elapsed
│   Status: "Designing API structure..."
│
├── X-Builder: ⏳ Waiting
│   Progress: ░░░░░░░░░░ 0%
│   Time: Not started
│   Status: "Waiting for architecture approval"
│
└── Y-Reviewer: ⏳ Waiting
    Progress: ░░░░░░░░░░ 0%
    Time: Not started
    Status: "Waiting for code to review"

Overall Progress: ▓▓▓░░░░░░░ 20%
ETA: 2 hours 45 minutes
```

---

## 📊 ADMIN DASHBOARD

### Dashboard URL: `http://localhost:8001/admin`

### Key Sections

#### 1. Quick Stats

```
┌─────────────────────────────────────────┐
│         SUPREMEAI ADMIN DASHBOARD       │
├─────────────────────────────────────────┤
│                                         │
│  Active Projects: 3      Completed: 8   │
│  Success Rate: 96.2%     Total Tasks: 11│
│  API Keys: 4 active      Errors: 0      │
│  AI Agents: 3 running    Avg Time: 2h   │
│                                         │
└─────────────────────────────────────────┘
```

#### 2. API Key Manager

- View all API keys
- Add new keys
- Rotate/disable keys
- Monitor usage per provider

#### 3. Project Management

- View all projects
- Assign AI agents
- Monitor progress
- View results

#### 4. AI Agent Status

- Show running agents
- Performance metrics
- Task history
- Success rates

#### 5. System Health

- Firebase status
- API quota usage
- Error rate
- Response times

#### 6. Audit Logs

- All admin actions
- Timestamps
- Details
- Status

---

## 📋 DAILY ADMIN TASKS

### ✅ Morning Tasks (9 AM)

```
□ Check Dashboard Status
  - System Health: Should be GREEN
  - Error Rate: Should be < 2%
  - All Services: RUNNING

□ Review Overnight Projects
  - Any failed projects?
  - Error logs?
  - Need intervention?

□ Check API Key Status
  - Any keys disabled?
  - Usage within limits?
  - Need to rotate?

□ Start Day
  - Mark system as "Ready for Day"
  - Brief team on tasks
  - Check notifications
```

### 🏃 Throughout Day

```
□ Monitor Active Projects
  - Check every 2 hours
  - Ensure smooth progress
  - Help if blocked

□ Watch Error Rate
  - Set alert at > 5%
  - Investigate if triggered
  - Fix issues ASAP

□ API Key Usage
  - Monitor quota consumption
  - Adjust if approaching limit
  - Optimize if needed

□ Respond to Alerts
  - Email notifications
  - SMS alerts
  - Slack messages
```

### 🛡️ Evening Tasks (6 PM)

```
□ End Day Report
  - How many projects today?
  - Success rate?
  - Any issues?

□ Daily Summary
  - Generate report
  - Share with team
  - Update metrics

□ Check Tomorrow
  - Scheduled tasks?
  - Expected workload?
  - Resources needed?

□ Security Check
  - Review access logs
  - Check for anomalies
  - All safe?

□ Prepare Backup
  - Export data
  - Verify backup
  - Store safely
```

### 📅 Weekly Tasks (Friday)

```
□ Weekly Report Generation
  - Performance metrics
  - Success rates
  - Error analysis
  - Trends

□ AI Agent Reviews
  - Performance scores
  - Success rates
  - Optimization needed?

□ API Key Audit
  - Review all active keys
  - Rotate if needed
  - Disable unused

□ System Optimization
  - Database cleanup
  - Logs archival
  - Performance tuning

□ Team Meeting
  - Share metrics
  - Discuss issues
  - Plan next week
```

### 🎯 Monthly Tasks

```
□ Security Audit
  - Review all access logs
  - Check for threats
  - Update security rules

□ Performance Review
  - Monthly metrics report
  - Year-to-date comparison
  - Improvement areas

□ Cost Analysis
  - API expenses
  - Firebase costs
  - Optimization opportunities

□ Backup & Recovery Test
  - Restore from backup
  - Verify data integrity
  - Test recovery process

□ Planning
  - Next month goals
  - Resource allocation
  - Budget review
```

---

## 🚨 EMERGENCY PROCEDURES

### Scenario 1: High Error Rate (> 10%)

```
IMMEDIATE ACTIONS:
1. Check error logs: Firebase Console → Functions → Logs
2. Identify error pattern
3. Check if API keys are valid
4. Check if Firebase is up

REMEDIATION:
□ Restart Java service
□ Verify API keys
□ Check Firebase connection
□ Clear cache/logs
□ Contact API providers if issue persists
```

### Scenario 2: API Key Compromised

```
IMMEDIATE ACTIONS:
1. Go to API provider console
2. Revoke the key
3. Disable in Admin Dashboard
4. Generate new key

WITHIN 1 HOUR:
□ Add new key via Admin Dashboard
□ Test with new key
□ Monitor for unauthorized usage
□ Update logs

WITHIN 24 HOURS:
□ Audit access logs
□ Report to security team
□ Update documentation
□ Implement preventive measures
```

### Scenario 3: Firebase Down

```
IMMEDIATE ACTIONS:
1. Verify: https://firebase.google.com/status
2. Wait for Google to fix (usually 5-30 mins)
3. Check alternate services

IF PROLONGED (> 1 hour):
□ Activate failover database
□ Switch to offline mode
□ Notify users of issue
□ Continue when restored

AFTER RESTORATION:
□ Sync data
□ Verify consistency
□ Resume normal operations
□ Post-mortem analysis
```

### Scenario 4: Too Many Requests (Rate Limited)

```
IMMEDIATE ACTIONS:
1. Check quota in Firebase Console
2. See which API exceeded limit

OPTIONS:
A. Request quota increase with provider
B. Reduce request volume
C. Optimize API calls
D. Use different provider temporarily

LONG-TERM:
□ Implement caching
□ Optimize queries
□ Batch requests
□ Monitor usage trending
```

---

## 🎓 Admin Training Checklist

### Before Starting Admin Role

- [ ] Read this entire document
- [ ] Set up Firebase account
- [ ] Generate personal API keys
- [ ] Access Admin Dashboard
- [ ] Practice adding API key
- [ ] Practice assigning AI
- [ ] Monitor test project
- [ ] Check monitoring dashboard
- [ ] Read security audit report
- [ ] Understand emergency procedures

### Hands-on Practice

- [ ] Create test project
- [ ] Assign all 3 AI agents
- [ ] Monitor completion
- [ ] Check generated code
- [ ] Review audit logs
- [ ] Test API key rotation
- [ ] Generate a report
- [ ] Restore from backup

### Ready to Go Live

Once all above done, you're ready! 🎉

---

## 📞 Quick Reference

### Important URLs

```
Firebase Console:    https://console.firebase.google.com
Google Cloud:        https://console.cloud.google.com
Admin Dashboard:     http://localhost:8001/admin
Monitoring:          http://localhost:8000
API Documentation:   Your local docs/api.md
```

### Important Commands

```bash
# Start system
.\gradlew run

# Start admin dashboard
python -m http.server 8001 --directory admin

# Start monitoring
python -m http.server 8000 --directory dashboard

# View logs
firebase functions:log

# Deploy changes
firebase deploy --only functions
```

### Emergency Contacts

```
On-call: Your Phone
Manager: Manager Email
Firebase Support: support@firebase.google.com
```

---

## ✅ Success Criteria

You're a good admin when:

```
✅ Error rate consistently < 2%
✅ All projects complete on time
✅ AI agents running smoothly
✅ API keys managed securely
✅ Daily reports generated
✅ Zero security incidents
✅ 95%+ success rate
✅ Fast problem resolution
✅ Team confident in system
✅ Backups verified monthly
```

---

## 🎖️ You're All Set

**You now have everything you need to:**

- ✅ Setup Firebase completely
- ✅ Manage API keys from any device
- ✅ Assign AI agents to projects
- ✅ Monitor system health
- ✅ Handle emergencies
- ✅ Run daily operations
- ✅ Generate reports
- ✅ Maintain security

**Start with:** Setting up your API keys in Admin Dashboard

**Then:** Create your first project and assign AI

**Finally:** Monitor it on the dashboard

**Good luck! 👑🚀**

---

**Created:** March 27, 2026  
**For:** SupremeAI Admins
**Version:** 3.5 Complete
