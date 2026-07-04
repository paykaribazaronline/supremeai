# 📄 ফাইল: test-results/e2e-chat-Chat-sends-message-Mobile-Chrome/error-context.md

**প্রকার:** .md  
**সাইজ:** 2,816 বাইট  
**আপডেট:** 2026-07-04T13:41:46.899248

---

## কোড

```md
# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e\chat.spec.ts >> Chat sends message
- Location: tests\e2e\chat.spec.ts:3:5

# Error details

```
Test timeout of 30000ms exceeded.
```

```
Error: page.fill: Test timeout of 30000ms exceeded.
Call log:
  - waiting for locator('[data-testid="chat-input"]')

```

# Page snapshot

```yaml
- generic [ref=e3]:
  - complementary [ref=e5]:
    - generic [ref=e6]:
      - generic [ref=e7]: ▲
      - generic [ref=e8]: SupremeAI
    - navigation [ref=e9]:
      - button "Sessions" [ref=e10]:
        - img [ref=e11]
        - text: Sessions
      - button "Workspace" [ref=e14]:
        - img [ref=e15]
        - text: Workspace
      - button "Auth Vault" [ref=e25]:
        - img [ref=e26]
        - text: Auth Vault
      - button "Automation" [ref=e37]:
        - img [ref=e38]
        - text: Automation
      - button "Knowledge" [ref=e41]:
        - img [ref=e42]
        - text: Knowledge
      - button "Secrets" [ref=e44]:
        - img [ref=e45]
        - text: Secrets
      - button "Usage" [ref=e48]:
        - img [ref=e49]
        - text: Usage
      - button "Settings" [ref=e51]:
        - img [ref=e52]
        - text: Settings
      - button "Site Actions" [ref=e55]:
        - img [ref=e56]
        - text: Site Actions
      - button "LLM Gateway" [ref=e58]:
        - img [ref=e59]
        - text: LLM Gateway
      - button "Admin Console" [ref=e62]:
        - img [ref=e63]
        - text: Admin Console
    - generic [ref=e65]:
      - generic [ref=e66]:
        - img [ref=e67]
        - generic [ref=e74]: Offline
      - button "Dark mode" [ref=e75]:
        - img [ref=e76]
        - text: Dark mode
  - main [ref=e78]:
    - generic [ref=e79]:
      - heading "What do you want to build today?" [level=1] [ref=e80]
      - generic [ref=e81]:
        - textbox "Give SupremeAI a task to work on..." [ref=e82]
        - button "Start Session" [disabled] [ref=e84]:
          - img [ref=e85]
          - text: Start Session
      - generic [ref=e88]:
        - heading "Recent sessions" [level=2] [ref=e89]
        - generic [ref=e90]: 0 total
      - paragraph [ref=e91]: No sessions yet. Start your first task above.
```

# Test source

```ts
  1 | import { test, expect } from '@playwright/test';
  2 | 
  3 | test('Chat sends message', async ({ page }) => {
  4 |   await page.goto('/');
> 5 |   await page.fill('[data-testid="chat-input"]', 'Hello SupremeAI!');
    |              ^ Error: page.fill: Test timeout of 30000ms exceeded.
  6 |   await page.click('[data-testid="chat-submit"]');
  7 |   await expect(page.getByText('Hello SupremeAI!').first()).toBeVisible();
  8 | });
  9 | 
```
```