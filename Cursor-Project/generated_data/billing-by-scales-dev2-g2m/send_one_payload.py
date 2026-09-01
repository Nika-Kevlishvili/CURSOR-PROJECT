#!/usr/bin/env python3
"""Send one Dev2 billing-by-scales create (and probe /batch) for G2M0000001."""
from __future__ import annotations

import json
import os
import ssl
import urllib.error
import urllib.request
from pathlib import Path

ENV_PATH = Path("/Users/lukachrikishvili/Desktop/CURSOR-PROJECT/Cursor-Project/.env")
BASE = "https://devapps.energo-pro.bg/backend/phoenix2-dev"
CTX = ssl._create_unverified_context()
OUT = Path(__file__).resolve().parent / "one-payload.json"

PERIODS = [
    ("2026-07-01", "2026-07-06"),
    ("2026-07-07", "2026-07-12"),
    ("2026-07-13", "2026-07-18"),
    ("2026-07-19", "2026-07-24"),
    ("2026-07-25", "2026-07-31"),
]
POD = "G2M000000165J9K26TYA0L9PXQ2OXBGDQ"
METER = "G2MM0000001"


def load_env() -> None:
    for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            os.environ.setdefault(k.strip(), v.strip().strip('"').strip("'"))


def login() -> str:
    body = json.dumps(
        {"user": os.environ["PORTAL_USER"], "password": os.environ["PASSWORD"]}
    ).encode()
    req = urllib.request.Request(
        os.environ["DEVAUTHAPI"],
        data=body,
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=30, context=CTX) as resp:
        payload = json.load(resp)
    token = payload.get("jwt")
    if not token:
        raise SystemExit("LOGIN_FAILED")
    return token


def request_json(method: str, url: str, token: str, data: object | None = None) -> tuple[int, object]:
    raw = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(
        url,
        data=raw,
        method=method,
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=180, context=CTX) as resp:
            body = resp.read()
            parsed = json.loads(body.decode()) if body else None
            return resp.status, parsed
    except urllib.error.HTTPError as e:
        body = e.read().decode(errors="replace")
        try:
            parsed = json.loads(body) if body else body
        except json.JSONDecodeError:
            parsed = body[:800]
        return e.code, parsed


def build_payload() -> dict:
    rows: list[dict] = []
    index = 0
    for i, (start, end) in enumerate(PERIODS):
        old_reading = i * 10
        new_reading = (i + 1) * 10
        rows.append(
            {
                "periodFrom": start,
                "periodTo": end,
                "meterNumber": METER,
                "scaleNumber": "1",
                "scaleCode": "BIG SCALE CODE",
                "scaleType": "BIG DATA TYPE",
                "newMeterReading": new_reading,
                "oldMeterReading": old_reading,
                "difference": 10,
                "multiplier": 1,
                "totalVolumes": 10,
                "index": index,
            }
        )
        index += 1
        rows.append(
            {
                "periodFrom": start,
                "periodTo": end,
                "meterNumber": METER,
                "scaleType": "BIG",
                "tariffScale": "BIG DATA TARIFF",
                "volumes": 100,
                "unitPrice": 1,
                "totalValue": 100,
                "index": index,
            }
        )
        index += 1
    return {
        "identifier": POD,
        "dateFrom": "2026-07-01",
        "dateTo": "2026-07-31",
        "billingPowerInKw": 2,
        "invoiceNumber": "G2MM0000001",
        "invoiceDate": "2026-07-01T00:00:00.000Z",
        "invoiceCorrection": None,
        "correction": False,
        "override": False,
        "billingByScalesTableCreateRequests": rows,
        "saveRecordForIntermediatePeriod": True,
        "saveRecordForMeterReadings": True,
    }


def main() -> None:
    load_env()
    token = login()
    print("LOGIN_OK", flush=True)

    status, docs = request_json("GET", f"{BASE}/v3/api-docs", token)
    if status == 200 and isinstance(docs, dict):
        paths = sorted(k for k in docs.get("paths", {}) if "billing-by-scale" in k)
        print("LIVE_API_DOCS_PATHS", paths, flush=True)
    else:
        print("LIVE_API_DOCS", status, str(docs)[:300], flush=True)

    payload = build_payload()
    OUT.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    print(f"WROTE {OUT} rows={len(payload['billingByScalesTableCreateRequests'])}", flush=True)

    for path in ("/billing-by-scales/batch", "/billing-by-scales"):
        body = [payload] if path.endswith("/batch") else payload
        status, parsed = request_json("POST", f"{BASE}{path}", token, body)
        print(f"POST {path} http={status} body={parsed}", flush=True)


if __name__ == "__main__":
    main()
