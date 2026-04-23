# সুপ্রিমএআই সেটআপ ও ব্যবহারের নির্দেশাবলী (বাংলা)

## 📋 সংক্ষিপ্ত বিবরণ

সুপ্রিমএআই হলো একটি মাল্টি-এজেন্ট সিস্টেম যা স্বয়ংক্রিয়ভাবে অ্যান্ড্রয়েড অ্যাপ তৈরি করে। এই গাইডে সেটআপ ও ব্যবহারের সহজ পদক্ষেপগুলো বর্ণনা করা হলো।

## 🚀 দ্রুত শুরু করা

### পূর্বশর্তসমূহ

- **জাভা (JDK 17+)** - [ডাউনলোড](https://www.oracle.com/java/technologies/downloads/)
- **গ্রেডল (Gradle 8.x)** - [ডাউনলোড](https://gradle.org/)
- **গিট** - [ডাউনলোড](https://git-scm.com/)
- **ফায়ারবেস অ্যাকাউন্ট** (ঐচ্ছিক)

### ধাপ ১: রিপোজিটরি ক্লোন করুন

```bash
git clone https://github.com/your-org/supremeai.git
cd supremeai
```

### ধাপ ২: নির্ভরতাগুলো ডাউনলোড করুন

```bash
./gradlew wrapper
```

### ধাপ ৩: API কীগুলো কনফিগার করুন

`src/main/resources/application.properties` ফাইলটি সম্পাদনা করুন:

```properties
# OpenAI API Key (ঐচ্ছিক)
OPENAI_API_KEY=sk-your-key-here

# Anthropic API Key (ঐচ্ছিক)
ANTHROPIC_API_KEY=sk-ant-your-key-here

# Google API Key (ঐচ্ছিক)
GOOGLE_API_KEY=your-google-key-here

# স্থানীয় ডেভেলপমেন্টের জন্য
spring.profiles.active=local
```

### ধাপ ৪: অ্যাপ্লিকেশন চালান

```bash
# ডেভেলপমেন্ট মোডে চালান
./gradlew bootRun
```

অ্যাপ্লিকেশন শুরু হলে এটি এভাবে পাওয়া যাবে:

- **অ্যাডমিন ড্যাশবোর্ড:** http://localhost:8001
- **API ডকুমেন্টেশন:** http://localhost:8001/swagger-ui.html

### ধাপ ৫: অ্যাডমিন ড্যাশবোর্ডে প্রবেশ করুন

ব্রাউজার খুলুন এবং এ লিঙ্কটি দিন: `http://localhost:8001/admin.html`

## 📊 বৈশিষ্ট্য ও ব্যবহার

### ১. অ্যান্ড্রয়েড অ্যাপ জেনারেটর

1. অ্যাডমিন ড্যাশবোর্ডে "New App" বোতামে ক্লিক করুন
2. আপনার অ্যাপের নাম ও বর্ণনা লিখুন
3. প্রয়োজনীয় ফিচারগুলো নির্বাচন করুন
4. "Generate" বোতামে ক্লিক করুন
5. সিস্টেম স্বয়ংক্রিয়ভাবে কোড তৈরি করবে

### ২. মাল্টি-এজেন্ট সিস্টেম

সিস্টেমে অনেকগুলো এজেন্ট রয়েছে যারা একসাথে কাজ করে:

- **X-Builder:** কোড জেনারেট করে এবং বিল্ড করে
- **Z-Architect:** সিস্টেম আর্কিটেকচার ডিজাইন করে
- **ExpertAgentRouter:** সঠিক এজেন্টকে কাজ দেয়

### ৩. AI প্রোভাইডার সমর্থন

সিস্টেমে ১১টি AI প্রোভাইডার সমর্থিত:

| প্রোভাইডার | ধরণ |
|------------|------|
| OpenAI | ক্লাউড API |
| Anthropic | ক্লাউড API |
| Google Gemini | ক্লাউড API |
| Groq | ক্লাউড API |
| HuggingFace | ক্লাউড API |
| Ollama | স্থানীয় |

## 🔧 সাধারণ কমান্ডসমূহ

```bash
# পুরো প্রজেক্ট বিল্ড করুন
./gradlew clean build

# টেস্টগুলো চালান
./gradlew test

# শুধু কম্পাইল করুন
./gradlew compileJava

# ডকুমেন্টেশন জেনারেট করুন
./gradlew javadoc

# ক্লিনআপ করুন
./gradlew clean
```

## 🐛 সাধারণ সমস্যা ও সমাধান

### সমস্যা ১: পোর্ট ৮০০১ ইতিমধ্যে ব্যবহৃত হচ্ছে

**সমাধান:**

```properties
# application.properties-এ পোর্ট পরিবর্তন করুন
server.port=8081
```

### সমস্যা ২: API Key ত্রুটি

**সমাধান:**

- `application.properties`-এ সঠিক API কী লিখুন
- এনভায়রনমেন্ট ভেরিয়েবল সঠিকভাবে সেট করুন

### সমস্যা ৩: গ্রেডল ত্রুটি

**সমাধান:**

```bash
# গ্রেডল ক্যাশে পরিষ্কার করুন
./gradlew clean
./gradlew --refresh-dependencies
```

## 📁 প্রজেক্ট স্ট্রাকচার

```
supremeai/
├── src/
│   ├── main/
│   │   ├── java/          # জাভা সোর্স কোড
│   │   └── resources/     # কনফিগারেশন ফাইল
├── docs_new/              # ডকুমেন্টেশন
├── admin/                 # অ্যাডমিন প্যানেল
└── build/                 # বিল্ড আউটপুট
```

## 🌐 প্রোডাকশন ডিপ্লয়মেন্ট

### অপশন ১: ডকার ব্যবহার করে

```bash
# ডকার ইমেজ তৈরি করুন
docker build -t supremeai .

# কন্টেইনার চালান
docker run -p 8001:8001 supremeai
```

### অপশন ২: কুবারনেটিস ব্যবহার করে

```bash
# k8s-service.yaml ব্যবহার করে ডিপ্লয় করুন
kubectl apply -f k8s-service.yaml
```

## 📚 আরও জানতে

- [API এন্ডপয়েন্টস](API_ENDPOINTS.md)
- [মাল্টি-এজেন্ট সিস্টেম](architecture/MULTI_AGENT_SYSTEM.md)
- [প্রোভাইডার কাভারেজ](PROVIDER_COVERAGE.md)
- [কন্ট্রিবিউটিং গাইড](12-GUIDES/CONTRIBUTING.md)

## 🤝 সহায়তা ও সমর্থন

- **ইস্যু রিপোর্ট:** [GitHub Issues](https://github.com/your-org/supremeai/issues)
- **ডিসকাস:** [GitHub Discussions](https://github.com/your-org/supremeai/discussions)
- **ইমেইল:** support@supremeai.example.com

## 📄 লাইসেন্স

এই প্রজেক্টটি MIT লাইসেন্সের অধীনে প্রকাশিত। বিস্তারিত জানতে [LICENSE](../../LICENSE) দেখুন।

---

**দ্রষ্টব্য:** এই প্রজেক্টটি বর্তমানে **আলফা** পর্যায়ে আছে, তাই কিছু বৈশিষ্ট্য অপূর্ণ থাকতে পারে। ধৈর্য্য ও সহযোগিতার জন্য ধন্যবাদ! 🙏
