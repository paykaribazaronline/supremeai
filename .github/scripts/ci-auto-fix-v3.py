import os
import sys
import subprocess
import re
from google import genai

# ==========================================
# ⚙️ CONFIGURATION & API SETUP
# ==========================================
API_KEY = os.getenv("SUPREMEAI_API_KEY") or os.getenv("GEMINI_API_KEY")
if not API_KEY:
    print("❌ ERROR: SUPREMEAI_API_KEY or GEMINI_API_KEY is not set.")
    sys.exit(1)

client = genai.Client(api_key=API_KEY)

def run_cmd(cmd):
    """Run a shell command and return output."""
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return result.stdout, result.stderr, result.returncode

# ==========================================
# 🛡️ GUARDRAIL: INFINITE LOOP PROTECTION
# ==========================================
def check_infinite_loop():
    """Prevent AI from endlessly fixing and failing."""
    stdout, _, _ = run_cmd("git log -3 --pretty=%B")
    if stdout.count("🤖 AI Auto-Fix") >= 2:
        print("🚨 GUARDRAIL TRIGGERED: 2 consecutive AI fixes failed. Aborting to prevent infinite CI loop.")
        sys.exit(1)

# ==========================================
# 🔍 STEP 1: EXTRACT ERROR LOGS & FILE PATH
# ==========================================
def extract_errors():
    print("🔍 Extracting failed test logs...")
    # Run pytest on last failed tests only to get a clean error log
    stdout, stderr, _ = run_cmd("poetry run pytest --lf -q --tb=short")
    
    if "no tests ran" in stdout.lower() or "passed" in stdout.lower() and "failed" not in stdout.lower():
        print("✅ No failing tests found. Exiting Auto-Fix.")
        sys.exit(0)
        
    # বাংলা মন্তব্য: এরর লগ থেকে ভুল হওয়া ফাইলের নাম ও পাথ খুঁজে বের করার চেষ্টা করা হচ্ছে
    from pathlib import Path
    failing_file = None
    matches = re.findall(r'([a-zA-Z0-9_\-/]+\.py)', stdout)
    existing_files = []
    for m in matches:
        p = Path(m)
        if p.exists() and p.is_file():
            existing_files.append(m)
        elif (Path("backend") / m).exists() and (Path("backend") / m).is_file():
            existing_files.append(f"backend/{m}")

    # টেস্ট ফাইল বাদ দিয়ে সোর্স ফাইলকে অগ্রাধিকার দেওয়া হচ্ছে
    source_files = [f for f in existing_files if "test_" not in f and "/tests/" not in f]
    if source_files:
        failing_file = source_files[0]
    elif existing_files:
        failing_file = existing_files[0]

    if failing_file:
        print(f"🎯 Identified potential failing file: {failing_file}")
    else:
        print("⚠️ Could not identify specific failing file from logs.")

    return stdout, failing_file

# ==========================================
# 🧠 STEP 2: CALL AI FOR THE FIX
# ==========================================
def get_ai_fix(error_log, file_path=None):
    print("🧠 Analyzing error and generating fix via SupremeAI...")
    
    # বাংলা মন্তব্য: ভুল হওয়া ফাইলের মূল সোর্স কোডটি AI প্রম্পটের সাথে যুক্ত করা হচ্ছে যাতে সঠিক সমাধান পাওয়া যায়
    file_context = ""
    if file_path and os.path.exists(file_path):
        try:
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            file_context = f"\nORIGINAL FILE CONTENT ({file_path}):\n```python\n{content}\n```\n"
        except Exception as e:
            print(f"⚠️ Could not read source file {file_path}: {e}")
            
    prompt = f"""
    You are an expert Senior Python Developer. The CI pipeline just failed.
    Analyze the following Pytest error log and original file content. 
    
    ERROR LOG:
    {error_log}
    {file_context}
    
    Provide the fixed complete code for the file that caused the error. 
    Output ONLY valid Python code inside a markdown block. Do not include explanations.
    Add a comment at the top containing the exact file path like this:
    # FILE_PATH: {file_path or 'backend/core/example.py'}
    """
    
    response = client.models.generate_content(model='gemini-2.5-flash', contents=prompt)
    return response.text

# ==========================================
# 🔧 STEP 3: APPLY & VALIDATE FIX
# ==========================================
def apply_and_validate_fix(ai_response):
    # Extract file path
    path_match = re.search(r'# FILE_PATH:\s*(\S+)', ai_response)
    if not path_match:
        print("❌ ERROR: AI did not provide a valid FILE_PATH.")
        sys.exit(1)
        
    file_path = path_match.group(1).strip()
    
    import os
    base_path = os.getcwd()
    if 'backend' in base_path:
        file_path = file_path.replace('backend/', '')

    # Extract code
    code_match = re.search(r'```python\n(.*?)\n```', ai_response, re.DOTALL)
    if not code_match:
        print("❌ ERROR: AI did not return a valid python code block.")
        sys.exit(1)
        
    new_code = code_match.group(1).strip()
    
    # Save the file
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(new_code)
    print(f"✅ Fix applied to {file_path}")
    
    # Validate Syntax
    print("🛡️ Validating syntax of the fixed file...")
    _, stderr, code = run_cmd(f"python -m py_compile {file_path}")
    if code != 0:
        print(f"🚨 SYNTAX ERROR in AI generated code:\n{stderr}")
        sys.exit(1)
        
    return file_path

# ==========================================
# 🚀 STEP 4: COMMIT AND PUSH
# ==========================================
def push_fix_to_repo(file_path):
    print("🚀 Pushing fix to GitHub...")
    run_cmd('git config --global user.name "SupremeAI Bot"')
    run_cmd('git config --global user.email "bot@supremeai.dev"')
    
    run_cmd(f"git add {file_path}")
    
    # Not using [skip ci] so the pipeline runs again to verify the fix!
    commit_msg = f"fix(ai): 🤖 AI Auto-Fix applied for CI failure in {os.path.basename(file_path)}"
    run_cmd(f'git commit -m "{commit_msg}"')
    
    # Push back to the current branch
    _, stderr, code = run_cmd("git push")
    if code != 0:
        print(f"❌ ERROR: Failed to push to GitHub. {stderr}")
        sys.exit(1)
        
    print(f"🎉 Auto-Fix successfully pushed! The CI pipeline will restart.")

if __name__ == "__main__":
    print("========================================")
    print("   🤖 SUPREME-AI AUTO-FIX ENGINE v3.0  ")
    print("========================================")
    
    check_infinite_loop()
    error_logs, failing_file = extract_errors()
    ai_response = get_ai_fix(error_logs, failing_file)
    fixed_file = apply_and_validate_fix(ai_response)
    push_fix_to_repo(fixed_file)
