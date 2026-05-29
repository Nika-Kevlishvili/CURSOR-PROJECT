import argparse
import base64
import json
import os
import sys
from urllib.request import Request, urlopen


def load_env_file(path: str) -> dict:
    vals: dict[str, str] = {}
    with open(path, "r", encoding="utf-8") as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            k = k.strip()
            v = v.strip()
            if (v.startswith('"') and v.endswith('"')) or (v.startswith("'") and v.endswith("'")):
                v = v[1:-1]
            vals[k] = v
    return vals


def adf_to_text(node) -> str:
    if node is None:
        return ""
    if isinstance(node, str):
        return node
    if isinstance(node, list):
        return "".join(adf_to_text(x) for x in node)
    if isinstance(node, dict):
        t = node.get("type")
        if t == "text":
            return node.get("text", "")
        if t == "hardBreak":
            return "\n"
        if t in ("paragraph", "heading", "blockquote", "listItem"):
            return adf_to_text(node.get("content")) + "\n"
        if t in ("bulletList", "orderedList", "doc"):
            return adf_to_text(node.get("content"))
        return adf_to_text(node.get("content"))
    return ""


def main() -> int:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

    ap = argparse.ArgumentParser()
    ap.add_argument("--issue", required=True)
    ap.add_argument("--author-contains", required=True)
    ap.add_argument(
        "--env-file",
        default=os.path.join("Cursor-Project", ".env"),
        help="Path to .env with JIRA_BASE_URL/JIRA_EMAIL/JIRA_API_TOKEN",
    )
    args = ap.parse_args()

    env = load_env_file(args.env_file)
    base = env.get("JIRA_BASE_URL")
    email = env.get("JIRA_EMAIL")
    token = env.get("JIRA_API_TOKEN")
    if not (base and email and token):
        raise SystemExit("Missing JIRA_BASE_URL/JIRA_EMAIL/JIRA_API_TOKEN in env file.")

    url = f"{base}/rest/api/3/issue/{args.issue}/comment?orderBy=created&maxResults=500"
    auth = base64.b64encode(f"{email}:{token}".encode("utf-8")).decode("ascii")
    req = Request(
        url,
        headers={
            "Authorization": f"Basic {auth}",
            "Accept": "application/json",
        },
    )
    with urlopen(req) as resp:
        data = json.load(resp)

    needle = args.author_contains.lower()
    comments = data.get("comments", [])
    matches = [
        c
        for c in comments
        if needle in ((c.get("author") or {}).get("displayName") or "").lower()
    ]
    if not matches:
        authors = sorted(
            {
                (c.get("author") or {}).get("displayName")
                for c in comments
                if (c.get("author") or {}).get("displayName")
            }
        )
        print(json.dumps({"error": "no matching author", "authors": authors}, ensure_ascii=False))
        return 0

    matches.sort(key=lambda c: c.get("created") or "")
    last = matches[-1]
    out = {
        "created": last.get("created"),
        "updated": last.get("updated"),
        "author": (last.get("author") or {}).get("displayName"),
        "plainText": adf_to_text(last.get("body")).strip(),
    }
    print(json.dumps(out, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

