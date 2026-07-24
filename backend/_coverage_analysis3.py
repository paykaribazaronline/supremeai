import json
from collections import defaultdict

data = json.load(open("coverage.json"))
files = data.get("files", {})


# Strip absolute path prefix for consistency
def normalize_path(p):
    # Remove absolute prefix like C:\Users\n\supremeai\supremeai_2.0\
    p = p.replace("\\", "/")
    if ":" in p:  # Windows absolute path
        parts = p.split("/")
        # Find where the relative path starts (usually after 'supremeai_2.0')
        for i, part in enumerate(parts):
            if part == "supremeai_2.0":
                return "/".join(parts[i + 1 :])
        return p
    return p


pkg_lines = defaultdict(lambda: {"covered": 0, "total": 0})
pkg_files_list = defaultdict(list)
for fpath, finfo in files.items():
    norm = normalize_path(fpath)
    parts = norm.split("/")
    pkg = parts[0] if parts else "other"
    pkg_lines[pkg]["total"] += finfo.get("num_statements", 0)
    pkg_lines[pkg]["covered"] += finfo.get("covered_lines", 0)
    pkg_files_list[pkg].append((norm, finfo))

total_covered = 0
total_stmts = 0
for pkg in sorted(pkg_lines.keys()):
    info = pkg_lines[pkg]
    pct = (info["covered"] / info["total"] * 100) if info["total"] else 0
    total_covered += info["covered"]
    total_stmts += info["total"]


zero_coverage = []
for fpath, finfo in files.items():
    if finfo.get("num_statements", 0) > 0 and finfo["covered_lines"] == 0:
        norm = normalize_path(fpath)
        zero_coverage.append((finfo["num_statements"], norm))
zero_coverage.sort(reverse=True)
for stmts, fpath in zero_coverage[:30]:
    pass

low_coverage = []
for fpath, finfo in files.items():
    if finfo.get("num_statements", 0) > 0:
        pct = finfo["covered_lines"] / finfo["num_statements"] * 100
        if pct < 20:
            norm = normalize_path(fpath)
            low_coverage.append(
                (pct, finfo["num_statements"], finfo["covered_lines"], norm)
            )
low_coverage.sort(key=lambda x: (-x[1], x[0]))  # largest first, then lowest %
for pct, stmts, covered, fpath in low_coverage[:30]:
    pass

needs_work = []
for fpath, finfo in files.items():
    if finfo.get("num_statements", 0) > 0:
        pct = finfo["covered_lines"] / finfo["num_statements"] * 100
        if pct < 50:
            norm = normalize_path(fpath)
            missing = finfo["num_statements"] - finfo["covered_lines"]
            needs_work.append(
                (missing, pct, finfo["num_statements"], finfo["covered_lines"], norm)
            )
needs_work.sort(reverse=True)
for missing, pct, stmts, covered, fpath in needs_work[:30]:
    pass
