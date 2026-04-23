# URL Redirect Setup

## Current Production URLs

| Environment | URL | Status |
|-------------|-----|--------|
| Production | `https://supremeai-lhlwyikwlq-uc.a.run.app` | Active |
| Local | `http://localhost:8001` | Development |

## Old URL Redirect

If you had a previous production URL, set up redirects to avoid broken links:

### Cloud Run Redirect

For Google Cloud Run, add a redirect handler in your application:

```java
@RestController
public class RedirectController {

    @GetMapping("/")
    public ResponseEntity<Void> redirectRoot() {
        return ResponseEntity.status(301)
            .header("Location", "/admin.html")
            .build();
    }
}
```

### DNS Level Redirect

If the old URL used a custom domain, configure DNS-level redirect:

1. Go to your DNS provider (Google Domains, Cloud DNS, etc.)
2. Add a CNAME or A record pointing to the new Cloud Run URL
3. Or use a URL redirect record if supported

### Nginx Redirect (if using reverse proxy)

```nginx
server {
    listen 80;
    server_name old-supremeai.example.com;

    location / {
        return 301 https://supremeai-lhlwyikwlq-uc.a.run.app$request_uri;
    }
}
```

## Cloud Run Custom Domain

To use a custom domain:

```bash
gcloud domains mappings create \
    --domain=supremeai.yourdomain.com \
    --projects=your-project-id \
    --services=supremeai
```

## Health Check Endpoint

Verify deployment health:

```bash
curl https://supremeai-lhlwyikwlq-uc.a.run.app/api/status
```

Expected response:

```json
{
  "status": "UP",
  "timestamp": "2026-04-23T..."
}
```
