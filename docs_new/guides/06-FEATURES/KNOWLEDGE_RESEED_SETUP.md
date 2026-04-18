# Knowledge Reseed Setup

Purpose: make automatic knowledge reseeding run on every push through GitHub Actions.

## Required GitHub Secrets

- `SUPREMEAI_BASE_URL`
  - Example: `https://your-domain.com`
  - Must point to the deployed SupremeAI backend base URL

- `SUPREMEAI_SETUP_TOKEN`
  - Must match the backend environment variable used to protect `/api/auth/setup`
  - The same token is used by the workflow to call `/api/learning/reseed`

## What Happens On Push

Workflow file: [.github/workflows/knowledge-reseed.yml](..\..\.github\workflows\knowledge-reseed.yml)

On every push to `main`, `master`, `develop`, or `feature/**`:

1. GitHub Actions checks whether both secrets exist.
2. If they exist, it sends `POST /api/learning/reseed` to the deployed backend.
3. SupremeAI re-injects all knowledge seeders.
4. Learning stats are refreshed remotely.

If the secrets are not configured, the workflow exits safely without failing the pipeline.

## Fastest Setup

Use the PowerShell helper:

```powershell
.\scripts\setup-knowledge-reseed-secrets.ps1 -BaseUrl "https://your-domain.com" -SetupToken "your-token"
```

Requirements:

- GitHub CLI (`gh`) installed
- `gh auth login` completed
- Permission to write repository secrets

## Manual GitHub CLI Commands

```powershell
gh secret set SUPREMEAI_BASE_URL --body "https://your-domain.com" --repo supremeai/supremeai
gh secret set SUPREMEAI_SETUP_TOKEN --body "your-token" --repo supremeai/supremeai
```

## Verify

After setting secrets:

1. Push any commit.
2. Open GitHub Actions.
3. Check the `Knowledge Reseed` workflow run.
4. Confirm the deployed system logs a reseed request.

## Notes

- This does not replace startup seeding. Startup seeding still happens when the app boots.
- Push-based reseeding is useful after documentation, rules, or seed logic changes.
- Keep `SUPREMEAI_SETUP_TOKEN` secret and never commit it into the repository.
