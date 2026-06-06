# src/main/java/com/supremeai প্যাকেজগুলোর গাইড

## প্যাকেজগুলোর বিবরণ

### কোর প্যাকেজ

| প্যাকেজ                    | ব্যবহার              |
| -------------------------- | -------------------- |
| `com.supremeai.service`    | ব্যবসায়িক লজিক      |
| `com.supremeai.controller` | REST API কন্ট্রোলার  |
| `com.supremeai.repository` | ডাটাবেস অপারেশন      |
| `com.supremeai.config`     | অ্যাপ্লিকেশন কনফিগ   |
| `com.supremeai.model`      | ডাটা মডেল            |
| `com.supremeai.dto`        | ডাটা টansfer অবজেক্ট |
| `com.supremeai.exception`  | একসেপশন হ্যান্ডলিং   |

### সিকিউরিটি ও অথেন্টিকেশন

| প্যাকেজ                            | ব্যবহার             |
| ---------------------------------- | ------------------- |
| `com.supremeai.security`           | সিকিউরিটি কনফিগ     |
| `com.supremeai.security.ratelimit` | রেট লিমিটিং         |
| `com.supremeai.filter`             | অনুরোধ ফিল্টার      |
| `com.supremeai.interceptor`        | অনুরোধ ইন্টারসেপ্টর |

### AI/ML মডিউল

| প্যাকেজ                            | ব্যবহার              |
| ---------------------------------- | -------------------- |
| `com.supremeai.ml`                 | মেশিন লার্ণিং        |
| `com.supremeai.agentorchestration` | এজেন্ট অর্কেস্ট্রেশন |
| `com.supremeai.intelligence`       | এআই ইন্টেলিজেন্স     |
| `com.supremeai.learning`           | লার্নিং সিস্টেম      |

### অপারেশনাল প্যাকেজ

| প্যাকেজ                    | ব্যবহার                |
| -------------------------- | ---------------------- |
| `com.supremeai.websocket`  | রিয়াল-টাইম কমিউনিকেশন |
| `com.supremeai.event`      | ইভেন্ট হ্যান্ডলিং      |
| `com.supremeai.middleware` | মিডিডলওয়্যার          |
| `com.supremeai.fallback`   | ফলব্যাক ম্যানেজমেন্ট   |
| `com.supremeai.deployment` | ডিপ্লোয়মেন্ট মনিটরিং  |

## ডাটাবেস কনফিগারেশন

### Firestore কনশেপ্ট

- কলেকশনগুলো `config/firestore.indexes.json` এ ডিফাইন করা
- সিকিউরিটি রুলস `config/firestore.rules` এ

### Firebase Data Connect

- জেনারেটেড কোড `src/dataconnect-admin-generated/` এ আছে
