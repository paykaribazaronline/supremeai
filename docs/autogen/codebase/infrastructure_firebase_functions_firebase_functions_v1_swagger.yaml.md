# 📄 ফাইল: infrastructure/firebase_functions/firebase_functions_v1/swagger.yaml

**প্রকার:** .yaml  
**সাইজ:** 2,281 বাইট  
**আপডেট:** 2026-07-05T19:24:05.974810

---

## কোড

```yaml
openapi: 3.0.0
info:
  title: SupremeAI API
  description: API documentation for SupremeAI coordinator and endpoints
  version: 1.0.0
servers:
  - url: https://supremeai-a.web.app/api
    description: Production Server
  - url: http://127.0.0.1:5000/api
    description: Local Emulator
paths:
  /chat/send:
    post:
      summary: Send a message to the unified chat handler
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
                userId:
                  type: string
                useScrape:
                  type: boolean
      responses:
        '200':
          description: Successful AI response
          content:
            application/json:
              schema:
                type: object
                properties:
                  success:
                    type: boolean
                  message:
                    type: string
                  sources:
                    type: array
                    items:
                      type: string
                  confidence:
                    type: number
                  chatType:
                    type: string
                  sourceType:
                    type: string
  /scrape/and-respond:
    post:
      summary: Scrape and respond directly
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
                userId:
                  type: string
      responses:
        '200':
          description: Successful scrape response
  /chat/classify:
    post:
      summary: Classify semantic intent of a message
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                message:
                  type: string
      responses:
        '200':
          description: Intent classification
  /health:
    get:
      summary: Check health status of the API router
      responses:
        '200':
          description: Health OK

```