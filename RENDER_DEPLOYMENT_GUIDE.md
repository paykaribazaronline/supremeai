# 🚀 Render.com Deployment Guide - SupremeAI v3.1.0

**Status:** Ready to deploy  
**Estimated Time:** 5-10 minutes  
**Cost:** FREE (free tier)  
**Expected URL:** https://supremeai.onrender.com

---

## ✅ Prerequisites (Already Done)

- ✅ GitHub repository: https://github.com/paykaribazaronline/supremeai
- ✅ Code pushed to `main` branch (commit: bdd176d)
- ✅ Dockerfile configured (multi-stage build)
- ✅ Docker image tested: `gcr.io/supremeai-a/supremeai:v3.1.0`

---

## 📝 Step-by-Step Deployment

### **Step 1: Sign Up for Render.com**

1. Go to **https://render.com**
2. Click **"Sign Up"**
3. Choose: **"Sign up with GitHub"**
4. Authorize Render to access your GitHub account
5. Complete setup with email verification

---

### **Step 2: Create New Web Service**

1. Click **"New +"** button (top-right)
2. Select **"Web Service"**
3. Click **"Connect a repository"**

---

### **Step 3: Connect GitHub Repository**

1. In the list, find: **supremeai**
2. Click **"Connect"** button next to it
3. **If not visible:**
   - Click **"Adjust GitHub app permissions"**
   - Grant access to `supremeai` repository
   - Return to Render and refresh

---

### **Step 4: Configure Service**

Fill in the form with these values:

```
┌─────────────────────────────────────────────────────────┐
│ NAME: supremeai                                         │
│ ENVIRONMENT: Docker                                     │
│ REGION: Oregon (us-west)                               │
│ BRANCH: main                                           │
│ DOCKERFILE PATH: ./Dockerfile                          │
│ BUILD COMMAND: (leave empty - uses Dockerfile)         │
│ START COMMAND: (leave empty - uses Dockerfile)         │
├─────────────────────────────────────────────────────────┤
│ PLAN: Free (2vCPU, 0.5GB RAM, 750 free dyno hours)    │
│ AUTO-DEPLOY: Yes (deploy on every push)               │
└─────────────────────────────────────────────────────────┘
```

---

### **Step 5: Environment Variables**

Click **"Add Environment Variable"** and add:

```
KEY                          VALUE
─────────────────────────────────────────
PORT                         8080
SPRING_PROFILES_ACTIVE       production
JAVA_TOOL_OPTIONS            -Xmx256m -Xms128m
SERVER_PORT                  8080
```

---

### **Step 6: Deploy!**

1. Scroll to bottom
2. Click **"Create Web Service"**
3. **Wait** 5-10 minutes for build to complete
4. You'll see:
   ```
   ✓ Deployment live!
   Your service is live at: https://supremeai.onrender.com
   ```

---

## 🔗 After Deployment

### Your Live Service

```
URL: https://supremeai.onrender.com
API: https://supremeai.onrender.com/api/v1/...
Health: https://supremeai.onrender.com/actuator/health
```

### Test It

```bash
# Check service is running
curl https://supremeai.onrender.com/actuator/health

# Expected response:
# {"status":"UP"}
```

### View Logs

1. Click on your service in Render dashboard
2. Go to **"Logs"** tab
3. See real-time deployment and runtime logs

### Manage Service

1. **Restart:** Click "⋯" → "Restart"
2. **Redeploy:** Push to GitHub (auto-deploys)
3. **Stop:** Click "⋯" → "Suspend"
4. **Delete:** Click "⋯" → "Delete"

---

## 📊 Render Free Tier Limits

| Feature | Limit |
|---------|-------|
| **Run Hours/Month** | 750 hours (enough for 24/7) |
| **Memory** | 0.5 GB |
| **CPU** | 2 vCPU (shared) |
| **Auto-sleep** | Yes (15 min idle) |
| **Custom Domain** | Not on free tier |

### Note on Auto-Sleep

- Service sleeps after 15 minutes of inactivity
- First request wakes it up (~30 seconds)
- **Solution:** Add monitoring to keep alive

---

## 🔄 Auto-Deployment

Every time you push to `main` branch:

```bash
git add .
git commit -m "Your message"
git push origin main
```

Render automatically:

1. Detects the push
2. Pulls latest code
3. Builds Docker image
4. Deploys new version
5. Zero downtime (rolling restart)

---

## 📈 Upgrade Path (Later)

### When You Need More

- **More uptime:** Use Render Pro ($7/month) - no auto-sleep
- **More power:** Increase instance size ($20+/month)
- **High availability:** Multiple instances ($50+/month)
- **Production:** Move to GCP Cloud Run (pay-per-use)

---

## 🆘 Troubleshooting

### Service fails to build

**Check logs:**

1. Go to "Events" tab
2. Look for error messages
3. Ensure `Dockerfile` exists in repo root

### Port binding error

**Solution:** Render automatically sets PORT=10000

- Update logs show: "Listening on port 10000"
- This is normal - Render routes traffic

### Service crashes after startup

**Check logs for:**

- Missing environment variables
- Database connection issues
- Memory limits (increase if needed)

### Out of memory

- **Fix:** Upgrade to Pro tier ($7/month)
- **Or:** Optimize application

---

## ✅ Success Checklist

- [ ] Render account created
- [ ] Repository connected
- [ ] Service deployed
- [ ] Logs showing "live"
- [ ] Health check passing
- [ ] Can access https://supremeai.onrender.com

---

## 🎉 You're Live

Once deployed, share your SupremeAI instance:

```
https://supremeai.onrender.com
```

Next, you can:

1. **Monitor:** Add alerts and logging
2. **Optimize:** Reduce startup time
3. **Scale:** Upgrade plan as needed
4. **Production:** Move to GCP Cloud Run

---

**Ready? Head to https://render.com and start the deployment! 🚀**
