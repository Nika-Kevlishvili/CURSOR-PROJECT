"""Download GOVERNMENT_COMPENSATION mass-import template from PreProd API."""
from __future__ import annotations

import json
import ssl
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
ENV_PATH = Path(r"C:\Users\N.kevlishvili\Cursor\Cursor-Project\.env")
OUT = HERE / "GovernmentCompensation-preprod-template.xlsx"
LOGIN = "https://testapps.energo-pro.bg/backend/portal/rest/v2/login"
TEMPLATE = (
    "http://10.236.20.31:7091/mass-import/GOVERNMENT_COMPENSATION/template/download"
)


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
    req = urllib.request.Request(
        LOGIN,
        data=body,
        headers={"Content-Type": "application/json"},
    )
    ctx = ssl.create_default_context()
    with urllib.request.urlopen(req, timeout=45, context=ctx) as resp:
        token = json.load(resp)["jwt"]
    if not token:
        raise SystemExit("login failed: empty jwt")
    print("login_ok")

    req2 = urllib.request.Request(TEMPLATE, headers={"Authorization": f"Bearer {token}"})
    with urllib.request.urlopen(req2, timeout=90) as resp:
        data = resp.read()
        print("status", resp.status, "len", len(data), "ctype", resp.headers.get("Content-Type"))
    OUT.write_bytes(data)
    print("wrote", OUT.name)


if __name__ == "__main__":
    main()
