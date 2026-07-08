# 📄 ফাইল: backend/tests/scout/test_web_crawler_agent.py

**প্রকার:** .py  
**সাইজ:** 637 বাইট  
**আপডেট:** 2026-07-08T03:35:53.453189

---

## কোড

```py
import asyncio
import sys

import pytest

sys.path.append("../..")
from scout.web_crawler_agent import APPROVED_DOMAINS, CrawlResult, crawl


class TestWebCrawlerAgent:
    def test_approved_domains(self):
        assert "github.com" in APPROVED_DOMAINS
        assert "arxiv.org" in APPROVED_DOMAINS

    def test_crawl_approved_domain(self):
        result = asyncio.run(crawl("https://github.com/test/repo"))
        assert isinstance(result, CrawlResult)

    def test_crawl_unapproved_domain(self):
        with pytest.raises(PermissionError, match="Domain not approved"):
            asyncio.run(crawl("https://example.com/test"))

```