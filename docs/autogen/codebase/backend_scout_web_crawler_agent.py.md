# 📄 ফাইল: backend/scout/web_crawler_agent.py

**প্রকার:** .py  
**সাইজ:** 358 বাইট  
**আপডেট:** 2026-07-07T08:37:57.153555

---

## কোড

```py



APPROVED_DOMAINS = ["github.com", "arxiv.org", "docs.python.org", "huggingface.co"]


class CrawlResult:
    url: str
    content: str
    status: int


async def crawl(url: str) -> CrawlResult:
    if not any(url.startswith(f"https://{d}") for d in APPROVED_DOMAINS):
        raise PermissionError(f"Domain not approved: {url}")
    return CrawlResult()

```