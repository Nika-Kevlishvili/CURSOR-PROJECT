"""Download official PODS template and reprint header for col 52; rebuild+upload CREATE/EDIT."""
import json
import random
import string
import urllib.request
import uuid
from pathlib import Path

import xlsxwriter
from openpyxl import load_workbook

BASE = Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\templates\pod-mass-import")
TOKEN = json.loads(
    Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\EnergoTS\fixtures\token.json").read_text(
        encoding="utf-8"
    )
)["token"]
API = "http://10.236.20.11:8091"
BASE.mkdir(parents=True, exist_ok=True)


def download_template() -> Path:
    out = BASE / "pods_official_template_dev.xlsx"
    req = urllib.request.Request(f"{API}/mass-import/PODS/template/download", method="GET")
    req.add_header("Authorization", f"Bearer {TOKEN}")
    with urllib.request.urlopen(req, timeout=60) as resp:
        out.write_bytes(resp.read())
    return out


def headers_from(path: Path) -> list:
    wb = load_workbook(path, read_only=True)
    ws = wb.active
    headers = [ws.cell(1, c).value for c in range(1, (ws.max_column or 53) + 1)]
    wb.close()
    while headers and headers[-1] is None:
        headers.pop()
    return headers


def write_xlsx(path: Path, headers: list, data_row: dict):
    wb = xlsxwriter.Workbook(str(path), {"strings_to_urls": False})
    ws = wb.add_worksheet("Sheet1")
    for i, h in enumerate(headers):
        ws.write_string(0, i, str(h) if h is not None else "")
    for col0, val in data_row.items():
        if val is None:
            continue
        if isinstance(val, (int, float)) and not isinstance(val, bool):
            ws.write_number(1, col0, val)
        else:
            ws.write_string(1, col0, str(val))
    wb.close()


def upload(path: Path) -> int:
    boundary = "----CursorBoundary" + uuid.uuid4().hex
    data = path.read_bytes()
    parts = [
        f"--{boundary}".encode(),
        f'Content-Disposition: form-data; name="file"; filename="{path.name}"'.encode(),
        b"Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        b"",
        data,
        f"--{boundary}--".encode(),
        b"",
    ]
    payload = b"\r\n".join(parts)
    req = urllib.request.Request(
        f"{API}/mass-import/PODS/files/upload", data=payload, method="POST"
    )
    req.add_header("Authorization", f"Bearer {TOKEN}")
    req.add_header("Content-Type", f"multipart/form-data; boundary={boundary}")
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            print("UPLOAD", path.name, resp.status)
            return resp.status
    except urllib.error.HTTPError as e:
        body = e.read()[:800]
        print("UPLOAD ERR", path.name, e.code, body)
        return e.code


def main():
    tpl = download_template()
    headers = headers_from(tpl)
    print("n_headers", len(headers))
    for i, h in enumerate(headers):
        if i >= 48 or (h and ("energy" in str(h).lower() or "share" in str(h).lower() or "model" in str(h).lower() or "described" in str(h).lower() or "customer" in str(h).lower())):
            print(f"  col{i}: {h}")
    print("HEADER_52=", headers[52] if len(headers) > 52 else None)

    suffix = "".join(random.choices(string.ascii_uppercase + string.digits, k=18))
    create_id = ("32XMI" + suffix)[:33]
    (BASE / "last-create-identifier.txt").write_text(create_id, encoding="utf-8")

    create_row = {
        3: create_id,
        5: "PDT3143 CREATE POD RERUN",
        6: "ЕСО ЕАД",
        8: 1,
        12: "CONSUMER",
        13: "NON_HOUSEHOLD",
        14: "LOW",
        15: "SETTLEMENT_PERIOD",
        19: "NO",
        20: "standard country",
        21: "standard region",
        22: "standard municipality",
        23: "standard populated place",
        24: "standard zip code",
        52: "ENERGY_GROUP",
    }

    create_path = BASE / "pods_mass_import_CREATE_only_pdt3143.xlsx"
    write_xlsx(create_path, headers, create_row)
    print("create_id", create_id)
    upload(create_path)
    # EDIT is uploaded after CREATE succeeds — see follow-up script if needed
    print("CREATE_UPLOADED")


if __name__ == "__main__":
    main()
