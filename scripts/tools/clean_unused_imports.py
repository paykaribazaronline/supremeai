#!/usr/bin/env python3
"""
Unused-import scanner/cleaner. Run from project root with:
  python3 scripts/remove_unused_imports.py           # apply changes
  python3 scripts/remove_unused_imports.py --dry       # preview only
"""

import re, glob, os, sys
from collections import defaultdict

PROJECT_DIR = os.path.abspath(
    sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith("--") else "."
)
DRY = "--dry" in sys.argv

os.chdir(PROJECT_DIR)

# ── 1. Build member index: pkg → simple-class-names from all project imports ──
_by_pkg = defaultdict(set)
for fp in glob.glob("src/**/*.java", recursive=True):
    try:
        for m in re.finditer(
            r"^\s*import\s+([\w.]+);\s*$", open(fp).read(), re.MULTILINE
        ):
            fqn = m.group(1)
            if fqn.endswith(".*"):
                continue
            parts = fqn.split(".")
            if len(parts) < 2:
                continue
            _by_pkg[".".join(parts[:-1])].add(parts[-1])
    except Exception:
        pass
WC_INDEX = {pkg: frozenset(v) for pkg, v in _by_pkg.items()}


# ── 2. Scan and apply ─────────────────────────────────────────────────────────
def scan(fp):
    lines = open(fp, encoding="utf-8", errors="replace").readlines()

    # Find candidate single-type imports: non-wildcard, non-static, non-java.lang
    ib = set()
    for i, line in enumerate(lines):
        s = line.strip()
        if not s.startswith("import "):
            continue
        if s.startswith("import static"):
            continue
        if ".*" in s:
            continue
        if re.match(r"^import\s+[\w.]+\;\s*$", s):
            ib.add(i)
    if not ib:
        return set()

    # Body text: everything except MyImports and package declaration
    bt = "".join(
        lines[j]
        for j in range(len(lines))
        if j not in ib and not lines[j].strip().startswith("package ")
    )
    bw = set(re.findall(r"[A-Za-z_][\w]*", bt))

    remove = set()
    for i in ib:
        m = re.match(r"^import\s+([\w.]+);", lines[i].strip())
        fqn = m.group(1)
        if fqn is None:
            continue
        simple = fqn.split(".")[-1]

        if fqn.startswith("java.lang."):
            continue

        if fqn.endswith(".*"):
            pkg = fqn[:-2]
            known = WC_INDEX.get(pkg, frozenset())
            used = any(c in bw for c in known) or (pkg + "." in bt)
            if not used:
                remove.add(i)
        else:
            # Check if simple type name appears used in body:
            #  - preceded by non-word/non-digit, followed by space/paren/brace/etc. (type usage: Map<String>, flush())
            #  - or anywhere in body as a standalone word (annotation @X, catch clause, etc.)
            pat1 = re.compile(
                r"(?<![.\w\d])"
                + re.escape(simple)
                + r"(?=[\s\(\)\{\}\,\;\<\>\/\.\:]|$)"
            )
            pat2 = re.compile(r"(?<!\w)" + re.escape(simple) + r"(?!\w)")
            if not pat1.search(bt) and not pat2.search(bt):
                remove.add(i)

    return remove


all_fixes = []
for fp in sorted(
    set(
        glob.glob("src/**/*.java", recursive=True)
        + glob.glob("src/*.java", recursive=False)
    )
):
    idxs = scan(fp)
    if idxs:
        with open(fp) as f:
            flines = f.readlines()
        all_fixes.append(
            (
                os.path.relpath(fp, PROJECT_DIR),
                idxs,
                [(i + 1, flines[i].strip()) for i in sorted(idxs)],
            )
        )

total = sum(len(r) for _, _, r in all_fixes)
if DRY:
    print(
        f"[DRY-RUN] {len(all_fixes)} files, {total} unused import lines would be removed:\n"
    )
    for rel, _, rm in all_fixes[:100]:
        for ln, line in rm:
            fq = re.match(r"^import\s+([\w.]+);", line)
            print(f"  {rel}:{ln}  {fq.group(1) if fq else line}")
    if len(all_fixes) > 100:
        extra = sum(len(r) for _, _, r in all_fixes[100:])
        print(f"  ... +{len(all_fixes)-100} more files, {extra} more lines")
    print(f"\nTotal: {total} lines.")
else:
    applied = 0
    for rel, idxs, _ in all_fixes:
        fp2 = os.path.join(PROJECT_DIR, rel)
        cur = open(fp2).readlines()
        new = list(cur)
        for i in sorted(idxs, reverse=True):
            del new[i]
        open(fp2, "w").writelines(new)
        applied += len(idxs)
    print(f"[APPLIED] {len(all_fixes)} files fixed, {applied} unused imports removed.")
