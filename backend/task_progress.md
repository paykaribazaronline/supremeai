# Phase 4 Task Progress

## Audit Target: backend/tools/ + backend/scripts/ + backend/utils/

### Issues Found

#### P1 (High) — 3 issues
- [ ] AUDIT-018: Command Injection — `agent_tools.py:180-183` — fragile `python -c "code"` string interpolation passed to `sandbox.execute_command()` which uses `sh -c cmd` in Docker. Replace with temp-file mount approach.
- [ ] AUDIT-019: Command Injection — `tools/devops/docker_sandbox.py:136-148` — `execute_command()` passes raw `cmd` string to `sh -c cmd` inside Docker container, enabling shell injection. Same pattern as Phase 1 fix but in a different file.
- [ ] AUDIT-020: Hardcoded OTP — `multi_account_rotator.py:268-272` — hardcoded `"123456"` and `"https://verify.com/link"` inserted into verification_queue. Should not inject test data into production DB.

#### P2 (Medium) — 5 issues
- [ ] AUDIT-021: Missing timeout — `freebuff_client.py:21` — `proc.communicate()` without timeout, can hang indefinitely.
- [ ] AUDIT-022: Silent failure — `multi_account_rotator.py:322-323` — `contextlib.suppress(Exception)` swallows wait_for_selector errors.
- [ ] AUDIT-023: Silent failure — `vpn_switcher.py:150-156` — broken nested try/except pattern suppresses errors (also in mcp_supabase.py, telegram_bot.py, playwright_browser_agent.py).
- [ ] AUDIT-024: Unhandled exception — `check_ollama.py:62-66` — `list_models()` has no try/except, crashes if Ollama server down.
- [ ] AUDIT-025: Broken exception pattern — `check_ollama.py:54-56` — broad except with only print(), no structured logging.

#### P3 (Low) — 2 issues
- [ ] AUDIT-026: Dead code / unused imports — scan for and clean up.
- [ ] AUDIT-027: SQL injection risk — `mcp_supabase.py:254` — f-string interpolation of table_name/columns into CREATE TABLE SQL.

### Fix Priority
1. AUDIT-018 (P1) — command injection in agent_tools.py
2. AUDIT-019 (P1) — command injection in docker_sandbox.py
3. AUDIT-020 (P1) — hardcoded OTP
4. AUDIT-021 (P2) — missing timeout
5. AUDIT-022/023 (P2) — silent failures
6. AUDIT-024/025 (P2/P3) — check_ollama reliability
7. AUDIT-027 (P3) — SQL injection risk
8. AUDIT-026 (P3) — dead code cleanup
9. Update PHASE_LOG.md with Phase 4 findings and fixes
