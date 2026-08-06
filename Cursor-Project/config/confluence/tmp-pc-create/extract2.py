import json
import os
import urllib.parse
import urllib.request
from pathlib import Path
from base64 import b64encode

# --- Swagger nested DTOs ---
spec = json.loads(Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\swagger\dev\swagger-spec.json").read_text(encoding="utf-8"))
schemas = spec["components"]["schemas"]
for name in [
    "ProductContractBankingDetails",
    "PriceComponentContractFormula",
    "ContractInterimAdvancePaymentsRequest",
    "ContractProductAdditionalParamsRequest",
]:
    s = schemas.get(name) or {}
    print("====", name, "====")
    print("required:", s.get("required"))
    for k, v in (s.get("properties") or {}).items():
        req = "REQ" if k in (s.get("required") or []) else "opt"
        print(f"  {req} {k}: type={v.get('type')} ref={v.get('$ref')} enum={v.get('enum')}")
    print()

# --- Confluence search ---
def env_val(name: str):
    v = os.environ.get(name)
    if v:
        return v
    for p in [Path(r"c:\Users\N.kevlishvili\Cursor\.env"), Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\.env")]:
        if not p.exists():
            continue
        for line in p.read_text(encoding="utf-8", errors="ignore").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, val = line.split("=", 1)
            if k.strip() == name:
                return val.strip().strip("'").strip('"')
    return None

wiki = env_val("CONFLUENCE_WIKI_BASE")
if not wiki:
    url = env_val("CONFLUENCE_URL") or ""
    if "/wiki" in url:
        wiki = url[: url.find("/wiki") + 5]
    else:
        jb = env_val("JIRA_BASE_URL")
        wiki = (jb.rstrip("/") + "/wiki") if jb else None
email = env_val("CONFLUENCE_EMAIL") or env_val("JIRA_EMAIL")
token = env_val("CONFLUENCE_API_TOKEN") or env_val("JIRA_API_TOKEN")
print("wiki=", wiki)
if wiki and email and token:
    auth = b64encode(f"{email}:{token}".encode()).decode()
    cql = 'title ~ "Product contract" AND type = page'
    q = urllib.parse.urlencode({"cql": cql, "limit": "25"})
    req = urllib.request.Request(
        f"{wiki.rstrip('/')}/rest/api/content/search?{q}",
        headers={"Authorization": f"Basic {auth}", "Accept": "application/json"},
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
        for r in data.get("results", []):
            print(f"{r.get('id')} | {r.get('title')}")
        out = Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\confluence\tmp-pc-create\search.json")
        out.write_text(json.dumps(data, indent=2), encoding="utf-8")
        print("saved", out)
    except Exception as e:
        print("CONFLUENCE_ERR", type(e).__name__, e)
else:
    print("MISSING_CREDS")
