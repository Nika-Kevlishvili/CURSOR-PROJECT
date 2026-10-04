import json
import urllib.request
from pathlib import Path

env = {}
for line in Path(r"C:\Users\N.kevlishvili\Cursor\Cursor-Project\.env").read_text(encoding="utf-8").splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        env[k.strip()] = v.strip().strip('"').strip("'")

login_body = json.dumps({"user": env["PORTAL_USER"], "password": env["PASSWORD"]}).encode()
req = urllib.request.Request(
    env["TESTAUTHAPI"],
    data=login_body,
    headers={"Content-Type": "application/json"},
)
token = json.load(urllib.request.urlopen(req, timeout=30))["jwt"]
print("login_ok")

url = "https://testapps.energo-pro.bg/backend/phoenix-epres/mass-import/GOVERNMENT_COMPENSATION/template/download"
req2 = urllib.request.Request(url, headers={"Authorization": f"Bearer {token}"})
with urllib.request.urlopen(req2, timeout=60) as resp:
    data = resp.read()
    print("status", resp.status, "len", len(data), "ctype", resp.headers.get("Content-Type"))

out = Path(__file__).with_name("GovernmentCompensation-template.xlsx")
out.write_bytes(data)
print("wrote", out)
