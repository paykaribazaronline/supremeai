সবকিছু ঠিক আছে, কিন্তু একটা env key missing/invalid থাকায় পুরো সিস্টেম ভেঙে গেছে" — এটা সাধারণত সবচেয়ে সাইলেন্ট এবং বিরক্তিকর ধরনের production failure, কারণ কোড লজিক্যালি ঠিক থাকে, শুধু configuration layer এ সমস্যা থাকে। এটা আপনার প্রজেক্টের "Zero-Gap Deployment" principle-এর সাথেও সরাসরি সামঞ্জস্যপূর্ণ।

তবে বাস্তবায়নের সময় কয়েকটা জিনিস মাথায় রাখা দরকার:

**১. "Valid" মানে দুই রকম হতে পারে — এটা আলাদা করে ভাবতে হবে**
- **Presence check**: key টা আদৌ set করা আছে কিনা, empty না তো (সস্তা, সবসময় করা যায়)
- **Format/shape check**: key টা সঠিক প্যাটার্নে আছে কিনা (যেমন `sk-...` দিয়ে শুরু, নির্দিষ্ট length) — এখনো সস্তা
- **Live validation**: আসল API call করে দেখা key টা কাজ করছে কিনা — এটা costly (rate limit consume করে, কিছু provider-এ টাকাও কাটতে পারে), তাই **শুধু critical key-গুলোর জন্য, এবং lightweight endpoint দিয়ে** (যেমন OpenAI-র `models.list`, Supabase-র health endpoint, Render-এর status API) করা উচিত। প্রতিটা push-এ live validation চালানো বিপজ্জনক — rate-limit-এ ব্লক হয়ে যেতে পারেন।

**২. Critical vs Optional আলাদা করা জরুরি**
সব key সমান গুরুত্বপূর্ণ না। যেমন `DATABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY` missing হলে পুরো backend startup-এই crash করবে (fail করা উচিত), কিন্তু কোনো optional third-party integration key (যেমন কোনো analytics service) missing হলে শুধু warning দিয়ে চলতে পারা উচিত।

**৩. Secret leak হওয়ার ঝুঁকি**
Validation job লিখতে গিয়ে ভুলেও log-এ actual key value print করা যাবে না (এমনকি error message-এও না)। GitHub Actions-এ `::add-mask::` ব্যবহার করে মাস্ক করতে হবে, আর error message-এ শুধু "KEY_NAME missing/invalid" বলবেন, value না।

---

## Full Plan: API Key ও Secret ম্যানেজমেন্ট

### Layer 1 — একটা Single Source of Truth তৈরি করা (Key Registry)

সবচেয়ে গুরুত্বপূর্ণ অংশ হলো একটা **মেশিন-রিডেবল রেজিস্ট্রি ফাইল**, যেখানে প্রতিটা key-এর নিচের তথ্যগুলো থাকবে:

```yaml
# secrets_registry.yaml (repo root বা docs/)
keys:
  - name: SUPABASE_SERVICE_ROLE_KEY
    used_in: [backend]
    environments: [staging, production]
    criticality: critical         # critical | important | optional
    validation: live              # presence | format | live
    validation_method: "supabase_health_check"
    owner: "backend team"
    rotation_days: 90
    format_regex: null

  - name: OPENAI_API_KEY
    used_in: [backend]
    environments: [staging, production]
    criticality: critical
    validation: live
    validation_method: "openai_models_list"
    rotation_days: 180

  - name: USER_HEALTH_URL
    used_in: [backend]
    environments: [production]
    criticality: important
    validation: format
    format_regex: "^https://"
```

**কেন YAML/JSON, Markdown doc না?** — আপনার প্রজেক্টের history-তেই দেখা গেছে (`task_progress.md`, `REMAINING_TASKS.md`, `FAILING_TESTS.md`) manually maintained tracking doc গুলো দ্রুত stale হয়ে যায়, কারণ কেউ update করতে ভুলে যায়। YAML রেজিস্ট্রি হলে সেটাকে CI script দিয়ে **প্রোগ্রাম্যাটিকালি পড়া, validate করা, এবং doc auto-generate করা** যায় — মানুষের memory-র উপর নির্ভর করতে হয় না।

### Layer 2 — Code থেকে Drift Detection (এইটা সবচেয়ে গুরুত্বপূর্ণ অংশ, আপনার প্রশ্নের দ্বিতীয় অংশের উত্তর)

"কোন environment-এ কোন key লাগবে" — এটা ম্যানুয়ালি ট্র্যাক করলে সময়ের সাথে বাস্তবতার থেকে সরে যাবে (যেমনটা আপনার অন্যান্য tracking doc-এর সাথে হয়েছে)। এর বদলে:

1. একটা script (`scripts/audit_env_usage.py`) পুরো codebase-এ grep/AST-scan করবে `os.getenv(...)`, `settings.X`, `process.env.X` প্যাটার্নের জন্য
2. যা পাবে সেটা `secrets_registry.yaml`-এর সাথে diff করবে
3. যদি কোডে ব্যবহৃত কোনো key registry-তে না থাকে → CI fail (registry stale)
4. যদি registry-তে থাকা কোনো key কোডে আর ব্যবহার হচ্ছে না → warning (dead entry, cleanup দরকার)

এভাবে registry নিজে থেকেই সবসময় সত্যি থাকতে বাধ্য হয়।

### Layer 3 — CI Job (আপনার মূল আইডিয়া)

`supreme-core-ci.yml`-এ নতুন job `secrets-validation`:

```yaml
secrets-validation:
  runs-on: ubuntu-latest
  environment: production   # অথবা staging, matrix দিয়ে দুইটাই
  steps:
    - uses: actions/checkout@v4
    - name: Load registry & validate
      env:
        # প্রতিটা secret এখানে GitHub Environment থেকে ইনজেক্ট হবে
        SUPABASE_SERVICE_ROLE_KEY: ${{ secrets.SUPABASE_SERVICE_ROLE_KEY }}
        OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        # ... registry থেকে script নিজেই বাকিগুলো env থেকে collect করবে
      run: python scripts/validate_secrets.py --env production
```

`validate_secrets.py`-এর লজিক:
- registry পড়ে প্রতিটা key-এর জন্য loop করবে
- `criticality: critical` + `validation: live` হলে → actual lightweight API call, exit code non-zero দিলে **job fail**
- `criticality: important` + fail হলে → GitHub Actions warning annotation (`::warning::`), job pass কিন্তু visible
- `criticality: optional` → শুধু presence log করবে

এই job-টা deploy job-গুলোর `needs:` এ বসাবেন (ঠিক যেভাবে আপনার আগের service-preflight job কাজ করছে) — যাতে critical key ছাড়া deploy-ই শুরু না হয়।

### Layer 4 — GitHub Environments দিয়ে scope আলাদা করা

একটা repo-level secret list না রেখে GitHub-এর **Environments** ফিচার (Settings → Environments) ব্যবহার করুন — `staging` আর `production` আলাদা environment, প্রতিটার নিজস্ব secret set। এর সুবিধা:
- Production key staging job accidentally access করতে পারবে না
- Production environment-এ "required reviewers" protection rule বসাতে পারবেন — কোনো workflow production secret ব্যবহার করার আগে approval লাগবে
- Key rotate করলে শুধু নির্দিষ্ট environment-এ update করলেই হয়

### Layer 5 — Rotation ও Ownership Tracking

Registry-তে থাকা `rotation_days` field ব্যবহার করে একটা scheduled workflow (weekly cron) চেক করবে কোনো key তার rotation window পার করেছে কিনা, করলে একটা GitHub Issue auto-create করবে reminder হিসেবে — actual rotation ম্যানুয়াল থাকবে (security-sensitive কাজ auto করা ঠিক না), কিন্তু reminder auto হবে।

---

## সংক্ষেপে Flow

```
Code-এ key ব্যবহার
      ↓
audit_env_usage.py → registry-র সাথে sync verify (drift ধরবে)
      ↓
secrets_registry.yaml → single source of truth (কোন key, কোন env, কতটা critical)
      ↓
validate_secrets.py → CI job-এ চলবে, criticality অনুযায়ী fail/warning
      ↓
GitHub Environments (staging/production) → actual secret storage, scoped access
```

এটাকে আপনি চাইলে আজকের ৫টা priority task-এর একটা হিসেবে ধরতে পারেন — শুরুতে শুধু registry ফাইল + drift-check script বানানো, তারপর ধাপে ধাপে validation job যোগ করা যুক্তিসঙ্গত হবে, একবারে সব বসানোর চেষ্টা না করে।

