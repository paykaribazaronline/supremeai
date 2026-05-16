# Chat Authentication & AI Orchestration Workflow

## Overview
This document describes how a chat request flows through the SupremeAI system after the security refactor.

## Request Flow

1.  **Frontend (React)**:
    *   User sends a message in `ChatWithAI.tsx`.
    *   `authUtils.fetchWithAuth` is used, which attaches either a **Firebase ID Token** (if logged in) or the string `"GUEST_MODE"` to the `Authorization` header.

2.  **API Gateway / Firewall**:
    *   Request reaches the Cloud Run instance.

3.  **Security Filters (Spring Boot)**:
    *   **AuthenticationFilter**:
        *   Recognizes the `/api/chat/` path.
        *   Extracts the token from the header.
        *   If it's a valid Firebase token, sets the `SecurityContext` with `ROLE_USER` or `ROLE_ADMIN`.
        *   If it's `"GUEST_MODE"`, sets the `SecurityContext` with `ROLE_GUEST`.
    *   **JwtAuthFilter**:
        *   Configured to **skip** `/api/chat/` and other Firebase-handled paths to avoid conflicts.

4.  **Controller (ChatController)**:
    *   Validates the request.
    *   Checks `@PreAuthorize` (supports ADMIN, USER, and GUEST).
    *   Passes the prompt to `MultiAIVotingService`.

5.  **AI Orchestration (MultiAIVotingService)**:
    *   Fetches active providers from Firestore/Database.
    *   Filters providers by work role (e.g., `general_chat`).
    *   Queries all selected providers in parallel.
    *   **Ensemble Voting**: Aggregates responses and selects the best one.
    *   **Solo-Mode Fallback**: If no providers are configured or respond, a meaningful system response is returned.

## Security Refactor Details
Previously, `/api/chat/` was skipped in `AuthenticationFilter` but enforced in `JwtAuthFilter`. Since `JwtAuthFilter` only validates backend-issued JWTs and not Firebase tokens, all dashboard requests were being rejected with 401/403 errors.

The fix involved:
- Allowing `AuthenticationFilter` to handle `/api/chat/`.
- Skipping `JwtAuthFilter` for `/api/chat/`.
- Updating `SecurityConfig` to permit these paths globally, delegating granular access control to the service layer.
