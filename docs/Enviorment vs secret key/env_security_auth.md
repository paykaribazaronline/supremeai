# Environment Provider 6: Security, Auth & Cloud Infra Environment Variables (`env_security_auth.md`)

লোকাল `.env` ফাইল থেকে প্রাপ্ত সকল অরিজিনাল Security, Authentication & Infrastructure Keys-এর সুনির্দিষ্ট তালিকা:

| No. | Real Environment Variable Key | Real Scanned Value / Pattern | Status / Description |
| :--- | :--- | :--- | :--- |
| 1 | `SUPREMEAI_JWT_SECRET` | 64-Byte Hex String (`a47ee260ff2d...`) | Master JWT Session Signing Secret |
| 2 | `SUPREMEAI_ENCRYPTION_KEY` | `9llmzMU2XSRhbAS-R__JMW1XLZzc0ll7obD_RqaVwno=` | Core Data Encryption Key |
| 3 | `ENCRYPTION_KEY` | `X-mE_EtEtiznG1yU-Z0cQjhdh_ZjO1QT4gv1gSIx4ao=` | Secondary Data Encryption Key |
| 4 | `ENCRYPTION_KEY_VERIFY` | `CLXfPaAy8Zy7yCdqo3iU8Y05D-MEKKcR4fYQH-2UdvU=` | Encryption Verification Key |
| 5 | `SUPREMEAI_ADMIN_PASSWORD_HASH` | `$2b$12$LJ3m8yV6qN4xZ7wE5rT8yU...` | Admin Login Cryptographic Password Hash |
| 6 | `SUPREMEAI_ADMIN_TOTP_SECRET` | `JBSWY3DPEHPK3PXP` | Admin 2FA TOTP Secret Key |
| 7 | `DOCS_PASSWORD` | `supreme-admin-2026-prod` | FastAPI Docs Access Protection Password |
| 8 | `CI_WEBHOOK_SECRET` | `njel.com.bd` | Deployment Webhook Validation Secret |
| 9 | `INFISICAL_TOKEN` | `3a7cffea-e1c6-499a-8f90-074272c3e388` | Infisical Secret Manager Token |
| 10 | `INFISICAL_CLIENT_SECRET` | `ae8e5b3af30e5b7fb02e3a...` | Infisical Client Auth Secret |
| 11 | `RENDER_API_KEY` | `rnd_S0H7uYcNWmqX3jcepMTBL9WXghGP` | Render Cloud Management Primary API Key |
| 12 | `RENDER_API_KEY_BACKUP` | `rnd_dJiHyZJbMy9n1rd9PMEq2YpeEPVE` | Render Management Backup API Key |
| 13 | `RENDER_DEPLOY_HOOK_URL` | `https://api.render.com/deploy/srv-d995glt7vvec73f3jgo0?key=lA6DhuDe1JM` | Render Auto-Deployment Hook URL |
| 14 | `VERCEL_ORG_ID` | `team_I6s8TrHnQpPEAdItpClYsLdS` | Vercel Organization ID |
| 15 | `VERCEL_PROJECT_ID` | `prj_55iZ5J8xhiPlQqoYCRGhf8e0BaoA` | Vercel Project ID |
| 16 | `VERCEL_TOKEN` | `vcp_1SjPe5m5Nz... (Masked for Security)` | Vercel Deployment Access Token |
| 17 | `DISCORD_OTP_WEBHOOK_URL` | `https://discord.com/api/webhooks/1488528944432156672/...` | Admin JIT OTP Discord Webhook |
| 18 | `DISCORD_WEBHOOK_URL` | `https://discord.com/api/webhooks/1488528944432156672/...` | System Alert Discord Webhook |
| 19 | `RESEND_API_KEY` | `re_tCRmS7HK_4s... (Masked for Security)` | Resend Transactional Email API Key |
| 20 | `SENTRY_DSN` | `https://5a86f1f338af09b7beae051df3dcf490@o4511390015750144.ingest.us.sentry.io/4511390030233600` | Sentry Crash Monitoring DSN |
| 21 | `GCP_KMS_KEY_RING` | `supremeai-a-prod-ring` | GCP Key Management Service Ring Name |
| 22 | `ADMIN_EMAILS` | `["niloyjoy7@gmail.com", "test@gmail.com"]` | System Admin Contact Emails List |
