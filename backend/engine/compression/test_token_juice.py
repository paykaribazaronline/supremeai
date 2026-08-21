"""Tests for TokenJuice Context & Tool Output Compression Engine."""

import json
import pytest
from backend.engine.compression.token_juice import TokenJuice


@pytest.fixture
def juice():
    return TokenJuice()


def test_compress_dom_html(juice):
    raw_html = """
    <!DOCTYPE html>
    <html>
      <head>
        <title>Dashboard</title>
        <script src="bundle.js"></script>
        <style>.box { color: red; }</style>
      </head>
      <body>
        <!-- Header comment -->
        <div class="header-nav bg-blue p-4">
          <h1 id="main-title">SupremeAI Dashboard</h1>
          <button id="btn-deploy" class="btn btn-primary" data-testid="deploy-btn" onclick="doDeploy()">Deploy App</button>
          <a href="/settings" class="nav-link">Settings</a>
          <svg><path d="M10 10 H 90 V 90 H 10 L 10 10"/></svg>
        </div>
        <div class="empty-container"></div>
        <div class="content-body">
          <p>Welcome to SupremeAI Workspace.</p>
        </div>
      </body>
    </html>
    """
    res = juice.compress(raw_html, content_type="html")
    assert res.content_type == "html"
    assert res.compression_ratio > 0.35  # At least 35% reduction on raw HTML
    assert "SupremeAI Dashboard" in res.compressed_text
    assert "btn-deploy" in res.compressed_text
    assert "data-testid=\"deploy-btn\"" in res.compressed_text
    assert "<script" not in res.compressed_text
    assert "<style" not in res.compressed_text
    assert "Header comment" not in res.compressed_text


def test_compress_json_payload(juice):
    raw_payload = {
        "status": "success",
        "$schema": "http://json-schema.org/draft-07/schema#",
        "etag": "w/123456789",
        "empty_field": None,
        "empty_list": [],
        "data": {
            "users": [
                {"id": 1, "name": "Alice", "role": "admin", "details": None},
                {"id": 2, "name": "Bob", "role": "developer"},
                {"id": 3, "name": "Charlie", "role": "reviewer"},
                {"id": 4, "name": "David", "role": "designer"},
                {"id": 5, "name": "Eve", "role": "tester"},
                {"id": 6, "name": "Frank", "role": "devops"},
                {"id": 7, "name": "Grace", "role": "security"},
            ]
        }
    }
    raw_str = json.dumps(raw_payload, indent=2)
    res = juice.compress(raw_str, content_type="json")
    
    assert res.content_type == "json"
    assert res.compression_ratio > 0.3
    # Check that nulls and metadata fields were pruned
    assert "empty_field" not in res.compressed_text
    assert "$schema" not in res.compressed_text
    # Check that large array was capped/sampled with note
    assert "omitted by TokenJuice" in res.compressed_text


def test_compress_terminal_logs(juice):
    raw_logs = """
    \x1b[32m[INFO]\x1b[0m Starting compilation...
    \x1b[33m[WARN]\x1b[0m Deprecated package detected
    Building chunks: [========================>] 100% | 50/50 [00:02<00:00, 24.12it/s]
    Repeating log heartbeat...
    Repeating log heartbeat...
    Repeating log heartbeat...
    Repeating log heartbeat...
    \x1b[31mERROR:\x1b[0m Failed to compile module `server.ts`
    Traceback (most recent call last):
      File "server.py", line 42, in <module>
        raise ValueError("Invalid configuration")
    ValueError: Invalid configuration
    """
    res = juice.compress(raw_logs, content_type="terminal")
    assert res.content_type == "terminal"
    assert "\x1b[" not in res.compressed_text
    assert "repeated 3 times" in res.compressed_text or "repeated 4 times" in res.compressed_text
    assert "ERROR:" in res.compressed_text
    assert "ValueError: Invalid configuration" in res.compressed_text


def test_compress_git_diff(juice):
    raw_diff = """
diff --git a/backend/main.py b/backend/main.py
index 123..456 100644
--- a/backend/main.py
+++ b/backend/main.py
@@ -10,3 +10,4 @@
 def start_server():
+    print("TokenJuice activated")
     return True
diff --git a/pnpm-lock.yaml b/pnpm-lock.yaml
index abc..def 100644
--- a/pnpm-lock.yaml
+++ b/pnpm-lock.yaml
@@ -1,500 +1,500 @@
- lockfile data 1
+ lockfile data 2
- lockfile data 3
+ lockfile data 4
diff --git a/src/App.tsx b/src/App.tsx
index 789..012 100644
--- a/src/App.tsx
+++ b/src/App.tsx
@@ -5,3 +5,4 @@
 export function App() {
+  return <Dashboard />;
 }
    """
    res = juice.compress(raw_diff, content_type="git_diff")
    assert res.content_type == "git_diff"
    assert "TokenJuice activated" in res.compressed_text
    assert "<Dashboard />" in res.compressed_text
    assert "lines of lockfile/asset diff omitted" in res.compressed_text


def test_compress_empty_and_generic(juice):
    empty_res = juice.compress("")
    assert empty_res.compressed_text == ""
    assert empty_res.compression_ratio == 0.0

    generic = juice.compress("Hello    world!\n\n\n\nHow   are   you?")
    assert generic.compressed_text == "Hello world!\n\nHow are you?"
