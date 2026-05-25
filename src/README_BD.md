# src/ ফোল্ডার গাইড

## উদ্দেশ্য

এই ফোল্ডারটি SupremeAI-র মূল সোর্স কোড ধারণ করে, যা স্প্রিং বুট ব্যাকএন্ড অ্যাপ্লিকেশনকে কভার করে।

## ফোল্ডার স্ট্রাকচার

```
src/
├── main/       # মূল অ্যাপ্লিকেশন কোড
│   ├── java/   # জাভা সোর্স
│   └── resources/  # রিসোর্সস
└── test/       # টেস্ট কোড
```

## মূল প্যাকেজগুলো

| প্যাকেজ | বিবরণ |
|--------|------|
| `com.supremeai.service` | ব্যবসায়িক লজিক |
| `com.supremeai.controller` | REST API কন্ট্রোলার |
| `com.supremeai.repository` | ডাটাবেস রেপোজিটরি |
| `com.supremeai.config` | অ্যাপ্লিকেশন কনফিগ |
| `com.supremeai.security` | সিকিউরিটি মডিউল |
| `com.supremeai.ml` | মেশিন লার্নিং মডিউল |
| `com.supremeai.agentorchestration` | এজেন্ট অর্কেস্ট্রেশন |

## মূল ক্লাস

- `SupremeaiApplication.java` - অ্যাপ্লিকেশন এন্ট্রি পয়েন্ট
- `Application.java` - স্প্রিং অ্যাপ্লিকেশন কনফিগ

## বিল্ড করা

```bash
# গ্রেডল বিল্ড
./gradlew build
```

## ডিবাগ করা

```bash
# ডিবাগ মোডে রান
./gradlew bootRun --debug
```