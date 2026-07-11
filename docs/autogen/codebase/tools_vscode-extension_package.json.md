# 📄 ফাইল: tools/vscode-extension/package.json

**প্রকার:** .json  
**সাইজ:** 9,318 বাইট  
**আপডেট:** 2026-07-11T20:08:21.509539

---

## কোড

```json
{
  "name": "supremeai-vscode",
  "displayName": "SupremeAI - AI-Powered Development Assistant",
  "description": "Real-time code analysis, learning, and AI assistance with SupremeAI Core Engine",
  "version": "6.0.0",
  "license": "MIT",
  "publisher": "supremeai",
  "repository": {
    "type": "git",
    "url": "https://github.com/your-org/supremeai.git"
  },
  "bugs": {
    "url": "https://github.com/your-org/supremeai/issues"
  },
  "homepage": "https://github.com/your-org/supremeai#readme",
  "engines": {
    "vscode": "^1.85.0"
  },
  "categories": [
    "Machine Learning",
    "Snippets",
    "Other"
  ],
  "localizations": {
    "en": {
      "packageDisplayName": "SupremeAI - AI-Powered Development Assistant",
      "packageDescription": "Real-time code analysis, learning, and AI assistance with SupremeAI Core Engine"
    },
    "bn": {
      "packageDisplayName": "সুপ্রিমএআই - এআই-চালিত ডেভেলপমেন্ট সহায়ক",
      "packageDescription": "সুপ্রিমএআই কোর ইঞ্জিনের সাথে রিয়েল-টাইম কোড বিশ্লেষণ, শিখুন এবং এআই সহায়তা"
    },
    "es": {
      "packageDisplayName": "SupremeAI - Asistente de desarrollo impulsado por IA",
      "packageDescription": "Análisis de código en tiempo real, aprendizaje y asistencia de IA con el motor SupremeAI Core"
    },
    "hi": {
      "packageDisplayName": "सुप्रीमएआई - एआई-संचालित विकास सहायक",
      "packageDescription": "सुप्रीमएआई कोर इंजन के साथ वास्तविक समय कोड विश्लेषण, सीखना और एआई सहायता"
    }
  },
  "keywords": [
    "AI",
    "Code Assistant",
    "Learning",
    "SupremeAI",
    "Auto-Completion",
    "Error Detection",
    "CodeFlow",
    "Analysis",
    "Code Generation",
    "Refactoring"
  ],
  "activationEvents": [],
  "main": "./out/extension.js",
  "contributes": {
    "commands": [
      {
        "command": "supremeai.login",
        "title": "Login",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.loginAsGuest",
        "title": "Login as Guest",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.logout",
        "title": "Logout",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.acceptSuggestion",
        "title": "Accept Suggestion",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.rejectSuggestion",
        "title": "Reject Suggestion",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.sendFeedback",
        "title": "Send Feedback",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.reportError",
        "title": "Report Error",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.forceLearn",
        "title": "Force Learn",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.analyzeCodeFlow",
        "title": "Analyze Code Flow",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.resolveError",
        "title": "Resolve Error",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.showSecurityIssues",
        "title": "Show Security Issues",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.showDependencies",
        "title": "Show Dependencies",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.openCodeFlowDashboard",
        "title": "Open Code Flow Dashboard",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.refreshCodeFlow",
        "title": "Refresh Code Flow",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.aiComplete",
        "title": "AI Code Completion",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.aiExplain",
        "title": "Explain Code",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.sendMessageToChat",
        "title": "Send to Chat",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.aiReview",
        "title": "Review Code",
        "category": "SupremeAI"
      },
      {
        "command": "supremeai.openExtensionSettings",
        "title": "Open Settings",
        "category": "SupremeAI"
      }
    ],
    "menus": {
      "editor/context": [
        {
          "command": "supremeai.resolveError",
          "when": "editorHasSelection",
          "group": "SupremeAI@1"
        },
        {
          "command": "supremeai.aiExplain",
          "when": "editorHasSelection",
          "group": "SupremeAI@1"
        },
        {
          "command": "supremeai.aiReview",
          "when": "editorHasSelection",
          "group": "SupremeAI@1"
        },
        {
          "command": "supremeai.sendMessageToChat",
          "when": "editorHasSelection",
          "group": "SupremeAI@2"
        },
        {
          "command": "supremeai.analyzeCodeFlow",
          "group": "SupremeAI@3"
        }
      ],
      "commandPalette": [
        {
          "command": "supremeai.resolveError",
          "when": "editorHasSelection"
        },
        {
          "command": "supremeai.sendMessageToChat",
          "when": "editorHasSelection"
        }
      ]
    },
    "themes": [
      {
        "label": "SupremeAI Dark",
        "uiTheme": "vs-dark",
        "path": "./node_modules/@supremeai/design-tokens/outputs/vscode/supremeai-theme.json"
      }
    ],
    "configuration": {
      "title": "SupremeAI",
      "properties": {
        "supremeai.backendUrl": {
          "type": "string",
          "default": "https://supremeai-api-lhlwyikwlq-uc.a.run.app",
          "description": "The production FastAPI endpoint on Google Cloud Run used to fetch skills and proposed states."
        },
        "supremeai.aiApiKey": {
          "type": "string",
          "default": "",
          "description": "The API key for authentication.",
          "secret": true
        },
        "supremeai.codegeex4.enabled": {
          "type": "boolean",
          "default": true,
          "description": "Enable CodeGeeX4 model."
        },
        "supremeai.codegeex4.model": {
          "type": "string",
          "default": "codegeex-4",
          "description": "The CodeGeeX4 model name."
        },
        "supremeai.enableRealTimeLearning": {
          "type": "boolean",
          "default": true,
          "description": "Enable real-time code learning and pattern recognition."
        },
        "supremeai.autoReportErrors": {
          "type": "boolean",
          "default": true,
          "description": "Automatically report unhandled errors to SupremeAI."
        },
        "supremeai.enableCodeFlow": {
          "type": "boolean",
          "default": true,
          "description": "Enable CodeFlow analysis features."
        },
        "supremeai.autoAnalyzeOnSave": {
          "type": "boolean",
          "default": false,
          "description": "Automatically analyze code on save."
        },
        "supremeai.aiModel": {
          "type": "string",
          "default": "supreme-large",
          "description": "Default AI model to use."
        }
      }
    },
    "viewsContainers": {
      "activitybar": [
        {
          "id": "supremeai-sidebar",
          "title": "SupremeAI",
          "icon": "media/icon.svg"
        }
      ]
    },
    "views": {
      "supremeai-sidebar": [
        {
          "id": "supremeaiChat",
          "name": "Chat",
          "type": "webview",
          "icon": "media/icon.svg"
        },
        {
          "id": "supremeaiAdminDashboard",
          "name": "Admin Dashboard",
          "when": "supremeai.authenticated && supremeai.isAdmin",
          "type": "webview",
          "icon": "media/icon.svg"
        },
        {
          "id": "supremeaiCustomerDashboard",
          "name": "User Settings",
          "when": "supremeai.authenticated",
          "type": "webview",
          "icon": "media/icon.svg"
        }
      ]
    }
  },
  "scripts": {
    "vscode:prepublish": "pnpm run package-ext",
    "compile": "tsc -p ./",
    "watch": "tsc -watch -p ./",
    "lint": "eslint src",
    "pretest": "pnpm run compile",
    "test": "vitest run",
    "unit": "vitest run",
    "package-ext": "esbuild src/extension.ts --bundle --outfile=out/extension.js --external:vscode --format=cjs --platform=node --minify"
  },
  "devDependencies": {
    "@types/fast-levenshtein": "^0.0.4",
    "@types/node": "18.x",
    "@types/vscode": "^1.85.0",
    "@typescript-eslint/eslint-plugin": "^6.0.0",
    "@typescript-eslint/parser": "^6.0.0",
    "axios": "^1.6.0",
    "esbuild": "^0.28.0",
    "eslint": "^8.0.0",
    "openai": "^4.0.0",
    "typescript": "^5.0.0",
    "vitest": "^3.2.6",
    "vscode": "^1.1.37"
  },
  "dependencies": {
    "@dataconnect/generated": "file:src/dataconnect-generated",
    "@supremeai/design-tokens": "workspace:^",
    "fast-levenshtein": "^3.0.0"
  }
}
```