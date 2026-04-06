# Incident Learning Playbook

**Version:** 1.0  
**Last Updated:** 2026-04-05  
**Status:** Active

---

## Goal

Teach SupremeAI from every failure so repeated problems are solved faster and prevented earlier.

This playbook uses the learning API to capture:

1. Problem statement
2. Root cause
3. Applied fix
4. Prevention checks
5. Confidence score

---

## New Learning Endpoints

1. POST /api/learning/incident
2. GET /api/learning/incidents
3. GET /api/learning/incidents/{category}
4. POST /api/learning/ingest/logs?maxFiles=14
5. GET /api/learning/insights

These endpoints are authenticated and integrate with existing learning memory.

Scheduled ingestion also runs daily with:

- `learning.incident-ingestion.cron` (default: `0 0 3 * * *`)

---

## Incident Capture Request

Use this after every production or CI incident:

```json
{
  "category": "SECURITY",
  "problem": "Any authenticated user could read all database records",
  "rootCause": "Root-level authorization was too broad",
  "fix": "Set root deny rules and per-path authorization",
  "preventionChecks": [
    "Unauthenticated read to protected paths is denied",
    "Non-admin write to admin paths is denied",
    "Cross-user reads are denied"
  ],
  "confidenceScore": 0.97,
  "metadata": {
    "environment": "production",
    "source": "postmortem",
    "ticket": "SEC-214"
  }
}
```

---

## What Gets Learned Automatically

When incident capture runs, SupremeAI stores:

1. Error memory for pattern matching
2. Incident playbook for future remediation
3. Category-tagged solutions for retrieval

This enables both reactive and proactive learning.

---

## Daily Operating Loop

Run this loop every day:

1. Collect all failures from CI, runtime logs, and support
2. Submit each incident via POST /api/learning/incident
3. Review playbooks via GET /api/learning/incidents
4. Convert high-confidence incidents into guardrails/tests
5. Track recurring categories and prioritize root architecture fixes

---

## Quality Standard For Incident Entries

A high-quality incident entry must include:

1. Specific problem text (not vague)
2. Single clear root cause
3. Verifiable fix
4. At least 2 prevention checks
5. Confidence score between 0 and 1

---

## Important Constraint

No system can solve literally any problem without limits. SupremeAI can continuously improve toward broad problem solving by:

1. Capturing incidents with structure
2. Building reusable playbooks
3. Converting playbooks into tests, policies, and automation

---

## Related Documentation

- docs/05-AUTHENTICATION-SECURITY/FIREBASE_REALTIME_RULES_RUNBOOK.md
- docs/05-AUTHENTICATION-SECURITY/SECURITY_GUIDE.md
- docs/06-FEATURES/TEACHING_AND_LEARNING_SYSTEM.md
