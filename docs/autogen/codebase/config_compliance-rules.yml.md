# 📄 ফাইল: config/compliance-rules.yml

**প্রকার:** .yml  
**সাইজ:** 188 বাইট  
**আপডেট:** 2026-07-11T15:05:35.219162

---

## কোড

```yml
rules:
  require_non_root: true
  require_healthcheck: true
  require_labels:
    - org.opencontainers.image.source
    - org.opencontainers.image.description
  require_dockerignore: true

```