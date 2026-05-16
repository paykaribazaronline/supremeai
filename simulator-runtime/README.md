# Simulator Runtime

Cloud Run service that serves generated application previews with device emulation.

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `APP_ID` | Generated app ID to serve | (required) |
| `DEVICE_TYPE` | Device profile (PIXEL_6, IPHONE_14, etc.) | `PIXEL_6` |
| `SIMULATOR_MODE` | Mode: preview or full | `preview` |
| `GOOGLE_APPLICATION_CREDENTIALS` | Path to service account key (optional) | uses default |

## Device Profiles

- `PIXEL_6` (412x915, DPR 2.6)
- `IPHONE_14` (390x844, DPR 3.0)
- `SAMSUNG_S22` (360x800, DPR 3.5)
- `IPAD_PRO` (1024x1366, DPR 2.0)
- `DESKTOP_1920` (1920x1080)
- `DESKTOP_1366` (1366x768)

## Endpoints

- `GET /` - Serves the app HTML with device emulation
- `GET /health` - Health check

## Deployment

```bash
gcloud run deploy simulator-app \
  --image gcr.io/supremeai-project/simulator-runtime:latest \
  --set-env-vars "APP_ID=abc123,DEVICE_TYPE=PIXEL_6" \
  --allow-unauthenticated
```

## How it works

1. Reads `APP_ID` and `DEVICE_TYPE` from environment.
2. Fetches the generated app document from Firestore `generated_apps` collection.
3. Injects device-specific viewport and JavaScript helpers into the HTML.
4. Returns transformed HTML.

**Note:** For production, pre-build the Docker image and push to Google Container Registry (GCR) or Artifact Registry.
