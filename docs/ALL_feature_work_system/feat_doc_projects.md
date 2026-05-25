# 📂 Feature: Multi-Project VCS Manager (`AdminProjects.tsx`)

> **[CLASSIFICATION: PROJECT MANAGEMENT]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminProjects.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **Active Projects Grid:** সিস্টেমে লোড থাকা প্রোজেক্টগুলোর লোগো ও বর্ণনা কার্ড গ্রিড।
    *   **Create Project Modal:** নতুন প্রোজেক্ট ইনিশিয়ালাইজ করার পপআপ উইন্ডো।
    *   **Delete & Switch Actions:** সক্রিয় প্রোজেক্ট ডিলিট বা অন্য প্রোজেক্টে ফোকাস সুইচ করার কন্ট্রোল।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `RouterController.java` / `api-router.js`
*   **প্রধান API রাউট:** `GET /api/projects`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "projects": [
        { "id": "PRJ_001", "name": "supremeai", "path": "/home/.../supremeai" }
      ]
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ড্যাশবোর্ড প্রোজেক্ট ট্যাব খুললে ব্যাকএন্ড থেকে সক্রিয় প্রজেক্টের লিস্ট আনা হয়।
2. ব্যাকএন্ড ফায়ারবেস বা লোকাল কনফিগ থেকে প্রোজেক্টগুলোর ফাইল পাথ রিড করে রেসপন্স দেয়।
3. ড্যাশবোর্ডে প্রোজেক্টগুলোর গিট ব্রাঞ্চ ও ডিরেক্টরি লোকেশন সুন্দর কলাম আকারে রেন্ডার হয়।
