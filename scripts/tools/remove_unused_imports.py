#!/usr/bin/env python3
"""
Remove unused Java non-static imports. Skips wildcard and java.lang imports conservatively.
Writes in-place. Prints summary.
"""

import re, glob, os, sys
from collections import defaultdict

PROJECT_DIR = sys.argv[1] if len(sys.argv) > 1 else "/home/nazifarabbu/supremeai"
DRY = "--dry-run" in sys.argv


def build_member_index():
    """pkg → frozenset(simple-names) from project import FQNs (excluding wildcards)."""
    by_pkg = defaultdict(set)
    for fp in glob.glob(os.path.join(PROJECT_DIR, "src/**/*.java"), recursive=True):
        try:
            with open(fp, "r", encoding="utf-8", errors="replace") as f:
                content = f.read()
        except Exception:
            continue
        for m in re.finditer(r"^\s*import\s+([\w.]+);\s*$", content, re.MULTILINE):
            fqn = m.group(1)
            if fqn.endswith(".*"):
                continue
            parts = fqn.split(".")
            if len(parts) < 2:
                continue
            by_pkg[".".join(parts[:-1])].add(parts[-1])
    return {p: frozenset(v) for p, v in by_pkg.items()}


def collect_unused(fp, member_idx, dry=False):
    """Return (should_remove_set, would-be-removed-list) for fp."""
    with open(fp, "r", encoding="utf-8", errors="replace") as f:
        lines = f.readlines()

    # Lines that are imports (only non-static single-type imports)
    import_set = set()
    for i, line in enumerate(lines):
        line_stripped = line.strip()
        # Only single-type non-static imports: import pkg.Type;
        if (
            re.match(r"^import\s+(?:static\s+)?[\w.]+\s*;\s*$", line_stripped)
            and ".*" not in line_stripped
        ):
            import_set.add(i)
    if not import_set:
        return set(), []

    bt = "".join(
        lines[i]
        for i in range(len(lines))
        if i not in import_set and not lines[i].strip().startswith("package ")
    )

    # body_words is used only for wildcard checks
    bw = set(re.findall(r"[A-Za-z_][\w]*", bt))

    remove_set = set()
    for i in import_set:
        line = lines[i].rstrip("\n")
        m = re.match(r"^\s*import\s+([\w.]+);", line)
        if not m:
            continue  # static import – skip for now
        fqn = m.group(1)

        if fqn.startswith("java.lang."):
            continue

        if fqn.endswith(".*"):
            pkg = fqn[:-2]
            known = member_idx.get(pkg, frozenset())
            used = any(c in bw for c in known) or (pkg + "." in bt)
            if not used:
                remove_set.add(i)
        else:
            simple = fqn.split(".")[-1]
            pat = r"(?<![.\w])" + re.escape(simple) + r"(?![.\w])"
            if not re.search(pat, bt):
                remove_set.add(i)

    removed_lines = [(i + 1, lines[i].strip()) for i in sorted(remove_set)]
    return remove_set, removed_lines


def main():
    os.chdir(PROJECT_DIR)
    member_idx = build_member_index()
    nt = sum(len(v) for v in member_idx.values())
    print(f"Indexed {len(member_idx)} packages / {nt} types.\n")

    java_files = sorted(
        set(
            glob.glob("src/**/*.java", recursive=True)
            + glob.glob("src/*.java", recursive=False)
        )
    )

    ne = 0  # number of files fixed
    nr = 0  # number of lines removed
    results = []
    for fp in java_files:
        nidxs, removed = collect_unused(fp, member_idx, dry=DRY)
        if not nidxs:
            continue
        if not DRY:
            with open(fp, "r", encoding="utf-8") as f:
                cur = f.readlines()
            for i in sorted(nidxs, reverse=True):
                del cur[i]
            with open(fp, "w", encoding="utf-8") as f:
                f.writelines(cur)
            ne += 1
            nr += len(nidxs)
        results.append((os.path.relpath(fp, PROJECT_DIR), removed))

    print(
        f"=== [{ 'DRY RUN' if DRY else 'APPLIED' }] {ne} files, {nr} unused import lines removed ===\n"
    )
    for rel, removed in results[:120]:
        for ln, line in removed:
            print(f"  {rel}:{ln}  {line.strip('import ').strip(';')}")
    if len(results) > 120:
        extra = sum(len(r) for _, r in results[120:])
        print(f"  … {len(results)-120} more files, {extra} more lines")


if __name__ == "__main__":
    main()
