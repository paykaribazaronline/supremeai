# 📝 Manual Tasks TODO

> [!IMPORTANT]
> This document tracks the remaining manual tasks and architectural updates required to make the project fully production-ready. Please check them off as you complete them.

## Security & Core Flow
- [ ] **Session Takeover Security:** In `backend/api/routes/session_takeover.py`, replace the dummy `token.startswith("tok_")` check with real Redis/Database token validation for production.
- [ ] **Payment Flow Validation:** In `backend/api/routes/payments.py`, provide a real Stripe API key in the production environment and manually test the full checkout and webhook flow.

## Code Quality & CI/CD
- [ ] **Update Linting Rules:** Add a rule (e.g., Ruff's `BLE001` for blind excepts or `flake8-blind-except`) in your CI/CD pipeline or `.pre-commit-config.yaml` to prevent future commits containing `except Exception: pass`.

## Integrations & Features
- [ ] **Review 40+ Mock Tools:** Gradually replace the dummy/stub implementations across the `backend/tools/` directory (e.g., `image_generator.py`, `discord_bot.py`) with actual working APIs. Update `FEATURE_STATUS.md` as each tool is completed.

---
*Created during Follow-up Audit Phase.*
