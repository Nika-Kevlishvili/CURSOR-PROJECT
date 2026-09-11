"""One-shot PreProd invoice cancellation via Excel upload (remaining REAL invoices)."""
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
NUMBERS = HERE / "invoice-cancellation-parts" / "remaining-real-one-shot.txt"
XLSX = HERE / "invoice-cancellation-parts" / "InvoiceCancellation-preprod-billing-1119-remaining.xlsx"
LOG = HERE / "invoice-cancellation-parts" / "run-all-log.jsonl"
TEMPLATE_ID = 1030
TAX_EVENT_DATE = "2026-09-08"
CREATE_TIMEOUT = 600


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


def log_line(obj: dict) -> None:
    LOG.parent.mkdir(parents=True, exist_ok=True)
    with LOG.open("a", encoding="utf-8") as f:
        f.write(json.dumps(obj, ensure_ascii=False) + "\n")


def load_remaining() -> list[str]:
    numbers = [ln.strip() for ln in NUMBERS.read_text(encoding="utf-8").splitlines() if ln.strip()]
    seen: set[str] = set()
    out: list[str] = []
    for n in numbers:
        if n in seen:
            continue
        seen.add(n)
        out.append(n)
    return out


def write_xlsx(numbers: list[str]) -> None:
    try:
        import xlsxwriter
    except ImportError:
        raise SystemExit("xlsxwriter missing")

    wb = xlsxwriter.Workbook(str(XLSX))
    ws = wb.add_worksheet()
    text = wb.add_format({"num_format": "@"})
    ws.write_string(0, 0, "Invoice Number")
    ws.write_string(0, 1, "Invoice Date")
    for i, n in enumerate(numbers):
        ws.write_string(i + 1, 0, n, text)
    wb.close()
    print("xlsx", XLSX, "rows", len(numbers))


def upload_file(token: str) -> int:
    boundary = "----CursorInvoiceCancellation"
    data = XLSX.read_bytes()
    filename = XLSX.name
    parts = []
    parts.append(f"--{boundary}\r\n".encode())
    parts.append(
        f'Content-Disposition: form-data; name="file"; filename="{filename}"\r\n'
        "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n".encode()
    )
    parts.append(data)
    parts.append(f"\r\n--{boundary}--\r\n".encode())
    body = b"".join(parts)
    req = urllib.request.Request(
        f"{API}/invoice-cancellation/upload-file",
        data=body,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/json",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            status = resp.status
    except urllib.error.HTTPError as e:
        status = e.code
        raw = e.read().decode("utf-8", errors="replace")
        print("upload_http", status, raw[:500])
        raise SystemExit("upload failed")
    print("upload_http", status, raw[:300])
    parsed = json.loads(raw)
    file_id = parsed.get("id")
    if not file_id:
        raise SystemExit(f"no file id: {raw[:500]}")
    return int(file_id)


def create(token: str, file_id: int, count: int) -> dict:
    payload = {
        "fileId": file_id,
        "taxEventDate": TAX_EVENT_DATE,
        "templateId": TEMPLATE_ID,
    }
    data = json.dumps(payload).encode()
    req = urllib.request.Request(
        f"{API}/invoice-cancellation",
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
            status = resp.status
            raw = resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as e:
        status = e.code
        raw = e.read().decode("utf-8", errors="replace")
    print("create_http", status, raw[:500])
    log_line(
        {
            "ts": datetime.now().isoformat(timespec="seconds"),
            "http": status,
            "fileId": file_id,
            "count": count,
            "body": raw[:800],
        }
    )
    if not (200 <= status < 300):
        raise SystemExit("create failed")
    return json.loads(raw)


def main() -> None:
    numbers = load_remaining()
    print("numbers", len(numbers))
    if len(numbers) < 100:
        raise SystemExit("too few remaining numbers")
    write_xlsx(numbers)
    token = login(load_env())
    print("login_ok")
    file_id = upload_file(token)
    print("fileId", file_id)
    result = create(token, file_id, len(numbers))
    print("done", result)


if __name__ == "__main__":
    main()
