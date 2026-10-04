"""Temporary Swagger key/enum grep helper for EXP-PARITY-04 authoring (read-only)."""
import json
import re
import sys

SPEC = r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\swagger\experiment\swagger-spec.json"

with open(SPEC, encoding="utf-8") as fh:
    spec = json.load(fh)

rx = re.compile(sys.argv[1], re.I)
hits = []


def walk(node, path):
    if isinstance(node, dict):
        for k, v in node.items():
            if rx.search(k):
                hits.append((f"{path}.{k}", json.dumps(v)[:260]))
            walk(v, f"{path}.{k}")
    elif isinstance(node, list):
        for i, v in enumerate(node):
            if isinstance(v, str) and rx.search(v):
                hits.append((f"{path}[{i}]", v))
            walk(v, f"{path}[{i}]")


walk(spec, "")
for p, v in hits[:80]:
    print(p, "=>", v)
print("TOTAL", len(hits))
