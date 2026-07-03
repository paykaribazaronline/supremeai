# 📄 ফাইল: scripts/generate_smart_docs.py

**প্রকার:** .py  
**সাইজ:** 11,400 বাইট  
**আপডেট:** 2026-07-03T13:02:02.760210

---

## কোড

```py
#!/usr/bin/env python3
"""
🤖 স্মার্ট ডকুমেন্টেশন জেনারেটর — SupremeAI 2.0
মডুলার কোডবেস এবং চেঞ্জলগ অটো-জেনারেশন

রিড করুন:
- docs/codebase/ - প্রজেক্টের সব কোড ফাইল মডুলার ফর্ম্যাটে
- docs/changes/ - প্রতিটি কমিটে কী পরিবর্তন হয়েছে

এটি আপনার AI এজেন্ট এবং অডিটরদের দক্ষতা বাড়ায়।
"""

import os
import subprocess
from pathlib import Path
from datetime import datetime
import json

# ফাইল এক্সটেনশন যা ডকুমেন্ট করতে হবে
DOCUMENT_EXTENSIONS = {
    '.py', '.ts', '.tsx', '.js', '.jsx',
    '.dart', '.yml', '.yaml', '.json',
    '.toml', '.lock', '.md', '.sql',
    '.go', '.rs', '.java', '.rb', '.cpp', '.c'
}

# এড়িয়ে যাওয়ার ফোল্ডার
IGNORE_DIRS = {
    '.git', 'node_modules', '.venv', '.env',
    '.docusaurus', 'docs', '.next', 'dist',
    'build', 'out', '__pycache__', '.pytest_cache',
    '.mypy_cache', 'venv', 'env', '.idea',
    '.vscode', 'coverage', 'logs'
}

def should_skip_path(path_str: str) -> bool:
    """পাথ স্কিপ করা উচিত কিনা চেক করুন"""
    for ignore in IGNORE_DIRS:
        if f"{os.sep}{ignore}{os.sep}" in f"{os.sep}{path_str}{os.sep}":
            return True
    return False

def sanitize_filename(file_path: str) -> str:
    """ফাইল পাথকে নিরাপদ ফাইলনামে রূপান্তর করুন"""
    # বাংলা মন্তব্য: পাথ সেপারেটর এবং স্পেস সহ যেকোনো বিশেষ অক্ষর বদলে দেওয়া
    name = file_path.replace(os.sep, '_').replace('..', '_parent_').replace(' ', '_')
    # আন্ডারস্কোর কমিয়ে দিন
    while '__' in name:
        name = name.replace('__', '_')
    return name

def generate_modular_docs():
    """মডুলার ডকুমেন্টেশন জেনারেট করুন"""
    root_dir = "."
    output_codebase = Path("docs/codebase")
    output_codebase.mkdir(parents=True, exist_ok=True)
    
    file_count = 0
    total_size = 0
    
    print(f"📝 মডুলার কোডবেস ডকুমেন্টেশন জেনারেট করছি...")
    
    for root, dirs, files in os.walk(root_dir):
        # এড়িয়ে যাওয়ার ফোল্ডার বাদ দিন
        dirs[:] = [d for d in dirs if not should_skip_path(os.path.join(root, d))]
        
        for file in files:
            file_path = Path(root) / file
            
            # এক্সটেনশন চেক করুন
            if file_path.suffix not in DOCUMENT_EXTENSIONS:
                continue
            
            try:
                content = file_path.read_text(encoding='utf-8', errors='replace')
                rel_path = file_path.relative_to(root_dir)
                
                # নিরাপদ ফাইলনাম তৈরি করুন
                modular_name = sanitize_filename(str(rel_path))
                output_file = output_codebase / f"{modular_name}.md"
                
                # মেটাডেটা যোগ করুন
                file_size = len(content.encode('utf-8'))
                total_size += file_size
                
                header = f"""# 📄 ফাইল: {rel_path}

**প্রকার:** {file_path.suffix}  
**সাইজ:** {file_size:,} বাইট  
**আপডেট:** {datetime.now().isoformat()}

---

## কোড

"""
                
                output_file.write_text(header + f"```{file_path.suffix[1:]}\n{content}\n```")
                file_count += 1
                
            except Exception as e:
                print(f"⚠️ স্কিপ করা হয়েছে {file_path}: {e}")
    
    print(f"✅ {file_count} ফাইল ডকুমেন্ট করা হয়েছে ({total_size:,} বাইট)")
    return file_count

def generate_changelog():
    """চেঞ্জলগ জেনারেট করুন (গত কমিটের পার্থক্য)"""
    output_changes = Path("docs/changes")
    output_changes.mkdir(parents=True, exist_ok=True)
    
    print(f"📊 চেঞ্জলগ জেনারেট করছি...")
    
    try:
        # গত কমিটের তুলনা পান
        diff_output = subprocess.check_output(
            ["git", "diff", "HEAD~1", "HEAD"],
            stderr=subprocess.DEVNULL
        ).decode('utf-8', errors='replace')
        
        if not diff_output.strip():
            print("ℹ️ কোনো পরিবর্তন নেই")
            return
        
        # কমিট ইনফো পান
        commit_hash = subprocess.check_output(
            ["git", "rev-parse", "--short", "HEAD"]
        ).decode().strip()
        
        commit_msg = subprocess.check_output(
            ["git", "log", "-1", "--pretty=%B"]
        ).decode().strip()
        
        commit_author = subprocess.check_output(
            ["git", "log", "-1", "--pretty=%an <%ae>"]
        ).decode().strip()
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # স্ট্যাটিস্টিক্স পান
        stats = subprocess.check_output(
            ["git", "diff", "--stat", "HEAD~1", "HEAD"]
        ).decode('utf-8', errors='replace')
        
        # চেঞ্জলগ ফাইল তৈরি করুন
        changelog_path = output_changes / f"commit_{commit_hash}_{timestamp}.md"
        
        changelog_content = f"""# 📋 চেঞ্জলগ: {commit_hash}

## কমিট তথ্য
- **হ্যাশ:** {commit_hash}
- **লেখক:** {commit_author}
- **সময়:** {datetime.now().isoformat()}
- **বার্তা:** {commit_msg}

## ফাইল পরিবর্তন
```
{stats}
```

## ডিটেইল পার্থক্য

```diff
{diff_output}
```

---
*এই রিপোর্ট স্বয়ংক্রিয়ভাবে তৈরি করা হয়েছে*
"""
        
        changelog_path.write_text(changelog_content)
        print(f"✅ চেঞ্জলগ সংরক্ষিত: {changelog_path.relative_to('.')}")
        
    except subprocess.CalledProcessError:
        print("ℹ️ প্রথম কমিট বা তুলনার জন্য কোনো পূর্ববর্তী কমিট নেই")
    except Exception as e:
        print(f"⚠️ চেঞ্জলগ জেনারেশন ব্যর্থ: {e}")

def generate_index():
    """ইন্ডেক্স ফাইল তৈরি করুন দ্রুত নেভিগেশনের জন্য"""
    docs_dir = Path("docs")
    codebase_dir = docs_dir / "codebase"
    changes_dir = docs_dir / "changes"
    
    print(f"📑 ইন্ডেক্স ফাইল তৈরি করছি...")
    
    index_content = """# 📚 SupremeAI ডকুমেন্টেশন ইন্ডেক্স

## মডুলার কোডবেস

এই ফোল্ডারটি আপনার সম্পূর্ণ প্রজেক্টের মডুলার ডকুমেন্টেশন রয়েছে।
প্রতিটি ফাইল আলাদা `.md` ফাইলে সংরক্ষিত, যা AI এজেন্ট এবং অডিটরদের জন্য দ্রুত অ্যাক্সেসের অনুমতি দেয়।

### দ্রুত অ্যাক্সেস:

- **Backend Code** — `backend_*` ফাইলগুলো ডাউনলোড করুন
- **Frontend Code** — `apps_*` ফাইলগুলো দেখুন
- **Configuration** — `.yml`, `.json`, `.toml` ফাইলগুলো খুঁজুন
- **Infrastructure** — `infrastructure_*`, `Dockerfile` ফাইলগুলো যাচাই করুন

---

## চেঞ্জলগ এবং অডিট ট্রেইল

প্রতিটি পুশ বা কমিটের জন্য একটি বিস্তারিত চেঞ্জলগ স্বয়ংক্রিয়ভাবে তৈরি হয়।

### সুবিধা:

✅ **ট্র্যাকিং:** প্রতিটি কমিটে কী পরিবর্তন হয়েছে তা দেখুন
✅ **অডিট:** অডিটররা সহজেই পরিবর্তনগুলো পর্যালোচনা করতে পারে  
✅ **CI/CD ট্রিগার:** স্বয়ংক্রিয়ভাবে GitHub Actions জব ট্রিগার করে

---

## AI Agent Context

AI এজেন্টরা `docs/codebase/` থেকে নির্দিষ্ট মডুলার ফাইল রিড করে:
- ৮০% টোকেন সঞ্চয়
- দ্রুত সমস্যা সমাধান
- আরও সঠিক প্রেক্ষাপট

---

*স্বয়ংক্রিয়ভাবে তৈরি —* `scripts/generate_smart_docs.py`
"""
    
    if codebase_dir.exists() and changes_dir.exists():
        # ফাইল গণনা করুন
        codebase_files = list(codebase_dir.glob("*.md"))
        changes_files = list(changes_dir.glob("*.md"))
        
        index_content += f"\n## পরিসংখ্যান\n\n"
        index_content += f"- **মডুলার কোড ফাইল:** {len(codebase_files)}\n"
        index_content += f"- **চেঞ্জলগ এন্ট্রি:** {len(changes_files)}\n"
        index_content += f"- **শেষ আপডেট:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n"
    
    (docs_dir / "INDEX.md").write_text(index_content)
    print(f"✅ ইন্ডেক্স তৈরি হয়েছে: docs/INDEX.md")

def main():
    """মেইন এক্সিকিউশন"""
    print("🚀 SupremeAI স্মার্ট ডকুমেন্টেশন জেনারেটর শুরু হচ্ছে...\n")
    
    try:
        # ১. মডুলার কোডবেস জেনারেট করুন
        generate_modular_docs()
        
        # ২. চেঞ্জলগ জেনারেট করুন
        generate_changelog()
        
        # ৩. ইন্ডেক্স তৈরি করুন
        generate_index()
        
        print("\n✅ সমস্ত ডকুমেন্টেশন সফলভাবে জেনারেট হয়েছে!")
        print("📂 সামগ্রী:\n   - docs/codebase/ — মডুলার কোড\n   - docs/changes/ — চেঞ্জলগ\n   - docs/INDEX.md — দ্রুত রেফারেন্স")
        
    except Exception as e:
        print(f"\n❌ ত্রুটি: {e}")
        exit(1)

if __name__ == "__main__":
    main()

```