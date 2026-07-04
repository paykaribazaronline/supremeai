# 📄 ফাইল: test-results/e2e-report.json

**প্রকার:** .json  
**সাইজ:** 218,770 বাইট  
**আপডেট:** 2026-07-04T13:24:28.393717

---

## কোড

```json
{
  "config": {
    "argv": [
      "C:\\Program Files\\nodejs\\node.exe",
      "C:\\Users\\n\\supremeai\\supremeai_2.0\\node_modules\\@playwright\\test\\cli.js",
      "test"
    ],
    "configFile": "C:\\Users\\n\\supremeai\\supremeai_2.0\\playwright.config.ts",
    "rootDir": "C:/Users/n/supremeai/supremeai_2.0/tests",
    "failOnFlakyTests": false,
    "forbidOnly": false,
    "fullyParallel": true,
    "globalSetup": null,
    "globalTeardown": null,
    "globalTimeout": 0,
    "grep": {},
    "grepInvert": null,
    "maxFailures": 0,
    "metadata": {
      "actualWorkers": 2
    },
    "preserveOutput": "always",
    "projects": [
      {
        "outputDir": "C:/Users/n/supremeai/supremeai_2.0/test-results",
        "repeatEach": 1,
        "retries": 0,
        "metadata": {
          "actualWorkers": 2
        },
        "id": "chromium",
        "name": "chromium",
        "testDir": "C:/Users/n/supremeai/supremeai_2.0/tests",
        "testIgnore": [],
        "testMatch": [
          "**/*.spec.ts"
        ],
        "timeout": 30000
      },
      {
        "outputDir": "C:/Users/n/supremeai/supremeai_2.0/test-results",
        "repeatEach": 1,
        "retries": 0,
        "metadata": {
          "actualWorkers": 2
        },
        "id": "firefox",
        "name": "firefox",
        "testDir": "C:/Users/n/supremeai/supremeai_2.0/tests",
        "testIgnore": [],
        "testMatch": [
          "**/*.spec.ts"
        ],
        "timeout": 30000
      },
      {
        "outputDir": "C:/Users/n/supremeai/supremeai_2.0/test-results",
        "repeatEach": 1,
        "retries": 0,
        "metadata": {
          "actualWorkers": 2
        },
        "id": "webkit",
        "name": "webkit",
        "testDir": "C:/Users/n/supremeai/supremeai_2.0/tests",
        "testIgnore": [],
        "testMatch": [
          "**/*.spec.ts"
        ],
        "timeout": 30000
      },
      {
        "outputDir": "C:/Users/n/supremeai/supremeai_2.0/test-results",
        "repeatEach": 1,
        "retries": 0,
        "metadata": {
          "actualWorkers": 2
        },
        "id": "Mobile Chrome",
        "name": "Mobile Chrome",
        "testDir": "C:/Users/n/supremeai/supremeai_2.0/tests",
        "testIgnore": [],
        "testMatch": [
          "**/*.spec.ts"
        ],
        "timeout": 30000
      },
      {
        "outputDir": "C:/Users/n/supremeai/supremeai_2.0/test-results",
        "repeatEach": 1,
        "retries": 0,
        "metadata": {
          "actualWorkers": 2
        },
        "id": "Mobile Safari",
        "name": "Mobile Safari",
        "testDir": "C:/Users/n/supremeai/supremeai_2.0/tests",
        "testIgnore": [],
        "testMatch": [
          "**/*.spec.ts"
        ],
        "timeout": 30000
      }
    ],
    "quiet": false,
    "reporter": [
      [
        "html",
        {
          "outputFolder": "playwright-report"
        }
      ],
      [
        "json",
        {
          "outputFile": "test-results/e2e-report.json"
        }
      ],
      [
        "list",
        null
      ]
    ],
    "reportSlowTests": {
      "max": 5,
      "threshold": 300000
    },
    "shard": null,
    "tags": [],
    "updateSnapshots": "missing",
    "updateSourceMethod": "patch",
    "version": "1.61.1",
    "workers": 2,
    "webServer": {
      "command": "pnpm --dir apps/studio-client dev --host 0.0.0.0 --port 5173",
      "url": "http://127.0.0.1:5173",
      "reuseExistingServer": true,
      "timeout": 120000
    }
  },
  "suites": [
    {
      "title": "e2e\\accessibility.spec.ts",
      "file": "e2e/accessibility.spec.ts",
      "column": 0,
      "line": 0,
      "specs": [],
      "suites": [
        {
          "title": "Accessibility Tests (WCAG)",
          "file": "e2e/accessibility.spec.ts",
          "line": 4,
          "column": 6,
          "specs": [
            {
              "title": "Homepage should not have any automatically detectable accessibility issues",
              "ok": true,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "chromium",
                  "projectName": "chromium",
                  "results": [
                    {
                      "workerIndex": 0,
                      "parallelIndex": 0,
                      "status": "passed",
                      "duration": 21022,
                      "errors": [],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:02:39.437Z",
                      "annotations": [],
                      "attachments": []
                    }
                  ],
                  "status": "expected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-ee181cbb1b50022aa313",
              "file": "e2e/accessibility.spec.ts",
              "line": 5,
              "column": 9
            },
            {
              "title": "Admin Dashboard should be accessible",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "chromium",
                  "projectName": "chromium",
                  "results": [
                    {
                      "workerIndex": 1,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 22267,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mreceived\u001b[39m\u001b[2m).\u001b[22mtoEqual\u001b[2m(\u001b[22m\u001b[32mexpected\u001b[39m\u001b[2m) // deep equality\u001b[22m\n\n\u001b[32m- Expected  -   1\u001b[39m\n\u001b[31m+ Received  + 149\u001b[39m\n\n\u001b[32m- Array []\u001b[39m\n\u001b[31m+ Array [\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure the document has a main landmark\",\u001b[39m\n\u001b[31m+     \"help\": \"Document should have one main landmark\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/landmark-one-main?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"landmark-one-main\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-main\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Document does not have a main landmark\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Document does not have a main landmark\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure that the page, or at least one of its frames contains a level-one heading\",\u001b[39m\n\u001b[31m+     \"help\": \"Page should contain a level-one heading\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/page-has-heading-one?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Page must have a level-one heading\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Page must have a level-one heading\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure all page content is contained by landmarks\",\u001b[39m\n\u001b[31m+     \"help\": \"All page content should be contained by landmarks\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/region?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"region\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<h2 class=\\\"text-xl font-mono font-bold text-[#ff0055] uppercase tracking-widest mb-2\\\">Dashboard Module Failure</h2>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"h2\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<p class=\\\"text-sm text-slate-400 font-mono mb-4\\\">A critical module in the admin dashboard has crashed. The rest of the system remains intact.</p>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"p\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<pre class=\\\"text-xs text-slate-400 font-mono bg-slate-900/80 p-3 rounded-lg mb-6 overflow-auto max-h-40\\\">React is not defined</pre>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"pre\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.keyboard\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+       \"RGAAv4\",\u001b[39m\n\u001b[31m+       \"RGAA-9.2.1\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+ ]\u001b[39m",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mreceived\u001b[39m\u001b[2m).\u001b[22mtoEqual\u001b[2m(\u001b[22m\u001b[32mexpected\u001b[39m\u001b[2m) // deep equality\u001b[22m\n\n\u001b[32m- Expected  -   1\u001b[39m\n\u001b[31m+ Received  + 149\u001b[39m\n\n\u001b[32m- Array []\u001b[39m\n\u001b[31m+ Array [\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure the document has a main landmark\",\u001b[39m\n\u001b[31m+     \"help\": \"Document should have one main landmark\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/landmark-one-main?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"landmark-one-main\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-main\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Document does not have a main landmark\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Document does not have a main landmark\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure that the page, or at least one of its frames contains a level-one heading\",\u001b[39m\n\u001b[31m+     \"help\": \"Page should contain a level-one heading\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/page-has-heading-one?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Page must have a level-one heading\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Page must have a level-one heading\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure all page content is contained by landmarks\",\u001b[39m\n\u001b[31m+     \"help\": \"All page content should be contained by landmarks\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/region?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"region\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<h2 class=\\\"text-xl font-mono font-bold text-[#ff0055] uppercase tracking-widest mb-2\\\">Dashboard Module Failure</h2>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"h2\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<p class=\\\"text-sm text-slate-400 font-mono mb-4\\\">A critical module in the admin dashboard has crashed. The rest of the system remains intact.</p>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"p\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<pre class=\\\"text-xs text-slate-400 font-mono bg-slate-900/80 p-3 rounded-lg mb-6 overflow-auto max-h-40\\\">React is not defined</pre>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"pre\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.keyboard\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+       \"RGAAv4\",\u001b[39m\n\u001b[31m+       \"RGAA-9.2.1\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+ ]\u001b[39m\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:23:53",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 53,
                          "line": 23
                        },
                        "snippet": "\u001b[0m \u001b[90m 21 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 22 |\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 23 |\u001b[39m         expect(accessibilityScanResults\u001b[33m.\u001b[39mviolations)\u001b[33m.\u001b[39mtoEqual([])\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                                                     \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 24 |\u001b[39m     })\u001b[33m;\u001b[39m\n \u001b[90m 25 |\u001b[39m })\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 53,
                            "line": 23
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mreceived\u001b[39m\u001b[2m).\u001b[22mtoEqual\u001b[2m(\u001b[22m\u001b[32mexpected\u001b[39m\u001b[2m) // deep equality\u001b[22m\n\n\u001b[32m- Expected  -   1\u001b[39m\n\u001b[31m+ Received  + 149\u001b[39m\n\n\u001b[32m- Array []\u001b[39m\n\u001b[31m+ Array [\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure the document has a main landmark\",\u001b[39m\n\u001b[31m+     \"help\": \"Document should have one main landmark\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/landmark-one-main?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"landmark-one-main\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-main\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Document does not have a main landmark\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Document does not have a main landmark\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure that the page, or at least one of its frames contains a level-one heading\",\u001b[39m\n\u001b[31m+     \"help\": \"Page should contain a level-one heading\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/page-has-heading-one?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Page must have a level-one heading\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Page must have a level-one heading\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure all page content is contained by landmarks\",\u001b[39m\n\u001b[31m+     \"help\": \"All page content should be contained by landmarks\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/region?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"region\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<h2 class=\\\"text-xl font-mono font-bold text-[#ff0055] uppercase tracking-widest mb-2\\\">Dashboard Module Failure</h2>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"h2\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<p class=\\\"text-sm text-slate-400 font-mono mb-4\\\">A critical module in the admin dashboard has crashed. The rest of the system remains intact.</p>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"p\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<pre class=\\\"text-xs text-slate-400 font-mono bg-slate-900/80 p-3 rounded-lg mb-6 overflow-auto max-h-40\\\">React is not defined</pre>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"pre\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.keyboard\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+       \"RGAAv4\",\u001b[39m\n\u001b[31m+       \"RGAA-9.2.1\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+ ]\u001b[39m\n\n  21 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  22 |\n> 23 |         expect(accessibilityScanResults.violations).toEqual([]);\n     |                                                     ^\n  24 |     });\n  25 | });\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:23:53"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:02:39.779Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-chromium\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-chromium\\video.webm"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-chromium\\video-1.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-chromium\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 53,
                        "line": 23
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-500664d0b985aac1baa3",
              "file": "e2e/accessibility.spec.ts",
              "line": 18,
              "column": 9
            },
            {
              "title": "Homepage should not have any automatically detectable accessibility issues",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "firefox",
                  "projectName": "firefox",
                  "results": [
                    {
                      "workerIndex": 6,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 3,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:42.402Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-firefox\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-b3d42475f7c88aec8c52",
              "file": "e2e/accessibility.spec.ts",
              "line": 5,
              "column": 9
            },
            {
              "title": "Admin Dashboard should be accessible",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "firefox",
                  "projectName": "firefox",
                  "results": [
                    {
                      "workerIndex": 7,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 5,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:44.281Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-firefox\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-f5638b87b7a414080a2d",
              "file": "e2e/accessibility.spec.ts",
              "line": 18,
              "column": 9
            },
            {
              "title": "Homepage should not have any automatically detectable accessibility issues",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "webkit",
                  "projectName": "webkit",
                  "results": [
                    {
                      "workerIndex": 13,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 7,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:00.867Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-webkit\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-fbd64fd80d69d10df3a0",
              "file": "e2e/accessibility.spec.ts",
              "line": 5,
              "column": 9
            },
            {
              "title": "Admin Dashboard should be accessible",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "webkit",
                  "projectName": "webkit",
                  "results": [
                    {
                      "workerIndex": 14,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 9,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:01.952Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-webkit\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-080eb04c5c3f482efa54",
              "file": "e2e/accessibility.spec.ts",
              "line": 18,
              "column": 9
            },
            {
              "title": "Homepage should not have any automatically detectable accessibility issues",
              "ok": true,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Chrome",
                  "projectName": "Mobile Chrome",
                  "results": [
                    {
                      "workerIndex": 20,
                      "parallelIndex": 0,
                      "status": "passed",
                      "duration": 23308,
                      "errors": [],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:10.020Z",
                      "annotations": [],
                      "attachments": []
                    }
                  ],
                  "status": "expected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-91fb6702d4d7a6d60dd3",
              "file": "e2e/accessibility.spec.ts",
              "line": 5,
              "column": 9
            },
            {
              "title": "Admin Dashboard should be accessible",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Chrome",
                  "projectName": "Mobile Chrome",
                  "results": [
                    {
                      "workerIndex": 21,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 16887,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mreceived\u001b[39m\u001b[2m).\u001b[22mtoEqual\u001b[2m(\u001b[22m\u001b[32mexpected\u001b[39m\u001b[2m) // deep equality\u001b[22m\n\n\u001b[32m- Expected  -   1\u001b[39m\n\u001b[31m+ Received  + 149\u001b[39m\n\n\u001b[32m- Array []\u001b[39m\n\u001b[31m+ Array [\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure the document has a main landmark\",\u001b[39m\n\u001b[31m+     \"help\": \"Document should have one main landmark\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/landmark-one-main?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"landmark-one-main\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-main\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Document does not have a main landmark\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Document does not have a main landmark\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure that the page, or at least one of its frames contains a level-one heading\",\u001b[39m\n\u001b[31m+     \"help\": \"Page should contain a level-one heading\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/page-has-heading-one?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Page must have a level-one heading\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Page must have a level-one heading\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure all page content is contained by landmarks\",\u001b[39m\n\u001b[31m+     \"help\": \"All page content should be contained by landmarks\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/region?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"region\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<h2 class=\\\"text-xl font-mono font-bold text-[#ff0055] uppercase tracking-widest mb-2\\\">Dashboard Module Failure</h2>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"h2\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<p class=\\\"text-sm text-slate-400 font-mono mb-4\\\">A critical module in the admin dashboard has crashed. The rest of the system remains intact.</p>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"p\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<pre class=\\\"text-xs text-slate-400 font-mono bg-slate-900/80 p-3 rounded-lg mb-6 overflow-auto max-h-40\\\">React is not defined</pre>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"pre\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.keyboard\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+       \"RGAAv4\",\u001b[39m\n\u001b[31m+       \"RGAA-9.2.1\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+ ]\u001b[39m",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mreceived\u001b[39m\u001b[2m).\u001b[22mtoEqual\u001b[2m(\u001b[22m\u001b[32mexpected\u001b[39m\u001b[2m) // deep equality\u001b[22m\n\n\u001b[32m- Expected  -   1\u001b[39m\n\u001b[31m+ Received  + 149\u001b[39m\n\n\u001b[32m- Array []\u001b[39m\n\u001b[31m+ Array [\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure the document has a main landmark\",\u001b[39m\n\u001b[31m+     \"help\": \"Document should have one main landmark\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/landmark-one-main?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"landmark-one-main\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-main\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Document does not have a main landmark\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Document does not have a main landmark\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure that the page, or at least one of its frames contains a level-one heading\",\u001b[39m\n\u001b[31m+     \"help\": \"Page should contain a level-one heading\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/page-has-heading-one?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Page must have a level-one heading\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Page must have a level-one heading\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure all page content is contained by landmarks\",\u001b[39m\n\u001b[31m+     \"help\": \"All page content should be contained by landmarks\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/region?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"region\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<h2 class=\\\"text-xl font-mono font-bold text-[#ff0055] uppercase tracking-widest mb-2\\\">Dashboard Module Failure</h2>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"h2\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<p class=\\\"text-sm text-slate-400 font-mono mb-4\\\">A critical module in the admin dashboard has crashed. The rest of the system remains intact.</p>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"p\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<pre class=\\\"text-xs text-slate-400 font-mono bg-slate-900/80 p-3 rounded-lg mb-6 overflow-auto max-h-40\\\">React is not defined</pre>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"pre\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.keyboard\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+       \"RGAAv4\",\u001b[39m\n\u001b[31m+       \"RGAA-9.2.1\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+ ]\u001b[39m\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:23:53",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 53,
                          "line": 23
                        },
                        "snippet": "\u001b[0m \u001b[90m 21 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 22 |\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 23 |\u001b[39m         expect(accessibilityScanResults\u001b[33m.\u001b[39mviolations)\u001b[33m.\u001b[39mtoEqual([])\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                                                     \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 24 |\u001b[39m     })\u001b[33m;\u001b[39m\n \u001b[90m 25 |\u001b[39m })\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 53,
                            "line": 23
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mreceived\u001b[39m\u001b[2m).\u001b[22mtoEqual\u001b[2m(\u001b[22m\u001b[32mexpected\u001b[39m\u001b[2m) // deep equality\u001b[22m\n\n\u001b[32m- Expected  -   1\u001b[39m\n\u001b[31m+ Received  + 149\u001b[39m\n\n\u001b[32m- Array []\u001b[39m\n\u001b[31m+ Array [\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure the document has a main landmark\",\u001b[39m\n\u001b[31m+     \"help\": \"Document should have one main landmark\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/landmark-one-main?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"landmark-one-main\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-main\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Document does not have a main landmark\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Document does not have a main landmark\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure that the page, or at least one of its frames contains a level-one heading\",\u001b[39m\n\u001b[31m+     \"help\": \"Page should contain a level-one heading\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/page-has-heading-one?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": null,\u001b[39m\n\u001b[31m+             \"id\": \"page-has-heading-one\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Page must have a level-one heading\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"any\": Array [],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix all of the following:\u001b[39m\n\u001b[31m+   Page must have a level-one heading\",\u001b[39m\n\u001b[31m+         \"html\": \"<html lang=\\\"en\\\" class=\\\"dark\\\" data-theme=\\\"dark\\\">\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"html\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.semantics\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+   Object {\u001b[39m\n\u001b[31m+     \"description\": \"Ensure all page content is contained by landmarks\",\u001b[39m\n\u001b[31m+     \"help\": \"All page content should be contained by landmarks\",\u001b[39m\n\u001b[31m+     \"helpUrl\": \"https://dequeuniversity.com/rules/axe/4.12/region?application=playwright\",\u001b[39m\n\u001b[31m+     \"id\": \"region\",\u001b[39m\n\u001b[31m+     \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+     \"nodes\": Array [\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<h2 class=\\\"text-xl font-mono font-bold text-[#ff0055] uppercase tracking-widest mb-2\\\">Dashboard Module Failure</h2>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"h2\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<p class=\\\"text-sm text-slate-400 font-mono mb-4\\\">A critical module in the admin dashboard has crashed. The rest of the system remains intact.</p>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"p\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+       Object {\u001b[39m\n\u001b[31m+         \"all\": Array [],\u001b[39m\n\u001b[31m+         \"any\": Array [\u001b[39m\n\u001b[31m+           Object {\u001b[39m\n\u001b[31m+             \"data\": Object {\u001b[39m\n\u001b[31m+               \"isIframe\": false,\u001b[39m\n\u001b[31m+             },\u001b[39m\n\u001b[31m+             \"id\": \"region\",\u001b[39m\n\u001b[31m+             \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+             \"message\": \"Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+             \"relatedNodes\": Array [],\u001b[39m\n\u001b[31m+           },\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+         \"failureSummary\": \"Fix any of the following:\u001b[39m\n\u001b[31m+   Some page content is not contained by landmarks\",\u001b[39m\n\u001b[31m+         \"html\": \"<pre class=\\\"text-xs text-slate-400 font-mono bg-slate-900/80 p-3 rounded-lg mb-6 overflow-auto max-h-40\\\">React is not defined</pre>\",\u001b[39m\n\u001b[31m+         \"impact\": \"moderate\",\u001b[39m\n\u001b[31m+         \"none\": Array [],\u001b[39m\n\u001b[31m+         \"target\": Array [\u001b[39m\n\u001b[31m+           \"pre\",\u001b[39m\n\u001b[31m+         ],\u001b[39m\n\u001b[31m+       },\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+     \"tags\": Array [\u001b[39m\n\u001b[31m+       \"cat.keyboard\",\u001b[39m\n\u001b[31m+       \"best-practice\",\u001b[39m\n\u001b[31m+       \"RGAAv4\",\u001b[39m\n\u001b[31m+       \"RGAA-9.2.1\",\u001b[39m\n\u001b[31m+     ],\u001b[39m\n\u001b[31m+   },\u001b[39m\n\u001b[31m+ ]\u001b[39m\n\n  21 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  22 |\n> 23 |         expect(accessibilityScanResults.violations).toEqual([]);\n     |                                                     ^\n  24 |     });\n  25 | });\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:23:53"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:11.367Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Chrome\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Chrome\\video-1.webm"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Chrome\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Chrome\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 53,
                        "line": 23
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-5cee6a536c5b96084d0d",
              "file": "e2e/accessibility.spec.ts",
              "line": 18,
              "column": 9
            },
            {
              "title": "Homepage should not have any automatically detectable accessibility issues",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Safari",
                  "projectName": "Mobile Safari",
                  "results": [
                    {
                      "workerIndex": 26,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 43,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:05:36.019Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-Mobile-Safari\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-48be1640fc89884f4421",
              "file": "e2e/accessibility.spec.ts",
              "line": 5,
              "column": 9
            },
            {
              "title": "Admin Dashboard should be accessible",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Safari",
                  "projectName": "Mobile Safari",
                  "results": [
                    {
                      "workerIndex": 27,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 8,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:05:37.025Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Safari\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-179c216fc7c0773a00af",
              "file": "e2e/accessibility.spec.ts",
              "line": 18,
              "column": 9
            }
          ]
        }
      ]
    },
    {
      "title": "e2e\\admin-dashboard.spec.ts",
      "file": "e2e/admin-dashboard.spec.ts",
      "column": 0,
      "line": 0,
      "specs": [],
      "suites": [
        {
          "title": "SupremeAI Nexus E2E Flow",
          "file": "e2e/admin-dashboard.spec.ts",
          "line": 3,
          "column": 6,
          "specs": [
            {
              "title": "should load the dashboard and verify Java Worker widget",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "chromium",
                  "projectName": "chromium",
                  "results": [
                    {
                      "workerIndex": 0,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 10854,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByText('SupremeAI')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByText('SupremeAI')\u001b[22m\n",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByText('SupremeAI')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByText('SupremeAI')\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:10:47",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                          "column": 47,
                          "line": 10
                        },
                        "snippet": "\u001b[0m \u001b[90m  8 |\u001b[39m\n \u001b[90m  9 |\u001b[39m     \u001b[90m// 2. Verify Nexus Header exists\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 10 |\u001b[39m     \u001b[36mawait\u001b[39m expect(page\u001b[33m.\u001b[39mgetByText(\u001b[32m'SupremeAI'\u001b[39m))\u001b[33m.\u001b[39mtoBeVisible()\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                                               \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 11 |\u001b[39m\n \u001b[90m 12 |\u001b[39m     \u001b[90m// 3. Verify Java Background Worker widget is rendered\u001b[39m\n \u001b[90m 13 |\u001b[39m     \u001b[36mconst\u001b[39m workerWidget \u001b[33m=\u001b[39m page\u001b[33m.\u001b[39mlocator(\u001b[32m'text=Java Background Worker'\u001b[39m)\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                            "column": 47,
                            "line": 10
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByText('SupremeAI')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByText('SupremeAI')\u001b[22m\n\n\n   8 |\n   9 |     // 2. Verify Nexus Header exists\n> 10 |     await expect(page.getByText('SupremeAI')).toBeVisible();\n     |                                               ^\n  11 |\n  12 |     // 3. Verify Java Background Worker widget is rendered\n  13 |     const workerWidget = page.locator('text=Java Background Worker');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:10:47"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:03.587Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-chromium\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-chromium\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-chromium\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                        "column": 47,
                        "line": 10
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-840ff965689fe6d6493e",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 5,
              "column": 7
            },
            {
              "title": "should be able to submit an orchestration command via chat",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "chromium",
                  "projectName": "chromium",
                  "results": [
                    {
                      "workerIndex": 2,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 9961,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByPlaceholder('[SupremeAI Nexus Command...]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByPlaceholder('[SupremeAI Nexus Command...]')\u001b[22m\n",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByPlaceholder('[SupremeAI Nexus Command...]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByPlaceholder('[SupremeAI Nexus Command...]')\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:30:29",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                          "column": 29,
                          "line": 30
                        },
                        "snippet": "\u001b[0m \u001b[90m 28 |\u001b[39m     \u001b[90m// Find the chat input\u001b[39m\n \u001b[90m 29 |\u001b[39m     \u001b[36mconst\u001b[39m chatInput \u001b[33m=\u001b[39m page\u001b[33m.\u001b[39mgetByPlaceholder(\u001b[32m'[SupremeAI Nexus Command...]'\u001b[39m)\u001b[33m;\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 30 |\u001b[39m     \u001b[36mawait\u001b[39m expect(chatInput)\u001b[33m.\u001b[39mtoBeVisible()\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                             \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 31 |\u001b[39m\n \u001b[90m 32 |\u001b[39m     \u001b[90m// Type a command that would theoretically trigger a background Java task\u001b[39m\n \u001b[90m 33 |\u001b[39m     \u001b[36mawait\u001b[39m chatInput\u001b[33m.\u001b[39mfill(\u001b[32m'Run full system security audit'\u001b[39m)\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                            "column": 29,
                            "line": 30
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByPlaceholder('[SupremeAI Nexus Command...]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByPlaceholder('[SupremeAI Nexus Command...]')\u001b[22m\n\n\n  28 |     // Find the chat input\n  29 |     const chatInput = page.getByPlaceholder('[SupremeAI Nexus Command...]');\n> 30 |     await expect(chatInput).toBeVisible();\n     |                             ^\n  31 |\n  32 |     // Type a command that would theoretically trigger a background Java task\n  33 |     await chatInput.fill('Run full system security audit');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:30:29"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:08.176Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-chromium\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-chromium\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-chromium\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                        "column": 29,
                        "line": 30
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-a9afdc9c7cb2ed54a956",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 25,
              "column": 7
            },
            {
              "title": "should load the dashboard and verify Java Worker widget",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "firefox",
                  "projectName": "firefox",
                  "results": [
                    {
                      "workerIndex": 8,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 4,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:45.754Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-firefox\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-491652adea7e374b3693",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 5,
              "column": 7
            },
            {
              "title": "should be able to submit an orchestration command via chat",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "firefox",
                  "projectName": "firefox",
                  "results": [
                    {
                      "workerIndex": 9,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 4,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:47.558Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-firefox\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-e3e38dbe989ee2539352",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 25,
              "column": 7
            },
            {
              "title": "should load the dashboard and verify Java Worker widget",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "webkit",
                  "projectName": "webkit",
                  "results": [
                    {
                      "workerIndex": 15,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 5,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:02.401Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-webkit\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-3fb10350a03b26d5bbcc",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 5,
              "column": 7
            },
            {
              "title": "should be able to submit an orchestration command via chat",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "webkit",
                  "projectName": "webkit",
                  "results": [
                    {
                      "workerIndex": 16,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 10,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:04.722Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-webkit\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-f0da5eebe25779d0cd1e",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 25,
              "column": 7
            },
            {
              "title": "should load the dashboard and verify Java Worker widget",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Chrome",
                  "projectName": "Mobile Chrome",
                  "results": [
                    {
                      "workerIndex": 20,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 10095,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByText('SupremeAI')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByText('SupremeAI')\u001b[22m\n",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByText('SupremeAI')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByText('SupremeAI')\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:10:47",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                          "column": 47,
                          "line": 10
                        },
                        "snippet": "\u001b[0m \u001b[90m  8 |\u001b[39m\n \u001b[90m  9 |\u001b[39m     \u001b[90m// 2. Verify Nexus Header exists\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 10 |\u001b[39m     \u001b[36mawait\u001b[39m expect(page\u001b[33m.\u001b[39mgetByText(\u001b[32m'SupremeAI'\u001b[39m))\u001b[33m.\u001b[39mtoBeVisible()\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                                               \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 11 |\u001b[39m\n \u001b[90m 12 |\u001b[39m     \u001b[90m// 3. Verify Java Background Worker widget is rendered\u001b[39m\n \u001b[90m 13 |\u001b[39m     \u001b[36mconst\u001b[39m workerWidget \u001b[33m=\u001b[39m page\u001b[33m.\u001b[39mlocator(\u001b[32m'text=Java Background Worker'\u001b[39m)\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                            "column": 47,
                            "line": 10
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByText('SupremeAI')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByText('SupremeAI')\u001b[22m\n\n\n   8 |\n   9 |     // 2. Verify Nexus Header exists\n> 10 |     await expect(page.getByText('SupremeAI')).toBeVisible();\n     |                                               ^\n  11 |\n  12 |     // 3. Verify Java Background Worker widget is rendered\n  13 |     const workerWidget = page.locator('text=Java Background Worker');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:10:47"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:33.856Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-Mobile-Chrome\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-Mobile-Chrome\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-Mobile-Chrome\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                        "column": 47,
                        "line": 10
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-7d5549aefb4e4e2fcf77",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 5,
              "column": 7
            },
            {
              "title": "should be able to submit an orchestration command via chat",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Chrome",
                  "projectName": "Mobile Chrome",
                  "results": [
                    {
                      "workerIndex": 22,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 9169,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByPlaceholder('[SupremeAI Nexus Command...]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByPlaceholder('[SupremeAI Nexus Command...]')\u001b[22m\n",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByPlaceholder('[SupremeAI Nexus Command...]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByPlaceholder('[SupremeAI Nexus Command...]')\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:30:29",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                          "column": 29,
                          "line": 30
                        },
                        "snippet": "\u001b[0m \u001b[90m 28 |\u001b[39m     \u001b[90m// Find the chat input\u001b[39m\n \u001b[90m 29 |\u001b[39m     \u001b[36mconst\u001b[39m chatInput \u001b[33m=\u001b[39m page\u001b[33m.\u001b[39mgetByPlaceholder(\u001b[32m'[SupremeAI Nexus Command...]'\u001b[39m)\u001b[33m;\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 30 |\u001b[39m     \u001b[36mawait\u001b[39m expect(chatInput)\u001b[33m.\u001b[39mtoBeVisible()\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                             \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 31 |\u001b[39m\n \u001b[90m 32 |\u001b[39m     \u001b[90m// Type a command that would theoretically trigger a background Java task\u001b[39m\n \u001b[90m 33 |\u001b[39m     \u001b[36mawait\u001b[39m chatInput\u001b[33m.\u001b[39mfill(\u001b[32m'Run full system security audit'\u001b[39m)\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                            "column": 29,
                            "line": 30
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: getByPlaceholder('[SupremeAI Nexus Command...]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for getByPlaceholder('[SupremeAI Nexus Command...]')\u001b[22m\n\n\n  28 |     // Find the chat input\n  29 |     const chatInput = page.getByPlaceholder('[SupremeAI Nexus Command...]');\n> 30 |     await expect(chatInput).toBeVisible();\n     |                             ^\n  31 |\n  32 |     // Type a command that would theoretically trigger a background Java task\n  33 |     await chatInput.fill('Run full system security audit');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts:30:29"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:40.757Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-Mobile-Chrome\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-Mobile-Chrome\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-Mobile-Chrome\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\admin-dashboard.spec.ts",
                        "column": 29,
                        "line": 30
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-8ceedf6c266ee271f8e8",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 25,
              "column": 7
            },
            {
              "title": "should load the dashboard and verify Java Worker widget",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Safari",
                  "projectName": "Mobile Safari",
                  "results": [
                    {
                      "workerIndex": 28,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 7,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:05:39.129Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-08ee0-d-verify-Java-Worker-widget-Mobile-Safari\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-41f61f78135bde40d262",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 5,
              "column": 7
            },
            {
              "title": "should be able to submit an orchestration command via chat",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Safari",
                  "projectName": "Mobile Safari",
                  "results": [
                    {
                      "workerIndex": 29,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 5,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:05:39.889Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-admin-dashboard-Suprem-8ff1e-hestration-command-via-chat-Mobile-Safari\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "7475c5559e3e24f1e588-14e856414cbc68b42523",
              "file": "e2e/admin-dashboard.spec.ts",
              "line": 25,
              "column": 7
            }
          ]
        }
      ]
    },
    {
      "title": "e2e\\chat.spec.ts",
      "file": "e2e/chat.spec.ts",
      "column": 0,
      "line": 0,
      "specs": [
        {
          "title": "Chat sends message",
          "ok": false,
          "tags": [],
          "tests": [
            {
              "timeout": 30000,
              "annotations": [],
              "expectedStatus": "passed",
              "projectId": "chromium",
              "projectName": "chromium",
              "results": [
                {
                  "workerIndex": 3,
                  "parallelIndex": 0,
                  "status": "timedOut",
                  "duration": 30585,
                  "error": {
                    "message": "\u001b[31mTest timeout of 30000ms exceeded.\u001b[39m",
                    "stack": "\u001b[31mTest timeout of 30000ms exceeded.\u001b[39m"
                  },
                  "errors": [
                    {
                      "message": "\u001b[31mTest timeout of 30000ms exceeded.\u001b[39m"
                    },
                    {
                      "location": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts",
                        "column": 14,
                        "line": 5
                      },
                      "message": "Error: page.fill: Test timeout of 30000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"chat-input\"]')\u001b[22m\n\n\n  3 | test('Chat sends message', async ({ page }) => {\n  4 |   await page.goto('/');\n> 5 |   await page.fill('[data-testid=\"chat-input\"]', 'Hello SupremeAI!');\n    |              ^\n  6 |   await page.click('[data-testid=\"chat-submit\"]');\n  7 |   await expect(page.getByText('Hello SupremeAI!').first()).toBeVisible();\n  8 | });\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts:5:14"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T13:03:16.212Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "screenshot",
                      "contentType": "image/png",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-chromium\\test-failed-1.png"
                    },
                    {
                      "name": "video",
                      "contentType": "video/webm",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-chromium\\video.webm"
                    },
                    {
                      "name": "error-context",
                      "contentType": "text/markdown",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-chromium\\error-context.md"
                    }
                  ]
                }
              ],
              "status": "unexpected"
            }
          ],
          "id": "d3d49f2899a0faacbcaf-be505a322d854db5b764",
          "file": "e2e/chat.spec.ts",
          "line": 3,
          "column": 5
        },
        {
          "title": "Chat sends message",
          "ok": false,
          "tags": [],
          "tests": [
            {
              "timeout": 30000,
              "annotations": [],
              "expectedStatus": "passed",
              "projectId": "firefox",
              "projectName": "firefox",
              "results": [
                {
                  "workerIndex": 10,
                  "parallelIndex": 1,
                  "status": "failed",
                  "duration": 13,
                  "error": {
                    "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                    "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                  },
                  "errors": [
                    {
                      "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T13:03:49.221Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "error-context",
                      "contentType": "text/markdown",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-firefox\\error-context.md"
                    }
                  ]
                }
              ],
              "status": "unexpected"
            }
          ],
          "id": "d3d49f2899a0faacbcaf-854e5b451a7a4fe0eaa9",
          "file": "e2e/chat.spec.ts",
          "line": 3,
          "column": 5
        },
        {
          "title": "Chat sends message",
          "ok": false,
          "tags": [],
          "tests": [
            {
              "timeout": 30000,
              "annotations": [],
              "expectedStatus": "passed",
              "projectId": "webkit",
              "projectName": "webkit",
              "results": [
                {
                  "workerIndex": 17,
                  "parallelIndex": 1,
                  "status": "failed",
                  "duration": 4,
                  "error": {
                    "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                    "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                  },
                  "errors": [
                    {
                      "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T13:04:05.409Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "error-context",
                      "contentType": "text/markdown",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-webkit\\error-context.md"
                    }
                  ]
                }
              ],
              "status": "unexpected"
            }
          ],
          "id": "d3d49f2899a0faacbcaf-e4c03f84f24ec542393f",
          "file": "e2e/chat.spec.ts",
          "line": 3,
          "column": 5
        },
        {
          "title": "Chat sends message",
          "ok": false,
          "tags": [],
          "tests": [
            {
              "timeout": 30000,
              "annotations": [],
              "expectedStatus": "passed",
              "projectId": "Mobile Chrome",
              "projectName": "Mobile Chrome",
              "results": [
                {
                  "workerIndex": 23,
                  "parallelIndex": 0,
                  "status": "timedOut",
                  "duration": 31412,
                  "error": {
                    "message": "\u001b[31mTest timeout of 30000ms exceeded.\u001b[39m",
                    "stack": "\u001b[31mTest timeout of 30000ms exceeded.\u001b[39m"
                  },
                  "errors": [
                    {
                      "message": "\u001b[31mTest timeout of 30000ms exceeded.\u001b[39m"
                    },
                    {
                      "location": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts",
                        "column": 14,
                        "line": 5
                      },
                      "message": "Error: page.fill: Test timeout of 30000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"chat-input\"]')\u001b[22m\n\n\n  3 | test('Chat sends message', async ({ page }) => {\n  4 |   await page.goto('/');\n> 5 |   await page.fill('[data-testid=\"chat-input\"]', 'Hello SupremeAI!');\n    |              ^\n  6 |   await page.click('[data-testid=\"chat-submit\"]');\n  7 |   await expect(page.getByText('Hello SupremeAI!').first()).toBeVisible();\n  8 | });\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts:5:14"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T13:04:46.547Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "screenshot",
                      "contentType": "image/png",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-Mobile-Chrome\\test-failed-1.png"
                    },
                    {
                      "name": "video",
                      "contentType": "video/webm",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-Mobile-Chrome\\video.webm"
                    },
                    {
                      "name": "error-context",
                      "contentType": "text/markdown",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-Mobile-Chrome\\error-context.md"
                    }
                  ]
                }
              ],
              "status": "unexpected"
            }
          ],
          "id": "d3d49f2899a0faacbcaf-b7664ec793cc8f664ec7",
          "file": "e2e/chat.spec.ts",
          "line": 3,
          "column": 5
        },
        {
          "title": "Chat sends message",
          "ok": false,
          "tags": [],
          "tests": [
            {
              "timeout": 30000,
              "annotations": [],
              "expectedStatus": "passed",
              "projectId": "Mobile Safari",
              "projectName": "Mobile Safari",
              "results": [
                {
                  "workerIndex": 30,
                  "parallelIndex": 0,
                  "status": "failed",
                  "duration": 9,
                  "error": {
                    "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                    "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                  },
                  "errors": [
                    {
                      "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T13:05:41.890Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "error-context",
                      "contentType": "text/markdown",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-Mobile-Safari\\error-context.md"
                    }
                  ]
                }
              ],
              "status": "unexpected"
            }
          ],
          "id": "d3d49f2899a0faacbcaf-ecff017140e0eb8cba6b",
          "file": "e2e/chat.spec.ts",
          "line": 3,
          "column": 5
        }
      ]
    },
    {
      "title": "e2e\\visual.spec.ts",
      "file": "e2e/visual.spec.ts",
      "column": 0,
      "line": 0,
      "specs": [],
      "suites": [
        {
          "title": "Visual Regression Tests",
          "file": "e2e/visual.spec.ts",
          "line": 3,
          "column": 6,
          "specs": [
            {
              "title": "Homepage layout should be stable",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "chromium",
                  "projectName": "chromium",
                  "results": [
                    {
                      "workerIndex": 4,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 7162,
                      "error": {
                        "message": "Error: A snapshot doesn't exist at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-chromium-win32.png, writing actual.",
                        "stack": "Error: A snapshot doesn't exist at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-chromium-win32.png, writing actual.\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:7:9",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                          "column": 9,
                          "line": 7
                        },
                        "snippet": "\u001b[0m \u001b[90m  5 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/'\u001b[39m)\u001b[33m;\u001b[39m\n \u001b[90m  6 |\u001b[39m         \u001b[90m// পুরো পেজের স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m  7 |\u001b[39m         \u001b[36mawait\u001b[39m expect(page)\u001b[33m.\u001b[39mtoHaveScreenshot(\u001b[32m'homepage-stable.png'\u001b[39m\u001b[33m,\u001b[39m { fullPage\u001b[33m:\u001b[39m \u001b[36mtrue\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m         \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m  8 |\u001b[39m     })\u001b[33m;\u001b[39m\n \u001b[90m  9 |\u001b[39m\n \u001b[90m 10 |\u001b[39m     test(\u001b[32m'ConsentMatrixModal should match the approved snapshot'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                            "column": 9,
                            "line": 7
                          },
                          "message": "Error: A snapshot doesn't exist at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-chromium-win32.png, writing actual.\n\n   5 |         await page.goto('/');\n   6 |         // পুরো পেজের স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\n>  7 |         await expect(page).toHaveScreenshot('homepage-stable.png', { fullPage: true });\n     |         ^\n   8 |     });\n   9 |\n  10 |     test('ConsentMatrixModal should match the approved snapshot', async ({ page }) => {\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:7:9"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:19.906Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "homepage-stable-expected.png",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-chromium-win32.png"
                        },
                        {
                          "name": "homepage-stable-actual.png",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-chromium\\homepage-stable-actual.png"
                        },
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-chromium\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-chromium\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-chromium\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                        "column": 9,
                        "line": 7
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-dc5a1598abe98c016bca",
              "file": "e2e/visual.spec.ts",
              "line": 4,
              "column": 9
            },
            {
              "title": "ConsentMatrixModal should match the approved snapshot",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "chromium",
                  "projectName": "chromium",
                  "results": [
                    {
                      "workerIndex": 5,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 7841,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: locator('[data-testid=\"consent-matrix-modal\"]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for locator('[data-testid=\"consent-matrix-modal\"]')\u001b[22m\n",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: locator('[data-testid=\"consent-matrix-modal\"]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for locator('[data-testid=\"consent-matrix-modal\"]')\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:16:29",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                          "column": 29,
                          "line": 16
                        },
                        "snippet": "\u001b[0m \u001b[90m 14 |\u001b[39m         \u001b[90m// একটি নির্দিষ্ট data-testid দিয়ে মোডালটি লোকেট করা হচ্ছে\u001b[39m\n \u001b[90m 15 |\u001b[39m         \u001b[36mconst\u001b[39m modal \u001b[33m=\u001b[39m page\u001b[33m.\u001b[39mlocator(\u001b[32m'[data-testid=\"consent-matrix-modal\"]'\u001b[39m)\u001b[33m;\u001b[39m \u001b[90m// এখানে আপনার মোডালের আসল সিলেক্টর ব্যবহার করুন\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 16 |\u001b[39m         \u001b[36mawait\u001b[39m expect(modal)\u001b[33m.\u001b[39mtoBeVisible()\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                             \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 17 |\u001b[39m\n \u001b[90m 18 |\u001b[39m         \u001b[90m// শুধুমাত্র মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\u001b[39m\n \u001b[90m 19 |\u001b[39m         \u001b[36mawait\u001b[39m expect(modal)\u001b[33m.\u001b[39mtoHaveScreenshot(\u001b[32m'consent-matrix-critical-risk.png'\u001b[39m)\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                            "column": 29,
                            "line": 16
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: locator('[data-testid=\"consent-matrix-modal\"]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for locator('[data-testid=\"consent-matrix-modal\"]')\u001b[22m\n\n\n  14 |         // একটি নির্দিষ্ট data-testid দিয়ে মোডালটি লোকেট করা হচ্ছে\n  15 |         const modal = page.locator('[data-testid=\"consent-matrix-modal\"]'); // এখানে আপনার মোডালের আসল সিলেক্টর ব্যবহার করুন\n> 16 |         await expect(modal).toBeVisible();\n     |                             ^\n  17 |\n  18 |         // শুধুমাত্র মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\n  19 |         await expect(modal).toHaveScreenshot('consent-matrix-critical-risk.png');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:16:29"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:32.573Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-chromium\\test-failed-1.png"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-chromium\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                        "column": 29,
                        "line": 16
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-8037f4050504ad1a666a",
              "file": "e2e/visual.spec.ts",
              "line": 10,
              "column": 9
            },
            {
              "title": "Homepage layout should be stable",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "firefox",
                  "projectName": "firefox",
                  "results": [
                    {
                      "workerIndex": 11,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 6,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:54.717Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-firefox\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-2572ae0a7a8be5e48ed4",
              "file": "e2e/visual.spec.ts",
              "line": 4,
              "column": 9
            },
            {
              "title": "ConsentMatrixModal should match the approved snapshot",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "firefox",
                  "projectName": "firefox",
                  "results": [
                    {
                      "workerIndex": 12,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 14,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\firefox-1532\\firefox\\firefox.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:03:58.649Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-firefox\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-7aa150462037e5056178",
              "file": "e2e/visual.spec.ts",
              "line": 10,
              "column": 9
            },
            {
              "title": "Homepage layout should be stable",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "webkit",
                  "projectName": "webkit",
                  "results": [
                    {
                      "workerIndex": 18,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 6,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:06.845Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-webkit\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-e9f73064758d39616942",
              "file": "e2e/visual.spec.ts",
              "line": 4,
              "column": 9
            },
            {
              "title": "ConsentMatrixModal should match the approved snapshot",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "webkit",
                  "projectName": "webkit",
                  "results": [
                    {
                      "workerIndex": 19,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 6,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:08.425Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-webkit\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-94e53f1a404ed38555d8",
              "file": "e2e/visual.spec.ts",
              "line": 10,
              "column": 9
            },
            {
              "title": "Homepage layout should be stable",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Chrome",
                  "projectName": "Mobile Chrome",
                  "results": [
                    {
                      "workerIndex": 24,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 10784,
                      "error": {
                        "message": "Error: A snapshot doesn't exist at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-Mobile-Chrome-win32.png, writing actual.",
                        "stack": "Error: A snapshot doesn't exist at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-Mobile-Chrome-win32.png, writing actual.\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:7:9",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                          "column": 9,
                          "line": 7
                        },
                        "snippet": "\u001b[0m \u001b[90m  5 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/'\u001b[39m)\u001b[33m;\u001b[39m\n \u001b[90m  6 |\u001b[39m         \u001b[90m// পুরো পেজের স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m  7 |\u001b[39m         \u001b[36mawait\u001b[39m expect(page)\u001b[33m.\u001b[39mtoHaveScreenshot(\u001b[32m'homepage-stable.png'\u001b[39m\u001b[33m,\u001b[39m { fullPage\u001b[33m:\u001b[39m \u001b[36mtrue\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m         \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m  8 |\u001b[39m     })\u001b[33m;\u001b[39m\n \u001b[90m  9 |\u001b[39m\n \u001b[90m 10 |\u001b[39m     test(\u001b[32m'ConsentMatrixModal should match the approved snapshot'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                            "column": 9,
                            "line": 7
                          },
                          "message": "Error: A snapshot doesn't exist at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-Mobile-Chrome-win32.png, writing actual.\n\n   5 |         await page.goto('/');\n   6 |         // পুরো পেজের স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\n>  7 |         await expect(page).toHaveScreenshot('homepage-stable.png', { fullPage: true });\n     |         ^\n   8 |     });\n   9 |\n  10 |     test('ConsentMatrixModal should match the approved snapshot', async ({ page }) => {\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:7:9"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:04:55.059Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "homepage-stable-expected.png",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts-snapshots\\homepage-stable-Mobile-Chrome-win32.png"
                        },
                        {
                          "name": "homepage-stable-actual.png",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-Mobile-Chrome\\homepage-stable-actual.png"
                        },
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-Mobile-Chrome\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-Mobile-Chrome\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-Mobile-Chrome\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                        "column": 9,
                        "line": 7
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-834895d1735964042720",
              "file": "e2e/visual.spec.ts",
              "line": 4,
              "column": 9
            },
            {
              "title": "ConsentMatrixModal should match the approved snapshot",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Chrome",
                  "projectName": "Mobile Chrome",
                  "results": [
                    {
                      "workerIndex": 25,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 9175,
                      "error": {
                        "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: locator('[data-testid=\"consent-matrix-modal\"]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for locator('[data-testid=\"consent-matrix-modal\"]')\u001b[22m\n",
                        "stack": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: locator('[data-testid=\"consent-matrix-modal\"]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for locator('[data-testid=\"consent-matrix-modal\"]')\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:16:29",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                          "column": 29,
                          "line": 16
                        },
                        "snippet": "\u001b[0m \u001b[90m 14 |\u001b[39m         \u001b[90m// একটি নির্দিষ্ট data-testid দিয়ে মোডালটি লোকেট করা হচ্ছে\u001b[39m\n \u001b[90m 15 |\u001b[39m         \u001b[36mconst\u001b[39m modal \u001b[33m=\u001b[39m page\u001b[33m.\u001b[39mlocator(\u001b[32m'[data-testid=\"consent-matrix-modal\"]'\u001b[39m)\u001b[33m;\u001b[39m \u001b[90m// এখানে আপনার মোডালের আসল সিলেক্টর ব্যবহার করুন\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 16 |\u001b[39m         \u001b[36mawait\u001b[39m expect(modal)\u001b[33m.\u001b[39mtoBeVisible()\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                             \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 17 |\u001b[39m\n \u001b[90m 18 |\u001b[39m         \u001b[90m// শুধুমাত্র মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\u001b[39m\n \u001b[90m 19 |\u001b[39m         \u001b[36mawait\u001b[39m expect(modal)\u001b[33m.\u001b[39mtoHaveScreenshot(\u001b[32m'consent-matrix-critical-risk.png'\u001b[39m)\u001b[33m;\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                            "column": 29,
                            "line": 16
                          },
                          "message": "Error: \u001b[2mexpect(\u001b[22m\u001b[31mlocator\u001b[39m\u001b[2m).\u001b[22mtoBeVisible\u001b[2m(\u001b[22m\u001b[2m)\u001b[22m failed\n\nLocator: locator('[data-testid=\"consent-matrix-modal\"]')\nExpected: visible\nTimeout: 5000ms\nError: element(s) not found\n\nCall log:\n\u001b[2m  - Expect \"toBeVisible\" with timeout 5000ms\u001b[22m\n\u001b[2m  - waiting for locator('[data-testid=\"consent-matrix-modal\"]')\u001b[22m\n\n\n  14 |         // একটি নির্দিষ্ট data-testid দিয়ে মোডালটি লোকেট করা হচ্ছে\n  15 |         const modal = page.locator('[data-testid=\"consent-matrix-modal\"]'); // এখানে আপনার মোডালের আসল সিলেক্টর ব্যবহার করুন\n> 16 |         await expect(modal).toBeVisible();\n     |                             ^\n  17 |\n  18 |         // শুধুমাত্র মোডালটির স্ক্রিনশট নিয়ে বেসলাইনের সাথে তুলনা করুন\n  19 |         await expect(modal).toHaveScreenshot('consent-matrix-critical-risk.png');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts:16:29"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:05:22.549Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-Mobile-Chrome\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-Mobile-Chrome\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-Mobile-Chrome\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\visual.spec.ts",
                        "column": 29,
                        "line": 16
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-f87f36e1bffe90bdadc9",
              "file": "e2e/visual.spec.ts",
              "line": 10,
              "column": 9
            },
            {
              "title": "Homepage layout should be stable",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Safari",
                  "projectName": "Mobile Safari",
                  "results": [
                    {
                      "workerIndex": 31,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 6,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:05:42.317Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-520ca-age-layout-should-be-stable-Mobile-Safari\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-10a9138ada1b2440200a",
              "file": "e2e/visual.spec.ts",
              "line": 4,
              "column": 9
            },
            {
              "title": "ConsentMatrixModal should match the approved snapshot",
              "ok": false,
              "tags": [],
              "tests": [
                {
                  "timeout": 30000,
                  "annotations": [],
                  "expectedStatus": "passed",
                  "projectId": "Mobile Safari",
                  "projectName": "Mobile Safari",
                  "results": [
                    {
                      "workerIndex": 32,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 7,
                      "error": {
                        "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝",
                        "stack": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                      },
                      "errors": [
                        {
                          "message": "Error: browserType.launch: Executable doesn't exist at C:\\Users\\n\\AppData\\Local\\ms-playwright\\webkit-2311\\Playwright.exe\n╔════════════════════════════════════════════════════════════╗\n║ Looks like Playwright was just installed or updated.       ║\n║ Please run the following command to download new browsers: ║\n║                                                            ║\n║     pnpm exec playwright install                           ║\n║                                                            ║\n║ <3 Playwright Team                                         ║\n╚════════════════════════════════════════════════════════════╝"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T13:05:43.571Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-visual-Visual-Regressi-e7857-match-the-approved-snapshot-Mobile-Safari\\error-context.md"
                        }
                      ]
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "51b4790f4568975de141-4bc9c5c49bc221b29e60",
              "file": "e2e/visual.spec.ts",
              "line": 10,
              "column": 9
            }
          ]
        }
      ]
    }
  ],
  "errors": [],
  "stats": {
    "startTime": "2026-07-04T13:02:34.462Z",
    "duration": 189566.321,
    "expected": 2,
    "skipped": 0,
    "unexpected": 33,
    "flaky": 0
  }
}
```