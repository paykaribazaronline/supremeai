# GCP Cloud Run Cleanup Plan

## Current Status
Total Cloud Run Services: 28
- **5 AI Models (Essential - Keep)**: `supreme-ai-qwen-coder`, `supreme-ai-llama-3-1`, `supreme-ai-phi-3`, `supreme-ai-nomic-embed`, `supreme-ai-deepseek-pro`
- **3 Main Application Services (Essential - Keep)**: `supremeai`, `supremeai-backend`, `n8n`
- **10+ Utility Services (Candidates for Removal)**

## Unused Services to Remove/Cleanup

| Service | Last Deploy | Purpose | Recommendation | Storage Impact |
|---------|-------------|---------|----------------|----------------|
| `analyzedeployment` | 2026-05-17 | Deployment analysis | **DELETE** - GitHub Actions covers this | ~50MB |
| `approverequirement` | 2026-05-17 | CI approval handler | **DELETE** - Redundant | ~45MB |
| `autoapprovescheduled` | 2026-05-17 | Scheduled auto-approval | **DELETE** - GitHub Actions covers this | ~50MB |
| `checkserverconnections` | 2026-05-17 | Connection checker | **DELETE** - Part of health monitoring | ~40MB |
| `collecthealthmetrics` | 2026-05-17 | Metrics collection | **DELETE** - Use Cloud Monitoring instead | ~55MB |
| `exportocrtoexcel` | 2026-05-17 | OCR export utility | **DELETE** - Not used | ~45MB |
| `getocrresults` | 2026-05-17 | OCR results fetcher | **DELETE** - Not used | ~40MB |
| `getsystemhealth` | 2026-05-17 | System health check | **DELETE** - Consolidate into one health service | ~40MB |
| `monitorconnections` | 2026-05-17 | Connection monitor | **DELETE** - Redundant with checkserverconnections | ~45MB |
| `monitorsystemhealth` | 2026-05-17 | Health monitoring | **DELETE** - Consolidate | ~50MB |
| `processbengaliocr` | 2026-05-17 | Bengali OCR processor | **DELETE** - Very specialized, check usage | ~60MB |
| `processrequirement` | 2026-05-17 | Requirement processor | **DELETE** - Part of workflow | ~45MB |
| `rotateagent` | 2026-05-17 | Agent rotation | **DELETE** - Logic should be in main app | ~40MB |
| `updateprogress` | 2026-05-17 | Progress updater | **DELETE** - Not essential | ~35MB |
| `reverse-engineering` | 2026-05-17 | Reverse engineering tool | **DELETE** - Legacy/unused | ~500MB (large) |
| `simulator-runtime` | 2026-05-17 | Simulator runtime | **DELETE** - Legacy/unused | ~300MB |
| `teldrive` | 2026-05-15 | Telegram drive | **DELETE** - Unknown purpose | ~100MB |

## Safe Services to Keep

| Service | Reason to Keep |
|---------|----------------|
| `supreme-ai-*` (5 AI models) | Core functionality |
| `supremeai` | Main application |
| `supremeai-backend` | Backend API |
| `n8n` | Workflow automation |
| `voice-hub` | Voice processing |

## Cleanup Commands

```bash
# Remove individual services
gcloud run services delete analyzedeployment --region us-central1 --quiet
gcloud run services delete approverequirement --region us-central1 --quiet
gcloud run services delete autoapprovescheduled --region us-central1 --quiet
gcloud run services delete checkserverconnections --region us-central1 --quiet
gcloud run services delete collecthealthmetrics --region us-central1 --quiet
gcloud run services delete exportocrtoexcel --region us-central1 --quiet
gcloud run services delete getocrresults --region us-central1 --quiet
gcloud run services delete getsystemhealth --region us-central1 --quiet
gcloud run services delete monitorconnections --region us-central1 --quiet
gcloud run services delete monitorsystemhealth --region us-central1 --quiet
gcloud run services delete processbengaliocr --region us-central1 --quiet
gcloud run services delete processrequirement --region us-central1 --quiet
gcloud run services delete rotateagent --region us-central1 --quiet
gcloud run services delete updateprogress --region us-central1 --quiet
gcloud run services delete reverse-engineering --region us-central1 --quiet
gcloud run services delete simulator-runtime --region us-central1 --quiet
gcloud run services delete teldrive --region us-central1 --quiet
```

## Expected Savings
- **Storage**: ~1.3GB+ reduction
- **Costs**: Reduced Cloud Run instance hours
- **Complexity**: Simplified architecture