# 📄 ফাইল: scripts/observability_report.json

**প্রকার:** .json  
**সাইজ:** 11,876 বাইট  
**আপডেট:** 2026-07-08T11:07:45.089784

---

## কোড

```json
{
    "silent_exceptions": [
        {
            "file": "core\\config_cache.py",
            "line": 113,
            "type": "RuntimeError"
        },
        {
            "file": "tests\\test_circuit_breaker.py",
            "line": 88,
            "type": "RuntimeError"
        },
        {
            "file": "tests\\test_monitoring.py",
            "line": 50,
            "type": "ImportError"
        },
        {
            "file": "tests\\test_new_interfaces.py",
            "line": 72,
            "type": "ImportError"
        },
        {
            "file": "tests\\test_sprint_g.py",
            "line": 410,
            "type": "Exception"
        },
        {
            "file": "tests\\test_telemetry.py",
            "line": 120,
            "type": "RuntimeError"
        },
        {
            "file": "tests\\test_voice_stream.py",
            "line": 18,
            "type": "ValueError"
        },
        {
            "file": "tools\\vision_agent.py",
            "line": 99,
            "type": "ImportError"
        },
        {
            "file": "tools\\vision_agent.py",
            "line": 109,
            "type": "ImportError"
        },
        {
            "file": "api\\routes\\session_takeover.py",
            "line": 45,
            "type": "CancelledError"
        }
    ],
    "print_statements": [
        {
            "file": "fix_tests.py",
            "line": 31
        },
        {
            "file": "run_roundtrip_tests.py",
            "line": 21
        },
        {
            "file": "core\\generation_monitor.py",
            "line": 70
        },
        {
            "file": "core\\generation_monitor.py",
            "line": 73
        },
        {
            "file": "core\\generation_monitor.py",
            "line": 72
        },
        {
            "file": "core\\knowledge_base.py",
            "line": 32
        },
        {
            "file": "core\\security_vault.py",
            "line": 29
        },
        {
            "file": "scripts\\check_ollama.py",
            "line": 35
        },
        {
            "file": "scripts\\load_seed_data.py",
            "line": 92
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 26
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 27
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 40
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 41
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 52
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 53
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 65
        },
        {
            "file": "scripts\\run_dependency_check.py",
            "line": 66
        },
        {
            "file": "tools\\agent_tools.py",
            "line": 10
        },
        {
            "file": "tools\\agent_tools.py",
            "line": 20
        },
        {
            "file": "tools\\agent_tools.py",
            "line": 29
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 130
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 141
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 111
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 68
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 117
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 157
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 158
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 159
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 160
        },
        {
            "file": "tools\\bengali_ocr_converter.py",
            "line": 161
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 163
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 164
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 167
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 191
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 192
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 193
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 196
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 199
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 202
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 206
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 174
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 180
        },
        {
            "file": "tools\\fuzz_sandbox.py",
            "line": 186
        },
        {
            "file": "tools\\git_knowledge_extractor.py",
            "line": 58
        },
        {
            "file": "tools\\git_knowledge_extractor.py",
            "line": 124
        },
        {
            "file": "tools\\git_knowledge_extractor.py",
            "line": 52
        },
        {
            "file": "tools\\git_knowledge_extractor.py",
            "line": 96
        },
        {
            "file": "tools\\langchain_agent_example.py",
            "line": 112
        },
        {
            "file": "tools\\langchain_agent_example.py",
            "line": 82
        },
        {
            "file": "tools\\langchain_agent_example.py",
            "line": 120
        },
        {
            "file": "tools\\langchain_agent_example.py",
            "line": 116
        },
        {
            "file": "tools\\langchain_agent_example.py",
            "line": 118
        },
        {
            "file": "tools\\multi_account_rotator.py",
            "line": 899
        },
        {
            "file": "tools\\multi_account_rotator.py",
            "line": 900
        },
        {
            "file": "tools\\multi_account_rotator.py",
            "line": 891
        },
        {
            "file": "tools\\multi_account_rotator.py",
            "line": 895
        },
        {
            "file": "tools\\pre_commit_ai.py",
            "line": 291
        },
        {
            "file": "tools\\pre_commit_ai.py",
            "line": 293
        },
        {
            "file": "tools\\pre_commit_ai.py",
            "line": 298
        },
        {
            "file": "tools\\pre_commit_ai.py",
            "line": 301
        },
        {
            "file": "tools\\pre_commit_ai.py",
            "line": 304
        },
        {
            "file": "tools\\seed_database.py",
            "line": 41
        },
        {
            "file": "tools\\seed_database.py",
            "line": 49
        },
        {
            "file": "tools\\seed_database.py",
            "line": 46
        },
        {
            "file": "tools\\seed_database.py",
            "line": 129
        },
        {
            "file": "tools\\seed_database.py",
            "line": 140
        },
        {
            "file": "tools\\seed_database.py",
            "line": 158
        },
        {
            "file": "tools\\seed_database.py",
            "line": 132
        },
        {
            "file": "tools\\seed_database.py",
            "line": 154
        },
        {
            "file": "tools\\seed_database.py",
            "line": 134
        },
        {
            "file": "tools\\seed_database.py",
            "line": 138
        },
        {
            "file": "tools\\seed_database.py",
            "line": 156
        },
        {
            "file": "tools\\seed_database.py",
            "line": 68
        },
        {
            "file": "tools\\seed_database.py",
            "line": 77
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 31
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 61
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 62
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 63
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 64
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 65
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 66
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 67
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 68
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 71
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 79
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 80
        },
        {
            "file": "scripts\\benchmark\\load_test_phase3.py",
            "line": 90
        },
        {
            "file": "api\\routes\\agent_workspace.py",
            "line": 43
        },
        {
            "file": "api\\routes\\agent_workspace.py",
            "line": 65
        },
        {
            "file": "api\\routes\\agent_workspace.py",
            "line": 105
        },
        {
            "file": "api\\routes\\task_workspace.py",
            "line": 69
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 85
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 90
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 69
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 71
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 191
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 156
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 159
        },
        {
            "file": "api\\routes\\websocket_agent.py",
            "line": 197
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 28
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 33
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 177
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 71
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 100
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 149
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 151
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 172
        },
        {
            "file": "api\\routes\\websocket_voice.py",
            "line": 165
        }
    ]
}
```