# 🚀 Feature: Automated Cloud Deployment Hub (`AdminDeployment.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: DEVOPS AUTOMATION]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminDeployment.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Deployment Target Selector:** গুগল ক্লাউড হোস্টিং, ভার্সেল বা লোকাল নোড অপশন।
    *   **Cloud Build Console:** রিয়েল-টাইমে চলা ডেপ্লয়মেন্ট বিল্ড লগের প্রজেকশন টার্মিনাল।
    *   **Direct Production URL Link:** ডেপ্লয় শেষ হলে লাইভ অ্যাপ ইউআরএল অ্যাক্সেস লিঙ্ক।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `DeploymentController.java`
*   **প্রধান API রাউট:** `POST /api/deployment/deploy`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "success": true,
      "deployedUrl": "https://supremeai-build.web.app",
      "uptimeSeconds": 0
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার ডিপ্লয় টার্গেট সিলেক্ট করে `LAUNCH BUILD` বোতামে প্রেস করেন।
2. ব্যাকএন্ড শেলে হোস্টিং CLI এবং `npm run build` রান করায়।
3. সাকসেসফুলি রিলিজ হওয়ার পর ইউজার ড্যাশবোর্ডে ডিপ্লয়মেন্ট স্ট্যাটাস ও লিঙ্ক দেখতে পান।
