"""Run PreProd invoice cancellation in small API chunks.

Does not use the UI. POST /invoice-cancellation with numeric invoice numbers only.
"""
from __future__ import annotations

import json
import ssl
import time
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

HERE = Path(__file__).resolve().parent
ENV_PATH = Path(r"C:\Users\N.kevlishvili\Cursor\Cursor-Project\.env")
LOGIN = "https://testapps.energo-pro.bg/backend/portal/rest/v2/login"
API = "http://10.236.20.31:7091"
SOURCE = HERE / "invoice-cancellation-parts" / "InvoiceCancellation-preprod-billing-1119-all.xlsx"
LOG = HERE / "invoice-cancellation-parts" / "run-log.jsonl"
TEMPLATE_ID = 1030
TAX_EVENT_DATE = "2026-09-08"
CHUNK = 25
MAX_IN_FLIGHT = 3
CREATE_TIMEOUT = 90
POLL_SEC = 4


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def login(env: dict[str, str]) -> str:
    body = json.dumps({"user": env["PORTAL_USER"], "password": env["PASSWORD"]}).encode()
    req = urllib.request.Request(LOGIN, data=body, headers={"Content-Type": "application/json"})
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, timeout=45, context=ctx) as resp:
        token = json.load(resp)["jwt"]
    if not token:
        raise SystemExit("login failed")
    return token


def get_json(url: str, token: str) -> tuple[int, dict | str]:
    req = urllib.request.Request(
        url,
        headers={"Authorization": f"Bearer {token}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, json.load(resp)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")


def process_status(token: str, process_id: int) -> str:
    status, body = get_json(f"{API}/process/{process_id}", token)
    if status != 200 or not isinstance(body, dict):
        return "UNKNOWN"
    return str(body.get("status") or "")


def post_json(url: str, token: str, payload: dict) -> tuple[int, str]:
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        url,
        data=data,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=CREATE_TIMEOUT) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")


def load_numbers() -> list[str]:
    from openpyxl import load_workbook

    wb = load_workbook(SOURCE, read_only=True)
    numbers: list[str] = []
    seen: set[str] = set()
    for row in wb.active.iter_rows(min_row=2, values_only=True):
        raw = str(row[0]).strip() if row[0] is not None else ""
        if not raw or raw in seen:
            continue
        if "-" in raw:
            raw = raw.rsplit("-", 1)[-1]
        seen.add(raw)
        numbers.append(raw)
    wb.close()
    return numbers


def chunks(items: list[str], size: int) -> list[list[str]]:
    return [items[i : i + size] for i in range(0, len(items), size)]


def log_line(obj: dict) -> None:
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with LOG.open("a", encoding="utf-8") as f:
        f.write(json.dumps(obj, ensure_ascii=False) + "\n")


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("--smoke", action="store_true")
    parser.add_argument("--limit", type=int, default=0, help="Max chunks to send (0=all)")
    parser.add_argument("--skip", type=int, default=0, help="Skip first N invoice numbers")
    parser.add_argument("--inflight", type=int, default=6, help="Max parallel cancellation processes")
    parser.add_argument("--from-file", dest="from_file", help="Text file with one invoice number per line")
    parser.add_argument("--stuck-minutes", type=int, default=15, help="Stop waiting on IN_PROGRESS after N minutes")
    args = parser.parse_args()
    inflight_limit = max(1, args.inflight)
    stuck_sec = max(60, args.stuck_minutes * 60)

    env = load_env()
    token = login(env)
    print("login_ok")
    if args.from_file:
        numbers = [
            ln.strip()
            for ln in Path(args.from_file).read_text(encoding="utf-8").splitlines()
            if ln.strip()
        ]
    else:
        numbers = load_numbers()
        if not args.smoke:
            numbers = [n for n in numbers if n != "1100001678"]
    if args.skip:
        numbers = numbers[args.skip :]
    print("numbers", len(numbers))
    if args.smoke:
        numbers = numbers[:1]
        groups = [numbers]
    else:
        groups = chunks(numbers, CHUNK)
        if args.limit:
            groups = groups[: args.limit]

    print("chunks", len(groups), "chunk_size", 1 if args.smoke else CHUNK, "inflight", inflight_limit)

    url = f"{API}/invoice-cancellation"
    token_at = time.time()

    def ensure_token() -> str:
        nonlocal token, token_at
        if time.time() - token_at > 25 * 60:
            token = login(env)
            token_at = time.time()
            print("token_refresh")
        return token

    def send(idx: int, group: list[str]) -> dict:
        tok = ensure_token()
        payload = {
            "invoices": ",".join(group),
            "fileId": None,
            "taxEventDate": TAX_EVENT_DATE,
            "templateId": TEMPLATE_ID,
        }
        status, body = post_json(url, tok, payload)
        process_id = None
        try:
            parsed = json.loads(body)
            process_id = parsed.get("processId")
        except Exception:
            parsed = None
        result = {
            "ts": datetime.now().isoformat(timespec="seconds"),
            "chunk": idx,
            "count": len(group),
            "first": group[0],
            "last": group[-1],
            "http": status,
            "processId": process_id,
            "body": body[:500],
        }
        log_line(result)
        print(f"chunk={idx} n={len(group)} http={status} process={process_id} body={body[:160]}")
        return result

    if args.smoke:
        send(0, groups[0])
        return

    inflight: list[int] = []
    seen_at: dict[int, float] = {}
    ok = 0
    fail = 0
    for i, group in enumerate(groups):
        while True:
            tok = ensure_token()
            still = []
            now = time.time()
            for pid in inflight:
                st = process_status(tok, pid)
                if st in {"COMPLETED", "COMPLETED_WITH_ERRORS", "ERROR", "CANCELED", "CANCELLED"}:
                    print(f"process {pid} -> {st}")
                    seen_at.pop(pid, None)
                elif st in {"UNKNOWN"}:
                    still.append(pid)
                else:
                    seen_at.setdefault(pid, now)
                    if now - seen_at[pid] >= stuck_sec:
                        print(f"process {pid} stuck {st} >{args.stuck_minutes}m, continue")
                        seen_at.pop(pid, None)
                    else:
                        still.append(pid)
            inflight = still
            if len(inflight) < inflight_limit:
                break
            time.sleep(POLL_SEC)

        result = send(i, group)
        if 200 <= result["http"] < 300 and result.get("processId"):
            ok += 1
            inflight.append(int(result["processId"]))
        else:
            fail += 1

    print(f"submitted ok={ok} fail={fail} waiting_inflight={inflight}")
    while inflight:
        tok = ensure_token()
        still = []
        now = time.time()
        for pid in inflight:
            st = process_status(tok, pid)
            print(f"process {pid} -> {st}")
            if st in {"COMPLETED", "COMPLETED_WITH_ERRORS", "ERROR", "CANCELED", "CANCELLED"}:
                seen_at.pop(pid, None)
                continue
            seen_at.setdefault(pid, now)
            if now - seen_at[pid] >= stuck_sec:
                print(f"process {pid} stuck {st} >{args.stuck_minutes}m, continue")
                seen_at.pop(pid, None)
            else:
                still.append(pid)
        inflight = still
        if inflight:
            time.sleep(POLL_SEC)
    print(f"done ok={ok} fail={fail} log={LOG}")


if __name__ == "__main__":
    main()
