"""Cancel billing 1119 REAL invoices on Test and PreProd via invoice-cancellation import."""
from __future__ import annotations

import json
import ssl
import urllib.error
import urllib.request
from datetime import datetime
from pathlib import Path

import psycopg2
import xlsxwriter

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
ENV_PATH = ROOT / ".env"
MCP_PATH = ROOT.parent / ".cursor" / "mcp.json"
OUT_DIR = HERE / "invoice-cancellation-parts"
XLSX = OUT_DIR / "InvoiceCancellation-billing-1119-real-27203.xlsx"
NUMBERS = OUT_DIR / "billing-1119-real-suffixes-27203.txt"
LOG = OUT_DIR / "run-both-2026-10-27.jsonl"
LOGIN = "https://testapps.energo-pro.bg/backend/portal/rest/v2/login"
TEMPLATE_ID = 1030
TAX_EVENT_DATE = "2026-09-08"
BILLING_ID = 1119
APIS = [
    ("test", "http://10.236.20.31:8091"),
    ("preprod", "http://10.236.20.31:7091"),
]


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.strip().startswith("#"):
            key, value = line.split("=", 1)
            env[key.strip()] = value.strip().strip('"').strip("'")
    return env


def load_numbers() -> list[str]:
    cfg = json.loads(MCP_PATH.read_text(encoding="utf-8"))
    dsn = cfg["mcpServers"]["PostgreSQLPreProd"]["env"]["DATABASE_URL"]
    conn = psycopg2.connect(dsn, connect_timeout=20)
    conn.set_session(readonly=True, autocommit=True)
    cur = conn.cursor()
    cur.execute("SET statement_timeout = '120s'")
    cur.execute(
        """
        SELECT right(invoice_number, 10)
        FROM invoice.invoices
        WHERE billing_id = %s
          AND status::text = 'REAL'
        ORDER BY id
        """,
        (BILLING_ID,),
    )
    numbers = [row[0] for row in cur.fetchall()]
    cur.close()
    conn.close()
    if len(numbers) != len(set(numbers)):
        raise SystemExit("duplicate invoice suffixes")
    if any(len(n) != 10 or not n.isdigit() for n in numbers):
        raise SystemExit("suffix is not 10 digits")
    return numbers


def write_xlsx(numbers: list[str]) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    NUMBERS.write_text("\n".join(numbers) + "\n", encoding="utf-8")
    wb = xlsxwriter.Workbook(str(XLSX))
    ws = wb.add_worksheet("Sheet1")
    text = wb.add_format({"num_format": "@"})
    ws.write_string(0, 0, "Invoice Number")
    ws.write_string(0, 1, "Invoice Date")
    for index, number in enumerate(numbers):
        ws.write_string(index + 1, 0, number, text)
    wb.close()
    print("xlsx", XLSX.name, "rows", len(numbers), flush=True)


def login(env: dict[str, str]) -> str:
    body = json.dumps({"user": env["PORTAL_USER"], "password": env["PASSWORD"]}).encode()
    req = urllib.request.Request(LOGIN, data=body, headers={"Content-Type": "application/json"})
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, timeout=45, context=ctx) as resp:
        token = json.load(resp)["jwt"]
    if not token:
        raise SystemExit("login failed")
    return token


def request(url: str, token: str, data: bytes | None = None, content_type: str | None = None, timeout: int = 120, method: str | None = None) -> tuple[int, str]:
    headers = {"Authorization": f"Bearer {token}", "Accept": "application/json"}
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read().decode("utf-8", errors="replace")
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read().decode("utf-8", errors="replace")


def log_line(obj: dict) -> None:
    with LOG.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(obj, ensure_ascii=False) + "\n")


def upload(api: str, token: str) -> int:
    boundary = "----CursorInvoiceCancellation1119"
    payload = XLSX.read_bytes()
    body = b"".join(
        [
            f"--{boundary}\r\n".encode(),
            (
                f'Content-Disposition: form-data; name="file"; filename="{XLSX.name}"\r\n'
                "Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n\r\n"
            ).encode(),
            payload,
            f"\r\n--{boundary}--\r\n".encode(),
        ]
    )
    status, raw = request(
        f"{api}/invoice-cancellation/upload-file",
        token,
        body,
        f"multipart/form-data; boundary={boundary}",
        timeout=300,
        method="POST",
    )
    print("upload", api, status, raw[:240], flush=True)
    if not (200 <= status < 300):
        raise SystemExit(f"upload failed {api}")
    file_id = json.loads(raw).get("id")
    if not file_id:
        raise SystemExit(f"no file id {api}")
    return int(file_id)


def create(api: str, token: str, file_id: int, count: int) -> dict:
    payload = json.dumps(
        {"fileId": file_id, "taxEventDate": TAX_EVENT_DATE, "templateId": TEMPLATE_ID}
    ).encode()
    status, raw = request(
        f"{api}/invoice-cancellation",
        token,
        payload,
        "application/json",
        timeout=900,
        method="POST",
    )
    print("create", api, status, raw[:400], flush=True)
    log_line(
        {
            "ts": datetime.now().isoformat(timespec="seconds"),
            "api": api,
            "http": status,
            "fileId": file_id,
            "count": count,
            "body": raw[:800],
        }
    )
    if not (200 <= status < 300):
        raise SystemExit(f"create failed {api}")
    return json.loads(raw)


def main() -> None:
    numbers = load_numbers()
    print("numbers", len(numbers), flush=True)
    write_xlsx(numbers)
    token = login(load_env())
    print("login_ok", flush=True)
    for name, api in APIS:
        try:
            file_id = upload(api, token)
            result = create(api, token, file_id, len(numbers))
            print("started", name, result, flush=True)
        except SystemExit as exc:
            print("env_failed", name, exc, flush=True)


if __name__ == "__main__":
    main()
