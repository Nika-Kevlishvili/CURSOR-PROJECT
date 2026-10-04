# -*- coding: utf-8 -*-
"""Build evidence-strict Billing_Regression_Cases.xlsx from mined JSON inputs."""
from __future__ import annotations

import json
import re
from collections import defaultdict
from pathlib import Path

from openpyxl import Workbook
from openpyxl.styles import Alignment, Font, PatternFill
from openpyxl.utils import get_column_letter

ROOT = Path(__file__).resolve().parents[2]  # Cursor-Project
EVID = Path(__file__).resolve().parent
OUT_XLSX = ROOT / "docs" / "Billing_Regression_Cases.xlsx"
OUT_XLSX_FALLBACK = ROOT / "docs" / "Billing_Regression_Cases_NEW.xlsx"
OUT_MD = ROOT / "docs" / "Billing_Regression_Ideal_Coverage_Catalog.md"

SHEETS = [
    "00_Summary",
    "00_Unmapped_Gaps",
    "01_Periodic_Start",
    "02_For_Volumes",
    "03_Pulling",
    "04_OverTime",
    "05_PerPiece",
    "06_With_Electricity",
    "07_Interim",
    "08_Discount",
    "09_Slot_Splitting",
    "10_Correction",
    "11_Reversal",
    "12_Cancellation",
    "13_Compensation",
    "14_Accounting_Period",
    "15_Conditions_ManualBR",
    "16_CrossCutting",
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

# Highest = direct billing process. Lower number = higher priority for sorting.
PRIORITY_RANK = {"Highest": 0, "High": 1, "Medium": 2, "Low": 3}

HEADER_FILL = PatternFill("solid", fgColor="1F4E79")
HEADER_FONT = Font(color="FFFFFF", bold=True)
YES_FILL = PatternFill("solid", fgColor="C6EFCE")
NO_FILL = PatternFill("solid", fgColor="FFC7CE")
PARTIAL_FILL = PatternFill("solid", fgColor="FFEB9C")
HIGHEST_FILL = PatternFill("solid", fgColor="F4B183")
HIGH_FILL = PatternFill("solid", fgColor="FFE699")
MEDIUM_FILL = PatternFill("solid", fgColor="DDEBF7")
LOW_FILL = PatternFill("solid", fgColor="E7E6E6")

# Feature/process container keys (billing run processes)
PROCESS_CONTAINER_KEYS = {
    "REG-55",
    "REG-106",
    "REG-107",
    "REG-108",
    "REG-109",
    "REG-110",
    "REG-558",
    "REG-559",
    "REG-560",
    "REG-561",
    "REG-562",
    "REG-563",
    "REG-564",
    "REG-565",
    "REG-566",
    "REG-633",
    "REG-635",
    "REG-964",
    "REG-993",
    "REG-1019",
    "REG-1050",
    "REG-1059",
    "REG-1064",
}

PROCESS_TITLE_RE = re.compile(
    r"(?i)\b("
    r"billing\s*run|start\s*billing|generate\s*(invoice|pdf|document)|"
    r"invoice\s*(correction|reversal|cancellation|cancelation)|"
    r"for\s*volumes|pulling|over\s*time|per\s*piece|with\s*electricity|"
    r"interim(\s+and\s+advance)?|discount\s*(calculation|flow|process)?|"
    r"slot\s*splitting|government\s*compensation|compensation\s*(defer|link|pdf)|"
    r"accounting\s*period|manual\s*(billing|invoice|credit|debit)|"
    r"process\s*periodicity|one[- ]time\s*billing|periodic\s*billing|"
    r"create\s*invoice|pdf\s*start|draft\s*generation|"
    r"volume\s*change\s*flow|price\s*change\s*flow"
    r")\b"
)

CRUD_SETUP_RE = re.compile(
    r"(?i)\b("
    r"create\s*-\s*price\s*component|edit\s*-\s*price\s*component|"
    r"create\s*-\s*iap|edit\s*-\s*iap|copy\s*from|"
    r"happy\s*path|full\s*data|"
    r"sortable|columns?\s*should|load\s*list|filter\s*information|"
    r"logical\s*operator|return\s*true\s*if\s*condition|"
    r"bodyempty|ui\s*only|listing"
    r")\b"
)

# Object master-data / UI / field-validation — EXCLUDE from process-only workbook
OBJECT_OR_UI_EXCLUDE_RE = re.compile(
    r"(?i)("
    r"create\s*-\s*(price\s*component|point\s*of\s*delivery|iap|interim\s*and\s*advance|"
    r"product\s*contract|service\s*contract|goods\s*order|service\s*order|pod\b)|"
    r"edit\s*-\s*(price\s*component|point\s*of\s*delivery|iap|interim\s*and\s*advance|"
    r"product\s*contract|service\s*contract|goods\s*order|service\s*order|pod\b)|"
    r"delete\s*-\s*(price\s*component|point\s*of\s*delivery|iap)|"
    r"conditions\s*-\s*price\s*component|"
    r"hardcoded\s*conditions|"
    r"logical\s*operator|"
    r"return\s*true\s*if\s*condition|"
    r"should\s*load\s*list|"
    r"columns?\s*should\s*be\s*sortable|"
    r"display\s+\d+\s+records|"
    r"possibility\s+to\s+change\s+the\s+number|"
    r"filter\s+information\s+in\s+order\s+to\s+create|"
    r"mandatory\s+fields|"
    r"bodyempty|"
    r"\[BodyEmpty\]|"
    r"copy\s+from|"
    r"nomenclature|"
    r"portal\s+tag|"
    r"communication\s+data"
    r")"
)

# Keep even if some UI words appear, when clearly a billing process outcome
PROCESS_KEEP_FORCE_RE = re.compile(
    r"(?i)\b("
    r"for\s+volumes|pulling|over\s*time|per\s*piece|with\s*electricity|"
    r"invoice\s*(correction|reversal|cancellation|cancelation)|"
    r"billing\s*run|start\s*billing|generate\s*(invoice|pdf)|"
    r"interim(\s+and\s+advance)?\s*(payment|billing|generation|flow|deduction)|"
    r"government\s*compensation|compensation\s*(defer|link|pdf|invoiced)|"
    r"slot\s*splitting|discount\s*(calculation|flow|process|in\s*billing)|"
    r"accounting\s*period|manual\s*(billing|invoice|credit|debit|interim)|"
    r"connected\s*invoice|payment\s*deadline|rps\s*due|"
    r"volume\s*change\s*flow|price\s*change\s*flow|"
    r"pdf\s*start|draft\s*generation|one[- ]time\s*billing|periodic\s*billing\s*run"
    r")\b"
)

CALC_CRITICAL_RE = re.compile(
    r"(?i)\b("
    r"scale|slp|restriction|combined\s*flow|tariff|"
    r"amount|vat|rounding|liability|receivable|offset|"
    r"due\s*date|payment\s*deadline|rps|"
    r"price\s*condition|connected\s*invoice|detailed[- ]data|"
    r"minimal\s*(amount|interim)|threshold|"
    r"volume\s*(delta|change)|billing[- ]by[- ]profile"
    r")\b"
)

ERROR_VAL_RE = re.compile(
    r"(?i)\b("
    r"error|validation|should\s*not|must\s*not|reject|http\s*400|"
    r"gap|conflict|isolation|unlinked|uninvoiced|"
    r"decision:|save:|error\s*state"
    r")\b"
)


def assign_priority(r: dict) -> tuple[str, str]:
    """Return (Priority Highest|High|Medium|Low, PriorityReason)."""
    cid = r.get("CaseId") or ""
    title = f"{r.get('CaseTitle') or ''} {r.get('EvidenceQuote') or ''} {r.get('Notes') or ''}"
    et = r.get("EvidenceType") or ""
    kind_note = r.get("Notes") or ""

    base_id = cid.split("/")[0]

    # Playwright asserting leaves under process tickets → Highest
    if et == "Playwright":
        if PROCESS_TITLE_RE.search(title) or base_id.startswith(("PDT-", "REG-", "RPS-")):
            return "Highest", "Process automation leaf (billing/cursor suite)"
        return "High", "Automation leaf — money/cross-cutting impact assumed"

    # Diagram: Decision/Save on process flows = Highest; Error = Medium
    if et == "Diagram":
        if PROCESS_TITLE_RE.search(title) or "Kind=Decision" in kind_note or "Kind=Save" in kind_note:
            if ERROR_VAL_RE.search(title) and "Kind=Error" in kind_note:
                return "Medium", "Diagram error path on process"
            if "Kind=Error" in kind_note:
                return "Medium", "Diagram error state"
            return "Highest", "Diagram process decision/save"
        if "Kind=Error" in kind_note or ERROR_VAL_RE.search(title):
            return "Medium", "Diagram error/validation"
        return "High", "Diagram supporting rule"

    # BodyEmpty / pure UI listing → Low
    if cid.startswith("CONF-EMPTY") or "BodyEmpty" in title:
        return "Low", "Empty Confluence body — no executable process rule"
    if CRUD_SETUP_RE.search(title) and not PROCESS_TITLE_RE.search(title):
        return "Low", "Setup/CRUD/UI listing — not the billing process itself"

    # Process-titled Jira/Confluence → Highest
    if PROCESS_TITLE_RE.search(title):
        return "Highest", "Billing process flow (run/generate/correct/cancel/compensate/…)"

    # Critical calculation / financial
    if CALC_CRITICAL_RE.search(title):
        return "High", "Critical calculation / financial / due-date impact"

    # Validation / error
    if ERROR_VAL_RE.search(title):
        return "Medium", "Validation / error / edge path"

    # Default by evidence type
    if et == "Jira":
        return "Medium", "Jira case — supporting (not clearly process/calc)"
    if et == "Confluence":
        return "Low", "Confluence rule — supporting/docs (not clearly process)"
    return "Medium", "Default supporting priority"


def is_process_case(r: dict) -> bool:
    """Keep ONLY cases with direct effect on billing/invoice outcomes.

    Drop: object CRUD/UI, listing chrome, permissions, BR numbering,
    IAP/Discount master-data setup, condition-builder UI, AP auto-create object.
    """
    cid = r.get("CaseId") or ""
    title = f"{r.get('CaseTitle') or ''} {r.get('EvidenceQuote') or ''}"
    et = r.get("EvidenceType") or ""
    notes = r.get("Notes") or ""
    cite = r.get("EvidenceCitation") or ""
    base_id = cid.split("/")[0]
    t = title.strip()

    if cid.startswith("CONF-EMPTY") or "[BodyEmpty]" in title:
        return False

    # Exclude goods order / service order entirely (out of this catalog scope)
    if re.search(r"(?i)\b(goods\s*order|service\s*order|pro\s*forma\s*invoice.*order)\b", title):
        return False
    if re.search(r"(?i)(from\s+goods\s+order|from\s+service\s+order|goods\s+orders?\s+status|service\s+orders?\s+status)", title):
        return False
    theme0 = r.get("Theme") or ""
    cite0 = (r.get("EvidenceCitation") or "") + " " + (r.get("AutomationFile") or "")
    if re.search(r"(?i)(goods.?order|service.?order)", cite0) and "perPiece" not in cite0.replace("\\", "/"):
        return False
    # legacy sheet name / theme remapped
    if theme0 in {"05_PerPiece_Orders"} and re.search(r"(?i)(goods|service)\s*order", title):
        return False

    # --- Hard exclude: objects / UI / meta (never keep) ---
    if re.search(
        r"(?i)^\s*(create|edit|delete)\s*-\s*(price\s*component|point\s*of\s*delivery|iap|"
        r"interim\s*and\s*advance|product\s*contract|service\s*contract|pod\b)",
        t,
    ):
        return False
    if re.search(r"(?i)\b(create|edit|delete)\s*-\s*price\s*component\s*-\s*", title):
        return False

    if re.search(
        r"(?i)("
        # UI / list / permission / numbering
        r"should\s*load\s*list|columns?\s*should\s*be\s*sortable|display\s+\d+\s+records|"
        r"latest\s+\w+.*load\s+first|in\s+each\s+row\s+in\s+.*list|"
        r"permission\s+'|check\s+system\s+user\s+permission|additional\s+permission|"
        r"billing\s*run'?s?\s+number|BILLING\d{5,}|"
        r"if\s+user\s+deletes\s+already\s+created\s+billing\s+run|"
        r"i\s+want\s+to\s+be\s+able\s+to\s+create\s+a\s+billing\s+run|"
        r"create\s+a\s+new\s+billing\s+run|"
        r"create\s+new\s+periodic\s+billing\s+run|"
        r"for\s+deleting\s+periodic\s+billing\s+run|"
        # IAP / Discount object setup (not invoice outcome)
        r"interim\s+and\s+advance\s+payment\s+object\s+contains|"
        r"during\s+the\s+creation\s+and\s+edit\s+mode|"
        r"obligatory\s+checkbox\s+is\s+selected|"
        r"matched\s+with\s+the\s+term\s+of\s+the\s+standard\s+invoice|"
        r"if\s+at\s+least\s+one\s+checkbox\s+is\s+selected|"
        r"editing\s+without\s+'edit|"
        r"already\s+included\s+in\s+a\s+contract\s+or\s+order,\s+editing|"
        r"from\s+date\s*-|to\s+date\s*-|certificate\s+number|"
        r"when\s+creating\s+a\s+discount|when\s+editing\s+a\s+discount|"
        r"able\s+to\s+create\s+discount|output\s*:\s*we\s+will\s+able\s+to\s+create\s+discount|"
        r"mandatory\s+to\s+complete\s+all\s+required\s+fields\s+across|"
        r"price\s+component\s+1st\s+tab|"
        r"find\s+in\s+system\s+price\s+component|"
        # Condition builder UI
        r"choosing\s+conditions\s+for\s+the\s+billing\s+run|"
        r"text\s+box\s+should\s+define|"
        r"display\s+two\s+hardcoded|"
        r"hardcoded\s+values|"
        r"logical\s+operator|return\s+true\s+if\s+condition|"
        r"conditions\s*-\s*price\s*component|"
        # AP object auto-create / pure object meta
        r"automatically\s+create\s+an\s+accounting\s+period|"
        r"does\s+not\s+include\s+versioning|"
        r"completion\s+of\s+all\s+requir|"
        r"copy\s+from|"
        r"button\s+create\s+invoice\s+appears|"
        r"defined\s+period\s+in\s+which\s+this\s+billing\s+run\s+should\s+be\s+started$|"
        r"after\s+periodic\s+billing\s+run\s+starts\s+its\s+record\s+should\s+be\s+displayed|"
        # Order UI chrome / permissions / examples without outcome
        r"\d+(st|nd|rd|th)\s+tab\s+appears|"
        r"awaiting\s+payment|"
        r"user\s+can\s+delete\s+generated\s+document|"
        r"^permissions?\s*-|"
        r"for\s+example:\s*if\s+there\s+is\s+a\s+condition|"
        r"from\s+old\s+invoice\s+should\s+be\s+taken\s+invoice\s+setting|"
        r"prefix\s+from\s+nom"
        r")",
        title,
    ):
        return False

    # --- Playwright: billing/cursor asserting leaves = direct effect ---
    if et == "Playwright":
        return True

    # --- Process feature containers: never keep as rows (sheets replace them) ---
    if (base_id in PROCESS_CONTAINER_KEYS or cid in PROCESS_CONTAINER_KEYS) and "/" not in cid:
        return False

    direct = re.search(
        r"(?i)\b("
        r"generate\s*(invoice|pdf|document)|start\s*(billing|generate)|"
        r"creates?\s+(the\s+)?(interim\s+)?invoice|"
        r"does\s+not\s+create\s+(the\s+)?(interim\s+)?invoice|"
        r"for\s+volumes|pulling|over\s*time\s*(periodical|one\s*time)?|"
        r"with\s*electricity|per\s*piece|"
        r"invoice\s*(correction|reversal|cancellation|cancelation|slot)|"
        r"correction\s*(billing|flow|process)|reversal\s*(flow|process)|"
        r"standard\s*billing|"
        r"government\s*compensation|compensation\s*(defer|link|pdf|invoiced|uninvoiced)|"
        r"interim\s*(deduction|generation)|"
        r"tariff\s*splitting|scale\s*code|slp\b|"
        r"connected\s*invoice|payment\s*deadline|due\s*date|"
        r"liability|receivable|offset|"
        r"deactivated\s*pod|recalculat|"
        r"pdf\s*(start|document)|draft\s*generation|real\s*invoice|"
        r"proforma.*cancel|cancel.*proforma|cancel(l)?ation|"
        r"slot\s*splitting|split\s+by|"
        r"billing\s+with\s+list\s+of|"
        r"in\s+progress\s+(generation|accounting)|"
        r"status.*\b(generated|completed)\b|"
        r"billing\s+process\s+is\s+started|"
        r"start\s+billing\s+for|"
        r"skip\s+interim\s+deduction|"
        r"invoice\s+amount|total\s+amount\s+including\s+vat|"
        r"measurement\s+type|"  # pulling measurement affects volumes billing
        r"accounting\s+period.*(invoice|open|closed|tax\s+event)"
        r")\b",
        title,
    )

    if et == "Diagram":
        if not direct:
            return False
        # Drop object-tab decisions that don't assert invoice outcome wording
        if re.search(r"(?i)price\s+component\s+1st\s+tab|discount\s+checkbox\s+marked", title):
            return False
        if re.search(
            r"(?i)(billing|invoice|correction|reversal|cancel|interim|compensation|pulling|"
            r"volume|electricity|discount|slot|over.?time|per.?piece|create-.*billing)",
            cite + " " + notes,
        ):
            return True
        return bool(direct)

    if et == "Jira":
        # Drop permission-only REG
        if re.search(r"(?i)^\s*permissions?\s*-", t):
            return False
        # REG test cases / tasks that name a billing process outcome
        if direct:
            return True
        # Theme-scoped billing process REG leftovers (already filtered CRUD)
        theme = r.get("Theme") or ""
        if theme in {
            "02_For_Volumes",
            "03_Pulling",
            "04_OverTime",
            "06_With_Electricity",
            "08_Discount",
            "09_Slot_Splitting",
            "10_Correction",
            "11_Reversal",
            "12_Cancellation",
            "13_Compensation",
        } and CALC_CRITICAL_RE.search(title):
            return True
        return False

    if et == "Confluence":
        # Must have direct invoice/billing effect wording
        if not direct:
            return False
        # Drop condition-builder examples / UI-only leftovers
        if re.search(
            r"(?i)(for\s+example:\s*if\s+there\s+is\s+a\s+condition|"
            r"customer\s+number\s+as\s+a\s+variable|"
            r"\d+(st|nd|rd|th)\s+tab\s+appears|"
            r"awaiting\s+payment|"
            r"permissions?\s*-)",
            title,
        ):
            return False
        return True

    return False


def apply_priorities(rows: list[dict]) -> None:
    for r in rows:
        prio, reason = assign_priority(r)
        r["Priority"] = prio
        r["PriorityReason"] = reason
        r["_prio_rank"] = PRIORITY_RANK.get(prio, 9)


def sort_theme_rows(rows: list[dict]) -> list[dict]:
    return sorted(
        rows,
        key=lambda x: (
            x.get("_prio_rank", 9),
            0 if x.get("AutomationStatus") == "No" else 1 if x.get("AutomationStatus") == "Partial" else 2,
            x.get("CaseId") or "",
            x.get("EvidenceType") or "",
        ),
    )


def load_json(name: str):
    p = EVID / name
    if not p.exists():
        return []
    data = json.loads(p.read_text(encoding="utf-8-sig"))
    return data if isinstance(data, list) else [data]


def norm_id(s: str) -> str:
    return (s or "").strip().upper()


def fix_auto_row(a: dict) -> dict:
    """Attach TicketId from filename; keep leaf AutomationId (incl. TC-BE-*)."""
    a = dict(a)
    aid = a.get("AutomationId") or ""
    f = (a.get("AutomationFile") or "").replace("\\", "/")
    name = a.get("AutomationName") or ""
    ticket = ""
    m = re.search(r"(PDT|REG|PHN)-(\d+)", f, re.I)
    if m:
        ticket = f"{m.group(1).upper()}-{m.group(2)}"
    if "RPS-POD" in f:
        ticket = "RPS-POD"
    a["TicketId"] = ticket
    # Prefer ticket as AutomationId only when bare/templated and not already REG/PDT
    if ticket and (not aid or aid.startswith("TC-BE") or "${" in name):
        if aid.startswith("TC-BE"):
            a["LeafId"] = aid
            a["AutomationId"] = f"{ticket}/{aid}"
        elif not aid or "${" in name:
            a["AutomationId"] = ticket
            tc = re.search(r"TC-BE-\d+", name, re.I)
            if tc:
                a["LeafId"] = tc.group(0).upper()
                a["AutomationId"] = f"{ticket}/{a['LeafId']}"
    else:
        tc = re.search(r"TC-BE-\d+", name, re.I) or re.search(r"TC-BE-\d+", a.get("AllIds") or "", re.I)
        if tc and ticket:
            a["LeafId"] = tc.group(0).upper()
        else:
            a["LeafId"] = ""
    if "${" in name:
        a["AutomationName"] = f"{name} [literal title in source; file={Path(f).name}]"
    return a


def build_auto_index():
    """Merge staging (preferred) + cursor automation indexes."""
    staging = [fix_auto_row(a) for a in load_json("automation_index_staging.json")]
    cursor = [fix_auto_row(a) for a in load_json("automation_index.json")]
    # staging first so match_automation picks it when multiple hits
    autos = staging + cursor
    by_id: dict[str, list] = defaultdict(list)
    for a in autos:
        for key in filter(
            None,
            [
                a.get("AutomationId"),
                a.get("TicketId"),
                a.get("LeafId"),
                *[(p or "").strip() for p in (a.get("AllIds") or "").split(",")],
            ],
        ):
            by_id[norm_id(key)].append(a)
        if a.get("TicketId") and a.get("LeafId"):
            by_id[norm_id(f"{a['TicketId']}/{a['LeafId']}")].append(a)
        # also index bare REG from title AllIds already covered
    return autos, by_id


def match_automation(case_id: str, by_id: dict, commented: dict):
    cid = norm_id(case_id)
    if cid in commented:
        c = commented[cid]
        return "No", c["AutomationName"], c["AutomationFile"], "Test exists but COMMENTED OUT — not executable"
    hits = by_id.get(cid) or []
    uniq = {}
    for h in hits:
        uniq[(h.get("AutomationFile"), h.get("Line"), h.get("AutomationName"))] = h
    hits = list(uniq.values())
    if not hits:
        return "No", "", "", ""
    # Prefer origin/staging paths
    staging_hits = [
        h
        for h in hits
        if str(h.get("SourceBranch") or "").startswith(("origin/staging", "staging"))
        or str(h.get("AutomationFile") or "").replace("\\", "/").startswith("src/tests/")
    ]
    pool = staging_hits or hits
    exact = [
        h
        for h in pool
        if cid.replace("RPS-POD/", "") in (h.get("AutomationName") or "").upper()
        or cid in (h.get("AutomationName") or "").upper()
    ]
    chosen = exact[0] if exact else pool[0]
    name = chosen.get("AutomationName") or ""
    f = chosen.get("AutomationFile") or ""
    note = ""
    if chosen.get("SourceBranch") == "origin/staging" or str(chosen.get("SourceBranch") or "").startswith("staging") or str(f).replace("\\", "/").startswith("src/tests/"):
        note = "Matched on EnergoTS staging"
    elif len(hits) > 1:
        note = f"Multiple asserting tests matched ({len(hits)}); primary shown"
    if len(hits) > 1 and note and "Multiple" not in note:
        note = note + f"; {len(hits)} match(es) total"
    return "Yes", name, f, note


def row(
    theme,
    case_id,
    title,
    etype,
    citation,
    quote,
    status,
    aname,
    afile,
    notes="",
):
    return {
        "Theme": theme,
        "CaseId": case_id,
        "CaseTitle": (title or "")[:500],
        "EvidenceType": etype,
        "EvidenceCitation": (citation or "")[:500],
        "EvidenceQuote": (quote or "")[:200],
        "AutomationStatus": status,
        "AutomationName": (aname or "")[:500],
        "AutomationFile": afile or "",
        "Notes": notes or "",
    }


def remap_theme(theme: str | None) -> str:
    t = theme or "16_CrossCutting"
    if t == "05_PerPiece_Orders":
        return "05_PerPiece"
    return t if t in SHEETS or t.startswith("00_") else "16_CrossCutting"


def main():
    autos, by_id = build_auto_index()
    commented_list = load_json("commented_tests.json")
    commented = {norm_id(c["AutomationId"]): c for c in commented_list}

    jira = load_json("jira_billing_filtered.json")
    conf = load_json("confluence_cases.json")
    diag = load_json("diagram_cases.json")
    page_meta = load_json("confluence_page_meta.json")

    all_rows: list[dict] = []

    # --- Jira Test Cases only (no feature/container rows — themes already split by sheet) ---
    for j in jira:
        if j.get("IssueType") != "Test Case":
            continue
        if j.get("IsContainer"):
            continue
        summary = (j.get("Summary") or "").strip()
        if re.search(r"(?i)\b(goods\s*order|service\s*order)\b", summary):
            continue
        if j["Key"] in PROCESS_CONTAINER_KEYS:
            # Container keys that somehow appear as Test Case — still skip epic/feature shells
            # Keep only if summary looks like a real leaf scenario (not bare feature name)
            if re.fullmatch(
                r"(?i)(billing\s*run|periodic\s*billing\s*run|for\s*volumes|pulling|"
                r"over\s*time.*|per\s*piece|with\s*electricity.*|interim.*|"
                r"discount|invoice\s*slot\s*splitting|correction|reversal|"
                r"invoice\s*cancellation.*|accounting\s*periods?|manual\s*billing\s*run|"
                r"conditions|price\s*conditions|invoices)",
                summary,
            ):
                continue
        theme = remap_theme(j.get("Theme"))
        cid = j["Key"]
        status, aname, afile, note = match_automation(cid, by_id, commented)
        notes = note
        if j.get("Status"):
            notes = (notes + "; " if notes else "") + f"Jira status: {j['Status']}"
        all_rows.append(
            row(
                theme,
                cid,
                j.get("Summary") or "",
                "Jira",
                f"{cid} | parent={j.get('Parent') or '-'} | type={j.get('IssueType')}",
                j.get("Summary") or "",
                status,
                aname,
                afile,
                notes,
            )
        )

    # Cursor PDT cases that are Playwright-only (not always in Jira REG)
    # Suite parents (PDT-xxxx / RPS-POD without /TC-BE) omitted — leaves added below

    # Expand asserting Playwright tests as leaf rows (no suite-parent aggregators)
    theme_by_ticket = {
        "PDT-2376": "06_With_Electricity",
        "REG-1172": "06_With_Electricity",
        "PDT-2750": "07_Interim",
        "PDT-2872": "07_Interim",
        "PDT-3113": "07_Interim",
        "PDT-2915": "10_Correction",
        "PDT-3072": "10_Correction",
        "PDT-3127": "10_Correction",
        "PDT-3087": "13_Compensation",
        "PDT-2891": "16_CrossCutting",
        "PDT-2937": "16_CrossCutting",
        "RPS-POD": "16_CrossCutting",
    }
    for a in autos:
        fpath = (a.get("AutomationFile") or "").replace("\\", "/")
        if re.search(r"(?i)goodsOrder|serviceOrder|goods.?order|service.?order", fpath):
            continue
        ticket = a.get("TicketId") or ""
        leaf = a.get("LeafId") or ""
        aid = a.get("AutomationId") or ""
        # Prefer composite leaf id; else use REG/PDT automation id (single asserting test)
        if leaf or "/" in aid:
            case_id = aid if "/" in aid else f"{ticket}/{leaf}"
        elif aid and re.match(r"(?i)^(REG|PDT)-\d+$", aid):
            case_id = aid
        elif ticket and re.match(r"(?i)^(REG|PDT)-\d+$", ticket):
            case_id = ticket
        else:
            continue
        # Skip bare suite parents if we also emit TC-BE leaves for same ticket
        if "/" not in case_id and any(
            (x.get("TicketId") == case_id or x.get("TicketId") == ticket) and x.get("LeafId")
            for x in autos
        ):
            continue
        # Theme: map by ticket, else infer from staging path
        theme = theme_by_ticket.get(ticket or case_id.split("/")[0])
        if not theme:
            fl = fpath.lower()
            if "forvolumes" in fl or "restriction" in fl:
                theme = "02_For_Volumes"
            elif "pulling" in fl:
                theme = "03_Pulling"
            elif "overtime" in fl:
                theme = "04_OverTime"
            elif "perpiece" in fl:
                theme = "05_PerPiece"
            elif "electricity" in fl:
                theme = "06_With_Electricity"
            elif "interim" in fl:
                theme = "07_Interim"
            elif "discount" in fl:
                theme = "08_Discount"
            elif "slot" in fl:
                theme = "09_Slot_Splitting"
            elif "correction" in fl:
                theme = "10_Correction"
            elif "reversal" in fl:
                theme = "11_Reversal"
            elif "cancel" in fl:
                theme = "12_Cancellation"
            elif "compensation" in fl:
                theme = "13_Compensation"
            elif "condition" in fl or "rounding" in fl:
                theme = "15_Conditions_ManualBR"
            elif "rps" in fl:
                theme = "16_CrossCutting"
            else:
                theme = "16_CrossCutting"
        src_note = "Asserting leaf"
        if a.get("SourceBranch") and str(a.get("SourceBranch")).startswith(("origin/staging", "staging")) or fpath.startswith("src/tests/"):
            src_note += "; EnergoTS staging"
        all_rows.append(
            row(
                theme,
                case_id,
                a.get("AutomationName") or case_id,
                "Playwright",
                f"{a.get('AutomationFile')}:{a.get('Line')}",
                (a.get("AutomationName") or "")[:200],
                "Yes",
                a.get("AutomationName") or "",
                a.get("AutomationFile") or "",
                f"{src_note}; TicketId={ticket or '-'}",
            )
        )

    # Confluence rules — if no REG id inside quote, stay as CONF-*; Automation usually No unless quote mentions REG
    for c in conf:
        quote = c.get("EvidenceQuote") or c.get("CaseTitle") or ""
        if re.search(r"(?i)\b(goods\s*order|service\s*order)\b", quote + " " + (c.get("EvidenceCitation") or "")):
            continue
        theme = remap_theme(c.get("Theme"))
        cid = c["CaseId"]
        # Try find REG in quote
        m = re.search(r"REG-\d+", quote)
        status, aname, afile, note = "No", "", "", ""
        if m:
            status, aname, afile, note = match_automation(m.group(0), by_id, commented)
            if status == "Yes":
                note = (note + "; " if note else "") + f"Matched via REG in quote ({m.group(0)})"
        all_rows.append(
            row(
                theme,
                cid,
                c.get("CaseTitle") or quote,
                "Confluence",
                c.get("EvidenceCitation") or "",
                quote,
                status,
                aname,
                afile,
                note,
            )
        )

    # Diagram cases
    for d in diag:
        title = d.get("CaseTitle") or ""
        if re.search(r"(?i)\b(goods\s*order|service\s*order)\b", title + " " + (d.get("EvidenceCitation") or "")):
            continue
        theme = remap_theme(d.get("Theme"))
        all_rows.append(
            row(
                theme,
                d["CaseId"],
                d.get("CaseTitle") or "",
                "Diagram",
                d.get("EvidenceCitation") or "",
                d.get("EvidenceQuote") or "",
                "No",
                "",
                "",
                f"Kind={d.get('Kind')}; no REG leaf auto-matched (see Unmapped if still open)",
            )
        )

    # Empty confluence pages as gap notes on theme sheets — skip goods/service order pages
    for p in page_meta:
        if not p.get("BodyEmpty"):
            continue
        title = p.get("Title") or ""
        if re.search(r"(?i)\b(goods\s*order|service\s*order)\b", title):
            continue
        theme = remap_theme(p.get("Theme") or "00_Unmapped_Gaps")
        all_rows.append(
            row(
                theme if theme in SHEETS else "00_Unmapped_Gaps",
                f"CONF-EMPTY-{p['PageId']}",
                f"[BodyEmpty] {title}",
                "Confluence",
                f"{title} | page {p['PageId']} | {p.get('Url')} | BodyLen={p.get('BodyLen')}",
                "Page body empty or drawio-only — no should-rules extracted from storage",
                "No",
                "",
                "",
                "BodyEmpty=true — use linked diagram / Jira leaves; do not invent rules from title",
            )
        )

    # Apply Partial: Jira Test Case with No automation, but same Theme has ≥1 Covered Jira TC
    # (REG leaves often have empty Parent in Jira; Theme approximates feature branch)
    covered_by_theme = defaultdict(int)
    for r in all_rows:
        if r["EvidenceType"] == "Jira" and r["AutomationStatus"] == "Yes" and r["CaseId"].startswith("REG-"):
            covered_by_theme[r["Theme"]] += 1
    for r in all_rows:
        if r["EvidenceType"] != "Jira" or r["AutomationStatus"] != "No":
            continue
        if not r["CaseId"].startswith("REG-"):
            continue
        if covered_by_theme.get(r["Theme"], 0) > 0:
            r["AutomationStatus"] = "Partial"
            extra = (
                f"Partial: theme {r['Theme']} has other Covered REG Test Cases; this leaf has no matching asserting test"
            )
            r["Notes"] = (r["Notes"] + "; " if r["Notes"] else "") + extra

    # Dedupe key: EvidenceType+CaseId+EvidenceQuote first 80
    dedup = {}
    for r in all_rows:
        k = (r["EvidenceType"], r["CaseId"], r["EvidenceQuote"][:80])
        dedup[k] = r
    all_rows = list(dedup.values())

    before_filter = len(all_rows)
    all_rows = [r for r in all_rows if is_process_case(r)]
    # Drop thematic container / suite-parent aggregators (sheets already group by theme)
    all_rows = [
        r
        for r in all_rows
        if not (
            (r.get("CaseId") or "") in PROCESS_CONTAINER_KEYS
            or (
                r.get("EvidenceType") == "Playwright"
                and "/" not in (r.get("CaseId") or "")
                and (r.get("CaseId") or "") in {"RPS-POD", "PDT-2872", "PDT-3087", "PDT-2891", "PDT-3127", "PDT-3072"}
                and "suite parent" in (r.get("Notes") or "").lower()
            )
            or "Feature/container" in (r.get("Notes") or "")
            or " | container" in (r.get("EvidenceCitation") or "")
        )
    ]
    filtered_out = before_filter - len(all_rows)

    apply_priorities(all_rows)

    # Unmapped gaps: Diagram or Confluence with Automation No and CaseId not REG-/PDT-
    # Also Jira Test Cases with Automation No
    unmapped = []
    for r in all_rows:
        if r["AutomationStatus"] == "No" and r["EvidenceType"] in ("Diagram", "Confluence"):
            if r["CaseId"].startswith("CONF-EMPTY"):
                unmapped.append(dict(r, Theme="00_Unmapped_Gaps"))
            elif r["EvidenceType"] == "Diagram":
                unmapped.append(dict(r, Theme="00_Unmapped_Gaps"))
        if r["EvidenceType"] == "Jira" and r["AutomationStatus"] == "No" and r["CaseId"].startswith("REG-"):
            # leaf without automation — also list in gaps for visibility? Keep on theme sheet primarily.
            pass

    # Add unmapped copies (diagram/empty) to gaps sheet content
    gap_rows = []
    seen_gap = set()
    for r in all_rows:
        if r["AutomationStatus"] != "No":
            continue
        if r["EvidenceType"] not in ("Diagram", "Confluence"):
            continue
        if r["CaseId"].startswith("REG-") or r["CaseId"].startswith("PDT-"):
            continue
        g = dict(r)
        g["Theme"] = "00_Unmapped_Gaps"
        key = (g["CaseId"], g["EvidenceQuote"][:60])
        if key in seen_gap:
            continue
        seen_gap.add(key)
        gap_rows.append(g)
    apply_priorities(gap_rows)
    gap_rows = sort_theme_rows(gap_rows)

    # Write workbook
    wb = Workbook()
    # remove default
    default = wb.active
    wb.remove(default)

    def write_sheet(name: str, rows: list[dict]):
        ws = wb.create_sheet(name)
        ws.append(HEADERS)
        for col, _ in enumerate(HEADERS, 1):
            cell = ws.cell(1, col)
            cell.fill = HEADER_FILL
            cell.font = HEADER_FONT
            cell.alignment = Alignment(wrap_text=True, vertical="center")
        for r in sort_theme_rows(rows):
            ws.append([r.get(h, "") for h in HEADERS])
            status = r.get("AutomationStatus")
            fill = YES_FILL if status == "Yes" else PARTIAL_FILL if status == "Partial" else NO_FILL
            st_cell = ws.cell(ws.max_row, HEADERS.index("AutomationStatus") + 1)
            st_cell.fill = fill
            prio = r.get("Priority")
            pfill = {
                "Highest": HIGHEST_FILL,
                "High": HIGH_FILL,
                "Medium": MEDIUM_FILL,
                "Low": LOW_FILL,
            }.get(prio)
            if pfill:
                ws.cell(ws.max_row, HEADERS.index("Priority") + 1).fill = pfill
        for col in range(1, len(HEADERS) + 1):
            ws.column_dimensions[get_column_letter(col)].width = min(48, max(12, len(HEADERS[col - 1]) + 4))
        ws.auto_filter.ref = ws.dimensions
        ws.freeze_panes = "A2"
        return ws

    # Summary sheet first
    ws = wb.create_sheet("00_Summary", 0)
    counts = defaultdict(lambda: defaultdict(int))
    prio_counts = defaultdict(lambda: defaultdict(int))
    for r in all_rows:
        counts[r["Theme"]][r["AutomationStatus"]] += 1
        counts[r["Theme"]]["Total"] += 1
        prio_counts[r["Theme"]][r.get("Priority") or "?"] += 1
        prio_counts["ALL"][r.get("Priority") or "?"] += 1
    ws["A1"] = "Billing Regression Cases — Process-only (evidence-strict)"
    ws["A1"].font = Font(bold=True, size=14)
    ws["A2"] = "Jira source: REST fallback (MCP unavailable)"
    ws["A3"] = "Confluence source: REST fallback (asterbit.atlassian.net/wiki)"
    ws["A4"] = "Diagrams: Cursor-Project/config/Diagrams/Bundle 5/*.semantic.md (QES excluded)"
    ws["A5"] = "SCOPE: direct billing/invoice effect only — goods/service orders excluded; object CRUD/UI excluded"
    ws["A6"] = f"Filtered out non-process rows: {filtered_out} (from {before_filter})"
    ws["A7"] = "Automation: prefer EnergoTS staging working tree src/tests/billing/**. Cursor leaves only when staging has no match. Goods/service orders excluded."
    ws["A8"] = "Priority: Highest → High → Medium → Low. Within same priority: No automation first, then Partial, then Yes."
    ws["A9"] = f"Generated from: {EVID}"
    ws["A11"] = "Theme"
    ws["B11"] = "Total"
    ws["C11"] = "Yes"
    ws["D11"] = "Partial"
    ws["E11"] = "No"
    ws["F11"] = "Highest"
    ws["G11"] = "High"
    ws["H11"] = "Medium"
    ws["I11"] = "Low"
    for cell in ws[11]:
        if cell.value:
            cell.fill = HEADER_FILL
            cell.font = HEADER_FONT
    r_i = 12
    total = defaultdict(int)
    for theme in SHEETS:
        if theme.startswith("00_"):
            continue
        c = counts.get(theme, {})
        pc = prio_counts.get(theme, {})
        ws.cell(r_i, 1, theme)
        ws.cell(r_i, 2, c.get("Total", 0))
        ws.cell(r_i, 3, c.get("Yes", 0))
        ws.cell(r_i, 4, c.get("Partial", 0))
        ws.cell(r_i, 5, c.get("No", 0))
        ws.cell(r_i, 6, pc.get("Highest", 0))
        ws.cell(r_i, 7, pc.get("High", 0))
        ws.cell(r_i, 8, pc.get("Medium", 0))
        ws.cell(r_i, 9, pc.get("Low", 0))
        for k in ("Total", "Yes", "Partial", "No"):
            total[k] += c.get(k, 0)
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
    r_i += 2
    ws.cell(r_i, 1, "How to read Automation columns")
    ws.cell(r_i + 1, 1, "AutomationStatus=Yes means AutomationName is the exact Playwright test(...) title and AutomationFile is the repo-relative path.")
    ws.cell(r_i + 2, 1, "EvidenceQuote is verbatim from Jira summary, Confluence body line, or diagram semantic.md.")
    ws.cell(r_i + 3, 1, "CONF-EMPTY / object CRUD / UI listing / PC field-validation rows are excluded (process-only workbook).")
    ws.cell(r_i + 4, 1, "Within each theme sheet, rows are sorted: Priority Highest→Low, then Automation No→Partial→Yes (gaps first), then CaseId.")
    r_i += 6
    ws.cell(r_i, 1, "Tier 1+2 Playwright entrypoints (EnergoTS)")
    suite_files = sorted({a.get("AutomationFile") for a in autos if a.get("AutomationFile")})
    for i, f in enumerate(suite_files):
        ws.cell(r_i + 1 + i, 1, f)
    for col in range(1, 10):
        ws.column_dimensions[get_column_letter(col)].width = 28

    write_sheet("00_Unmapped_Gaps", gap_rows)

    by_theme = defaultdict(list)
    for r in all_rows:
        by_theme[r["Theme"]].append(r)

    for theme in SHEETS:
        if theme.startswith("00_"):
            continue
        write_sheet(theme, by_theme.get(theme, []))

    try:
        wb.save(OUT_XLSX)
        saved = OUT_XLSX
    except PermissionError:
        wb.save(OUT_XLSX_FALLBACK)
        saved = OUT_XLSX_FALLBACK
        print(f"Main xlsx locked; wrote fallback: {OUT_XLSX_FALLBACK}")

    # Short MD index
    OUT_MD.write_text(
        f"""# Billing Regression Coverage — Index (Process-only)

**Single source of truth:** [`Billing_Regression_Cases.xlsx`](Billing_Regression_Cases.xlsx)

## Scope
**Only cases with direct effect on billing/invoices.** Removed:
- Object CRUD/setup (PC, POD, IAP, Discount fields, condition-builder UI)
- Listing/UI chrome, permissions, billing-run numbering
- Master-data checkboxes/tabs that do not assert invoice create/amount/status

Kept: start/generate billing, For volumes / pulling / overtime / electricity / interim invoice thresholds, discount-in-invoice calc, slot splitting, correction/reversal/cancellation, compensation link/PDF, RPS due date, connected invoices, Playwright billing leaves.

Filtered out at generation: **{filtered_out}** rows (from {before_filter}).

## Evidence
| EvidenceType | How we know |
|--------------|-------------|
| **Jira** | REST issue summary for REG Test Case / feature key |
| **Confluence** | Verbatim should/must/if line from page body + page ID/URL |
| **Diagram** | Decision / Save text from Bundle 5 `.semantic.md` (process diagrams) |
| **Playwright** | Cursor/billing suite mapped by ticket id / file |

**AutomationStatus=Yes** only when an **asserting** `test(...)` title/id matches the CaseId.

## Priority (within each theme sheet)
Sorted **Highest → High → Medium → Low**; within same priority, **No** automation first.

| Priority | Meaning |
|----------|---------|
| **Highest** | Direct billing process (run/generate/correct/reverse/cancel/compensate/…) |
| **High** | Critical calculation / financial / due-date (in-process) |
| **Medium** | Process validation / error / edge |
| **Low** | Residual supporting (mostly filtered out) |

## Disclosures
- Jira source: REST fallback (MCP unavailable)
- Confluence source: REST fallback (`asterbit.atlassian.net/wiki`)

## Totals (at generation)
- Rows: {total['Total']}
- Automation Yes / Partial / No: {total['Yes']} / {total['Partial']} / {total['No']}
- Priority Highest/High/Medium/Low: {prio_counts['ALL'].get('Highest', 0)} / {prio_counts['ALL'].get('High', 0)} / {prio_counts['ALL'].get('Medium', 0)} / {prio_counts['ALL'].get('Low', 0)}
- Unmapped gap rows: {len(gap_rows)}

Evidence working files: `docs/_billing_evidence/`
""",
        encoding="utf-8",
    )

    print(f"Wrote {saved}")
    print(f"Wrote {OUT_MD}")
    print(
        f"Totals Total={total['Total']} Yes={total['Yes']} Partial={total['Partial']} No={total['No']} Gaps={len(gap_rows)}"
    )
    print(
        f"Priority Highest={prio_counts['ALL'].get('Highest', 0)} High={prio_counts['ALL'].get('High', 0)} "
        f"Medium={prio_counts['ALL'].get('Medium', 0)} Low={prio_counts['ALL'].get('Low', 0)}"
    )
    print(f"Process-only filter: kept {len(all_rows)} / removed {filtered_out} (from {before_filter})")


if __name__ == "__main__":
    main()
