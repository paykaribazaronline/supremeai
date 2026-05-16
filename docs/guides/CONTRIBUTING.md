# Contributing to SupremeAI

## Commit Convention
Use these prefixes for all commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `chore:` Build/tooling changes
- `refactor:` Code restructuring
- `test:` Test additions/fixes
- `Cleanup:` Code cleanup

Example: `feat: add Python support to CodeFlow analyzer`

## PR Process
1. Create branch from `main`: `git checkout -b feat/your-feature`
2. Make changes with focused, atomic commits
3. Run tests: `./gradlew test`
4. Submit PR with description linking to issue
5. Self-review if no collaborators exist

## Code Standards
- **Java**: Follow Spring Boot conventions, use Lombok for boilerplate
- **TypeScript**: Strict mode, no `any` types, ESLint compliance
- **Tests**: Minimum 10% JaCoCo coverage for Java

## Critical Rules
- No hardcoded secrets (use env vars)
- No committed `node_modules/`, `build/`, `out/`
- All API endpoints need validation (`@Valid`)
- Security checks in service layer, not just controllers