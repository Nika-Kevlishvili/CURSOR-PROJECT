#!/usr/bin/env python3
"""Build an unused G2M billing-by-scales batch body and POST it to Dev2.

Does not store secrets. Token is read from EnergoTS fixtures/token.json at runtime.
Used-identifier dumps are merged from agent-tools JSON pages.
"""
from __future__ import annotations

import argparse
import json
import ssl
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

USED_DUMPS = [
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/a9998c5d-1837-40cb-8cbf-19263371c26c.txt"
    ),
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/b60f6d57-0088-45c1-93a6-75e157e94c16.txt"
    ),
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/d591af1a-89b5-4a44-bb20-e170dc7875b9.txt"
    ),
    Path(
        "/Users/lukachrikishvili/.cursor/projects/Users-lukachrikishvili-Desktop-CURSOR-PROJECT/agent-tools/dc557d8a-d99a-4abd-9821-9477fcdcb097.txt"
    ),
]
BATCH_DIR = Path(__file__).resolve().parent.parent / "500k billing-by-scales"
TOKEN_PATH = Path(
    "/Users/lukachrikishvili/Desktop/CURSOR-PROJECT/Cursor-Project/EnergoTS/fixtures/token.json"
)
URL = "https://devapps.energo-pro.bg/backend/phoenix2-dev/billing-by-scales/batch"
OUT_DIR = Path("/tmp/bds-limit-probes")
CTX = ssl._create_unverified_context()


def load_used() -> set[str]:
    used: set[str] = set()
    for path in USED_DUMPS:
        if not path.exists():
            continue
        rows = json.loads(path.read_text())
        for row in rows:
            ident = row.get("identifier") if isinstance(row, dict) else None
            if ident:
                used.add(str(ident))
    extra = Path("/tmp/bds-limit-extra-used.txt")
    if extra.exists():
        used.update(line.strip() for line in extra.read_text().splitlines() if line.strip())
    return used


def pick_requests(need: int, used: set[str]) -> list[dict]:
    picked: list[dict] = []
    # Later parts were never used in the earlier 10k/probe work.
    files = sorted(BATCH_DIR.glob("billing-by-scales-batch-dev2-part-*-of-050.json"))
    files = [f for f in files if "-part-0" in f.name and int(f.name.split("part-")[1][:3]) >= 20]
    for path in files:
        data = json.loads(path.read_text())
        for row in data.get("requests") or []:
            ident = str(row.get("identifier") or "")
            if not ident or ident in used:
                continue
            picked.append(row)
            used.add(ident)
            if len(picked) >= need:
                return picked
    raise SystemExit(f"only found {len(picked)} unused requests, need {need}")


def post(body: bytes, timeout: int) -> tuple[int, bytes, float]:
    token = json.loads(TOKEN_PATH.read_text())["token"]
    req = urllib.request.Request(
        URL,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "*/*",
        },
    )
    started = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout, context=CTX) as resp:
            return resp.status, resp.read(), time.time() - started
    except urllib.error.HTTPError as err:
        return err.code, err.read(), time.time() - started


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("size", type=int)
    parser.add_argument("--timeout", type=int, default=180)
    args = parser.parse_args()
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    used = load_used()
    print(f"used_identifiers={len(used)}", flush=True)
    requests = pick_requests(args.size, used)
    body_obj = {"requests": requests}
    body = json.dumps(body_obj, separators=(",", ":")).encode()
    payload_path = OUT_DIR / f"n{args.size}.json"
    payload_path.write_bytes(body)
    extra = Path("/tmp/bds-limit-extra-used.txt")
    with extra.open("a") as fh:
        for row in requests:
            fh.write(str(row["identifier"]) + "\n")

    print(
        f"posting size={args.size} upload_bytes={len(body)} file={payload_path}",
        flush=True,
    )
    status, raw, elapsed = post(body, args.timeout)
    resp_path = OUT_DIR / f"n{args.size}-resp.json"
    resp_path.write_bytes(raw)
    head = raw[:400].decode("utf-8", "replace")
    print(f"http_code={status} accept_s={elapsed:.3f} body={head}", flush=True)

    summary = {
        "size": args.size,
        "http_code": status,
        "accept_s": elapsed,
        "upload_bytes": len(body),
        "identifiers": [row["identifier"] for row in requests],
    }
    try:
        parsed = json.loads(raw.decode("utf-8"))
        summary["accepted"] = parsed.get("accepted")
        summary["stateIds"] = parsed.get("stateIds")
    except json.JSONDecodeError:
        parsed = None
    (OUT_DIR / f"n{args.size}-summary.json").write_text(json.dumps(summary))
    if status >= 500:
        return 2
    if status != 202:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
