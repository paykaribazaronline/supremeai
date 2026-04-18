# Quickstart Troubleshooting Guide

**Version:** 1.0  
**Last Updated:** April 6, 2026  
**Purpose:** Help users resolve common setup and deployment issues quickly.

---

## Table of Contents

1. [Common Issues](#common-issues)
2. [Error Messages](#error-messages)
3. [Solutions](#solutions)
4. [Contact Support](#contact-support)

---

## Common Issues

- Environment variables not set
- Build fails on first run
- Database connection errors
- API keys missing or invalid
- Permission denied errors

---

## Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| `FIREBASE_SERVICE_ACCOUNT not set` | Missing env var | Add to .env file |
| `JWT_SECRET too short` | Weak secret | Use 32+ chars |
| `Database connection refused` | DB not running | Start DB service |
| `Permission denied` | File/folder perms | Run as admin or fix perms |

---

## Solutions

1. Double-check your `.env` file for all required variables.
2. Run `./gradlew build` to verify build passes.
3. If using Docker, ensure containers are running: `docker-compose up -d`
4. For Firebase issues, run `firebase login` and `firebase use --add`.
5. For permission errors, try running your terminal as administrator.

---

## Contact Support

- [Open an issue on GitHub](https://github.com/your-org/supremeai/issues)
- Email: support@supremeai.com

---

**Related Docs:**

- [Environment Variables Reference](../01-SETUP-DEPLOYMENT/ENVIRONMENT_VARIABLES_REFERENCE.md)
- [Developer Onboarding Guide](../12-GUIDES/DEVELOPER_ONBOARDING.md)
- [Documentation Completion Guide](../12-GUIDES/DOCUMENTATION_COMPLETION_GUIDE.md)

**Status:** ✅ Complete
