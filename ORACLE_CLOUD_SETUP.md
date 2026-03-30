# ☁️ Oracle Cloud (OCI) Setup Guide - SupremeAI

এই গাইডটি আপনাকে ওরাকেল ক্লাউডে SupremeAI সিস্টেম ডেপ্লয় করতে সাহায্য করবে।

---

## 🛠️ Step 1: ওরাকেল ক্লাউড অ্যাকাউন্ট ও CLI
1. **OCI অ্যাকাউন্ট:** [oracle.com/cloud/free](https://www.oracle.com/cloud/free/) এ গিয়ে অ্যাকাউন্ট খুলুন।
   - **Home Region:** আপনার জন্য **Singapore** সবথেকে ভালো (স্ক্রিনশট অনুযায়ী)।
   - **Account Name:** `niloyjoy7`
2. **OCI CLI ইনস্টল (Windows):**
   ```powershell
   (New-Object System.Net.WebClient).DownloadFile('https://raw.githubusercontent.com/oracle/oci-cli/master/scripts/install/install.ps1', 'install.ps1');
   .\install.ps1
   ```
3. **কনফিগারেশন:**
   ```powershell
   oci setup config
   ```
   এখানে আপনার `User OCID`, `Tenancy OCID`, এবং `Region` দিতে হবে (OCI Console এর Profile সেকশনে পাবেন)।

---

## 🏗️ Step 2: ইনফ্রাস্ট্রাকচার তৈরি

### Compute Instance (VM) তৈরি:
SupremeAI রান করার জন্য একটি "Always Free" ARM Instance (4 OCPUs, 24GB RAM) বেছে নিন।
- **Image:** Oracle Linux 8 বা Ubuntu 22.04
- **Shape:** VM.Standard.A1.Flex (ARM Ampere)
- **💡 প্রো-টিপ:** সিঙ্গাপুর রিজিয়নে অনেক সময় ARM খালি থাকে না, সেক্ষেত্রে কয়েকবার চেষ্টা করতে হতে পারে অথবা regular AMD instance ব্যবহার করতে পারেন।

### ভেরিয়েবল সেটআপ:
```bash
# জাভা ১৭ ইনস্টল (VM এর ভেতরে)
sudo dnf install java-17-openjdk -y

# Docker ইনস্টল
sudo dnf config-manager --add-repo=https://download.docker.com/linux/centos/docker-ce.repo
sudo dnf install docker-ce docker-ce-cli containerd.io -y
sudo systemctl start docker
```

---

## 📦 Step 3: Container Registry (OCIR)
ওরাকেল ক্লাউডে আপনার ডকার ইমেজ রাখার জন্য:
1. OCI Console -> Developer Services -> **Container Registry**.
2. একটি Repository তৈরি করুন (নাম দিন: `supremeai/core`).
3. ডকার লগইন:
   ```bash
   # সিঙ্গাপুরের জন্য region-key সাধারণত 'ap-singapore-1'
   docker login ap-singapore-1.ocir.io
   # Username: <tenancy-namespace>/<username>
   # Password: <Auth Token>
   ```

---

## 🚀 Step 4: ডেপ্লয়মেন্ট কমান্ডস

আপনার লোকাল মেশিন থেকে ইমেজ পুশ করুন:
```powershell
# ইমেজ বিল্ড
docker build -t ap-singapore-1.ocir.io/<namespace>/supremeai:v1 .

# ওরাকেল ক্লাউডে পুশ
docker push ap-singapore-1.ocir.io/<namespace>/supremeai:v1
```

---

## 🛡️ Step 5: সিকিউরিটি রুলস (VCN)
OCI কনসোলে গিয়ে আপনার **Security List** এ পোর্ট `8080` এবং `443` ওপেন করতে হবে যাতে আপনার অ্যাডমিন অ্যাপ ক্লাউডের সাথে কথা বলতে পারে।

---

**নোট:** অ্যাকাউন্ট ভেরিফিকেশনের জন্য আপনার ইমেইল এবং পেমেন্ট কার্ড (যদি চায়) প্রস্তুত রাখুন। 🚀
