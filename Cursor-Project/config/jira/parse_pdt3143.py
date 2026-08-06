import json
import re
from pathlib import Path

p = Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\jira\PDT-3143-full.json")
d = json.loads(p.read_text(encoding="utf-8-sig"))
f = d.get("fields", d)


def adf_text(node, acc=None):
    if acc is None:
        acc = []
    if node is None:
        return acc
    if isinstance(node, str):
        acc.append(node)
        return acc
    if isinstance(node, dict):
        if node.get("type") == "text":
            acc.append(node.get("text") or "")
        attrs = node.get("attrs") or {}
        if isinstance(attrs, dict) and attrs.get("url"):
            acc.append(attrs["url"])
        for m in node.get("marks") or []:
            if isinstance(m, dict) and m.get("type") == "link":
                acc.append((m.get("attrs") or {}).get("href") or "")
        for v in node.values():
            adf_text(v, acc)
    elif isinstance(node, list):
        for i in node:
            adf_text(i, acc)
    return acc


out = {
    "summary": f.get("summary"),
    "status": (f.get("status") or {}).get("name"),
    "issuetype": (f.get("issuetype") or {}).get("name"),
    "environment": f.get("environment"),
    "description_text": "\n".join(adf_text(f.get("description")))[:5000],
    "cf10103_text": "\n".join(adf_text(f.get("customfield_10103")))[:5000],
    "ac_text": "\n".join(adf_text(f.get("customfield_10048")))[:4000],
}

links = []
for l in f.get("issuelinks") or []:
    t = l.get("type") or {}
    for side, label in (("outwardIssue", t.get("outward")), ("inwardIssue", t.get("inward"))):
        issue = l.get(side) or {}
        if issue:
            links.append(
                f"{label}: {issue.get('key')} {((issue.get('fields') or {}).get('summary'))}"
            )
out["links"] = links
out["attachments"] = [
    {"filename": a.get("filename"), "size": a.get("size")} for a in (f.get("attachment") or [])
]
comments = []
for c in ((f.get("comment") or {}).get("comments") or [])[-8:]:
    comments.append(
        {
            "author": (c.get("author") or {}).get("displayName"),
            "body": "\n".join(adf_text(c.get("body")))[:1200],
        }
    )
out["comments"] = comments
alltxt = out["description_text"] + "\n" + out["cf10103_text"] + "\n" + out["ac_text"]
for c in comments:
    alltxt += "\n" + c["body"]
out["urls"] = re.findall(r"https?://[^\s\]\)\"<>]+", alltxt)

Path(r"c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\jira\PDT-3143-summary.json").write_text(
    json.dumps(out, ensure_ascii=False, indent=2), encoding="utf-8"
)
print("SUMMARY:", out["summary"])
print("STATUS:", out["status"], "| TYPE:", out["issuetype"])
print("ENV:", out["environment"])
print("LINKS:", out["links"])
print("URLS:", out["urls"][:15])
print("ATT:", out["attachments"])
print("---DESC---")
print(out["description_text"][:2500] or out["cf10103_text"][:2500])
print("---AC---")
print(out["ac_text"][:2500])
print("---COMMENTS---")
for c in comments:
    print(c["author"], ":", c["body"][:500])
    print("---")
