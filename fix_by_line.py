import sys
import re
from pathlib import Path

audit_output = """
❌ FAIL: F:\\supremeai\\backend\\main.py:24 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\main.py:28 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\competitive_kit.py:833 - Broad exception handler discards exception (no log/re-raise, error not surfaced to caller)
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:367 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:368 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:369 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:370 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:371 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:374 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:375 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:377 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:378 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:380 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:383 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:384 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:386 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:387 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:390 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:391 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:393 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\env_validator.py:395 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:275 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:288 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:302 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:411 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:425 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:597 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:679 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:1142 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:1163 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\scripts\\superai_free_tier_monitor.py:1185 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:658 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:659 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:660 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:662 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:663 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:666 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:667 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:668 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:669 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:670 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:671 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:672 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:673 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:676 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:677 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:688 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:689 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:690 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:691 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:694 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:696 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\services\\security_auditor.py:698 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:658 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:659 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:660 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:662 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:663 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:666 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:667 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:668 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:669 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:670 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:671 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:672 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:673 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:676 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:677 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:688 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:689 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:690 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:691 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:694 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:696 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\core\\security\\audit\\security_auditor.py:698 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\api\\routes\\service_topology.py:278 - Silent exception handler (`except Exception: pass`)
❌ FAIL: F:\\supremeai\\backend\\api\\routes\\session_takeover.py:95 - Unsafe `print()` statement in backend logic
❌ FAIL: F:\\supremeai\\backend\\api\\routes\\session_takeover.py:97 - Unsafe `print()` statement in backend logic
"""

files_to_fix = {}

for line in audit_output.strip().splitlines():
    if "FAIL:" in line:
        parts = line.split(" - ")
        file_line = parts[0].replace("❌ FAIL: ", "").strip()
        issue = parts[1].strip()
        file_path, line_no = file_line.rsplit(":", 1)
        line_no = int(line_no)
        
        if file_path not in files_to_fix:
            files_to_fix[file_path] = []
        files_to_fix[file_path].append((line_no, issue))

for file_path, fixes in files_to_fix.items():
    if not Path(file_path).exists():
        continue
    content = Path(file_path).read_text(encoding="utf-8")
    lines = content.splitlines()
    
    # We process in reverse order to not mess up line numbers if we add lines
    fixes.sort(key=lambda x: x[0], reverse=True)
    
    for line_no, issue in fixes:
        idx = line_no - 1
        if "except Exception: pass" in issue or "discards exception" in issue:
            if "except" in lines[idx]:
                for i in range(idx, min(idx+10, len(lines))):
                    if "pass" in lines[i] and not lines[i].strip().startswith("#"):
                        lines[i] = lines[i].replace("pass", "import logging; logging.warning('Ignored exception')")
                        break
                    if "..." in lines[i] and not lines[i].strip().startswith("#"):
                        lines[i] = lines[i].replace("...", "import logging; logging.warning('Ignored exception')")
                        break
        elif "Unsafe `print()` statement" in issue:
            if "print(" in lines[idx]:
                lines[idx] = lines[idx].replace("print(", "import logging; logging.getLogger(__name__).info(")
                
    Path(file_path).write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Fixed {file_path}")

