# 📄 ফাইল: backend/tests/test_prompt_handler.py

**প্রকার:** .py  
**সাইজ:** 778 বাইট  
**আপডেট:** 2026-07-08T01:44:17.643977

---

## কোড

```py
import pytest
from core.prompt_handler import normalize_prompt, estimate_tokens

def test_normalize_prompt_string():
    prompt = "Hello World"
    assert normalize_prompt(prompt) == "Hello World"

def test_normalize_prompt_list():
    prompt = [
        {"role": "system", "content": "You are a helpful assistant."},
        {"role": "user", "content": "Tell me a joke."}
    ]
    assert normalize_prompt(prompt) == "Tell me a joke."

def test_normalize_prompt_empty_list():
    prompt = []
    assert normalize_prompt(prompt) == ""

def test_estimate_tokens():
    prompt = "12345678"  # 8 chars
    assert estimate_tokens(prompt) == 2

def test_estimate_tokens_list():
    prompt = [
        {"role": "user", "content": "1234"}
    ]
    assert estimate_tokens(prompt) == 1

```