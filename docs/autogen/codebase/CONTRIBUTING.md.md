# 📄 ফাইল: CONTRIBUTING.md

**প্রকার:** .md  
**সাইজ:** 13,217 বাইট  
**আপডেট:** 2026-07-07T18:37:32.269728

---

## কোড

```md
# Contributing to SupremeAI 2.0 — Phase 2 Guide

**Status**: Phase 1 Complete (Production Readiness Systems), Phase 2 Active (Team Integration)  
**Updated**: July 3, 2026  

এই ডকুমেন্টটি SupremeAI 2.0-তে কাজ করার সম্পূর্ণ গাইড। আমরা ৫টি শক্তিশালী উৎপাদন-তৈরি সিস্টেম স্থাপন করেছি যা আপনার কাজকে নিরাপদ এবং দক্ষ করে তোলে।

## 🚀 দ্রুত শুরু (৫ মিনিট)

```bash
# ১. রেপো ক্লোন করুন
git clone https://github.com/paykaribazaronline/supremeai.git && cd supremeai

# ২. ব্যাকএন্ড সেটআপ করুন
cd backend && poetry install --with dev --without ml && cd ..

# ৩. নিরাপত্তা হুক সেটআপ করুন
python scripts/safety_guard.py --setup-hook

# ৪. পরিবেশ পরীক্ষা করুন
cd backend && pytest tests/test_swarm_orchestrator.py -v
```

## 📋 Phase 1 Systems আপনার জন্য কী করে

আমরা তৈরি করেছি 5টি সিস্টেম যা স্বয়ংক্রিয়ভাবে আপনার কোড রক্ষা করে এবং উন্নত করে:

### ১. 🛡️ Safety Guard — সমালোচনামূলক ফাইল সুরক্ষা

**কী ঘটে**: আপনি যখন সংবেদনশীল ফাইল সম্পাদনা করার চেষ্টা করেন (auth, payment, admin, CI/CD), এই সিস্টেম আপনাকে থামায় এবং অনুমোদনের অনুরোধ করে।

**সুরক্ষিত ফাইল প্যাটার্ন**:
- `**/auth*.py` — অথেন্টিকেশন
- `**/security*.py` — নিরাপত্তা কনফিগ  
- `**/payment*.py` — পেমেন্ট প্রসেসিং
- `**/admin*.py` — অ্যাডমিন ইন্টারফেস
- `.github/workflows/*.yml` — CI/CD ওয়ার্কফ্লো

**দৈনন্দিন ব্যবহার**:
```bash
# স্থানীয়ভাবে পরীক্ষা করুন (কমিট করার আগে)
python scripts/safety_guard.py --check-only

# অথবা প্রি-কমিট হুক সক্ষম করুন (স্বয়ংক্রিয়)
python scripts/safety_guard.py --setup-hook
```

### ২. 🔍 Multi-Model Validator — কোড নিরাপত্তা চেক

**কী ঘটে**: আপনার কোড 3টি ভিন্ন AI মডেল দিয়ে বিশ্লেষণ করা হয় SQL ইনজেকশন, XSS, race conditions, মেমোরি লিক, ইত্যাদির জন্য।

**টেস্ট করে**:
- 🔴 নিরাপত্তা দুর্বলতা (GPT-4o Mini)
- 🟡 লজিক ত্রুটি (Groq Llama)
- 🟢 পারফরম্যান্স সমস্যা (Gemini)

**ব্যবহার**:
```bash
# আপনার ফাইল যাচাই করুন
python scripts/multi_model_validator.py backend/core/my_module.py

# JSON রিপোর্ট দেখুন
cat validator-report.json | python -m json.tool
```

### ৩. 💰 Autocache Proxy — API খরচ 90% কমান

**কী ঘটে**: একই প্রশ্নের জন্য বারবার LLM কল না করে খরচ বাঁচান। সিমান্টিক ক্যাশিং দিয়ে।

**অর্থনৈতিক প্রভাব**:
- OpenAI GPT-4o: প্রতি কল $0.02 → ক্যাশ হিট: বিনামূল্যে
- প্রতি মাসে 1000+ "একই" কল → $2000 → $200 সাশ্রয়

**ব্যবহার**:
```python
from core.autocache_proxy import get_autocache

cache = get_autocache()

# আপনার API কলকে ক্যাশ-সক্ষম করুন
result = await cache.intercept_api_call(
    model="gpt-4o",
    prompt="Explain microservices",
    provider="openai"
)

# খরচ ট্র্যাক করুন
summary = cache.get_cost_summary()
print(f"সংরক্ষিত: ${summary['openai']['monthly_saved']}")
```

### ৪. 📊 Codegraph — AI এজেন্ট জ্ঞান ভিত্তি

**কী ঘটে**: আপনার সম্পূর্ণ কোডবেসের একটি মেশিন-পাঠযোগ্য ম্যাপ তৈরি করা হয়। AI এজেন্টরা এটি ব্যবহার করে কোথায় কী আছে তা বুঝতে।

**তৈরি করা হয়**:
- `docs/codebase/knowledge_graph/module_graph.dot` — ভিজ্যুয়াল ডায়াগ্রাম
- `docs/codebase/knowledge_graph/knowledge_index.json` — AI-বান্ধব সূচক

**কেন গুরুত্বপূর্ণ**:
- Copilot আপনার কোড সম্পর্কে আরও ভালো পরামর্শ দেয়
- নতুন টিম মেম্বাররা দ্রুত শিখতে পারে
- প্রভাব বিশ্লেষণ স্বয়ংক্রিয় (পরিবর্তন কী ভাঙ্গে?)

```bash
# জ্ঞান ভিত্তি আপডেট করুন
python scripts/codegraph_integration.py --full

# প্রভাব বিশ্লেষণ করুন
python scripts/codegraph_integration.py --analyze-impact core/llm_gateway.py
```

### ৫. 🧠 AI Agent System Prompt — AI নির্দেশাবলী

**কী ঘটে**: GitHub Copilot এবং অন্যান্য AI এজেন্টরা এই নির্দেশাবলী পায় যা তাদের বলে:
- কী করতে পারে (বাগ ফিক্স, টেস্ট)
- কী করতে পারে না (auth মডিউল, পেমেন্ট পরিবর্তন)

**অবস্থান**: `docs/AI_AGENT_SYSTEM_PROMPT.md`

---

## 🔄 স্বয়ংক্রিয় CI/CD পাইপলাইন

প্রতিটি `push` বা `pull_request` এ:

```
১. 🛡️ Safety Guard → সংবেদনশীল ফাইল চেক করুন
   ↓
২. 🔍 Multi-Model Validator → নিরাপত্তা/লজিক পরীক্ষা করুন  
   ↓
३. 📊 Codegraph → জ্ঞান ভিত্তি আপডেট করুন
   ↓
४. 🧪 Backend Tests → pytest চালান (25%+ কভারেজ)
   ↓
५. 🔐 Security Audit → CodeQL + Trivy স্ক্যান করুন
   ↓
✅ সব পাস → PR মার্জ করা যায়
```

এটি সব স্বয়ংক্রিয় — আপনাকে কিছু করতে হবে না!

---

## 📝 নতুন বৈশিষ্ট্য যোগ করার ধাপ

### স্ট্যান্ডার্ড ওয়ার্কফ্লো:

```bash
# ১. ব্র্যাঞ্চ তৈরি করুন
git checkout -b feature/my-awesome-feature

# ২. কোড লেখুন + টেস্ট যোগ করুন
# ... সম্পাদনা ...

# ३. স্থানীয় যাচাই করুন
python scripts/safety_guard.py --check-only
python scripts/multi_model_validator.py backend/core/
cd backend && pytest tests/ -v

# ४. বাংলা বার্তা সহ কমিট করুন
git commit -m "feat: নতুন বৈশিষ্ট্যের নাম

- কী যোগ করেছেন: সংক্ষিপ্ত বর্ণনা
- কেন: ব্যবসায়িক মূল্য
- টেস্ট করা হয়েছে: test_feature.py"

# ५. পুশ করুন এবং GitHub PR খুলুন
git push origin feature/my-awesome-feature
```

### যদি সংবেদনশীল ফাইল স্পর্শ করেন:

```bash
# কমিট করার চেষ্টা করুন
git commit -m "fix: update auth logic"
# ❌ Safety Guard BLOCKS আপনাকে

# অনুমোদন অনুরোধ করুন
python scripts/safety_guard.py --request-approval --reason "প্রয়োজনীয় নিরাপত্তা প্যাচ"

# অপেক্ষা করুন:
# - admin@supremeai.dev
# - security@supremeai.dev
# অনুমোদন দেয়

# তারপর পুনরায় চেষ্টা করুন (এখন কাজ করবে)
git commit -m "fix: update auth logic [APPROVED]"
```

---

## 🧪 টেস্টিং গাইড

### ব্যাকএন্ড টেস্ট চালান:

```bash
cd backend

# সব টেস্ট চালান
pytest tests/ -v

# নির্দিষ্ট ফাইল
pytest tests/test_swarm_orchestrator.py -v

# কভারেজ দেখুন
pytest tests/ -v --cov=core --cov-report=term-missing --cov-fail-under=25

# দ্রুত চালান (শুধু গুরুত্বপূর্ণ)
pytest tests/ -m "not slow" -x
```

### নতুন টেস্ট লিখুন:

```python
"""আমার বৈশিষ্ট্যের জন্য টেস্ট"""
import pytest
from unittest.mock import patch

@pytest.mark.anyio  # বা @pytest.mark.asyncio
async def test_my_feature_does_x():
    """আমার বৈশিষ্ট্য X করে"""
    # Arrange - সেটআপ
    input_data = {"key": "value"}
    
    # Act - পরীক্ষা
    result = await my_function(input_data)
    
    # Assert - যাচাই
    assert result["success"] is True
    assert result["output"] == "expected"

def test_error_handling():
    """ত্রুটি সঠিকভাবে সামলানো হয়"""
    with pytest.raises(ValueError):
        bad_function()
```

---

## 🎨 কোড স্টাইল

### বাংলা মন্তব্য (সব নতুন কোডে):

```python
# বাংলা মন্তব্য: কী করছি এবং কেন
def important_function(data: Dict[str, Any]) -> bool:
    """
    কী করে: ডাটা প্রক্রিয়া করে এবং সংরক্ষণ করে
    পরামিতি: data - প্রক্রিয়া করার জন্য ডাটা
    রিটার্ন: সফল হলে True
    উদাহরণ:
        result = important_function({"name": "Ali"})
    """
    # বাংলা মন্তব্য: জটিল লজিক ব্যাখ্যা করুন
    if validate(data):
        save(data)
        return True
    return False
```

### Type Hints (সর্বদা):

```python
from typing import Dict, List, Optional, Any

def process(
    data: Dict[str, Any],
    timeout: int = 30
) -> Optional[List[str]]:
    pass
```

### ব্যতিক্রম হ্যান্ডলিং:

```python
from loguru import logger

try:
    result = await api_call()
except TimeoutError as e:
    logger.error(f"API সময় শেষ: {e}")
    raise  # পুনরায় থ্রো করুন যাতে caller জানতে পারে
except Exception as e:
    logger.warning(f"অপ্রত্যাশিত ত্রুটি: {e}")
    return default_value
```

---

## ✅ মার্জের আগে চেকলিস্ট

আপনার PR প্রস্তুত?

- [ ] নতুন কোডে বাংলা মন্তব্য (`# বাংলা মন্তব্য: ...`)
- [ ] টেস্ট লেখা এবং পাস করেছে (`pytest -v`)
- [ ] কভারেজ যুক্তিসঙ্গত (25%+ মূল পথ)
- [ ] Safety Guard পাস (`python scripts/safety_guard.py --check-only`)
- [ ] Multi-Model কোনো CRITICAL সমস্যা পায়নি
- [ ] কোনো Hard-coded API কী বা পাসওয়ার্ড নেই
- [ ] ডকুমেন্টেশন আপডেট করেছেন
- [ ] বাংলা কমিট মেসেজ যোগ করেছেন

---

## 📚 দরকারি সম্পদ

- [Project Architecture](docs/03-architecture/README.md)
- [API Documentation](docs/06-api/README.md)
- [AI Agent Guide](docs/AI_AGENT_SYSTEM_PROMPT.md)
- [Database Schema](docs/codebase/knowledge_graph/knowledge_index.json)

## 🆘 সাহায্য

- **বাগ রিপোর্ট**: [GitHub Issues](https://github.com/paykaribazaronline/supremeai/issues)
- **প্রশ্ন**: [GitHub Discussions](https://github.com/paykaribazaronline/supremeai/discussions)
- **নিরাপত্তা সমস্যা**: security@supremeai.dev

---

**হ্যাপি কোডিং! 🚀**  
SupremeAI দল


```