"""Download Invoice Cancellation import template from PreProd."""
from __future__ import annotations

import json
import ssl
import urllib.error
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
ENV_PATH = Path(r"C:\Users\N.kevlishvili\Cursor\Cursor-Project\.env")
LOGIN = "https://testapps.energo-pro.bg/backend/portal/rest/v2/login"
API = "http://10.236.20.31:7091"
CANDIDATES = [
    "/invoice-cancellation/download-template",
    "/mass-import/INVOICE_CANCELLATION/template/download",
    "/mass-import/INVOICE_CANCELLATIONS/template/download",
]


def load_env() -> dict[str, str]:
    env: dict[str, str] = {}
    for line in ENV_PATH.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.strip().startswith("#"):
            k, v = line.split("=", 1)
            env[k.strip()] = v.strip().strip('"').strip("'")
    return env


def main() -> None:
    env = load_env()
    body = json.dumps({"user": env["PORTAL_USER"], "password": env["PASSWORD"]}).encode()
    req = urllib.request.Request(LOGIN, data=body, headers={"Content-Type": "application/json"})
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, timeout=45, context=ctx) as resp:
        token = json.load(resp)["jwt"]
    print("login_ok")
    headers = {"Authorization": f"Bearer {token}"}
    for path in CANDIDATES:
        url = API + path
        try:
            req2 = urllib.request.Request(url, headers=headers)
            with urllib.request.urlopen(req2, timeout=90) as resp:
                data = resp.read()
                ctype = resp.headers.get("Content-Type")
                disp = resp.headers.get("Content-Disposition")
                safe = path.strip("/").replace("/", "_")
                out = HERE / f"invoice-cancellation-template-{safe}.xlsx"
                out.write_bytes(data)
                print("OK", resp.status, "len", len(data), "ctype", ctype, "disp", disp, "out", out.name)
        except urllib.error.HTTPError as e:
            print("HTTP", e.code, path, e.read()[:200])
        except Exception as e:
            print("ERR", path, type(e).__name__, str(e)[:200])


if __name__ == "__main__":
    main()
