import re, glob
from collections import defaultdict, Counter

pkg_map = defaultdict(set)
for fp in sorted(glob.glob('src/**/*.java', recursive=True)):
    try:
        for m in re.finditer(r'^\s*import\s+([\w.]+);\s*$', open(fp).read(), re.MULTILINE):
            fqn = m.group(1)
            if fqn.endswith('.*'): continue
            parts = fqn.split('.')
            if len(parts) < 2: continue
            pkg_map['.'.join(parts[:-1])].add(parts[-1])
    except Exception:
        pass

print(f"Indexed {len(pkg_map)} packages:\n")
for pkg in sorted(pkg_map.keys()):
    members = sorted(pkg_map[pkg])
    print(f"  {pkg:70s} ({len(members)} types)")
    for name in members[:5]:
        print(f"    {name}")

# Also show some "likely wildcard-cut" packages with >1 types
from collections import Counter
ctr = Counter((fp,len(v)) for fp,v in pkg_map.items() for _ in pkg_map[fp])
print(f"\nPackages with many types (potential wildcard-holdovers):")
for pkg in sorted(pkg_map.keys(), key=lambda p: -len(pkg_map[p])):
    if len(pkg_map[pkg]) >= 3:
        print(f"  {pkg}: {len(pkg_map[pkg])} → {sorted(pkg_map[pkg])[:10]}")
