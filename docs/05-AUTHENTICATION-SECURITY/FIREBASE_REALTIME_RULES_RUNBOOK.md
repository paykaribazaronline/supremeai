# Firebase Realtime Database Rules Runbook

**Version:** 1.0  
**Last Updated:** 2026-04-05  
**Status:** Active

---

## Purpose

This runbook teaches SupremeAI operators and automation how to:

1. Set secure Realtime Database rules
2. Deploy rules safely
3. Configure admin custom claims
4. Validate access controls
5. Roll back quickly if needed

---

## Golden Security Model

Always enforce these rules:

1. Default deny at root (`.read=false`, `.write=false`)
2. Allow only explicit paths
3. Use Firebase Auth identity checks (`auth.uid`)
4. Use admin custom claim checks (`auth.token.admin === true`)
5. Prevent privilege escalation on user profile fields

---

## Source of Truth

Rules must be stored in repository and deployed from code:

- `database.rules.json`
- `firebase.json` with `database.rules` pointing to that file

Never treat the Firebase Console Rules tab as the primary source.

---

## Deploy Procedure

From repository root:

```bash
firebase deploy --only database
```

Expected success output includes:

1. `database: rules syntax ... is valid`
2. `database: rules ... released successfully`

---

## Manual Console Procedure (Fallback)

Use only when CLI deployment is temporarily unavailable.

1. Open Firebase Console -> Realtime Database -> Rules
2. Paste exact content of `database.rules.json`
3. Click Publish
4. Immediately reconcile by committing/updating repository rules if a temporary manual edit was made

---

## Admin Custom Claims (Required)

Rules rely on `auth.token.admin === true` for privileged paths.

If claims are missing, admin reads/writes will be denied.

Example claim assignment with Admin SDK (Node.js):

```js
// Run in trusted backend/admin script only.
await admin.auth().setCustomUserClaims(uid, { admin: true });
```

User must refresh ID token after claim changes.

---

## Required Validation Checklist

After each rules deployment, validate all 6 scenarios:

1. Unauthenticated read to `/projects` is denied
2. Unauthenticated write to `/users` is denied
3. Authenticated non-admin cannot read another user's node
4. Authenticated non-admin cannot set `isAdmin=true`
5. Authenticated admin can read/write admin-only paths
6. Notification read is limited to owner or admin

Do not mark deployment successful unless all checks pass.

---

## Rollback Strategy

If legitimate traffic is blocked after rules release:

1. Restore last known-good `database.rules.json` from git
2. Redeploy:

```bash
firebase deploy --only database
```

3. Log incident with:
   - failing path
   - auth context
   - expected vs actual behavior
   - fix commit hash

---

## Common Mistakes To Avoid

1. Setting `.read` or `.write` to `auth != null` at root
2. Assuming authentication equals authorization
3. Forgetting to set admin custom claims
4. Editing console rules without committing source file
5. Allowing users to write privileged fields (`isAdmin`, `role`, `permissions`)

---

## Operational Policy

1. Every rules change must go through repository review
2. Every release must run the validation checklist
3. Every incident must update this runbook with new prevention guidance

---

## Related Documentation

- `docs/05-AUTHENTICATION-SECURITY/SECURITY_GUIDE.md`
- `docs/05-AUTHENTICATION-SECURITY/AUTHENTICATION_GUIDE.md`
- `firebase.json`
- `database.rules.json`
