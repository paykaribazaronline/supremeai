# SupremeAI SDK Documentation

Complete API client SDKs for SupremeAI Platform with type-safe interfaces and comprehensive error handling.

## Supported Languages

- **JavaScript/Node.js** - npm package
- **Python** - pip package
- **Java** - Maven dependency
- **Go** - go module
- **Dart** - Flutter compatible

## Installation

### JavaScript
```bash
npm install supremeai-sdk
```

```javascript
const SupremeAI = require('supremeai-sdk');
const client = new SupremeAI.Client({ token: 'your-jwt-token' });
```

### Python
```bash
pip install supremeai-sdk
```

```python
from supremeai import Client

client = Client(token='your-jwt-token')
```

### Java
Add to your `pom.xml`:
```xml
<dependency>
    <groupId>org.supremeai</groupId>
    <artifactId>supremeai-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
SupremeAIClient client = new SupremeAIClient("your-jwt-token");
```

### Go
```bash
go get github.com/supremeai/go-sdk
```

```go
import "github.com/supremeai/go-sdk"

client, err := supremeai.NewClient("your-jwt-token")
```

## API Information

### Get API Info
Returns root API information and available versions.

**JavaScript:**
```javascript
const info = await client.getAPIInfo();
console.log(info.currentVersion); // "v2"
```

**Python:**
```python
info = client.get_api_info()
print(info['currentVersion'])  # "v2"
```

**Java:**
```java
Map<String, Object> info = client.getAPIInfo();
System.out.println(info.get("currentVersion")); // "v2"
```

**Go:**
```go
info, err := client.GetAPIInfo()
if err != nil {
    log.Fatal(err)
}
fmt.Println(info["currentVersion"]) // "v2"
```

## Webhook Management

### Register Webhook
Create a new webhook with event subscriptions.

**JavaScript:**
```javascript
const webhook = await client.registerWebhook(
  'project-123',
  'https://example.com/webhook',
  ['project.created', 'agent.updated'],
  'secret-key'
);
console.log(webhook.id); // Webhook UUID
```

**Python:**
```python
webhook = client.register_webhook(
    project_id='project-123',
    url='https://example.com/webhook',
    events=['project.created', 'agent.updated'],
    secret_key='secret-key'
)
print(webhook['id'])
```

**Java:**
```java
Map<String, Object> webhook = client.registerWebhook(
    "project-123",
    "https://example.com/webhook",
    Arrays.asList("project.created", "agent.updated"),
    "secret-key"
);
System.out.println(webhook.get("id"));
```

**Go:**
```go
webhook, err := client.RegisterWebhook(
    "project-123",
    "https://example.com/webhook",
    []string{"project.created", "agent.updated"},
    "secret-key",
)
if err != nil {
    log.Fatal(err)
}
fmt.Println(webhook["id"])
```

### List Webhooks
Retrieve all registered webhooks.

**JavaScript:**
```javascript
const webhooks = await client.listWebhooks();
console.log(`Found ${webhooks.length} webhooks`);
```

**Python:**
```python
webhooks = client.list_webhooks()
for webhook in webhooks:
    print(f"Webhook: {webhook['id']} -> {webhook['url']}")
```

**Java:**
```java
Object webhooks = client.listWebhooks();
System.out.println(webhooks);
```

**Go:**
```go
webhooks, err := client.ListWebhooks()
if err != nil {
    log.Fatal(err)
}
for _, webhook := range webhooks {
    fmt.Println(webhook["id"])
}
```

### Test Webhook
Send test payload to verify webhook configuration.

**JavaScript:**
```javascript
const result = await client.testWebhook('webhook-id-123', {
  message: 'Test webhook'
});
```

**Python:**
```python
result = client.test_webhook('webhook-id-123', {
    'message': 'Test webhook'
})
```

**Java:**
```java
Map<String, Object> result = client.testWebhook(
    "webhook-id-123",
    Map.of("message", "Test webhook")
);
```

**Go:**
```go
result, err := client.TestWebhook("webhook-id-123", map[string]interface{}{
    "message": "Test webhook",
})
```

### Delete Webhook
Permanently delete a webhook.

**JavaScript:**
```javascript
await client.deleteWebhook('webhook-id-123');
```

**Python:**
```python
client.delete_webhook('webhook-id-123')
```

**Java:**
```java
client.deleteWebhook("webhook-id-123");
```

**Go:**
```go
err := client.DeleteWebhook("webhook-id-123")
```

## Batch Operations

### Create Batch
Create a new batch operation for combining multiple API requests.

**JavaScript:**
```javascript
const batch = await client.createBatch('my-batch');
console.log(`Created batch: ${batch.batchId}`);
```

**Python:**
```python
batch = client.create_batch('my-batch')
print(f"Batch ID: {batch['batchId']}")
```

**Java:**
```java
Map<String, Object> batch = client.createBatch("my-batch");
System.out.println(batch.get("batchId"));
```

**Go:**
```go
batch, err := client.CreateBatch("my-batch")
if err != nil {
    log.Fatal(err)
}
fmt.Println(batch["batchId"])
```

### Add Request to Batch
Add a request to an existing batch.

**JavaScript:**
```javascript
await client.addRequestToBatch('batch-id-123', {
  method: 'GET',
  endpoint: '/v2/projects'
});
```

**Python:**
```python
client.add_request_to_batch('batch-id-123', {
    'method': 'GET',
    'endpoint': '/v2/projects'
})
```

**Java:**
```java
client.addRequestToBatch("batch-id-123", Map.of(
    "method", "GET",
    "endpoint", "/v2/projects"
));
```

**Go:**
```go
_, err := client.AddRequestToBatch("batch-id-123", map[string]interface{}{
    "method":   "GET",
    "endpoint": "/v2/projects",
})
```

### Execute Batch
Execute all requests in a batch and receive aggregated responses.

**JavaScript:**
```javascript
const result = await client.executeBatch('batch-id-123');
console.log(`Executed ${result.requestCount} requests`);
console.log(`Received ${result.responseCount} responses`);
```

**Python:**
```python
result = client.execute_batch('batch-id-123')
print(f"Completed at: {result['completedAt']}")
```

**Java:**
```java
Map<String, Object> result = client.executeBatch("batch-id-123");
System.out.println(result.get("status")); // "completed"
```

**Go:**
```go
result, err := client.ExecuteBatch("batch-id-123")
if err != nil {
    log.Fatal(err)
}
fmt.Printf("Status: %v\n", result["status"])
```

### Cancel Batch
Cancel a pending batch operation.

**JavaScript:**
```javascript
await client.cancelBatch('batch-id-123');
```

**Python:**
```python
client.cancel_batch('batch-id-123')
```

**Java:**
```java
client.cancelBatch("batch-id-123");
```

**Go:**
```go
err := client.CancelBatch("batch-id-123")
```

### List Batches
Retrieve all batches.

**JavaScript:**
```javascript
const batches = await client.listBatches();
batches.forEach(batch => {
  console.log(`${batch.name}: ${batch.requestCount} requests`);
});
```

**Python:**
```python
batches = client.list_batches()
for batch in batches:
    print(f"{batch['name']}: {batch['requestCount']} requests")
```

**Java:**
```java
Object batches = client.listBatches();
System.out.println(batches);
```

**Go:**
```go
batches, err := client.ListBatches()
if err != nil {
    log.Fatal(err)
}
for _, batch := range batches {
    fmt.Printf("%v: %d requests\n", batch["name"], batch["requestCount"])
}
```

## Error Handling

### JavaScript
```javascript
try {
  const webhook = await client.registerWebhook(...);
} catch (error) {
  console.error('API Error:', error.message);
}
```

### Python
```python
try:
    webhook = client.register_webhook(...)
except Exception as e:
    print(f"API Error: {str(e)}")
```

### Java
```java
try {
    Map<String, Object> webhook = client.registerWebhook(...);
} catch (Exception e) {
    System.err.println("API Error: " + e.getMessage());
}
```

### Go
```go
webhook, err := client.RegisterWebhook(...)
if err != nil {
    log.Fatalf("API Error: %v", err)
}
```

## Development & Testing

### Run Tests
Each SDK includes comprehensive test suites:

**JavaScript:**
```bash
npm test
```

**Python:**
```bash
pytest tests/
```

**Java:**
```bash
mvn test
```

**Go:**
```bash
go test ./...
```

## Contributing

To contribute to SDK development:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/sdk-enhancement`)
3. Make your changes and add tests
4. Submit pull request

## License

Apache 2.0 License - See LICENSE file for details

## Support

For issues and questions:
- GitHub Issues: https://github.com/supremeai/sdks/issues
- Documentation: https://supremeai.example.com/docs/sdk
- Email: sdk-support@supremeai.example.com
