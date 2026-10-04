"""Temporary Swagger inspection helper for EXP-PARITY-04 authoring.

Read-only: prints endpoint/operation/schema facts from the refreshed Experiment spec.
"""
import json
import re
import sys

SPEC = r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\swagger\experiment\swagger-spec.json"

with open(SPEC, encoding="utf-8") as fh:
    spec = json.load(fh)

paths = spec["paths"]
schemas = spec.get("components", {}).get("schemas", {})


def list_paths(pattern):
    rx = re.compile(pattern)
    for p in sorted(paths):
        if rx.search(p):
            print(p, sorted(k for k in paths[p] if k in ("get", "post", "put", "delete", "patch")))


def show_op(path, method):
    op = paths[path][method]
    print(f"=== {method.upper()} {path}")
    print("  operationId:", op.get("operationId"))
    for prm in op.get("parameters", []):
        print(
            "  param:",
            prm.get("name"),
            "in=" + str(prm.get("in")),
            "required=" + str(prm.get("required")),
            "schema=" + json.dumps(prm.get("schema", {}))[:200],
        )
    rb = op.get("requestBody")
    if rb:
        print("  requestBody required:", rb.get("required"))
        for ct, media in rb.get("content", {}).items():
            print("   ", ct, json.dumps(media.get("schema", {}))[:300])
    for code, resp in op.get("responses", {}).items():
        for ct, media in (resp.get("content") or {}).items():
            print("  response", code, ct, json.dumps(media.get("schema", {}))[:200])
        if not resp.get("content"):
            print("  response", code, "(no content)")


def show_schema(name, depth=0, seen=None):
    seen = seen or set()
    if name in seen:
        print("  " * depth + f"{name} (already shown)")
        return
    seen.add(name)
    sch = schemas.get(name)
    if sch is None:
        print("  " * depth + f"{name}: NOT FOUND")
        return
    print("  " * depth + f"--- {name} required={sch.get('required')}")
    for prop, pv in (sch.get("properties") or {}).items():
        ref = pv.get("$ref") or (pv.get("items", {}) or {}).get("$ref")
        print(
            "  " * (depth + 1)
            + f"{prop}: type={pv.get('type')} format={pv.get('format')} enum={pv.get('enum')} ref={ref}"
        )


def grep_schema(pattern):
    rx = re.compile(pattern, re.I)
    for name in sorted(schemas):
        if rx.search(name):
            print(name)


if __name__ == "__main__":
    cmd = sys.argv[1]
    if cmd == "paths":
        list_paths(sys.argv[2])
    elif cmd == "op":
        show_op(sys.argv[2], sys.argv[3])
    elif cmd == "schema":
        show_schema(sys.argv[2])
    elif cmd == "grepschema":
        grep_schema(sys.argv[2])
