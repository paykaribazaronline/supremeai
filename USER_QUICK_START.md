# SupremeAI — Non-Technical User Quick Start
# সুপ্রিম AI — সাধারণ ব্যবহারকারীর জন্য কুইক স্টার্ট গাইড

---

## English — 5-Minute Getting Started

### What can you do here?
You can describe an app idea in plain English (or Bengali), and SupremeAI will **create the code for you** — no programming knowledge needed.

### Step 1 — Just download & open
```
Download or clone: https://github.com/nazifarabbu/supremeai
```

### Step 2 — Run the one-click launcher
```bash
# Open a terminal / command prompt in the project folder
chmod +x start-supremeai.sh
./start-supremeai.sh
```

The script will:
- ✅ Check if Java 21 is installed (you'll get a download link if missing)
- ✅ Launch a GUI wizard if you don't have GCP credentials (easy drag-and-drop setup)
- ✅ Build and start the backend automatically
- ✅ Ask if you want to launch the browser-based dashboard

### Step 3 — Open the dashboard
When the script finishes, open **http://localhost:5173** (or http://localhost:8080) in your browser.

### Step 4 — Generate your first app
1. Click **"Generate New App"**
2. Enter your app name: e.g. `My Shop`
3. Describe what it does: e.g. `Track daily sales and stock for my grocery store`
4. Click **"Build My App Now"**
5. Watch the AI pipeline generate the code in real-time

Done! You'll get source code and a live preview.

---

### Need help with a step?

| Problem | Solution |
|---|---|
| Need Java? | `https://adoptium.net/` → Download JDK 21 → Install → Restart terminal |
| Need Node.js? | `https://nodejs.org/` → Download "LTS" → Install → Restart terminal |
| Firestore credential? | The setup wizard GUI will help you configure it simply. |
| App doesn't generate? | Check `tail -f backend.log` in the terminal for error info |
| Want to try the UI only (no backend)? | Run `./launch-dashboard.sh` → open `http://localhost:3000/admin` |

---

## বাংলা — ৫ মিনিটে শুরু করুন

### এখানে কী করতে পারবেন?
আপনি অ্যাপের ধারণা বাংলায় লিখে দিতে পারেন, এবং SupremeAI স্বয়ংক্রিয়ভাবে অ্যাপের কোড তৈরি করবে — কোনো প্রোগ্রামিং জ্ঞান লাগবে না।

### ধাপ ১ — ডাউনলোড করুন
```bash
https://github.com/nazifarabbu/supremeai ওয়েবসাইট থেকে প্রোজেক্ট ডাউনলোড বা ক্লোন করুন
```

### ধাপ ২ — ওয়ান-ক্লিক লঞ্চার চালান
```bash
# প্রম্পট/টার্মিনাল খুলুন → প্রজেক্ট ফোল্ডারে চলে যান
chmod +x start-supremeai.sh
./start-supremeai.sh
```

স্ক্রিপ্টটি স্বয়ংক্রিয়ভাবে:
- ✅ Java 21 আছে কিনা চেক করবে (না থাকলে ডাউনলোড লিঙ্ক দেবে)
- ✅ ক্রেডেনশিয়াল না থাকলে সহজে সেটআপ করার জন্য একটি GUI উইজার্ড চালু করবে।
- ✅ এরপর স্বয়ংক্রিয়ভাবে ব্যাকএন্ড বিল্ড করে চালু করবে।
- ✅ ড্যাশবোর্ড চালু করতে চান কিনা জিজ্ঞাসা করবে।

### ধাপ ৩ — ড্যাশবোর্ড ওপেন করুন

স্ক্রিপ্ট শেষ হলে ব্রাউজারে `http://localhost:5173` ওপেন করুন।

### ধাপ ৪ — প্রথম অ্যাপ তৈরি করুন

1. **"Generate New App"** বাটনে ক্লিক করুন
2. অ্যাপের নাম দিন (e.g. `আমার দোকান`)
3. অ্যাপ কী কাজ করবে তা লিখুন (e.g. `দৈনিক কেনাবেচা ও স্টক রাখার জন্য`)
4. **"Build My App Now"** বাটনে ক্লিক করুন
5. AI পাইপলাইন কোড তৈরি করতে দেখুন — রিয়েল-টাইমে!

কাজ শেষ! আপনি কোড এবং লাইভ প্রিভিউ পাবেন।

---

### কোনো সমস্যা?

| সমস্যা | সমাধান |
|---|---|
| Java নেই? | https://adoptium.net/ থেকে JDK 21 ইনস্টল করুন |
| Node.js নেই? | https://nodejs.org/ থেকে Auto Install করুন |
| Firestore ক্রেডেনশিয়াল? | সেটআপ উইজার্ড আপনাকে সাহায্য করবে |
| অ্যাপ জেনারেট হয় না? | টার্মিনালে `tail -f backend.log` দেখুন |
