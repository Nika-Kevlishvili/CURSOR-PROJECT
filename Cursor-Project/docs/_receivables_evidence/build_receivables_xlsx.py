# -*- coding: utf-8 -*-
"""Build a process-only receivables regression catalog, same rules as billing."""
from __future__ import annotations

import json
import re
import urllib.request
from base64 import b64encode
from collections import defaultdict
from html.parser import HTMLParser
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

ROOT = Path(__file__).resolve().parents[2]
EVID = Path(__file__).resolve().parent
ENERGOTS = ROOT / "EnergoTS"
DIAGRAMS = ROOT / "config" / "Diagrams" / "Bundle 6"
OUT_XLSX = ROOT / "docs" / "Receivables_Regression_Cases.xlsx"
OUT_MD = ROOT / "docs" / "Receivables_Regression_Coverage_Catalog.md"
JIRA_JSON = EVID / "jira_receivables_test_cases.json"

SHEETS = [
    "00_Summary",
    "00_Unmapped_Gaps",
    "01_Customer_Liability",
    "02_Customer_Receivable",
    "03_Deposit",
    "04_Manual_Offsetting",
    "05_Automatic_Offsetting",
    "06_Direct_Offsetting",
    "07_Offline_Payment",
    "08_Online_Payment",
    "09_Payment_Reversal",
    "10_Payment_Mass_Import",
    "11_Reminder",
    "12_Reminder_For_Disconnection",
    "13_Request_For_Disconnection",
    "14_Cancel_Disconnection",
    "15_Disconnection",
    "16_Reconnection",
    "17_Late_Payment_Fine",
    "18_Default_Interest",
    "19_Rescheduling",
    "20_Mass_Blocking",
    "21_Coordinator_Objection",
    "22_Objection_Withdrawal",
    "23_Tax_Calculation",
    "24_Payment_Package",
    "25_Collection_Channel",
    "26_Interest_Rate",
    "27_Customer_Assessment",
]

HEADERS = [
    "Theme",
    "Priority",
    "PriorityReason",
    "CaseId",
    "CaseTitle",
    "EvidenceType",
    "EvidenceCitation",
    "EvidenceQuote",
    "AutomationStatus",
    "AutomationName",
    "AutomationFile",
    "Notes",
]

PRIORITY_RANK = {"Highest": 0, "High": 1, "Medium": 2, "Low": 3}
HEADER_FILL = PatternFill("solid", fgColor="1F4E79")
HEADER_FONT = Font(color="FFFFFF", bold=True)
YES_FILL = PatternFill("solid", fgColor="C6EFCE")
NO_FILL = PatternFill("solid", fgColor="FFC7CE")
PARTIAL_FILL = PatternFill("solid", fgColor="FFEB9C")
PRIO_FILL = {
    "Highest": PatternFill("solid", fgColor="F4B183"),
    "High": PatternFill("solid", fgColor="FFE699"),
    "Medium": PatternFill("solid", fgColor="DDEBF7"),
    "Low": PatternFill("solid", fgColor="E7E6E6"),
}

# Outcome of a receivables process: balance, offset, payment, fine, deposit, reschedule, supply flag, or the document that process produces.
PROCESS_FORCE_RE = re.compile(
    r"(?i)("
    r"offset|refund process|liability creation|automatic liability|"
    r"payment reverse|reverse payment|mlo reverse|"
    r"mass import|online payment|offline payment|"
    r"reschedul|late payment fine|\blpf\b|"
    r"disconnection of power|request for disconnection|"
    r"cancel+ation of request|cancellation of request|"
    r"reconnection|"
    r"reminder create|reminder -|sending sms|sending email|communication object|"
    r"block for|block interest|block list|"
    r"deposit process|"
    r"direct offset|automatic offset|"
    r"claimed penalty offset"
    r")"
)

BILLING_OWNED_RE = re.compile(
    r"(?i)("
    r"interim and advance|price component|for volumes|billing run|"
    r"invoice correction|invoice cancellation|invoice reversal|"
    r"government compensation|on regeneration|"
    r"correct liability offset after"
    r")"
)

UI_RE = re.compile(
    r"(?i)("
    r"\blisting\b|\bsorting\b|\bpaging\b|\bpermission\b|\btranslates?\b|"
    r"\bpreview\b|search and|dropdown filters|"
    r"point of delivery|re-signing|sales portal get|^get:"
    r")"
)

OBJECT_SETUP_RE = re.compile(
    r"(?i)("
    r"^create - |^create \| |^edit - |^edit \| |"
    r"deposits create|deposits edit|deposits listing|deposits preview|"
    r"create payment package|create collection channel|create customer |"
    r"create interest rate|create mass operation|create reminder$|create payment$|"
    r"^liability create$|task create|full data \| all fields"
    r")"
)

CONF_PAGES = [
    ("585698824", "Phase 2 - Automatic Offsetting Process (2)", "05_Automatic_Offsetting"),
    ("585699256", "Phase 2 - Automatic Liability Offsetting Process (2)", "05_Automatic_Offsetting"),
    ("585698837", "Phase 2 - Direct Offsetting Process (2)", "06_Direct_Offsetting"),
    ("585698894", "Phase 2 - Manual Liabilities offsetting. (2)", "04_Manual_Offsetting"),
    ("585698905", "Phase 2 - Manual Liabilities Offsetting Reversal (2)", "04_Manual_Offsetting"),
    ("585698848", "Phase 2 - Reminder Process JOB (2)", "11_Reminder"),
    ("585699036", "Phase 2 - Rescheduling Calculation (2)", "19_Rescheduling"),
    ("585699047", "Phase 2 - Rescheduling Reversal (2)", "19_Rescheduling"),
    ("585698940", "Phase 2 - Late payment fine calculation (2)", "17_Late_Payment_Fine"),
    ("585698951", "Phase 2 - Late payment fine Reversal (2)", "17_Late_Payment_Fine"),
    ("585697796", "Phase 2 - Create Late Payment Fine from Rescheduling", "17_Late_Payment_Fine"),
    ("585698970", "Phase 2 - Default Interest Rate Calculation-Process (2)", "18_Default_Interest"),
    ("585698981", "Phase 2 - Online payment process (2)", "08_Online_Payment"),
    ("611090433", "Phase 2 - Online payment Cash terminal", "08_Online_Payment"),
    ("611024897", "Phase 2 - Online payment Virtual POS terminal", "08_Online_Payment"),
    ("585730691", "Phase 2 - Online payment easy pay final", "08_Online_Payment"),
    ("585698992", "Phase 2 - Payment Mass Import - JOB (2)", "10_Payment_Mass_Import"),
    ("585697034", "Phase 2 - Deposit Refunding/Deleting (2)", "03_Deposit"),
    ("585696679", "Phase 2 - Customer liability automatic creation action (Deposit) (2)", "03_Deposit"),
    ("585696866", "Phase 2 - Customer receivable automatic creation (deposit) (2)", "02_Customer_Receivable"),
    ("585698098", "Phase 2 - Request for disconnection liabilities selecting logic (2)", "13_Request_For_Disconnection"),
    ("585699014", "Phase 2 - Tax Calculation (2)", "23_Tax_Calculation"),
]

DIAGRAM_THEME = {
    "automatic offsetting": "05_Automatic_Offsetting",
    "occurrence date payment": "05_Automatic_Offsetting",
    "manual liability offsetting": "04_Manual_Offsetting",
    "reversal of payment": "09_Payment_Reversal",
    "reversal of lpf": "17_Late_Payment_Fine",
    "reversal of rescheduling": "19_Rescheduling",
    "online payment": "08_Online_Payment",
    "request for disconnection": "13_Request_For_Disconnection",
    "tax calculation": "23_Tax_Calculation",
    "objection withdrawal": "22_Objection_Withdrawal",
    "objection to a change": "21_Coordinator_Objection",
}

RULE_RE = re.compile(
    r"(?i)(should|must|shall|creates?|does not|do not|will not|current amount|offset|revers|refund|fine|liabilit|receivable)"
)
NOISE_RE = re.compile(r"(?i)(this box is for|successful+ message|^\s*start\s*$|^\s*end\s*$)")


class _Text(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.parts: list[str] = []

    def handle_data(self, data: str) -> None:
        if data and data.strip():
            self.parts.append(data.strip())


def html_to_text(html: str) -> str:
    parser = _Text()
    parser.feed(html or "")
    return re.sub(r"\s+", " ", " ".join(parser.parts))


def load_env() -> dict[str, str]:
    found: dict[str, str] = {}
    candidates = [
        ROOT.parent / ".env",
        ROOT / ".env",
        ENERGOTS / ".env",
    ]
    for path in candidates:
        if not path.exists():
            continue
        for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            found.setdefault(key.strip(), value.strip().strip("'\""))
    return found


def confluence_storage(page_id: str, env: dict[str, str]) -> str:
    email = env.get("CONFLUENCE_EMAIL") or env.get("JIRA_EMAIL")
    token = env.get("CONFLUENCE_API_TOKEN") or env.get("JIRA_API_TOKEN")
    base = env.get("CONFLUENCE_WIKI_BASE") or "https://asterbit.atlassian.net/wiki"
    if not email or not token:
        raise RuntimeError("missing confluence credentials")
    url = f"{base.rstrip('/')}/rest/api/content/{page_id}?expand=body.storage"
    req = urllib.request.Request(url)
    auth = b64encode(f"{email}:{token}".encode()).decode()
    req.add_header("Authorization", f"Basic {auth}")
    req.add_header("Accept", "application/json")
    with urllib.request.urlopen(req, timeout=40) as resp:
        payload = json.loads(resp.read().decode("utf-8"))
    return payload.get("body", {}).get("storage", {}).get("value") or ""


def sentences(text: str) -> list[str]:
    chunks = re.split(r"(?<=[.])\s+", text)
    kept = []
    for chunk in chunks:
        chunk = chunk.strip()
        if len(chunk) < 45 or len(chunk) > 500:
            continue
        if not re.search(r"(?i)\b(should|must|shall)\b", chunk):
            continue
        if chunk.lower().startswith("http") or "diagrams.net" in chunk[:120]:
            continue
        if NOISE_RE.search(chunk):
            continue
        if re.search(r"(?i)(listing|preview tab|permission|button create)", chunk):
            continue
        kept.append(chunk)
        if len(kept) >= 6:
            break
    return kept


THEME_MARKERS = [
    (re.compile(r"payment reverse|reverse payment"), "09_Payment_Reversal"),
    (re.compile(r"offline payment"), "07_Offline_Payment"),
    (re.compile(r"online payment|cash terminal|virtual pos|systech|bulgarian post|easy-pay|easypay"), "08_Online_Payment"),
    (re.compile(r"payment mass import"), "10_Payment_Mass_Import"),
    (re.compile(r"\bmlo\b|manual liability offset|manual offset"), "04_Manual_Offsetting"),
    (re.compile(r"automatic offset"), "05_Automatic_Offsetting"),
    (re.compile(r"direct offset|claimed penalty offset"), "06_Direct_Offsetting"),
    (re.compile(r"deposit"), "03_Deposit"),
    (re.compile(r"late payment fine|\blpf\b"), "17_Late_Payment_Fine"),
    (re.compile(r"default interest"), "18_Default_Interest"),
    (re.compile(r"block for|block interest|block list"), "20_Mass_Blocking"),
    (re.compile(r"reminder for disconnection|trigger is rfd"), "12_Reminder_For_Disconnection"),
    (re.compile(r"reminder"), "11_Reminder"),
    (re.compile(r"reschedul"), "19_Rescheduling"),
    (re.compile(r"cancel+ation of request|cancellation of request"), "14_Cancel_Disconnection"),
    (re.compile(r"request for disconnection"), "13_Request_For_Disconnection"),
    (re.compile(r"reconnection"), "16_Reconnection"),
    (re.compile(r"disconnection"), "15_Disconnection"),
    (re.compile(r"objection withdrawal"), "22_Objection_Withdrawal"),
    (re.compile(r"coordinator objection"), "21_Coordinator_Objection"),
    (re.compile(r"customer receivable|receivable generation"), "02_Customer_Receivable"),
    (re.compile(r"customer liability|\bliability\b"), "01_Customer_Liability"),
    (re.compile(r"\btax\b"), "23_Tax_Calculation"),
]


def theme_for_jira(summary: str) -> str | None:
    title = summary.strip()
    low = title.lower()
    if BILLING_OWNED_RE.search(title) or "interim" in low:
        return None
    if re.search(r"(?i)(point of delivery|\bget\s*:|list of all|sales portal)", title):
        return None
    if re.search(r"(?i)^cancel (reverses|is refused)", title) and "reschedul" not in low:
        return None
    forced = bool(PROCESS_FORCE_RE.search(title))
    if not forced and (UI_RE.search(title) or OBJECT_SETUP_RE.search(title)):
        return None
    if re.search(r"(?i)interest rate", title) and not forced:
        return None
    if re.search(r"(?i)customer assessment|\bassessment\b", title) and not forced:
        return None
    if re.search(r"trigger is rfd|reminder for disconnection", low):
        return "12_Reminder_For_Disconnection"
    hits = []
    for pattern, theme in THEME_MARKERS:
        match = pattern.search(low)
        if match:
            hits.append((match.start(), theme))
    if not hits:
        return None
    hits.sort()
    theme = hits[0][1]
    if theme in {"22_Objection_Withdrawal", "21_Coordinator_Objection", "24_Payment_Package", "25_Collection_Channel"} and not forced:
        return None
    return theme


def assign_priority(title: str, evidence: str) -> tuple[str, str]:
    blob = f"{title} {evidence}"
    if evidence == "Playwright":
        return "Highest", "Process automation leaf (receivable suite)"
    if evidence == "Diagram":
        if re.search(r"(?i)error", title):
            return "Medium", "Diagram error path on process"
        return "Highest", "Diagram process decision or outcome step"
    if re.search(r"(?i)^deposits edit", title):
        return "Medium", "Edit is blocked after the object took part in offsetting"
    if re.search(r"(?i)(offset|reverse|refund|reschedul|fine|disconnection|reconnection|payment|deposit process|block for|reminder)", blob):
        if re.search(r"(?i)(negative|refused|should not|must not|rejected|without )", blob):
            return "Medium", "Validation / error / edge path"
        return "Highest", "Receivable process flow (offset/payment/fine/deposit/reschedule/supply/reminder)"
    if re.search(r"(?i)(amount|vat|interest|due date|current amount|calculation)", blob):
        return "High", "Critical calculation / financial impact"
    if evidence == "Confluence":
        return "High", "Confluence process rule"
    return "Medium", "Process leaf — supporting"


def row(theme, case_id, title, etype, citation, quote, status, aname, afile, notes=""):
    return {
        "Theme": theme,
        "CaseId": case_id,
        "CaseTitle": (title or "")[:500],
        "EvidenceType": etype,
        "EvidenceCitation": (citation or "")[:500],
        "EvidenceQuote": (quote or "")[:240],
        "AutomationStatus": status,
        "AutomationName": (aname or "")[:500],
        "AutomationFile": afile or "",
        "Notes": notes or "",
    }


def index_playwright() -> list[dict]:
    roots = [ENERGOTS / "tests" / "receivableManagement"]
    cursor = ENERGOTS / "tests" / "cursor"
    specs = []
    for root in roots:
        if root.is_dir():
            specs.extend(root.rglob("*.spec.ts"))
    if cursor.is_dir():
        for path in cursor.glob("*.spec.ts"):
            name = path.name.lower()
            if any(k in name for k in ("payment", "offset", "deposit", "reschedul", "disconnection", "reminder", "late-payment", "blocking")):
                if "government" in name or "invoice-correction" in name or "invoice-cancellation" in name:
                    continue
                specs.append(path)
    test_re = re.compile(r"""(?:test|it)(?:\.(only|skip))?\(\s*(['"`])(.+?)\2""")
    id_re = re.compile(r"(REG|PDT|PHN)-(\d+)", re.I)
    rows = []
    for path in specs:
        rel = path.relative_to(ENERGOTS).as_posix()
        text = path.read_text(encoding="utf-8", errors="replace")
        for i, line in enumerate(text.splitlines(), 1):
            match = test_re.search(line)
            if not match:
                continue
            skipped = match.group(1) == "skip"
            title = match.group(3)
            if re.search(r"(?i)data preparation|clean up|helper", title):
                continue
            ids = [f"{a.upper()}-{b}" for a, b in id_re.findall(title)]
            rows.append(
                {
                    "title": title,
                    "file": rel,
                    "line": i,
                    "ids": ids,
                    "skipped": skipped,
                }
            )
    return rows


def match_automation(case_id: str, tests: list[dict]) -> tuple[str, str, str, str]:
    hits = [t for t in tests if case_id.upper() in t["title"].upper() and not t["skipped"]]
    if not hits:
        skipped = [t for t in tests if case_id.upper() in t["title"].upper() and t["skipped"]]
        if skipped:
            t = skipped[0]
            return "No", t["title"], t["file"], "Test exists but is skipped"
        return "No", "", "", ""
    t = hits[0]
    note = "Matched on EnergoTS receivableManagement / cursor leaf"
    if len(hits) > 1:
        note += f"; {len(hits)} match(es)"
    return "Yes", t["title"], t["file"], note


def theme_for_file(path: Path) -> str:
    name = path.name.lower()
    rel = path.as_posix().lower()
    for key, theme in DIAGRAM_THEME.items():
        if key in name:
            return theme
    if "deposit" in rel:
        return "03_Deposit"
    if "reminder" in rel and "disconnection" in rel:
        return "12_Reminder_For_Disconnection"
    if "blocking" in rel:
        return "20_Mass_Blocking"
    if "interest" in rel:
        return "18_Default_Interest"
    if "payment" in rel and "reverse" in rel:
        return "09_Payment_Reversal"
    if "online" in rel:
        return "08_Online_Payment"
    if "offline" in rel or "/payment/" in rel:
        return "07_Offline_Payment"
    if "offset" in rel:
        return "04_Manual_Offsetting"
    if "reschedul" in rel:
        return "19_Rescheduling"
    if "reconnection" in rel:
        return "16_Reconnection"
    if "cancel" in rel and "disconnection" in rel:
        return "14_Cancel_Disconnection"
    if "request" in rel and "disconnection" in rel:
        return "13_Request_For_Disconnection"
    if "disconnection" in rel:
        return "15_Disconnection"
    if "reminder" in rel:
        return "11_Reminder"
    if "receivable" in rel:
        return "02_Customer_Receivable"
    if "liability" in rel:
        return "01_Customer_Liability"
    if "collection" in rel:
        return "25_Collection_Channel"
    if "package" in rel:
        return "24_Payment_Package"
    return "06_Direct_Offsetting"


def diagram_rows() -> list[dict]:
    rows = []
    if not DIAGRAMS.is_dir():
        return rows
    for path in sorted(DIAGRAMS.glob("*.semantic.md")):
        theme = None
        low = path.name.lower()
        for key, value in DIAGRAM_THEME.items():
            if key in low:
                theme = value
                break
        if not theme:
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        section = ""
        n = 0
        for line in text.splitlines():
            if line.startswith("## "):
                section = line[3:].strip().lower()
                continue
            if section.startswith("decision") and line.startswith("### "):
                n += 1
                title = line[4:].strip()
                rows.append(
                    row(
                        theme,
                        f"DIAG-{path.stem[:24]}-D{n}",
                        f"Decision: {title}",
                        "Diagram",
                        path.name,
                        title,
                        "No",
                        "",
                        "",
                        "Kind=Decision",
                    )
                )
            if section.startswith("process") and line.startswith("- "):
                step = line[2:].strip()
                if not re.search(r"(?i)(should|must|shall|current amount|reaches 0|deduction|refund|revers)", step):
                    continue
                if NOISE_RE.search(step):
                    continue
                n += 1
                rows.append(
                    row(
                        theme,
                        f"DIAG-{path.stem[:24]}-S{n}",
                        step,
                        "Diagram",
                        path.name,
                        step,
                        "No",
                        "",
                        "",
                        "Kind=Step",
                    )
                )
    return rows


def confluence_rows(env: dict[str, str]) -> tuple[list[dict], int]:
    rows = []
    failed = 0
    for page_id, title, theme in CONF_PAGES:
        try:
            html = confluence_storage(page_id, env)
        except Exception as exc:
            failed += 1
            rows.append(
                row(
                    theme,
                    f"CONF-{page_id}",
                    title,
                    "Confluence",
                    f"{title} | page {page_id} | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/{page_id}",
                    "",
                    "No",
                    "",
                    "",
                    f"Page fetch failed: {type(exc).__name__}",
                )
            )
            continue
        text = html_to_text(html)
        found = sentences(text)
        if not found:
            rows.append(
                row(
                    theme,
                    f"CONF-{page_id}",
                    title,
                    "Confluence",
                    f"{title} | page {page_id} | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/{page_id}",
                    text[:200],
                    "No",
                    "",
                    "",
                    "No should/must outcome sentence extracted",
                )
            )
            continue
        for i, quote in enumerate(found, 1):
            rows.append(
                row(
                    theme,
                    f"CONF-{page_id}-{i}",
                    quote,
                    "Confluence",
                    f"{title} | page {page_id} | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/{page_id}",
                    quote,
                    "No",
                    "",
                    "",
                    "Verbatim outcome sentence from Phase 2 Bundle 6",
                )
            )
    return rows, failed


def sort_theme_rows(rows: list[dict]) -> list[dict]:
    return sorted(
        rows,
        key=lambda x: (
            x.get("_prio_rank", 9),
            0 if x.get("AutomationStatus") == "No" else 1 if x.get("AutomationStatus") == "Partial" else 2,
            x.get("CaseId") or "",
        ),
    )


def main() -> None:
    jira = json.loads(JIRA_JSON.read_text(encoding="utf-8-sig"))
    tests = index_playwright()
    before = len(jira)
    all_rows: list[dict] = []
    kept_jira = 0
    for issue in jira:
        summary = issue.get("Summary") or ""
        theme = theme_for_jira(summary)
        if not theme:
            continue
        kept_jira += 1
        status, aname, afile, note = match_automation(issue["Key"], tests)
        if issue.get("Status"):
            note = (note + "; " if note else "") + f"Jira status: {issue['Status']}"
        all_rows.append(
            row(
                theme,
                issue["Key"],
                summary,
                "Jira",
                f"{issue['Key']} | type=Test Case",
                summary,
                status,
                aname,
                afile,
                note,
            )
        )

    for test in tests:
        if test["skipped"]:
            continue
        theme = theme_for_jira(test["title"])
        if theme is None:
            forced = bool(PROCESS_FORCE_RE.search(test["title"]))
            if not forced or BILLING_OWNED_RE.search(test["title"]) or UI_RE.search(test["title"]):
                continue
            theme = theme_for_file(Path(test["file"]))
        case_id = test["ids"][0] if test["ids"] else Path(test["file"]).stem
        all_rows.append(
            row(
                theme,
                case_id,
                test["title"],
                "Playwright",
                f"{test['file']}:{test['line']}",
                test["title"],
                "Yes",
                test["title"],
                test["file"],
                "Asserting leaf; EnergoTS working tree",
            )
        )

    all_rows.extend(diagram_rows())
    env = load_env()
    conf_rows, conf_failed = confluence_rows(env)
    all_rows.extend(conf_rows)

    dedup = {}
    for item in all_rows:
        key = (item["EvidenceType"], item["CaseId"], item["EvidenceQuote"][:80])
        dedup[key] = item
    all_rows = list(dedup.values())

    covered = defaultdict(int)
    for item in all_rows:
        if item["EvidenceType"] == "Jira" and item["AutomationStatus"] == "Yes":
            covered[item["Theme"]] += 1
    for item in all_rows:
        if item["EvidenceType"] == "Jira" and item["AutomationStatus"] == "No" and covered[item["Theme"]] > 0:
            item["AutomationStatus"] = "Partial"
            extra = f"Partial: theme {item['Theme']} has another covered REG test; this leaf has no asserting test"
            item["Notes"] = (item["Notes"] + "; " if item["Notes"] else "") + extra

    for item in all_rows:
        prio, reason = assign_priority(item["CaseTitle"], item["EvidenceType"])
        item["Priority"] = prio
        item["PriorityReason"] = reason
        item["_prio_rank"] = PRIORITY_RANK[prio]

    gap_rows = []
    seen = set()
    for item in all_rows:
        if item["AutomationStatus"] != "No":
            continue
        if item["EvidenceType"] not in ("Diagram", "Confluence"):
            continue
        if str(item["CaseId"]).startswith(("REG-", "PDT-")):
            continue
        key = (item["CaseId"], item["EvidenceQuote"][:60])
        if key in seen:
            continue
        seen.add(key)
        gap_rows.append(dict(item, Theme="00_Unmapped_Gaps"))

    wb = Workbook()
    wb.remove(wb.active)

    def write_sheet(name: str, rows: list[dict]) -> None:
        ws = wb.create_sheet(name)
        ws.append(HEADERS)
        for col in range(1, len(HEADERS) + 1):
            cell = ws.cell(1, col)
            cell.fill = HEADER_FILL
            cell.font = HEADER_FONT
            cell.alignment = Alignment(wrap_text=True, vertical="center")
        for item in sort_theme_rows(rows):
            ws.append([item.get(h, "") for h in HEADERS])
            status = item.get("AutomationStatus")
            fill = YES_FILL if status == "Yes" else PARTIAL_FILL if status == "Partial" else NO_FILL
            ws.cell(ws.max_row, HEADERS.index("AutomationStatus") + 1).fill = fill
            pfill = PRIO_FILL.get(item.get("Priority"))
            if pfill:
                ws.cell(ws.max_row, HEADERS.index("Priority") + 1).fill = pfill
        for col in range(1, len(HEADERS) + 1):
            ws.column_dimensions[get_column_letter(col)].width = min(48, max(14, len(HEADERS[col - 1]) + 2))
        ws.auto_filter.ref = ws.dimensions
        ws.freeze_panes = "A2"

    counts = defaultdict(lambda: defaultdict(int))
    prio_counts = defaultdict(lambda: defaultdict(int))
    for item in all_rows:
        counts[item["Theme"]][item["AutomationStatus"]] += 1
        counts[item["Theme"]]["Total"] += 1
        prio_counts[item["Theme"]][item["Priority"]] += 1
        prio_counts["ALL"][item["Priority"]] += 1

    ws = wb.create_sheet("00_Summary", 0)
    ws["A1"] = "Receivables Regression Cases — Process-only (evidence-strict)"
    ws["A1"].font = Font(bold=True, size=14)
    ws["A2"] = "Scope anchor: Jira epic REG-56 Receivables Management, plus Confluence Phase 2 Bundle 6 processes that are not object forms."
    ws["A3"] = "Jira source: REST search/jql on oppa-support (full Test Case list). MCP confirmed REG-56 children."
    ws["A4"] = "Confluence source: REST fallback (asterbit.atlassian.net/wiki). MCP timed out."
    ws["A5"] = "Diagrams: Cursor-Project/config/Diagrams/Bundle 6/*.semantic.md"
    ws["A6"] = "SCOPE: direct receivable outcome only — offset, payment, reversal, deposit liability/refund, fine, reschedule, disconnection/reconnection, reminder document, blocking of those outcomes."
    ws["A7"] = f"Jira candidates in the keyword dump: {before}. Jira leaves kept: {kept_jira}. Object CRUD, listing, permissions, interim-and-advance, and billing-owned invoice leaves were removed."
    ws["A8"] = "Excluded on purpose: collection-channel create, interest-rate create, customer-assessment create, bare create payment/receivable/liability, goods/service orders, invoice correction/cancellation (those stay in the billing catalog)."
    ws["A9"] = f"Confluence process pages requested: {len(CONF_PAGES)}. Fetch failures: {conf_failed}."
    ws["A10"] = "Priority: Highest, then High, Medium, Low. Within the same priority: No automation, then Partial, then Yes."
    ws["A12"] = "Theme"
    headers_sum = ["Total", "Yes", "Partial", "No", "Highest", "High", "Medium", "Low"]
    for i, name in enumerate(headers_sum, 2):
        ws.cell(12, i, name)
    for cell in ws[12]:
        if cell.value:
            cell.fill = HEADER_FILL
            cell.font = HEADER_FONT
    r_i = 13
    total = defaultdict(int)
    for theme in SHEETS:
        if theme.startswith("00_"):
            continue
        c = counts.get(theme, {})
        pc = prio_counts.get(theme, {})
        ws.cell(r_i, 1, theme)
        values = [c.get("Total", 0), c.get("Yes", 0), c.get("Partial", 0), c.get("No", 0), pc.get("Highest", 0), pc.get("High", 0), pc.get("Medium", 0), pc.get("Low", 0)]
        for col, value in enumerate(values, 2):
            ws.cell(r_i, col, value)
        total["Total"] += values[0]
        total["Yes"] += values[1]
        total["Partial"] += values[2]
        total["No"] += values[3]
        r_i += 1
    ws.cell(r_i, 1, "ALL_THEMES")
    ws.cell(r_i, 2, total["Total"])
    ws.cell(r_i, 3, total["Yes"])
    ws.cell(r_i, 4, total["Partial"])
    ws.cell(r_i, 5, total["No"])
    ws.cell(r_i, 6, prio_counts["ALL"].get("Highest", 0))
    ws.cell(r_i, 7, prio_counts["ALL"].get("High", 0))
    ws.cell(r_i, 8, prio_counts["ALL"].get("Medium", 0))
    ws.cell(r_i, 9, prio_counts["ALL"].get("Low", 0))
    for col in range(1, 10):
        ws.column_dimensions[get_column_letter(col)].width = 28

    write_sheet("00_Unmapped_Gaps", gap_rows)
    by_theme = defaultdict(list)
    for item in all_rows:
        by_theme[item["Theme"]].append(item)
    for theme in SHEETS:
        if theme.startswith("00_"):
            continue
        write_sheet(theme, by_theme.get(theme, []))

    wb.save(OUT_XLSX)
    OUT_MD.write_text(
        f"""# Receivables Regression Coverage — Index (Process-only)

**Single source of truth:** [`Receivables_Regression_Cases.xlsx`](Receivables_Regression_Cases.xlsx)

## Scope
Same rule as the billing catalog. A row stays only when it asserts a receivable-process outcome: offset, payment posted or reversed, deposit liability or refund, late-payment fine, rescheduling, disconnection or reconnection, the reminder document, or a block that stops one of those.

Removed: object create/edit forms, listing, permissions, translations, interim-and-advance setup, and invoice correction/cancellation leaves (those stay in the billing catalog).

## Evidence
| EvidenceType | How we know |
|---|---|
| **Jira** | REG Test Case summary. Epic REG-56 is the process list. |
| **Confluence** | Outcome sentence from a Phase 2 Bundle 6 process page |
| **Diagram** | Decision or outcome step from Bundle 6 `.semantic.md` |
| **Playwright** | Asserting `test(...)` under `tests/receivableManagement` or a matching cursor leaf |

Jira source: REST search. Confluence source: REST fallback (`asterbit.atlassian.net/wiki`).

## Totals
- Rows: {total['Total']}
- Jira leaves kept: {kept_jira} (from {before} keyword candidates)
- Automation Yes / Partial / No: {total['Yes']} / {total['Partial']} / {total['No']}
- Priority Highest/High/Medium/Low: {prio_counts['ALL'].get('Highest', 0)} / {prio_counts['ALL'].get('High', 0)} / {prio_counts['ALL'].get('Medium', 0)} / {prio_counts['ALL'].get('Low', 0)}
""",
        encoding="utf-8",
    )
    print(f"jira_kept={kept_jira} from {before}")
    print(f"rows={total['Total']} yes={total['Yes']} partial={total['Partial']} no={total['No']}")
    print(f"conf_failed={conf_failed}")
    print(f"saved={OUT_XLSX}")


if __name__ == "__main__":
    main()
