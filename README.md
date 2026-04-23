# SupremeAI - Multi-Agent App Generator System

**Status: ⚠️ Alpha (In Active Development)**

## 🌍 Language / ভাষা

[English](#introduction) | [বাংলা](#introduction-bengali)

---

<a id="introduction"></a>

### 🇬🇧 Introduction (English)

Welcome to SupremeAI! This repository contains a comprehensive multi-agent system for automated Android app generation. The platform is currently in **Alpha** phase and under active development. Expect breaking changes and incomplete features.

> **⚠️ Production URL Changed**  
> Old URL (`https://supremeai-565236080752.us-central1.run.app`) is no longer active.  
> New URL: [https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html](https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html)

---

<a id="introduction-bengali"></a>

### 🇧🇩 পরিচিতি (বাংলা)

সুপ্রিমএআই-এ স্বাগতম! এই রিপোজিটরিতে অ্যান্ড্রয়েড অ্যাপ স্বয়ংক্রিয় তৈরির জন্য একটি ব্যাপক মাল্টি-এজেন্ট সিস্টেম রয়েছে। এই প্ল্যাটফর্ম বর্তমানে **আলফা** পর্যায়ে আছে এবং সক্রিয়ভাবে উন্নয়ন চলছে। ভেঙে যাওয়ার পরিবর্তন এবং অসম্পূর্ণ বৈশিষ্ট্যের আশা করুন।

> **⚠️ প্রোডাকশন URL পরিবর্তন**  
> পুরোনো URL (`https://supremeai-565236080752.us-central1.run.app`) এখন ব্যবহার করা যাচ্ছে না।  
> নতুন URL: [https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html](https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html)

## 📊 Feature Status / বৈশিষ্ট্যের অবস্থা

### English Version

| Feature | Status | Notes |
|---------|--------|-------|
| **Authentication** | ✅ Working | Basic auth infrastructure implemented and functional |
| **Admin Dashboard** | ✅ Working | Available at `/admin.html` and localhost:8001 |
| **Backend API** | ✅ Working | Spring Boot controllers, agent orchestration, learning system |
| **Multi-Agent System** | ❌ Pending | X-Builder, Z-Architect roles not fully implemented |
| **Android App Generator** | ❌ Pending | Core pipeline exists, needs end-to-end verification |
| **IntelliJ Plugin** | ✅ Working | K2 mode analysis implemented, v1.2.0 built successfully |
| **VS Code Extension** | ❌ Pending | Extension scaffolded, needs completion |
| **Provider Coverage** | ⚠️ Partial | 11 providers supported - configure keys in `application.properties` |
| **Self-Healing/Resilience** | ⚠️ Partial | Circuit breakers, recovery config, watchdog health checks exist |
| **ML/Analytics** | ⚠️ Partial | Vector database, prediction models, analytics controllers present |
| **K8s/Docker Deployment** | ✅ Available | Dockerfile, cloudbuild.yaml, k8s-service.yaml configured |

### বাংলা ভার্সন

| বৈশিষ্ট্য | অবস্থা | বিবরণ |
|-----------|---------|----------|
| **প্রমাণীকরণ** | ✅ কাজ করছে | বেসিক অথ ইনফ্রাস্ট্রাকচার বাস্তবায়িত এবং কার্যকর |
| **অ্যাডমিন ড্যাশবোর্ড** | ✅ কাজ করছে | `/admin.html` এবং localhost:8001 এ উপলব্ধ |
| **ব্যাকএন্ড API** | ✅ কাজ করছে | স্প্রিং বুট কন্ট্রোলার, এজেন্ট অর্কেস্ট্রেশন, লার্নিং সিস্টেম |
| **মাল্টি-এজেন্ট সিস্টেম** | ❌ অপেক্ষমাণ | X-Builder, Z-Architect ভূমিকা পুরোপুরি বাস্তবায়িত নয় |
| **অ্যান্ড্রয়েড অ্যাপ জেনারেটর** | ❌ অপেক্ষমাণ | কোর পাইপলাইন আছে, এন্ড-টু-এন্ড যাচাই প্রয়োজন |
| **ইন্টেলিজেল প্লাগইন** | ✅ কাজ করছে | K2 মোড এনালাইসিস বাস্তবায়িত, v1.2.0 সফলভাবে তৈরি |
| **ভিএস কোড এক্সটেনশন** | ❌ অপেক্ষমাণ | এক্সটেনশন স্ক্যাফোল্ড করা হয়েছে, সম্পূর্ণ করতে হবে |
| **প্রোভাইডার কাভারেজ** | ⚠️ আংশিক | 11 প্রোভাইডার সমর্থিত - `application.properties`-এ কী কনফিগার করতে হবে |
| **সেল্ফ-হিলিং/দুর্বলতা প্রতিরোধ** | ⚠️ আংশিক | সার্কিট ব্রেকার, রিকভারি কনফিগ, ওয়াচডগ হেলথ চেক বিদ্যমান |
| **ML/এনালিটিক্স** | ⚠️ আংশিক | ভেক্টর ডেটাবেস, প্রেডিকশন মডেল, এনালিটিক্স কন্ট্রোলার উপস্থিত |
| **K8s/ডকার ডিপ্লয়মেন্ট** | ✅ উপলব্ধ | Dockerfile, cloudbuild.yaml, k8s-service.yaml কনফিগার করা আছে |

## 🌐 Environments / পরিবেশ

### English Version

The system has a production endpoint (for testing only) and a local development environment:

- **Production (Test):** [https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html](https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html)
- **Local Development:** Follow setup instructions below.

> ℹ️ **Note:** The old production URL (`https://supremeai-565236080752.us-central1.run.app`) has been decommissioned. Please update any bookmarks or links.

### বাংলা ভার্সন

সিস্টেমের একটি প্রোডাকশন এন্ডপয়েন্ট (শুধুমাত্র পরীক্ষার জন্য) এবং একটি স্থানীয় ডেভেলপমেন্ট এনভায়রনমেন্ট রয়েছে:

- **প্রোডাকশন (পরীক্ষা):** [https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html](https://supremeai-lhlwyikwlq-uc.a.run.app/admin.html)
- **স্থানীয় ডেভেলপমেন্ট:** নিচের সেটআপ নির্দেশাবলী অনুসরণ করুন।

> ℹ️ **নোট:** পুরোনো প্রোডাকশন URL (`https://supremeai-565236080752.us-central1.run.app`) এখন ব্যবহার বন্ধ করা দেয়া হয়েছে। অনুগ্রহ করে আপনার বুকমার্ক বা লিঙ্ক আপডেট করুন।

## 📚 Documentation / ডকুমেন্টেশন

### English Version

All documentation is organized into the `docs_new/` directory:

- **[Documentation Index](docs_new/README.md)** - Main documentation hub
- **[API Endpoints](docs_new/guides/API_ENDPOINTS.md)** - Complete REST endpoint reference
- **[Multi-Agent System](docs_new/architecture/MULTI_AGENT_SYSTEM.md)** - Agent architecture and status
- **[Provider Coverage](docs_new/guides/PROVIDER_COVERAGE.md)** - AI provider configuration guide
- **[IDE Plugins](docs_new/guides/IDE_PLUGINS_STATUS.md)** - VS Code & IntelliJ plugin status
- **[URL Redirects](docs_new/guides/URL_REDIRECT_SETUP.md)** - Old URL migration guide
- **[Contributing Guide](docs_new/guides/12-GUIDES/CONTRIBUTING.md)** - How to contribute

### Documentation Structure

| Directory | Purpose |
|-----------|---------|
| `docs_new/architecture/` | System design, technical specifications, ADRs |
| `docs_new/guides/` | User guides, setup, tutorials, contributing |
| `docs_new/reports/` | Progress reports, verification, analytics |
| `docs_new/troubleshooting/` | Debugging guides, common issues |
| `docs_new/workflow/` | Project management, planning, processes |

> 📁 **Note:** Documentation has been restructured from `docs/` to `docs_new/` for better organization.

### বাংলা ভার্সন

সব ডকুমেন্টেশন `docs_new/` ডিরেক্টরিতে সংগঠিত করা হয়েছে:

- **[ডকুমেন্টেশন ইনডেক্স](docs_new/README.md)** - মূল ডকুমেন্টেশন হাব
- **[API এন্ডপয়েন্টস](docs_new/guides/API_ENDPOINTS.md)** - সম্পূর্ণ REST এন্ডপয়েন্ট রেফারেন্স
- **[মাল্টি-এজেন্ট সিস্টেম](docs_new/architecture/MULTI_AGENT_SYSTEM.md)** - এজেন্ট আর্কিটেকচার এবং স্ট্যাটাস
- **[প্রোভাইডার কাভারেজ](docs_new/guides/PROVIDER_COVERAGE.md)** - AI প্রোভাইডার কনফিগারেশন গাইড
- **[আইডিই প্লাগইনস](docs_new/guides/IDE_PLUGINS_STATUS.md)** - VS Code এবং IntelliJ প্লাগইন স্ট্যাটাস
- **[URL রিডাইরেক্ট](docs_new/guides/URL_REDIRECT_SETUP.md)** - পুরোনো URL মাইগ্রেশন গাইড
- **[কন্ট্রিবিউটিং গাইড](docs_new/guides/12-GUIDES/CONTRIBUTING.md)** - কিভাবে অবদান রাখবেন

### ডকুমেন্টেশন স্ট্রাকচার

| ডিরেক্টরি | উদ্দেশ্য |
|-----------|----------|
| `docs_new/architecture/` | সিস্টেম ডিজাইন, টেকনিক্যাল স্পেসিফিকেশন, ADRs |
| `docs_new/guides/` | ইউজার গাইড, সেটআপ, টিউটোরিয়াল, অবদান রাখা |
| `docs_new/reports/` | প্রগ্রেস রিপোর্ট, ভেরিফিকেশন, এনালিটিক্স |
| `docs_new/troubleshooting/` | ডিবাগিং গাইড, সাধারণ সমস্যা |
| `docs_new/workflow/` | প্রজেক্ট ম্যানেজমেন্ট, প্ল্যানিং, প্রসেস |

> 📁 **নোট:** ভালো সংগঠনের জন্য ডকুমেন্টেশন `docs/` থেকে `docs_new/`-এ স্থানান্তরিত হয়েছে।

## 🚀 Quick Start / দ্রুত শুরু

### English Version

1. Clone the repository
2. Set up Firebase credentials (see [Setup Guide](docs_new/guides/01-SETUP-DEPLOYMENT/GOOGLE_CLOUD_DEPLOYMENT.md))
3. Configure API keys in `src/main/resources/application.properties` or via environment variables
4. Run the app: `./gradlew bootRun`
5. Access admin dashboard at `http://localhost:8001`

**Quick Links:**

- [API Endpoints](docs_new/guides/API_ENDPOINTS.md) - All available REST endpoints
- [Provider Setup](docs_new/guides/PROVIDER_COVERAGE.md) - Configure AI providers
- [IDE Plugins](docs_new/guides/IDE_PLUGINS_STATUS.md) - VS Code & IntelliJ setup
- [URL Redirect Setup](docs_new/guides/URL_REDIRECT_SETUP.md) - Old URL migration
- [Bengali Setup Guide](docs_new/guides/BENGALI_SETUP_GUIDE.md) - সেটআপ নির্দেশাবলী (বাংলা)
- [Bengali User Guide](docs_new/guides/BENGALI_USER_GUIDE.md) - ব্যবহারকারী গাইড (বাংলা)
- [Performance Optimization](docs_new/guides/PERFORMANCE_OPTIMIZATION.md) - পারফরম্যান্স অপটিমাইজেশন

### বাংলা ভার্সন / দ্রুত শুরু

1. রিপোজিটরি ক্লোন করুন
2. ফায়ারবেস ক্রেডেনশিয়াল সেটআপ করুন ([সেটআপ গাইড](docs_new/guides/01-SETUP-DEPLOYMENT/GOOGLE_CLOUD_DEPLOYMENT.md) দেখুন)
3. `src/main/resources/application.properties` বা এনভায়রনমেন্ট ভেরিয়েবলে API কীগুলো কনফিগার করুন
4. অ্যাপ চালান: `./gradlew bootRun`
5. অ্যাডমিন ড্যাশবোর্ডে প্রবেশ করুন: `http://localhost:8001`

**দ্রুত লিংকসমূহ:**

- [API এন্ডপয়েন্টস](docs_new/guides/API_ENDPOINTS.md) - সমস্ত REST এন্ডপয়েন্ট
- [প্রোভাইডার সেটআপ](docs_new/guides/PROVIDER_COVERAGE.md) - AI প্রোভাইডার কনফিগারেশন
- [আইডিই প্লাগইনস](docs_new/guides/IDE_PLUGINS_STATUS.md) - VS Code এবং IntelliJ সেটআপ
- [URL রিডাইরেক্ট সেটআপ](docs_new/guides/URL_REDIRECT_SETUP.md) - পুরোনো URL মাইগ্রেশন

## 🔧 Development / উন্নয়ন

### English Version

See [Contributing Guide](docs_new/guides/12-GUIDES/CONTRIBUTING.md) for development guidelines and contribution procedures.

### Quick Commands

```bash
# Build (skip tests)
./gradlew clean build -x test

# Run tests
./gradlew test

# Run locally
./gradlew bootRun
```

**Related Documentation:**

- [IDE Plugins](docs_new/guides/IDE_PLUGINS_STATUS.md) - VS Code & IntelliJ setup
- [Multi-Agent System](docs_new/architecture/MULTI_AGENT_SYSTEM.md) - Agent architecture
- [Contributing Guide](docs_new/guides/12-GUIDES/CONTRIBUTING.md) - Full development guidelines
- [Performance Optimization](docs_new/guides/PERFORMANCE_OPTIMIZATION.md) - Optimization strategies

### 🌍 Internationalization (i18n)

The application now supports bilingual interface (English/Bengali):

- **Language Files:** `src/main/resources/messages_en.properties`, `messages_bn.properties`
- **Admin Dashboard:** Language switcher in top-right corner
- **Performance Dashboard:** Available at `/performance-dashboard.html`

### বাংলা ভার্সন / উন্নয়ন

ভাষা পরিবর্তনের জন্য [CONTRIBUTING গাইড](docs_new/guides/12-GUIDES/CONTRIBUTING.md) দেখুন।

### দ্রুত কমান্ডসমূহ

```bash
# বিল্ড করুন (টেস্ট ছাড়া)
./gradlew clean build -x test

# টেস্ট চালান
./gradlew test

# স্থানীয়ভাবে চালান
./gradlew bootRun
```

**সম্পর্কিত ডকুমেন্টেশন:**

- [আইডিই প্লাগইনস](docs_new/guides/IDE_PLUGINS_STATUS.md) - VS Code এবং IntelliJ সেটআপ
- [মাল্টি-এজেন্ট সিস্টেম](docs_new/architecture/MULTI_AGENT_SYSTEM.md) - এজেন্ট আর্কিটেকচার
- [কন্ট্রিবিউটিং গাইড](docs_new/guides/12-GUIDES/CONTRIBUTING.md) - সম্পূর্ণ উন্নয়ন নির্দেশাবলী
- [পারফরম্যান্স অপটিমাইজেশন](docs_new/guides/PERFORMANCE_OPTIMIZATION.md) - অপটিমাইজেশন কৌশল

### 🌐 আন্তর্জাতিকীকরণ (i18n)

অ্যাপ্লিকেশন এখন দ্বিভাষী ইন্টারফেস সমর্থন করে (ইংরেজি/বাংলা):

- **ভাষা ফাইল:** `src/main/resources/messages_en.properties`, `messages_bn.properties`
- **অ্যাডমিন ড্যাশবোর্ড:** ডান উপরের কোণে ভাষা পরিবর্তন অপশন
- **পারফরম্যান্স ড্যাশবোর্ড:** `/performance-dashboard.html` এ উপলব্ধ

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
