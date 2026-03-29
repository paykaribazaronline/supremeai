# ☁️ Oracle Cloud (OCI) Setup Guide - SupremeAI

এই গাইডটি আপনাকে ওরাকেল ক্লাউডে SupremeAI সিস্টেম ডেপ্লয় করতে সাহায্য করবে।

---

## 🛠️ Step 1: ওরাকেল ক্লাউড অ্যাকাউন্ট ও CLI
1. **OCI অ্যাকাউন্ট:** [oracle.com/cloud/free](https://www.oracle.com/cloud/free/) এ গিয়ে একটি অ্যাকাউন্ট খুলুন।
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
- **Shape:** VM.Standard.A1.Flex

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
   docker login <region-key>.ocir.io
   # Username: <tenancy-namespace>/<username>
   # Password: <Auth Token (User settings থেকে জেনারেট করা)>
   ```

---

## 🚀 Step 4: ডেপ্লয়মেন্ট কমান্ডস

আপনার লোকাল মেশিন থেকে ইমেজ পুশ করুন:
```powershell
# ইমেজ বিল্ড
docker build -t <region>.ocir.io/<namespace>/supremeai:v1 .

# ওরাকেল ক্লাউডে পুশ
docker push <region>.ocir.io/<namespace>/supremeai:v1
```

VM এ গিয়ে রান করুন:
```bash
docker run -d -p 8080:8080 \
  -e FIREBASE_CONFIG_PATH=/app/service-account.json \
  -v $(pwd)/service-account.json:/app/service-account.json \
  <region>.ocir.io/<namespace>/supremeai:v1
```

---

## 🛡️ Step 5: নেটওয়ার্ক সিকিউরিটি (VCN)
OCI কনসোলে গিয়ে আপনার Instance এর **Ingress Rules** এ পোর্ট `8080` ওপেন করতে হবে:
- **Source CIDR:** `0.0.0.0/0`
- **IP Protocol:** `TCP`
- **Destination Port Range:** `8080`

---

## 🎯 সারাংশ
ওরাকেল ক্লাউড ব্যবহার করলে আপনি শক্তিশালী রিসোর্স একদম ফ্রিতে পাবেন। আপনার `CloudDeploymentService.java` ফাইলে এখন OCI সাপোর্ট যোগ করা হচ্ছে।
