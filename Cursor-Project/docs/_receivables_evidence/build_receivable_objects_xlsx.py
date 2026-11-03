# -*- coding: utf-8 -*-
"""Object-layer regression cases for every Receivables Management object."""
from __future__ import annotations

import importlib.util
import json
import re
from collections import defaultdict
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
OUT_XLSX = ROOT / "docs" / "Receivables_Object_Cases.xlsx"
OUT_MD = ROOT / "docs" / "Receivables_Object_Cases_Catalog.md"
JIRA_JSON = HERE / "jira_receivables_test_cases.json"

spec = importlib.util.spec_from_file_location("recv", HERE / "build_receivables_xlsx.py")
recv = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recv)

SHEETS = [
    "00_Summary",
    "00_Unmapped_Gaps",
    "01_Customer_Liability",
    "02_Customer_Receivable",
    "03_Deposit",
    "04_Payment",
    "05_Payment_Package",
    "06_Collection_Channel",
    "07_Manual_Liability_Offsetting",
    "08_Reminder",
    "09_Reminder_For_Disconnection",
    "10_Request_For_Disconnection",
    "11_Cancel_Disconnection",
    "12_Disconnection",
    "13_Reconnection",
    "14_Interest_Rate",
    "15_Late_Payment_Fine",
    "16_Mass_Blocking",
    "17_Customer_Assessment",
    "18_Rescheduling",
    "19_Coordinator_Objection",
    "20_Objection_Withdrawal",
]

HEADERS = recv.HEADERS
PRIORITY_RANK = recv.PRIORITY_RANK

MARKERS = [
    (re.compile(r"objection withdrawal"), "20_Objection_Withdrawal"),
    (re.compile(r"coordinator objection|objection to a change|objection after a claimed"), "19_Coordinator_Objection"),
    (re.compile(r"customer assessment|\bassessment\b"), "17_Customer_Assessment"),
    (re.compile(r"interest rate"), "14_Interest_Rate"),
    (re.compile(r"payment package"), "05_Payment_Package"),
    (re.compile(r"collection channel"), "06_Collection_Channel"),
    (re.compile(r"payment mass import|offline payment|online payment|payment reverse|reverse payment|create payment|\bpayment\b"), "04_Payment"),
    (re.compile(r"block for|block interest|block list|mass operation for blocking"), "16_Mass_Blocking"),
    (re.compile(r"reschedul"), "18_Rescheduling"),
    (re.compile(r"late payment fine|\blpf\b"), "15_Late_Payment_Fine"),
    (re.compile(r"trigger is rfd|reminder for disconnection"), "09_Reminder_For_Disconnection"),
    (re.compile(r"reminder"), "08_Reminder"),
    (re.compile(r"cancel+ation of request|cancellation of (a |the )?request|cancellation of a disconnection"), "11_Cancel_Disconnection"),
    (re.compile(r"request for disconnection"), "10_Request_For_Disconnection"),
    (re.compile(r"reconnection"), "13_Reconnection"),
    (re.compile(r"disconnection"), "12_Disconnection"),
    (re.compile(r"deposit"), "03_Deposit"),
    (re.compile(r"\bmlo\b|manual liability"), "07_Manual_Liability_Offsetting"),
    (re.compile(r"customer receivable|receivable generation|create customer receivable"), "02_Customer_Receivable"),
    (re.compile(r"customer liability|liability create"), "01_Customer_Liability"),
]

DROP_RE = re.compile(
    r"(?i)("
    r"interim and advance|price component|for volumes|billing run|"
    r"invoice correction|invoice cancellation|invoice reversal|on regeneration|"
    r"point of delivery|re-signing|sales portal|\bget\s*:|list of all|"
    r"\blisting\b|\bsorting\b|\bpaging\b|\bpermission\b|\btranslates?\b|\bpreview\b|"
    r"dropdown filters|search and|task create"
    r")"
)

CONF_PAGES = [
    ("585696596", "Phase 2 - Customer Liability - Create (2)", "01_Customer_Liability"),
    ("623804417", "Phase 2 - Customer liability automatic creation (Claimed penalty)", "01_Customer_Liability"),
    ("585696679", "Phase 2 - Customer liability automatic creation action (Deposit) (2)", "01_Customer_Liability"),
    ("585696788", "Phase 2 - Customer Receivable - Create (2)", "02_Customer_Receivable"),
    ("585696866", "Phase 2 - Customer receivable automatic creation (deposit) (2)", "02_Customer_Receivable"),
    ("585696959", "Phase 2 - Deposit - Create (2)", "03_Deposit"),
    ("585696977", "Phase 2 - Deposit - Edit (2)", "03_Deposit"),
    ("585697034", "Phase 2 - Deposit Refunding/Deleting (2)", "03_Deposit"),
    ("585697170", "Phase 2 - Payment - Create (2)", "04_Payment"),
    ("585697466", "Phase 2 - Online Payment- Create Payment (2)", "04_Payment"),
    ("585697488", "Phase 2 - Payment package Create (2)", "05_Payment_Package"),
    ("585697551", "Phase 2 - Collection channel - Create (2)", "06_Collection_Channel"),
    ("585697669", "Phase 2 - Manual Liabilities offsetting - Create (2)", "07_Manual_Liability_Offsetting"),
    ("585698427", "Phase 2 - Create Reminder (2)", "08_Reminder"),
    ("585698348", "Phase 2 - Reminder for disconnection of power supply - Create (2)", "09_Reminder_For_Disconnection"),
    ("585697986", "Phase 2 - Request for disconnection of the power supply - Create (2)", "10_Request_For_Disconnection"),
    ("585698098", "Phase 2 - Request for disconnection liabilities selecting logic (2)", "10_Request_For_Disconnection"),
    ("585698120", "Phase 2 - Cancellation of a disconnection of the power supply - Create (2)", "11_Cancel_Disconnection"),
    ("585698196", "Phase 2 - Disconnection of the power supply - Create (2)", "12_Disconnection"),
    ("585698272", "Phase 2 - Reconnection of the power supply - Create (2)", "13_Reconnection"),
    ("585697727", "Phase 2 - Late Payment Fine - Create (2)", "15_Late_Payment_Fine"),
    ("585697796", "Phase 2 - Create Late Payment Fine from Rescheduling", "15_Late_Payment_Fine"),
    ("585697083", "Phase 2 - Mass operations for blocking - Create (2)", "16_Mass_Blocking"),
    ("585698741", "Phase 2 - Create customer assessment (2)", "17_Customer_Assessment"),
    ("585698517", "Phase 2 - Create rescheduling (2)", "18_Rescheduling"),
    ("585699036", "Phase 2 - Rescheduling Calculation (2)", "18_Rescheduling"),
    ("585697846", "Phase 2 - Create Objection to a change of a balancing group coordinator (2)", "19_Coordinator_Objection"),
    ("585697916", "Phase 2 - Create Objection withdrawal to a change of a balancing group coordinator (2)", "20_Objection_Withdrawal"),
]

FILE_THEME = [
    ("deposit", "03_Deposit"),
    ("paymentpackage", "05_Payment_Package"),
    ("collectionchannel", "06_Collection_Channel"),
    ("manualliability", "07_Manual_Liability_Offsetting"),
    ("offset", "07_Manual_Liability_Offsetting"),
    ("reminderfordisconnection", "09_Reminder_For_Disconnection"),
    ("reminder", "08_Reminder"),
    ("requestfordisconnection", "10_Request_For_Disconnection"),
    ("cancelationofrequest", "11_Cancel_Disconnection"),
    ("reconnection", "13_Reconnection"),
    ("disconnection", "12_Disconnection"),
    ("interestrate", "14_Interest_Rate"),
    ("latepayment", "15_Late_Payment_Fine"),
    ("blocking", "16_Mass_Blocking"),
    ("rescheduling", "18_Rescheduling"),
    ("payment", "04_Payment"),
    ("customerreceivable", "02_Customer_Receivable"),
    ("receivable", "02_Customer_Receivable"),
    ("customerliability", "01_Customer_Liability"),
    ("liability", "01_Customer_Liability"),
]


def append_offsetting_test_cases(wb, summary, after_row: int) -> int:
    """Add automatic and direct offsetting test cases as their own sheets."""
    spec_off = importlib.util.spec_from_file_location("offset_scope", HERE / "build_offsetting_scope_xlsx.py")
    offset = importlib.util.module_from_spec(spec_off)
    spec_off.loader.exec_module(offset)
    _, tc_rows = offset.test_cases()
    names = (
        ("Automatic", "21_TC_Automatic"),
        ("Direct", "22_TC_Direct"),
    )
    for process, sheet_name in names:
        if sheet_name in wb.sheetnames:
            del wb[sheet_name]
        offset.write_tc_sheet(wb, sheet_name, [item for item in tc_rows if item[0] == process])
    auto_n = sum(1 for item in tc_rows if item[0] == "Automatic")
    direct_n = sum(1 for item in tc_rows if item[0] == "Direct")
    summary.cell(after_row + 2, 1, "21_TC_Automatic and 22_TC_Direct are the automatic and direct offsetting test cases.")
    summary.cell(after_row + 3, 1, "Automatic cases: %s. Direct cases: %s. One scenario per row. These sheets are not extra objects." % (auto_n, direct_n))
    summary.cell(after_row + 2, 1).alignment = Alignment(wrap_text=True, vertical="center")
    summary.cell(after_row + 3, 1).alignment = Alignment(wrap_text=True, vertical="center")
    return len(tc_rows)


def theme_for(title: str) -> str | None:
    low = title.lower()
    if DROP_RE.search(title) or "interim" in low:
        return None
    if re.search(r"trigger is rfd|reminder for disconnection", low):
        return "09_Reminder_For_Disconnection"
    hits = []
    for pattern, theme in MARKERS:
        match = pattern.search(low)
        if match:
            hits.append((match.start(), theme))
    if not hits:
        return None
    hits.sort()
    theme = hits[0][1]
    if theme == "06_Collection_Channel" and re.search(r"online payment|offline payment|systech|bulgarian post|easy-pay|cash terminal|virtual pos", low):
        return "04_Payment"
    if theme == "17_Customer_Assessment" and "task create" in low:
        return None
    return theme


def priority_for(title: str, evidence: str) -> tuple[str, str]:
    low = title.lower()
    if evidence == "Playwright":
        return "Highest", "Object automation leaf"
    if evidence == "Diagram":
        return "Highest", "Diagram outcome on this object"
    if re.search(r"(?i)(negative|refused|should not|must not|edit - when|without )", title):
        return "Medium", "Validation or blocked edit"
    if re.search(r"customer assessment|\bassessment\b", low) and "offset" not in low:
        return "Medium", "Assessment does not post a balance"
    if re.search(r"collection channel|interest rate|payment package", low) and not re.search(r"offset|online payment|offline payment", low):
        return "High", "Object is an entrance. It does not itself post the balance"
    if re.search(r"reminder|request for disconnection", low) and not re.search(r"disconnection of power|reconnection|fee", low):
        return "High", "Document or request. It does not set the supply flag"
    if re.search(r"deposits create|deposits edit|deposit - create", low) and "process" not in low and "offset" not in low and "refund" not in low:
        return "High", "Deposit object save. The liability or refund is a separate row"
    if re.search(r"offset|reverse|refund|liability creation|fine|reschedul|disconnection of power|reconnection|payment|block for|block interest|mlo|liability create|customer receivable|customer liability", low):
        return "Highest", "Object posts or changes a balance, a fine, a schedule, or supply"
    if evidence == "Confluence":
        return "High", "Confluence rule for saving this object"
    return "High", "Object save or edit"


def theme_for_file(path: str) -> str | None:
    compact = path.lower().replace("\\", "/").replace("_", "").replace("-", "")
    for key, theme in FILE_THEME:
        if key in compact:
            return theme
    return None


def main() -> None:
    jira = json.loads(JIRA_JSON.read_text(encoding="utf-8-sig"))
    tests = recv.index_playwright()
    before = len(jira)
    rows = []
    kept = 0
    for issue in jira:
        summary = issue.get("Summary") or ""
        theme = theme_for(summary)
        if not theme:
            continue
        kept += 1
        status, aname, afile, note = recv.match_automation(issue["Key"], tests)
        if issue.get("Status"):
            note = (note + "; " if note else "") + f"Jira status: {issue['Status']}"
        rows.append(
            recv.row(theme, issue["Key"], summary, "Jira", f"{issue['Key']} | type=Test Case", summary, status, aname, afile, note)
        )

    for test in tests:
        if test["skipped"]:
            continue
        theme = theme_for(test["title"]) or theme_for_file(test["file"])
        if theme is None or DROP_RE.search(test["title"]):
            continue
        case_id = test["ids"][0] if test["ids"] else Path(test["file"]).stem
        rows.append(
            recv.row(
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

    for item in recv.diagram_rows():
        theme = theme_for(item["CaseTitle"]) or theme_for(item["EvidenceCitation"])
        if not theme:
            continue
        item = dict(item)
        item["Theme"] = theme
        rows.append(item)

    env = recv.load_env()
    conf_failed = 0
    for page_id, title, theme in CONF_PAGES:
        citation = f"{title} | page {page_id} | https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/{page_id}"
        try:
            text = recv.html_to_text(recv.confluence_storage(page_id, env))
        except Exception as exc:
            conf_failed += 1
            rows.append(recv.row(theme, f"CONF-{page_id}", title, "Confluence", citation, "", "No", "", "", f"Page fetch failed: {type(exc).__name__}"))
            continue
        found = recv.sentences(text)
        if not found:
            rows.append(recv.row(theme, f"CONF-{page_id}", title, "Confluence", citation, text[:200], "No", "", "", "No should/must sentence extracted"))
            continue
        for i, quote in enumerate(found, 1):
            rows.append(recv.row(theme, f"CONF-{page_id}-{i}", quote, "Confluence", citation, quote, "No", "", "", "Verbatim should/must from the object page"))

    dedup = {}
    for item in rows:
        dedup[(item["EvidenceType"], item["CaseId"], item["EvidenceQuote"][:80])] = item
    rows = list(dedup.values())

    covered = defaultdict(int)
    for item in rows:
        if item["EvidenceType"] == "Jira" and item["AutomationStatus"] == "Yes":
            covered[item["Theme"]] += 1
    for item in rows:
        if item["EvidenceType"] == "Jira" and item["AutomationStatus"] == "No" and covered[item["Theme"]]:
            item["AutomationStatus"] = "Partial"
            extra = f"Partial: {item['Theme']} has another covered case; this leaf has no asserting test"
            item["Notes"] = (item["Notes"] + "; " if item["Notes"] else "") + extra
        prio, reason = priority_for(item["CaseTitle"], item["EvidenceType"])
        item["Priority"] = prio
        item["PriorityReason"] = reason
        item["_prio_rank"] = PRIORITY_RANK[prio]

    gaps = []
    seen = set()
    for item in rows:
        if item["AutomationStatus"] != "No" or item["EvidenceType"] not in ("Confluence", "Diagram"):
            continue
        key = (item["CaseId"], item["EvidenceQuote"][:60])
        if key in seen:
            continue
        seen.add(key)
        gaps.append(dict(item, Theme="00_Unmapped_Gaps"))

    wb = Workbook()
    wb.remove(wb.active)
    header_fill = recv.HEADER_FILL
    header_font = recv.HEADER_FONT

    def write_sheet(name: str, items: list[dict]) -> None:
        ws = wb.create_sheet(name)
        ws.append(HEADERS)
        for col in range(1, len(HEADERS) + 1):
            cell = ws.cell(1, col)
            cell.fill = header_fill
            cell.font = header_font
            cell.alignment = Alignment(wrap_text=True, vertical="center")
        for item in recv.sort_theme_rows(items):
            ws.append([item.get(h, "") for h in HEADERS])
            status = item.get("AutomationStatus")
            fill = recv.YES_FILL if status == "Yes" else recv.PARTIAL_FILL if status == "Partial" else recv.NO_FILL
            ws.cell(ws.max_row, HEADERS.index("AutomationStatus") + 1).fill = fill
            pfill = recv.PRIO_FILL.get(item.get("Priority"))
            if pfill:
                ws.cell(ws.max_row, HEADERS.index("Priority") + 1).fill = pfill
        for col in range(1, len(HEADERS) + 1):
            ws.column_dimensions[get_column_letter(col)].width = min(48, max(14, len(HEADERS[col - 1]) + 2))
        ws.auto_filter.ref = ws.dimensions
        ws.freeze_panes = "A2"

    counts = defaultdict(lambda: defaultdict(int))
    prios = defaultdict(lambda: defaultdict(int))
    for item in rows:
        counts[item["Theme"]][item["AutomationStatus"]] += 1
        counts[item["Theme"]]["Total"] += 1
        prios[item["Theme"]][item["Priority"]] += 1
        prios["ALL"][item["Priority"]] += 1

    ws = wb.create_sheet("00_Summary", 0)
    ws["A1"] = "Receivables Object Cases — one sheet per object"
    ws["A1"].font = Font(bold=True, size=14)
    ws["A2"] = "Objects are the Create-page objects under Confluence Phase 2 Bundle 6 and Jira epic REG-56."
    ws["A3"] = "Same evidence rule as the process catalog: every row cites Jira, Confluence, a diagram, or a Playwright test."
    ws["A4"] = "Jira source: REST search/jql on oppa-support. Confluence source: REST fallback (asterbit.atlassian.net/wiki)."
    ws["A5"] = "Dropped: listing, sorting, paging, permissions, translations, preview, get-by-id, and billing objects (interim and advance, price component, invoice correction)."
    ws["A6"] = "Kept: create, edit, and the business result of that object (balance, offset, reversal, fine, reschedule, supply, reminder document)."
    ws["A7"] = f"Jira candidates in the receivables dump: {before}. Jira leaves kept: {kept}. Confluence pages: {len(CONF_PAGES)}. Fetch failures: {conf_failed}."
    ws["A8"] = "Process-only workbook remains Receivables_Regression_Cases.xlsx. A case can appear here as well when the object itself is what the case saves or changes."
    ws["A9"] = "Priority: Highest = the object posts or changes a balance, fine, schedule, or supply. High = the object is saved but does not itself post that balance. Medium = validation or blocked edit."
    ws["A12"] = "Object"
    for i, name in enumerate(["Total", "Yes", "Partial", "No", "Highest", "High", "Medium", "Low"], 2):
        ws.cell(12, i, name)
        ws.cell(12, i).fill = header_fill
        ws.cell(12, i).font = header_font
    ws.cell(12, 1).fill = header_fill
    ws.cell(12, 1).font = header_font
    r_i = 13
    total = defaultdict(int)
    for theme in SHEETS:
        if theme.startswith("00_"):
            continue
        c = counts.get(theme, {})
        p = prios.get(theme, {})
        values = [c.get("Total", 0), c.get("Yes", 0), c.get("Partial", 0), c.get("No", 0), p.get("Highest", 0), p.get("High", 0), p.get("Medium", 0), p.get("Low", 0)]
        ws.cell(r_i, 1, theme)
        for col, value in enumerate(values, 2):
            ws.cell(r_i, col, value)
        total["Total"] += values[0]
        total["Yes"] += values[1]
        total["Partial"] += values[2]
        total["No"] += values[3]
        r_i += 1
    ws.cell(r_i, 1, "ALL_OBJECTS")
    ws.cell(r_i, 2, total["Total"])
    ws.cell(r_i, 3, total["Yes"])
    ws.cell(r_i, 4, total["Partial"])
    ws.cell(r_i, 5, total["No"])
    ws.cell(r_i, 6, prios["ALL"].get("Highest", 0))
    ws.cell(r_i, 7, prios["ALL"].get("High", 0))
    ws.cell(r_i, 8, prios["ALL"].get("Medium", 0))
    ws.cell(r_i, 9, prios["ALL"].get("Low", 0))
    for col in range(1, 10):
        ws.column_dimensions[get_column_letter(col)].width = 32

    write_sheet("00_Unmapped_Gaps", gaps)
    by_theme = defaultdict(list)
    for item in rows:
        by_theme[item["Theme"]].append(item)
    for theme in SHEETS:
        if theme.startswith("00_"):
            continue
        write_sheet(theme, by_theme.get(theme, []))
    append_offsetting_test_cases(wb, ws, r_i)
    wb.save(OUT_XLSX)
    OUT_MD.write_text(
        f"""# Receivables Object Cases — Index

**Workbook:** [`Receivables_Object_Cases.xlsx`](Receivables_Object_Cases.xlsx)

One sheet per receivables object. Listing, permissions, translations, and preview are excluded. Each row cites Jira, Confluence, a diagram, or a Playwright test.

- Rows: {total['Total']}
- Jira leaves kept: {kept} from {before}
- Automation Yes / Partial / No: {total['Yes']} / {total['Partial']} / {total['No']}

Sheets `21_TC_Automatic` and `22_TC_Direct` hold the automatic and direct offsetting test cases. They are not extra objects.

Jira source: REST search. Confluence source: REST fallback (`asterbit.atlassian.net/wiki`).
""",
        encoding="utf-8",
    )
    print(f"kept={kept} from {before} rows={total['Total']} yes={total['Yes']} partial={total['Partial']} no={total['No']} conf_failed={conf_failed}")
    print(OUT_XLSX)


if __name__ == "__main__":
    main()
