"""Fetch invoice document JSON from TEST1 / End Supplier / SLR endpoints. Do not print secrets."""

from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ENV_PATH = ROOT / "Cursor-Project" / "EnergoTS" / ".env"
OUT_DIR = Path(__file__).resolve().parent

TARGETS = [
    {
        "id": "test1-epres-new-billing",
        "label": "EPRES New Billing TEST (TEST 1)",
        "base": "https://testapps.energo-pro.bg/backend/phoenix-epres",
        "direct": "http://10.236.20.31:8091",
    },
    {
        "id": "phoenix2-end-supplier",
        "label": "EPRS Phoenix 2 End Supplier",
        "base": "https://testapps.energo-pro.bg/backend/phoenix-test2",
        "direct": None,
    },
    {
        "id": "phoenix2-slr",
        "label": "EPRS Phoenix 2 Supplier of Last Resort",
        "base": "https://testapps.energo-pro.bg/backend/phoenix2-slr-test",
        "direct": "http://10.236.20.86:8091",
    },
]


def parse_env(path: Path) -> dict[str, str]:
    data: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        t = line.strip()
        if not t or t.startswith("#") or "=" not in t:
            continue
        key, val = t.split("=", 1)
        data[key.strip()] = val.strip().strip('"').strip("'")
    return data


def http_json(
    method: str,
    url: str,
    token: str | None = None,
    body: dict | None = None,
    timeout: int = 60,
) -> tuple[int, object]:
    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = None if body is None else json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read()
            payload = json.loads(raw.decode("utf-8")) if raw else None
            return resp.status, payload
    except urllib.error.HTTPError as exc:
        raw = exc.read()
        try:
            payload = json.loads(raw.decode("utf-8")) if raw else {"error": str(exc)}
        except json.JSONDecodeError:
            payload = {"error": exc.reason, "body": raw[:500].decode("utf-8", errors="replace")}
        return exc.code, payload


def login(env: dict[str, str]) -> str:
    auth_url = env.get("TESTAUTHAPI")
    user = env.get("PORTAL_USER")
    password = env.get("PASSWORD")
    if not auth_url or not user or not password:
        raise SystemExit("Missing TESTAUTHAPI / PORTAL_USER / PASSWORD in EnergoTS .env")
    print(f"Auth URL: {auth_url}")
    status, payload = http_json("POST", auth_url, body={"user": user, "password": password})
    if status != 200 or not isinstance(payload, dict) or not payload.get("jwt"):
        raise SystemExit(f"Auth failed status={status} keys={list(payload)[:8] if isinstance(payload, dict) else type(payload)}")
    print("Auth OK")
    return str(payload["jwt"])


def pick_invoice_id(listing: object) -> int | None:
    if not isinstance(listing, dict):
        return None
    rows = listing.get("content") or listing.get("data") or []
    if not isinstance(rows, list):
        return None
    preferred = []
    fallback = []
    for row in rows:
        if not isinstance(row, dict):
            continue
        raw_id = row.get("id") or row.get("invoiceId")
        try:
            invoice_id = int(raw_id)
        except (TypeError, ValueError):
            continue
        number = str(row.get("invoiceNumber") or "")
        status = str(row.get("invoiceStatus") or row.get("status") or "")
        doc_type = str(row.get("invoiceDocumentType") or row.get("documentType") or "")
        item = (invoice_id, number, status, doc_type)
        if status.upper() == "REAL" and "D" not in number.split("-")[-1][:1]:
            preferred.append(item)
        else:
            fallback.append(item)
    chosen = (preferred or fallback)
    return chosen[0][0] if chosen else None


LISTING_BODIES = [
    {
        "page": 0,
        "size": 20,
        "searchBy": "ALL",
        "documentTypes": ["INVOICE"],
        "invoiceStatuses": ["REAL"],
        "sortBy": "ID",
        "sortDirection": "DESC",
    },
    {"page": 0, "size": 20, "searchBy": "ALL", "sortBy": "ID", "sortDirection": "DESC"},
    {"page": 0, "size": 20},
]


def fetch_one(base: str, token: str) -> tuple[int | None, object, str]:
    listing_url = f"{base.rstrip('/')}/invoice/listing"
    listing = None
    status = None
    for body in LISTING_BODIES:
        status, listing = http_json("POST", listing_url, token=token, body=body)
        if status not in (200, 206):
            continue
        invoice_id = pick_invoice_id(listing)
        if invoice_id is not None:
            break
    else:
        if status not in (200, 206):
            return None, {"listingStatus": status, "listing": listing}, f"listing HTTP {status}"
        debug = {"listingStatus": status}
        if isinstance(listing, dict):
            debug["keys"] = list(listing.keys())
            debug["totalElements"] = listing.get("totalElements")
            content = listing.get("content") or []
            debug["contentLen"] = len(content) if isinstance(content, list) else type(content).__name__
            if isinstance(content, list) and content:
                row = content[0] if isinstance(content[0], dict) else {"raw": str(content[0])[:200]}
                debug["firstRowKeys"] = list(row.keys()) if isinstance(row, dict) else None
                debug["firstRowSample"] = {
                    k: row.get(k) for k in ("id", "invoiceId", "invoiceNumber", "invoiceStatus", "status")
                } if isinstance(row, dict) else row
        else:
            debug["listingType"] = type(listing).__name__
        (OUT_DIR / "_slr-listing-debug.json").write_text(json.dumps(debug, default=str, indent=2), encoding="utf-8")
        return None, listing, "no invoice id in listing"
    gen_url = f"{base.rstrip('/')}/billing-run/generate-invoice-data?invoiceId={invoice_id}"
    gen_status, document = http_json("GET", gen_url, token=token, timeout=120)
    if gen_status != 200:
        return invoice_id, {"generateStatus": gen_status, "body": document}, f"generate HTTP {gen_status}"
    return invoice_id, document, "ok"


def main() -> int:
    env = parse_env(ENV_PATH)
    token = login(env)
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    summary = []
    for target in TARGETS:
        bases = [target["base"]]
        if target.get("direct"):
            bases.append(target["direct"])
        last_note = ""
        saved = False
        used_base = None
        invoice_id = None
        for base in bases:
            print(f"Trying {target['id']} via {base}")
            invoice_id, document, note = fetch_one(base, token)
            if note == "no invoice id in listing" and target["id"] == "phoenix2-slr":
                for probe_id in (1, 2, 10, 46, 100, 1000):
                    gen_url = f"{base.rstrip('/')}/billing-run/generate-invoice-data?invoiceId={probe_id}"
                    gen_status, document = http_json("GET", gen_url, token=token, timeout=120)
                    print(f"  probe invoiceId={probe_id} HTTP {gen_status}")
                    if gen_status == 200 and isinstance(document, dict):
                        invoice_id, note = probe_id, "ok"
                        break
            last_note = note
            if note == "ok" and isinstance(document, dict):
                out = OUT_DIR / f"invoice-json-{target['id']}.json"
                wrapper = {
                    "source": {
                        "environment": target["label"],
                        "id": target["id"],
                        "baseUrl": base,
                        "endpoint": f"{base.rstrip('/')}/billing-run/generate-invoice-data?invoiceId={invoice_id}",
                        "invoiceId": invoice_id,
                    },
                    "document": document,
                }
                out.write_text(json.dumps(wrapper, ensure_ascii=False, indent=2), encoding="utf-8")
                print(f"Wrote {out} invoiceId={invoice_id}")
                saved = True
                used_base = base
                break
            print(f"  {note}")
        summary.append(
            {
                "id": target["id"],
                "ok": saved,
                "base": used_base,
                "invoiceId": invoice_id,
                "note": last_note if not saved else "ok",
            }
        )
    (OUT_DIR / "_fetch-summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(json.dumps(summary, indent=2))
    return 0 if all(item["ok"] for item in summary) else 2


if __name__ == "__main__":
    sys.exit(main())
