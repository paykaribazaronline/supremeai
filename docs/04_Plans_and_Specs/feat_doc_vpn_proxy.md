# 🛡️ Feature: VPN Proxy IP Tunnel Control (`AdminVPN.tsx`)

> **Status:** 🟢 Updated for v5 Architecture


> **[CLASSIFICATION: FAILOVER IP CONTROL]**  
> **DEVELOPMENT STATUS: PARTIALLY COMPLETED**  

---

## 🎨 ১. ফ্রন্টএন্ড আর্কিটেকচার (Frontend UI)
*   **কম্পোনেন্ট:** `dashboard/src/pages/AdminVPN.tsx`
*   **ভিজুয়াল উইজেট:**
    *   **IP Connection Status Map:** প্রক্সি আইপি বা কান্ট্রি লোকেশনের স্ট্যাটাস ম্যাপ।
    *   **Dynamic IP Rotation Switch:** ভিপিএন বা টানেল রোটেট করার বাটন।
    *   **Proxy Failover logs:** নেটওয়ার্ক ফেইলওভার ও ট্র্যাফিক লগের প্রজেকশন এরিয়া।

---

## ⚙️ ২. ব্যাকএন্ড আর্কিটেকচার (Backend Controller & API)
*   **কন্ট্রোলার:** `FailoverController.java`
*   **প্রধান API রাউট:** `POST /api/vpn/rotate-ip`
*   **ডেটা ফরম্যাট:**
    ```json
    {
      "success": true,
      "proxyIp": "194.22.45.109",
      "country": "Netherlands",
      "latencyMs": 112
    }
    ```

---

## 🔄 ৩. সম্পূর্ণ কাজের প্রসেস (Step-by-Step Data Flow)
1. ইউজার ড্যাশবোর্ডে `Rotate IP Proxy` বোতামে চাপ দেন।
2. ব্যাকএন্ডের ফেইলওভার কন্ট্রোলার ভিপিএন টানেল এপিআই কল করে আইপি চেঞ্জ করে।
3. চেঞ্জ সাকসেস হলে নতুন আইপি অ্যাড্রেস ও লোকেশন ম্যাপ ড্যাশবোর্ডে লাইভ রেন্ডার হয়।
