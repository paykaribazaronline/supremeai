# SupremeAI Administrator Guidelines

## Dashboard Management
- **Dynamic Configuration**: All system settings, API keys, and model roles must be managed via the Admin Dashboard. Avoid direct Firestore edits unless necessary.
- **Provider Rotation**: Regularly check the health status of AI providers. If a primary provider shows high failure rates, rotate it to secondary or emergency status.
- **Quota Monitoring**: Monitor provider quotas daily to prevent service interruptions.

## Security Protocols
- **API Key Safety**: Never store API keys in plain text files. Use the encrypted storage provided by the system.
- **Admin Access**: Multi-factor authentication (MFA) must be enabled for all administrative accounts.
- **Log Audit**: Review the `ActivityLog` frequently for any suspicious behavior or failed authentication attempts.

## System Resilience
- **Offline Mode**: Ensure the `core_knowledge.json` is updated before any major system maintenance. This serves as the ultimate fallback.
- **Backup & Recovery**: Daily backups of the Firestore database and project configuration are mandatory.

## User Management
- **Role Assignment**: Only assign 'ADMIN' roles to trusted personnel. Most users should operate under 'USER' or 'DEVELOPER' roles with limited permissions.
- **Language Support**: When adding system-wide messages or instructions, ensure translations are provided in both English and Bengali.

## System Stability & Maintenance
- **Firestore Model Integrity**: Never add a field named `id` to a Firestore model that uses `@DocumentId`. Use `documentId` for the annotation and provide `getId()`/`setId()` wrappers if needed.
- **Timestamp Robustness**: Always use `Object` for Date fields in models and use the standard `convertToDate` helper to handle `Long`, `Timestamp`, and `Map` formats from Firestore.
- **Service Resilience**: Ensure all AI-related services (Voting, Fallback, Learning) use Reactive patterns (`Mono`/`Flux`) and have circuit breakers configured.
- **Cache Initialization**: If the dashboard or chat feels slow, check the `KnowledgeSeederServiceEnhanced` logs. A failure in cache initialization usually indicates a data type mismatch in `SystemLearning`.
