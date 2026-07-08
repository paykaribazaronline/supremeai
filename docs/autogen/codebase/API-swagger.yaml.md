# 📄 ফাইল: API-swagger.yaml

**প্রকার:** .yaml  
**সাইজ:** 1,942 বাইট  
**আপডেট:** 2026-07-08T03:02:32.553773

---

## কোড

```yaml
openapi: 3.0.0
info:
  title: "SupremeAI 2.0 API"
  description: "API for the SupremeAI 2.0 platform, including admin, chat, and webhook endpoints."
  version: "2.0.0"
servers:
  - url: "https://supremeai-api-xxxx.a.run.app"
    description: "Production Server (Google Cloud Run)"

paths:
  /telegram/webhook:
    post:
      summary: "Telegram Webhook"
      description: "Endpoint for receiving updates from the Telegram Bot API."
      tags:
        - "telegram"
      requestBody:
        description: "Telegram update payload"
        required: true
        content:
          application/json:
            schema:
              type: "object"
              example:
                update_id: 123456789
                message:
                  message_id: 123
                  from: { id: 98765, is_bot: false, first_name: "John", last_name: "Doe" }
                  chat: { id: 98765, first_name: "John", type: "private" }
                  date: 1678886400
                  text: "/start"
      responses:
        "200":
          description: "Update received successfully"

  /telegram/health:
    get:
      summary: "Telegram Bot Health Check"
      description: "Verifies if the Telegram bot is configured and can connect to the Telegram API."
      tags:
        - "telegram"
      responses:
        "200":
          description: "Health status of the bot"
          content:
            application/json:
              schema:
                type: "object"
                properties:
                  configured:
                    type: "boolean"
                  bot:
                    type: "object"
                    properties:
                      id:
                        type: "integer"
                      is_bot:
                        type: "boolean"
                      first_name:
                        type: "string"
                      username:
                        type: "string"
```