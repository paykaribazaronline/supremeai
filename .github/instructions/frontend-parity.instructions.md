---
description: "Use when changing user-facing UI or flows in React dashboard, Flutter admin app, or combined deploy assets. Enforces feature parity across surfaces and navigation wiring consistency."
name: "Frontend Parity Rules"
applyTo: ["dashboard/src/**", "flutter_admin_app/lib/**", "combined_deploy/**"]
---
# Frontend Parity Rules

- Treat React dashboard and Flutter admin app as one product surface with parity requirements.
- If a user-facing feature is added, changed, or removed in one surface, mirror the same capability in the other surface.
- Do not mark features as "missing" until both surfaces are checked side by side.
- Keep labels, states, and core behavior aligned across React and Flutter.
- Preserve cloud-first behavior in UI flows: call backend/cloud APIs first and use local fallback only when necessary.
- Avoid hardcoded AI model names, IDs, URLs, or provider assumptions in frontend logic.

## Navigation Wiring Checklist

- For Flutter screens, ensure route wiring and discoverability are complete:
  - add route in app_routes.dart
  - add import and registration in main.dart where required
  - add entry point from home screen or quick action
- For React pages, ensure route and menu access are wired consistently.
- For combined deploy assets, keep behavior aligned with current backend APIs and dashboard feature set.

## Validation

- React checks (when touched):
  - cd dashboard
  - npm run lint
  - npm run build
- Flutter checks (when touched):
  - cd flutter_admin_app
  - flutter analyze
  - flutter test

## Link Targets

- Master project doc: MASTER_PROJECT_DOCUMENTATION.md
- Docs index: docs/README.md
- CommandHub docs: command-hub/README.md
- Contributing: docs/12-GUIDES/CONTRIBUTING.md
