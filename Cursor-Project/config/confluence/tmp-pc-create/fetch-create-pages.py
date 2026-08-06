import json
import os
import re
import urllib.parse
import urllib.request
from pathlib import Path
from base64 import b64encode
from html.parser import HTMLParser

def env_val(name: str):
    v = os.environ.get(name)
    if v:
        return v.strip()
    for p in [
        Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\.env"),
        Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\EnergoTS\.env"),
        Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\Cursor Setup\env.example"),
    ]:
        if not p.exists():
            continue
        for line in p.read_text(encoding="utf-8", errors="ignore").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, val = line.split("=", 1)
            if k.strip() == name:
                return val.strip().strip("'").strip('"').strip()
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
auth = b64encode(f"{email}:{token}".encode()).decode()
headers = {"Authorization": f"Basic {auth}", "Accept": "application/json"}

out_dir = Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\confluence\tmp-pc-create")
out_dir.mkdir(parents=True, exist_ok=True)

# broader search for create pages
queries = [
    'title = "Product Contract Create" AND type = page',
    'title ~ "Product Contract Create" AND type = page',
    'title ~ "Product contract create" AND type = page',
    'title = "Product Contract" AND type = page',
]
all_hits = {}
for cql in queries:
    q = urllib.parse.urlencode({"cql": cql, "limit": "25"})
    req = urllib.request.Request(f"{wiki.rstrip('/')}/rest/api/content/search?{q}", headers=headers)
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    for r in data.get("results", []):
        all_hits[r["id"]] = r.get("title")
print("SEARCH HITS:")
for i, t in sorted(all_hits.items(), key=lambda x: x[1] or ""):
    print(f"  {i} | {t}")

# fetch key pages
page_ids = ["880248396", "2228419"]
# also try classic create if found
for i, t in all_hits.items():
    tl = (t or "").lower()
    if "create" in tl and "experiment" not in tl and "mass" not in tl and "express" not in tl:
        page_ids.append(i)
    if t == "Product Contract Create" or t == "Product contract create":
        page_ids.append(i)
page_ids = list(dict.fromkeys(page_ids))

class Stripper(HTMLParser):
    def __init__(self):
        super().__init__()
        self.parts = []
        self.skip = False
    def handle_starttag(self, tag, attrs):
        if tag in ("style", "script"):
            self.skip = True
    def handle_endtag(self, tag):
        if tag in ("style", "script"):
            self.skip = False
        if tag in ("p", "li", "tr", "h1", "h2", "h3", "h4", "br"):
            self.parts.append("\n")
    def handle_data(self, data):
        if not self.skip:
            self.parts.append(data)
    def text(self):
        t = "".join(self.parts)
        t = re.sub(r"\n{3,}", "\n\n", t)
        return t.strip()

for pid in page_ids:
    q = urllib.parse.urlencode({"expand": "body.storage,body.view,version,space"})
    req = urllib.request.Request(f"{wiki.rstrip('/')}/rest/api/content/{pid}?{q}", headers=headers)
    with urllib.request.urlopen(req, timeout=60) as resp:
        page = json.loads(resp.read().decode("utf-8"))
    (out_dir / f"{pid}.json").write_text(json.dumps(page, indent=2), encoding="utf-8")
    body = (page.get("body") or {}).get("storage", {}).get("value") or (page.get("body") or {}).get("view", {}).get("value") or ""
    s = Stripper()
    try:
        s.feed(body)
        text = s.text()
    except Exception:
        text = re.sub(r"<[^>]+>", " ", body)
    (out_dir / f"{pid}.txt").write_text(text, encoding="utf-8")
    print("\n====", pid, page.get("title"), "====")
    print(text[:6000])
    print("...[truncated]..." if len(text) > 6000 else "")
