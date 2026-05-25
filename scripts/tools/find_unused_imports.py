#!/usr/bin/env python3
"""Detect and print all unused imports across the project, readably."""
import re, glob, os

PROJECT_DIR = "/home/nazifarabbu/supremeai"

by_pkg = {}          # pkg → {simple names}
tp  = {}
for fp in glob.glob(os.path.join(PROJECT_DIR, "src/**/*.java"), recursive=True):
    try:
        with open(fp, 'r', encoding='utf-8', errors='replace') as f:
            content = f.read()
    except Exception:
        continue
    for m in re.finditer(r'^\s*import\s+([\w.]+);\s*$', content, re.MULTILINE):
        fqn = m.group(1)
        if fqn.endswith('.*'):
            continue
        parts = fqn.split('.')
        if len(parts) < 2:
            continue
        pkg = '.'.join(parts[:-1])
        by_pkg.setdefault(pkg, set()).add(parts[-1])

sum_total = sum(len(v) for v in by_pkg.values())
print(f"Index: {len(by_pkg)} packages, {sum_total} types\n")

imports_by_file = {}   # fp → [(i_src, simple_name, full_fqn, line_str), ...]

for fp in sorted(set(glob.glob("src/**/*.java", recursive=True))):
    try:
        with open(fp, 'r', encoding='utf-8', errors='replace') as f:
            lines = f.readlines()
    except Exception:
        continue
    
    iset = set()
    for i, line in enumerate(lines):
        s = line.strip()
        # match import pkg.Type;  (not wildcard, not static)
        if re.match(r'^import\s+[^;\n]+\;\s*$', s) and '.*' not in s and not s.startswith('import static'):
            iset.add(i)
    if not iset:
        continue
    
    bt = ''.join(
        lines[i] for i in range(len(lines))
        if i not in iset and not lines[i].strip().startswith('package ')
    )
    
    fm = []
    for i in iset:
        m = re.match(r'^import\s+([\w.]+);', lines[i].strip())
        fqn = m.group(1)
        simple = fqn.split('.')[-1]
        
        if fqn.startswith('java.lang.'):
            continue
        
        if fqn.endswith('.*'):
            pkg0  = fqn[:-2]
            known = by_pkg.get(pkg0, frozenset())
            used  = any(c in bt for c in known) or (pkg0 + '.' in bt)
            if not used:
                fm.append((i, simple, fqn, lines[i].strip()))
        else:
            pat = r'(?<![.\w])' + re.escape(simple) + r'(?![.\w])'
            if not re.search(pat, bt):
                fm.append((i, simple, fqn, lines[i].strip()))
    
    if fm:
        imports_by_file[fp] = fm

total = sum(len(v) for v in imports_by_file.values())
print(f"=== {len(imports_by_file)} files, {total} unused imports ===\n")

rel = os.path.relpath
for fp, fm_i in imports_by_file.items():
    rfp = os.path.relpath(fp, PROJECT_DIR)
    for i, simple, fqn, line in fm_i:
        print(f"  {rfp}:{i+1}  {fqn}")
