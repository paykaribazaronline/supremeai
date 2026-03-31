# API Reference - SupremeAI Backend

**API Version:** v1.0  
**Last Updated:** March 31, 2026  
**Base URL:** `http://localhost:8080/api` (local) | `https://api.supremeai.com/api` (production)  
**Authentication:** JWT Bearer Token (Authorization header)

---

## 📋 Table of Contents

1. [Authentication](#authentication)
2. [Projects API](#projects-api)
3. [Agents API](#agents-api)
4. [Providers API](#providers-api)
5. [Metrics API](#metrics-api)
6. [Health Check](#health-check)
7. [Error Codes](#error-codes)

---

## Authentication

All API endpoints require a valid JWT token in the `Authorization` header.

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@supremeai.com",
  "password": "secure-password"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "user": {
    "id": "user-123",
    "email": "admin@supremeai.com",
    "role": "ADMIN"
  }
}
```

### Using the Token
```http
GET /api/projects
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

---

## Projects API

### List All Projects
```http
GET /api/projects
```

**Query Parameters:**
- `page` (int, default: 0) - Page number
- `size` (int, default: 20) - Results per page
- `search` (string) - Search by project name
- `status` (string) - Filter by status: ACTIVE, INACTIVE, ARCHIVED

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "proj-001",
      "name": "E-Commerce AI",
      "description": "AI-powered product recommendations",
      "status": "ACTIVE",
      "createdAt": "2026-03-15T10:30:00Z",
      "updatedAt": "2026-03-31T14:20:00Z",
      "agents": [
        {
          "id": "agent-1",
          "role": "ARCHITECT",
          "name": "DeepSeek Model"
        }
      ],
      "metrics": {
        "totalRequests": 5234,
        "avgResponseTime": 145,
        "successRate": 98.5
      }
    }
  ],
  "totalElements": 42,
  "totalPages": 3,
  "currentPage": 0
}
```

### Get Project Details
```http
GET /api/projects/{projectId}
```

**Response (200 OK):**
```json
{
  "id": "proj-001",
  "name": "E-Commerce AI",
  "description": "AI-powered product recommendations",
  "status": "ACTIVE",
  "createdAt": "2026-03-15T10:30:00Z",
  "updatedAt": "2026-03-31T14:20:00Z",
  "config": {
    "apiKeys": ["sk-xxx"],
    "agents": ["agent-1", "agent-2"],
    "webhookUrl": "https://myapp.com/webhook"
  },
  "metrics": {
    "totalRequests": 5234,
    "totalTokens": 1234567,
    "costs": {
      "input": 12.34,
      "output": 56.78,
      "total": 69.12
    }
  }
}
```

### Create Project
```http
POST /api/projects
Content-Type: application/json

{
  "name": "New Project",
  "description": "Project description",
  "agents": ["agent-1", "agent-2"]
}
```

**Response (201 Created):**
```json
{
  "id": "proj-002",
  "name": "New Project",
  "status": "ACTIVE",
  "createdAt": "2026-03-31T17:45:00Z"
}
```

### Update Project
```http
PUT /api/projects/{projectId}
Content-Type: application/json

{
  "name": "Updated Name",
  "description": "Updated description",
  "status": "ACTIVE"
}
```

### Delete Project
```http
DELETE /api/projects/{projectId}
```

**Response (204 No Content)**

---

## Agents API

### List Available Agents
```http
GET /api/agents
```

**Response (200 OK):**
```json
{
  "agents": [
    {
      "id": "agent-architect",
      "name": "Z-Architect",
      "role": "ARCHITECT",
      "provider": "deepseek",
      "model": "deepseek-chat",
      "status": "AVAILABLE",
      "capabilities": [
        "system-design",
        "code-architecture",
        "performance-optimization"
      ]
    },
    {
      "id": "agent-builder",
      "name": "X-Builder",
      "role": "BUILDER",
      "provider": "openai",
      "model": "gpt-4",
      "status": "AVAILABLE",
      "capabilities": [
        "code-generation",
        "implementation",
        "debugging"
      ]
    },
    {
      "id": "agent-reviewer",
      "name": "Y-Reviewer",
      "role": "REVIEWER",
      "provider": "claude",
      "model": "claude-3-opus",
      "status": "AVAILABLE",
      "capabilities": [
        "code-review",
        "quality-assurance",
        "best-practices"
      ]
    }
  ]
}
```

### Assign Agent to Project
```http
POST /api/projects/{projectId}/agents
Content-Type: application/json

{
  "agentId": "agent-architect"
}
```

**Response (200 OK):**
```json
{
  "projectId": "proj-001",
  "agentId": "agent-architect",
  "assignedAt": "2026-03-31T17:45:00Z"
}
```

### Get Agent Performance
```http
GET /api/agents/{agentId}/performance
```

**Query Parameters:**
- `period` (string) - Time period: DAY, WEEK, MONTH, default: WEEK
- `metric` (string) - Metric type: ACCURACY, SPEED, COST

**Response (200 OK):**
```json
{
  "agentId": "agent-architect",
  "period": "WEEK",
  "metrics": {
    "requests": 234,
    "successRate": 98.5,
    "avgResponseTime": 1245,
    "avgTokens": 2340,
    "cost": 45.67,
    "accuracy": 0.98,
    "rating": 4.8
  }
}
```

---

## Providers API

### List API Providers
```http
GET /api/providers/configured
```

**Response (200 OK):**
```json
{
  "providers": [
    {
      "id": "provider-deepseek",
      "name": "DeepSeek",
      "type": "LLM",
      "status": "ACTIVE",
      "models": [
        {
          "name": "deepseek-chat",
          "contextWindow": 4096,
          "maxTokens": 2048
        }
      ],
      "rateLimitsPer": {
        "requests": 3000,
        "tokens": 600000
      },
      "pricing": {
        "input": 0.14,
        "output": 0.28
      }
    }
  ]
}
```

### Get Available Providers (Discovery)
```http
GET /api/providers/available
```

**Response (200 OK):**
```json
{
  "availableProviders": [
    {
      "name": "gpt-4",
      "category": "LLM",
      "rating": 4.9,
      "description": "Latest OpenAI model"
    }
  ]
}
```

### Add Provider
```http
POST /api/providers/add
Content-Type: application/json

{
  "provider": "openai",
  "apiKey": "sk-...",
  "name": "OpenAI GPT-4"
}
```

**Response (201 Created):**
```json
{
  "id": "provider-openai",
  "name": "OpenAI GPT-4",
  "status": "ACTIVE"
}
```

### Test Provider Connection
```http
POST /api/providers/test
Content-Type: application/json

{
  "providerId": "provider-openai",
  "prompt": "Say hello"
}
```

**Response (200 OK):**
```json
{
  "status": "SUCCESS",
  "message": "Connection successful",
  "response": "Hello! How can I help?",
  "latency": 345
}
```

### Remove Provider
```http
DELETE /api/providers/{providerId}
```

**Response (204 No Content)**

---

## Metrics API

### Get System Metrics
```http
GET /api/metrics
```

**Query Parameters:**
- `period` (string) - DAY, WEEK, MONTH
- `granularity` (string) - MINUTE, HOUR, DAY

**Response (200 OK):**
```json
{
  "period": "WEEK",
  "metrics": {
    "totalRequests": 23456,
    "totalTokens": 23456789,
    "totalCost": 1234.56,
    "avgResponseTime": 234,
    "successRate": 98.7,
    "errorRate": 1.3,
    "topAgents": [
      {
        "agentId": "agent-architect",
        "name": "Z-Architect",
        "requests": 5234,
        "successRate": 99.1
      }
    ]
  }
}
```

### Get Project Metrics
```http
GET /api/projects/{projectId}/metrics
```

**Response (200 OK):**
```json
{
  "projectId": "proj-001",
  "metrics": {
    "requests": 5234,
    "tokens": 1234567,
    "cost": 123.45,
    "avgResponseTime": 145,
    "successRate": 98.5,
    "topEndpoint": "/api/analyze"
  }
}
```

### Get Cost Breakdown
```http
GET /api/metrics/costs
```

**Query Parameters:**
- `projectId` (string, optional) - Filter by project
- `period` (string) - Time period

**Response (200 OK):**
```json
{
  "period": "MONTH",
  "costs": {
    "total": 5432.10,
    "byProvider": {
      "openai": 2345.67,
      "deepseek": 1234.56,
      "anthropic": 1851.87
    },
    "byProject": {
      "proj-001": 2345.67,
      "proj-002": 1234.56
    },
    "currency": "USD"
  }
}
```

---

## Health Check

### System Health
```http
GET /health
```

**Response (200 OK):**
```json
{
  "status": "UP",
  "timestamp": "2026-03-31T17:45:00Z",
  "components": {
    "database": {
      "status": "UP"
    },
    "firebase": {
      "status": "UP"
    },
    "cache": {
      "status": "UP"
    }
  }
}
```

---

## Error Codes

### Common Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | Request successful |
| 201 | Created | Resource created successfully |
| 204 | No Content | Request successful, no response body |
| 400 | Bad Request | Invalid parameters |
| 401 | Unauthorized | Missing or invalid token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Resource already exists |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

### Error Response Format

```json
{
  "error": {
    "code": "INVALID_PROJECT_ID",
    "message": "Project with id 'proj-999' not found",
    "timestamp": "2026-03-31T17:45:00Z",
    "path": "/api/projects/proj-999"
  }
}
```

---

## Rate Limiting

All endpoints are rate limited:

- **Default:** 1000 requests per minute per IP
- **Per User:** 5000 requests per minute when authenticated
- **Headers:**
  - `X-RateLimit-Limit`: Maximum requests
  - `X-RateLimit-Remaining`: Requests remaining
  - `X-RateLimit-Reset`: Unix timestamp of reset time

---

## Webhooks

Projects can be configured with webhook URLs that receive notifications:

### Webhook Events

- `project.created`
- `project.updated`
- `agent.assigned`
- `request.completed`
- `error.occurred`

### Webhook Payload

```json
{
  "event": "request.completed",
  "projectId": "proj-001",
  "timestamp": "2026-03-31T17:45:00Z",
  "data": {
    "requestId": "req-123",
    "status": "SUCCESS",
    "responseTime": 234,
    "tokens": {
      "input": 100,
      "output": 250
    },
    "cost": 0.12
  }
}
```

---

## SDK Availability

Official SDKs available:

- **JavaScript/TypeScript:** `npm install @supremeai/sdk`
- **Python:** `pip install supremeai`
- **Java:** Available in Maven Central
- **Go:** `go get github.com/supremeai/sdk-go`

---

## Examples

### Complete Request Workflow

```bash
# 1. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@supremeai.com","password":"password"}'

# Response includes accessToken

# 2. Create project
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"My Project"}'

# 3. Assign agent
curl -X POST http://localhost:8080/api/projects/proj-001/agents \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"agentId":"agent-architect"}'

# 4. Get metrics
curl -X GET "http://localhost:8080/api/projects/proj-001/metrics" \
  -H "Authorization: Bearer $TOKEN"
```

---

**For support, see [AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md) or [README.md](README.md)**
