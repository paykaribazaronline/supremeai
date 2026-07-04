# 📄 ফাইল: test-results/e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-chromium/error-context.md

**প্রকার:** .md  
**সাইজ:** 7,200 বাইট  
**আপডেট:** 2026-07-04T22:20:06.016265

---

## কোড

```md
# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: e2e\accessibility.spec.ts >> Accessibility Tests (WCAG) >> Admin Dashboard should be accessible
- Location: tests\e2e\accessibility.spec.ts:18:9

# Error details

```
Error: expect(received).toEqual(expected) // deep equality

- Expected  -   1
+ Received  + 149

- Array []
+ Array [
+   Object {
+     "description": "Ensure the document has a main landmark",
+     "help": "Document should have one main landmark",
+     "helpUrl": "https://dequeuniversity.com/rules/axe/4.12/landmark-one-main?application=playwright",
+     "id": "landmark-one-main",
+     "impact": "moderate",
+     "nodes": Array [
+       Object {
+         "all": Array [
+           Object {
+             "data": null,
+             "id": "page-has-main",
+             "impact": "moderate",
+             "message": "Document does not have a main landmark",
+             "relatedNodes": Array [],
+           },
+         ],
+         "any": Array [],
+         "failureSummary": "Fix all of the following:
+   Document does not have a main landmark",
+         "html": "<html lang=\"en\" class=\"dark\" data-theme=\"dark\">",
+         "impact": "moderate",
+         "none": Array [],
+         "target": Array [
+           "html",
+         ],
+       },
+     ],
+     "tags": Array [
+       "cat.semantics",
+       "best-practice",
+     ],
+   },
+   Object {
+     "description": "Ensure that the page, or at least one of its frames contains a level-one heading",
+     "help": "Page should contain a level-one heading",
+     "helpUrl": "https://dequeuniversity.com/rules/axe/4.12/page-has-heading-one?application=playwright",
+     "id": "page-has-heading-one",
+     "impact": "moderate",
+     "nodes": Array [
+       Object {
+         "all": Array [
+           Object {
+             "data": null,
+             "id": "page-has-heading-one",
+             "impact": "moderate",
+             "message": "Page must have a level-one heading",
+             "relatedNodes": Array [],
+           },
+         ],
+         "any": Array [],
+         "failureSummary": "Fix all of the following:
+   Page must have a level-one heading",
+         "html": "<html lang=\"en\" class=\"dark\" data-theme=\"dark\">",
+         "impact": "moderate",
+         "none": Array [],
+         "target": Array [
+           "html",
+         ],
+       },
+     ],
+     "tags": Array [
+       "cat.semantics",
+       "best-practice",
+     ],
+   },
+   Object {
+     "description": "Ensure all page content is contained by landmarks",
+     "help": "All page content should be contained by landmarks",
+     "helpUrl": "https://dequeuniversity.com/rules/axe/4.12/region?application=playwright",
+     "id": "region",
+     "impact": "moderate",
+     "nodes": Array [
+       Object {
+         "all": Array [],
+         "any": Array [
+           Object {
+             "data": Object {
+               "isIframe": false,
+             },
+             "id": "region",
+             "impact": "moderate",
+             "message": "Some page content is not contained by landmarks",
+             "relatedNodes": Array [],
+           },
+         ],
+         "failureSummary": "Fix any of the following:
+   Some page content is not contained by landmarks",
+         "html": "<h2 class=\"text-xl font-mono font-bold text-[#ff0055] uppercase tracking-widest mb-2\">Dashboard Module Failure</h2>",
+         "impact": "moderate",
+         "none": Array [],
+         "target": Array [
+           "h2",
+         ],
+       },
+       Object {
+         "all": Array [],
+         "any": Array [
+           Object {
+             "data": Object {
+               "isIframe": false,
+             },
+             "id": "region",
+             "impact": "moderate",
+             "message": "Some page content is not contained by landmarks",
+             "relatedNodes": Array [],
+           },
+         ],
+         "failureSummary": "Fix any of the following:
+   Some page content is not contained by landmarks",
+         "html": "<p class=\"text-sm text-slate-400 font-mono mb-4\">A critical module in the admin dashboard has crashed. The rest of the system remains intact.</p>",
+         "impact": "moderate",
+         "none": Array [],
+         "target": Array [
+           "p",
+         ],
+       },
+       Object {
+         "all": Array [],
+         "any": Array [
+           Object {
+             "data": Object {
+               "isIframe": false,
+             },
+             "id": "region",
+             "impact": "moderate",
+             "message": "Some page content is not contained by landmarks",
+             "relatedNodes": Array [],
+           },
+         ],
+         "failureSummary": "Fix any of the following:
+   Some page content is not contained by landmarks",
+         "html": "<pre class=\"text-xs text-slate-400 font-mono bg-slate-900/80 p-3 rounded-lg mb-6 overflow-auto max-h-40\">React is not defined</pre>",
+         "impact": "moderate",
+         "none": Array [],
+         "target": Array [
+           "pre",
+         ],
+       },
+     ],
+     "tags": Array [
+       "cat.keyboard",
+       "best-practice",
+       "RGAAv4",
+       "RGAA-9.2.1",
+     ],
+   },
+ ]
```

# Page snapshot

```yaml
- generic [ref=e4]:
  - heading "Dashboard Module Failure" [level=2] [ref=e5]
  - paragraph [ref=e6]: A critical module in the admin dashboard has crashed. The rest of the system remains intact.
  - generic [ref=e7]: React is not defined
  - button "Reboot Dashboard Module" [ref=e8]
```

# Test source

```ts
  1  | import { test, expect } from '@playwright/test';
  2  | import AxeBuilder from '@axe-core/playwright';
  3  | 
  4  | test.describe('Accessibility Tests (WCAG)', () => {
  5  |     test('Homepage should not have any automatically detectable accessibility issues', async ({ page }) => {
  6  |         await page.goto('/');
  7  | 
  8  |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();
  9  | 
  10 |         // কোনো ভায়োলেশন থাকলে তা প্রিন্ট করার জন্য একটি সহায়ক লগ
  11 |         if (accessibilityScanResults.violations.length > 0) {
  12 |             console.log('Accessibility violations found on homepage:', JSON.stringify(accessibilityScanResults.violations, null, 2));
  13 |         }
  14 | 
  15 |         expect(accessibilityScanResults.violations).toEqual([]);
  16 |     });
  17 | 
  18 |     test('Admin Dashboard should be accessible', async ({ page }) => {
  19 |         await page.goto('/admin'); // আপনার অ্যাডমিন পেজের URL
  20 | 
  21 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();
  22 | 
> 23 |         expect(accessibilityScanResults.violations).toEqual([]);
     |                                                     ^ Error: expect(received).toEqual(expected) // deep equality
  24 |     });
  25 | });
```
```