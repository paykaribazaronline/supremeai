# Reverse Engineering Service

FastAPI microservice for website reverse engineering and API discovery.
Deployed as a Cloud Run service. Triggered by Pub/Sub push subscription from Spring Boot backend.

## Local Development

```bash
cd reverse-engineering
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8080
```

Health check: `curl http://localhost:8080/health`

## Deployment

### Prerequisites
- Firebase project with Firestore enabled
- Pub/Sub topic `reverse-engineering-jobs` created
- Service account with Firestore and Pub/Sub permissions
- gcloud CLI installed and authenticated

### Deploy to Cloud Run

```bash
gcloud run deploy reverse-engineering \
  --source . \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars GOOGLE_CLOUD_PROJECT=your-project-id
```

### Create Pub/Sub Push Subscription

```bash
gcloud pubsub subscriptions create reverse-engineering-jobs-push \
  --topic reverse-engineering-jobs \
  --push-endpoint=https://reverse-engineering-xxxxxx-uc.a.run.app/pubsub/push \
  --push-auth-service-account=reverse-engineering@your-project.iam.gserviceaccount.com
```

Or use Terraform (see infrastructure/terraform/).

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/health` | Health check |
| POST | `/reveng` | Start reverse engineering job (async) |
| GET | `/reveng/{jobId}` | Get job status & results |
| POST | `/reveng/{jobId}/complete` | Manually mark job complete |
| POST | `/pubsub/push` | Pub/Sub push webhook (internal) |

## Firestore Collections

- `reverse_engineering_jobs` - job documents with fields:
  - `jobId`, `userId`, `websiteUrl`, `status`, `discoveredApis`, `scrapedData`, `createdAt`, `updatedAt`, `completedAt`

## Request Example

```bash
curl -X POST "http://localhost:8080/reveng" \
  -H "Content-Type: application/json" \
  -d '{
    "jobId": "reveng_abc123",
    "userId": "user_123",
    "websiteUrl": "https://example.com",
    "scrapeDepth": 1,
    "discoverApis": true
  }'
```

## Response

```json
{
  "jobId": "reveng_abc123",
  "status": "PENDING"
}
```

## Spring Boot Integration

The Spring Boot app publishes jobs to Pub/Sub topic `reverse-engineering-jobs` using `PubSubTemplate`.

```java
@Autowired
private PubSubTemplate pubSubTemplate;

Map<String, Object> message = Map.of(
    "jobId", jobId,
    "userId", userId,
    "websiteUrl", websiteUrl
);
pubSubTemplate.publish("reverse-engineering-jobs", message);
```

## Future Enhancements

- Add authentication to push endpoint (JWT verification)
- Implement screenshot capture using headless browser (Selenium)
- Add API response schema inference (sample actual API calls)
- Support for JavaScript-heavy SPAs (Puppeteer/Playwright)
- Rate limiting and retry logic
- Job result TTL and archival
- Notifications via FCM when job completes
