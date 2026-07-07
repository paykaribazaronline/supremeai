# 📄 ফাইল: test-results/e2e-report.json

**প্রকার:** .json  
**সাইজ:** 75,587 বাইট  
**আপডেট:** 2026-07-07T19:14:31.229091

---

## কোড

```json
{
  "config": {
    "argv": [
      "C:\\Program Files\\nodejs\\node.exe",
      "C:\\Users\\n\\supremeai\\supremeai_2.0\\node_modules\\@playwright\\test\\cli.js",
      "test",
      "tests/e2e/chat.spec.ts",
      "tests/e2e/accessibility.spec.ts"
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
                      "duration": 22242,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 7
                        },
                        "snippet": "\u001b[0m \u001b[90m  5 |\u001b[39m     test(\u001b[32m'Homepage should not have any automatically detectable accessibility issues'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m  6 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/'\u001b[39m)\u001b[33m;\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m  7 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'[data-testid=\"dashboard-sidebar\"]'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m  8 |\u001b[39m\n \u001b[90m  9 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 10 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 7
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n\n   5 |     test('Homepage should not have any automatically detectable accessibility issues', async ({ page }) => {\n   6 |         await page.goto('/');\n>  7 |         await page.waitForSelector('[data-testid=\"dashboard-sidebar\"]', { state: 'visible', timeout: 15000 });\n     |                    ^\n   8 |\n   9 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  10 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:11:17.588Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-chromium\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-chromium\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-chromium\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 7
                      }
                    }
                  ],
                  "status": "unexpected"
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
                      "duration": 22362,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 21
                        },
                        "snippet": "\u001b[0m \u001b[90m 19 |\u001b[39m     test(\u001b[32m'Admin Dashboard should be accessible'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m 20 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/admin'\u001b[39m)\u001b[33m;\u001b[39m \u001b[90m// আপনার অ্যাডমিন পেজের URL\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 21 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'text=Admin Gate'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 22 |\u001b[39m\n \u001b[90m 23 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 24 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 21
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n\n  19 |     test('Admin Dashboard should be accessible', async ({ page }) => {\n  20 |         await page.goto('/admin'); // আপনার অ্যাডমিন পেজের URL\n> 21 |         await page.waitForSelector('text=Admin Gate', { state: 'visible', timeout: 15000 });\n     |                    ^\n  22 |\n  23 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  24 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:11:17.532Z",
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
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-chromium\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 21
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-500664d0b985aac1baa3",
              "file": "e2e/accessibility.spec.ts",
              "line": 19,
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
                      "workerIndex": 3,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 26730,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 7
                        },
                        "snippet": "\u001b[0m \u001b[90m  5 |\u001b[39m     test(\u001b[32m'Homepage should not have any automatically detectable accessibility issues'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m  6 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/'\u001b[39m)\u001b[33m;\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m  7 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'[data-testid=\"dashboard-sidebar\"]'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m  8 |\u001b[39m\n \u001b[90m  9 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 10 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 7
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n\n   5 |     test('Homepage should not have any automatically detectable accessibility issues', async ({ page }) => {\n   6 |         await page.goto('/');\n>  7 |         await page.waitForSelector('[data-testid=\"dashboard-sidebar\"]', { state: 'visible', timeout: 15000 });\n     |                    ^\n   8 |\n   9 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  10 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:11:41.287Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-firefox\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-firefox\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-firefox\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 7
                      }
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
                      "workerIndex": 4,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 22553,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 21
                        },
                        "snippet": "\u001b[0m \u001b[90m 19 |\u001b[39m     test(\u001b[32m'Admin Dashboard should be accessible'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m 20 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/admin'\u001b[39m)\u001b[33m;\u001b[39m \u001b[90m// আপনার অ্যাডমিন পেজের URL\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 21 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'text=Admin Gate'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 22 |\u001b[39m\n \u001b[90m 23 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 24 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 21
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n\n  19 |     test('Admin Dashboard should be accessible', async ({ page }) => {\n  20 |         await page.goto('/admin'); // আপনার অ্যাডমিন পেজের URL\n> 21 |         await page.waitForSelector('text=Admin Gate', { state: 'visible', timeout: 15000 });\n     |                    ^\n  22 |\n  23 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  24 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:12:13.347Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-firefox\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-firefox\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-firefox\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 21
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-f5638b87b7a414080a2d",
              "file": "e2e/accessibility.spec.ts",
              "line": 19,
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
                      "workerIndex": 6,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 17065,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 7
                        },
                        "snippet": "\u001b[0m \u001b[90m  5 |\u001b[39m     test(\u001b[32m'Homepage should not have any automatically detectable accessibility issues'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m  6 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/'\u001b[39m)\u001b[33m;\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m  7 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'[data-testid=\"dashboard-sidebar\"]'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m  8 |\u001b[39m\n \u001b[90m  9 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 10 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 7
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n\n   5 |     test('Homepage should not have any automatically detectable accessibility issues', async ({ page }) => {\n   6 |         await page.goto('/');\n>  7 |         await page.waitForSelector('[data-testid=\"dashboard-sidebar\"]', { state: 'visible', timeout: 15000 });\n     |                    ^\n   8 |\n   9 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  10 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:12:38.247Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-webkit\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-webkit\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-webkit\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 7
                      }
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
                      "workerIndex": 7,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 17432,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 21
                        },
                        "snippet": "\u001b[0m \u001b[90m 19 |\u001b[39m     test(\u001b[32m'Admin Dashboard should be accessible'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m 20 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/admin'\u001b[39m)\u001b[33m;\u001b[39m \u001b[90m// আপনার অ্যাডমিন পেজের URL\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 21 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'text=Admin Gate'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 22 |\u001b[39m\n \u001b[90m 23 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 24 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 21
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n\n  19 |     test('Admin Dashboard should be accessible', async ({ page }) => {\n  20 |         await page.goto('/admin'); // আপনার অ্যাডমিন পেজের URL\n> 21 |         await page.waitForSelector('text=Admin Gate', { state: 'visible', timeout: 15000 });\n     |                    ^\n  22 |\n  23 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  24 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:12:49.426Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-webkit\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-webkit\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-webkit\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 21
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-080eb04c5c3f482efa54",
              "file": "e2e/accessibility.spec.ts",
              "line": 19,
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
                  "projectId": "Mobile Chrome",
                  "projectName": "Mobile Chrome",
                  "results": [
                    {
                      "workerIndex": 9,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 17056,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 7
                        },
                        "snippet": "\u001b[0m \u001b[90m  5 |\u001b[39m     test(\u001b[32m'Homepage should not have any automatically detectable accessibility issues'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m  6 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/'\u001b[39m)\u001b[33m;\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m  7 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'[data-testid=\"dashboard-sidebar\"]'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m  8 |\u001b[39m\n \u001b[90m  9 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 10 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 7
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n\n   5 |     test('Homepage should not have any automatically detectable accessibility issues', async ({ page }) => {\n   6 |         await page.goto('/');\n>  7 |         await page.waitForSelector('[data-testid=\"dashboard-sidebar\"]', { state: 'visible', timeout: 15000 });\n     |                    ^\n   8 |\n   9 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  10 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:13:08.161Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-Mobile-Chrome\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-Mobile-Chrome\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-Mobile-Chrome\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 7
                      }
                    }
                  ],
                  "status": "unexpected"
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
                      "workerIndex": 10,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 18532,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 21
                        },
                        "snippet": "\u001b[0m \u001b[90m 19 |\u001b[39m     test(\u001b[32m'Admin Dashboard should be accessible'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m 20 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/admin'\u001b[39m)\u001b[33m;\u001b[39m \u001b[90m// আপনার অ্যাডমিন পেজের URL\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 21 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'text=Admin Gate'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 22 |\u001b[39m\n \u001b[90m 23 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 24 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 21
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n\n  19 |     test('Admin Dashboard should be accessible', async ({ page }) => {\n  20 |         await page.goto('/admin'); // আপনার অ্যাডমিন পেজের URL\n> 21 |         await page.waitForSelector('text=Admin Gate', { state: 'visible', timeout: 15000 });\n     |                    ^\n  22 |\n  23 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  24 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:13:30.265Z",
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
                        "column": 20,
                        "line": 21
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-5cee6a536c5b96084d0d",
              "file": "e2e/accessibility.spec.ts",
              "line": 19,
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
                      "workerIndex": 12,
                      "parallelIndex": 1,
                      "status": "failed",
                      "duration": 19769,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 7
                        },
                        "snippet": "\u001b[0m \u001b[90m  5 |\u001b[39m     test(\u001b[32m'Homepage should not have any automatically detectable accessibility issues'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m  6 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/'\u001b[39m)\u001b[33m;\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m  7 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'[data-testid=\"dashboard-sidebar\"]'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m  8 |\u001b[39m\n \u001b[90m  9 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 10 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 7
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"dashboard-sidebar\"]') to be visible\u001b[22m\n\n\n   5 |     test('Homepage should not have any automatically detectable accessibility issues', async ({ page }) => {\n   6 |         await page.goto('/');\n>  7 |         await page.waitForSelector('[data-testid=\"dashboard-sidebar\"]', { state: 'visible', timeout: 15000 });\n     |                    ^\n   8 |\n   9 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  10 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:7:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:13:55.049Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-Mobile-Safari\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-Mobile-Safari\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-57b49-ctable-accessibility-issues-Mobile-Safari\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 7
                      }
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
                      "workerIndex": 13,
                      "parallelIndex": 0,
                      "status": "failed",
                      "duration": 17194,
                      "error": {
                        "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n",
                        "stack": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20",
                        "location": {
                          "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                          "column": 20,
                          "line": 21
                        },
                        "snippet": "\u001b[0m \u001b[90m 19 |\u001b[39m     test(\u001b[32m'Admin Dashboard should be accessible'\u001b[39m\u001b[33m,\u001b[39m \u001b[36masync\u001b[39m ({ page }) \u001b[33m=>\u001b[39m {\n \u001b[90m 20 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mgoto(\u001b[32m'/admin'\u001b[39m)\u001b[33m;\u001b[39m \u001b[90m// আপনার অ্যাডমিন পেজের URL\u001b[39m\n\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 21 |\u001b[39m         \u001b[36mawait\u001b[39m page\u001b[33m.\u001b[39mwaitForSelector(\u001b[32m'text=Admin Gate'\u001b[39m\u001b[33m,\u001b[39m { state\u001b[33m:\u001b[39m \u001b[32m'visible'\u001b[39m\u001b[33m,\u001b[39m timeout\u001b[33m:\u001b[39m \u001b[35m15000\u001b[39m })\u001b[33m;\u001b[39m\n \u001b[90m    |\u001b[39m                    \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\n \u001b[90m 22 |\u001b[39m\n \u001b[90m 23 |\u001b[39m         \u001b[36mconst\u001b[39m accessibilityScanResults \u001b[33m=\u001b[39m \u001b[36mawait\u001b[39m \u001b[36mnew\u001b[39m \u001b[33mAxeBuilder\u001b[39m({ page })\u001b[33m.\u001b[39manalyze()\u001b[33m;\u001b[39m\n \u001b[90m 24 |\u001b[39m\u001b[0m"
                      },
                      "errors": [
                        {
                          "location": {
                            "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                            "column": 20,
                            "line": 21
                          },
                          "message": "TimeoutError: page.waitForSelector: Timeout 15000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('text=Admin Gate') to be visible\u001b[22m\n\n\n  19 |     test('Admin Dashboard should be accessible', async ({ page }) => {\n  20 |         await page.goto('/admin'); // আপনার অ্যাডমিন পেজের URL\n> 21 |         await page.waitForSelector('text=Admin Gate', { state: 'visible', timeout: 15000 });\n     |                    ^\n  22 |\n  23 |         const accessibilityScanResults = await new AxeBuilder({ page }).analyze();\n  24 |\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts:21:20"
                        }
                      ],
                      "stdout": [],
                      "stderr": [],
                      "retry": 0,
                      "startTime": "2026-07-04T23:14:08.205Z",
                      "annotations": [],
                      "attachments": [
                        {
                          "name": "screenshot",
                          "contentType": "image/png",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Safari\\test-failed-1.png"
                        },
                        {
                          "name": "video",
                          "contentType": "video/webm",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Safari\\video.webm"
                        },
                        {
                          "name": "error-context",
                          "contentType": "text/markdown",
                          "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-accessibility-Accessib-1a8a2-hboard-should-be-accessible-Mobile-Safari\\error-context.md"
                        }
                      ],
                      "errorLocation": {
                        "file": "C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\accessibility.spec.ts",
                        "column": 20,
                        "line": 21
                      }
                    }
                  ],
                  "status": "unexpected"
                }
              ],
              "id": "cb4d96879b22e77d8b2c-179c216fc7c0773a00af",
              "file": "e2e/accessibility.spec.ts",
              "line": 19,
              "column": 9
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
                  "workerIndex": 2,
                  "parallelIndex": 1,
                  "status": "timedOut",
                  "duration": 31446,
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
                      "message": "Error: page.click: Test timeout of 30000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"tab-chat\"]')\u001b[22m\n\n\n  3 | test('Chat sends message', async ({ page }) => {\n  4 |   await page.goto('/#/workspace');\n> 5 |   await page.click('[data-testid=\"tab-chat\"]');\n    |              ^\n  6 |   await page.waitForSelector('[data-testid=\"chat-input\"]', { state: 'visible', timeout: 15000 });\n  7 |   await page.fill('[data-testid=\"chat-input\"]', 'Hello SupremeAI!');\n  8 |   await page.click('[data-testid=\"chat-submit\"]');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts:5:14"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T23:11:41.151Z",
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
                  "workerIndex": 5,
                  "parallelIndex": 1,
                  "status": "timedOut",
                  "duration": 30192,
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
                      "message": "Error: page.click: Test timeout of 30000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"tab-chat\"]')\u001b[22m\n\n\n  3 | test('Chat sends message', async ({ page }) => {\n  4 |   await page.goto('/#/workspace');\n> 5 |   await page.click('[data-testid=\"tab-chat\"]');\n    |              ^\n  6 |   await page.waitForSelector('[data-testid=\"chat-input\"]', { state: 'visible', timeout: 15000 });\n  7 |   await page.fill('[data-testid=\"chat-input\"]', 'Hello SupremeAI!');\n  8 |   await page.click('[data-testid=\"chat-submit\"]');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts:5:14"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T23:12:14.222Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "screenshot",
                      "contentType": "image/png",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-firefox\\test-failed-1.png"
                    },
                    {
                      "name": "video",
                      "contentType": "video/webm",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-firefox\\video.webm"
                    },
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
                  "workerIndex": 8,
                  "parallelIndex": 0,
                  "status": "timedOut",
                  "duration": 33119,
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
                      "message": "Error: page.click: Test timeout of 30000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"tab-chat\"]')\u001b[22m\n\n\n  3 | test('Chat sends message', async ({ page }) => {\n  4 |   await page.goto('/#/workspace');\n> 5 |   await page.click('[data-testid=\"tab-chat\"]');\n    |              ^\n  6 |   await page.waitForSelector('[data-testid=\"chat-input\"]', { state: 'visible', timeout: 15000 });\n  7 |   await page.fill('[data-testid=\"chat-input\"]', 'Hello SupremeAI!');\n  8 |   await page.click('[data-testid=\"chat-submit\"]');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts:5:14"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T23:12:56.414Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "screenshot",
                      "contentType": "image/png",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-webkit\\test-failed-1.png"
                    },
                    {
                      "name": "video",
                      "contentType": "video/webm",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-webkit\\video.webm"
                    },
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
                  "workerIndex": 11,
                  "parallelIndex": 0,
                  "status": "timedOut",
                  "duration": 32368,
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
                      "message": "Error: page.click: Test timeout of 30000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"tab-chat\"]')\u001b[22m\n\n\n  3 | test('Chat sends message', async ({ page }) => {\n  4 |   await page.goto('/#/workspace');\n> 5 |   await page.click('[data-testid=\"tab-chat\"]');\n    |              ^\n  6 |   await page.waitForSelector('[data-testid=\"chat-input\"]', { state: 'visible', timeout: 15000 });\n  7 |   await page.fill('[data-testid=\"chat-input\"]', 'Hello SupremeAI!');\n  8 |   await page.click('[data-testid=\"chat-submit\"]');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts:5:14"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T23:13:31.665Z",
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
                  "workerIndex": 14,
                  "parallelIndex": 1,
                  "status": "timedOut",
                  "duration": 30257,
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
                      "message": "Error: page.click: Test timeout of 30000ms exceeded.\nCall log:\n\u001b[2m  - waiting for locator('[data-testid=\"tab-chat\"]')\u001b[22m\n\n\n  3 | test('Chat sends message', async ({ page }) => {\n  4 |   await page.goto('/#/workspace');\n> 5 |   await page.click('[data-testid=\"tab-chat\"]');\n    |              ^\n  6 |   await page.waitForSelector('[data-testid=\"chat-input\"]', { state: 'visible', timeout: 15000 });\n  7 |   await page.fill('[data-testid=\"chat-input\"]', 'Hello SupremeAI!');\n  8 |   await page.click('[data-testid=\"chat-submit\"]');\n    at C:\\Users\\n\\supremeai\\supremeai_2.0\\tests\\e2e\\chat.spec.ts:5:14"
                    }
                  ],
                  "stdout": [],
                  "stderr": [],
                  "retry": 0,
                  "startTime": "2026-07-04T23:14:16.312Z",
                  "annotations": [],
                  "attachments": [
                    {
                      "name": "screenshot",
                      "contentType": "image/png",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-Mobile-Safari\\test-failed-1.png"
                    },
                    {
                      "name": "video",
                      "contentType": "video/webm",
                      "path": "C:\\Users\\n\\supremeai\\supremeai_2.0\\test-results\\e2e-chat-Chat-sends-message-Mobile-Safari\\video.webm"
                    },
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
    }
  ],
  "errors": [],
  "stats": {
    "startTime": "2026-07-04T23:11:14.165Z",
    "duration": 212769.994,
    "expected": 0,
    "skipped": 0,
    "unexpected": 15,
    "flaky": 0
  }
}
```