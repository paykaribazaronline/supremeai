# 📄 ফাইল: firebase.json

**প্রকার:** .json  
**সাইজ:** 4,998 বাইট  
**আপডেট:** 2026-07-07T12:54:09.733355

---

## কোড

```json
{
  "firestore": {
    "rules": "config/firestore.rules",
    "indexes": "config/firestore.indexes.json"
  },
  "hosting": [
    {
      "target": "studio",
      "public": "apps/web-chat/dist",
      "ignore": [
        "firebase.json",
        "**/.*",
        "**/node_modules/**",
        "**/src/**",
        "**/scripts/**",
        "build.gradle*",
        "gradlew*",
        ".env*",
        "*.log",
        "package-lock.json"
      ],
      "rewrites": [
        {
          "source": "/admin-api/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/api/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/telemetry/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/ws",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/ws/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/task/execute",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/skills",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/admin/rules",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "**",
          "destination": "/index.html"
        }
      ],
      "headers": [
        {
          "source": "/index.html",
          "headers": [
            {
              "key": "Cache-Control",
              "value": "no-cache, no-store, must-revalidate"
            }
          ]
        },
        {
          "source": "/assets/**",
          "headers": [
            {
              "key": "Cache-Control",
              "value": "public, max-age=31536000, immutable"
            }
          ]
        }
      ]
    },
    {
      "target": "admin",
      "public": "apps/studio-client/dist",
      "ignore": [
        "firebase.json",
        "**/.*",
        "**/node_modules/**",
        "**/src/**",
        "**/scripts/**",
        "build.gradle*",
        "gradlew*",
        ".env*",
        "*.log",
        "package-lock.json"
      ],
      "rewrites": [
        {
          "source": "/admin-api/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/api/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/telemetry/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/ws",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/ws/**",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/task/execute",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/skills",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "/admin/rules",
          "run": {
            "serviceId": "supremeai-api",
            "region": "us-central1"
          }
        },
        {
          "source": "**",
          "destination": "/index.html"
        }
      ],
      "headers": [
        {
          "source": "/index.html",
          "headers": [
            {
              "key": "Cache-Control",
              "value": "no-cache, no-store, must-revalidate"
            }
          ]
        },
        {
          "source": "/assets/**",
          "headers": [
            {
              "key": "Cache-Control",
              "value": "public, max-age=31536000, immutable"
            }
          ]
        }
      ]
    }
  ],
  "functions": {
    "source": "functions",
    "region": "us-central1"
  },
  "emulators": {
    "auth": {
      "port": 9099
    },
    "firestore": {
      "port": 8081
    },
    "functions": {
      "port": 5003
    },
    "hosting": {
      "port": 5002
    },
    "ui": {
      "port": 4001
    },
    "dataconnect": {
      "dataDir": "dataconnect/.dataconnect/pgliteData"
    }
  },
  "dataconnect": {
    "source": "dataconnect"
  }
}

```