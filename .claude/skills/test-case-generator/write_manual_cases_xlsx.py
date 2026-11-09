#!/usr/bin/env python3
"""Write the human-readable manual test-case workbook (no Playwright).

Reads a JSON file and writes .xlsx with the PHN-4503 columns.
Refuses to write if a cell contains API, HTTP, or Playwright instructions.

JSON shape:
{
  "sheetName": "Backend" | "Frontend",
  "cases": [
    {
      "id": "TC-BE-1",
      "type": "Positive" | "Negative",
      "title": "...",
      "acceptanceCriteria": "AC-1, AC-2",
      "preconditions": "...",
      "steps": "...",
      "expectedResult": "..."
    }
  ]
}
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

HEADERS = (
    "ID",
    "Type",
    "Title",
    "Acceptance criteria",
    "Preconditions",
    "Steps",
    "Expected result",
    "Result",
)

COLUMN_WIDTHS = (14, 12, 46, 32, 62, 52, 62, 12)

# Automation leaks. Quoted on-screen messages may still contain a field key
# ("noticeReceivingDate-Notice receiving date is required"), so bare camelCase
# is allowed. A field assignment ("billingType:") is not.
FORBIDDEN = (
    re.compile(r"\b(GET|POST|PUT|PATCH|DELETE)\s+/", re.IGNORECASE),
    re.compile(r"\bHTTP\s+\d{3}\b", re.IGNORECASE),
    re.compile(r"\b(test\.step|test\.beforeAll|beforeAll|GeneratePayload|CheckResponse)\b"),
    re.compile(r"`"),
    re.compile(r"\.(spec|fixtures)\.ts\b", re.IGNORECASE),
    re.compile(r"\b(playwright|npx)\b", re.IGNORECASE),
    re.compile(r"\b[a-z]+[A-Z][A-Za-z0-9]*\s*:"),
    re.compile(r"Cursor-Project/"),
    re.compile(r"\.java\b"),
    re.compile(r"\bsee the (markdown|spec)\b", re.IGNORECASE),
)

CELL_LIMIT = 32767


def fail(message: str) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(1)


def load_cases(path: Path) -> tuple[str, list[dict]]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        fail(f"Cannot read JSON: {exc}")

    sheet = str(payload.get("sheetName", "")).strip()
    if sheet not in ("Backend", "Frontend"):
        fail('sheetName must be "Backend" or "Frontend"')

    cases = payload.get("cases")
    if not isinstance(cases, list) or not cases:
        fail("cases must be a non-empty array")

    prefix = "TC-BE-" if sheet == "Backend" else "TC-FE-"
    types: set[str] = set()
    seen: set[str] = set()
    cleaned: list[dict] = []

    for index, case in enumerate(cases, start=1):
        if not isinstance(case, dict):
            fail(f"Case {index} is not an object")
        case_id = str(case.get("id", "")).strip()
        case_type = str(case.get("type", "")).strip()
        if not re.fullmatch(prefix + r"[1-9][0-9]*", case_id):
            fail(f"Case {index} id must look like {prefix}1 (got {case_id!r})")
        if case_id in seen:
            fail(f"Duplicate id {case_id}")
        seen.add(case_id)
        if case_type not in ("Positive", "Negative"):
            fail(f"{case_id} type must be Positive or Negative")
        types.add(case_type)

        row = {
            "id": case_id,
            "type": case_type,
            "title": _required_text(case, "title", case_id),
            "acceptanceCriteria": str(case.get("acceptanceCriteria", "")).strip(),
            "preconditions": _required_text(case, "preconditions", case_id),
            "steps": _required_text(case, "steps", case_id),
            "expectedResult": _required_text(case, "expectedResult", case_id),
        }
        if str(case.get("result", "")).strip():
            fail(f"{case_id} Result must be empty. Testers fill it after execution.")
        _reject_forbidden(case_id, row)
        cleaned.append(row)

    if types != {"Positive", "Negative"}:
        fail("The workbook needs at least one Positive and one Negative case")

    expected_ids = [f"{prefix}{n}" for n in range(1, len(cleaned) + 1)]
    actual_ids = [row["id"] for row in cleaned]
    if actual_ids != expected_ids:
        fail(f"Ids must be {prefix}1..{prefix}{len(cleaned)} in order. Got {actual_ids}")

    return sheet, cleaned


def _required_text(case: dict, key: str, case_id: str) -> str:
    value = str(case.get(key, "")).strip()
    if not value:
        fail(f"{case_id} {key} is empty")
    if len(value) > CELL_LIMIT:
        fail(f"{case_id} {key} exceeds {CELL_LIMIT} characters")
    return value


def _reject_forbidden(case_id: str, row: dict) -> None:
    fields = {
        "Title": row["title"],
        "Acceptance criteria": row["acceptanceCriteria"],
        "Preconditions": row["preconditions"],
        "Steps": row["steps"],
        "Expected result": row["expectedResult"],
    }
    for label, text in fields.items():
        for pattern in FORBIDDEN:
            match = pattern.search(text)
            if match:
                fail(
                    f"{case_id} {label} contains automation text {match.group(0)!r}. "
                    "Rewrite that cell in manual tester language and run again."
                )


def _cell(ref: str, text: str, style: int) -> str:
    safe = escape(text)
    return (
        f'<c r="{ref}" t="inlineStr" s="{style}">'
        f'<is><t xml:space="preserve">{safe}</t></is></c>'
    )


def _col_letter(index: int) -> str:
    # 1-based
    name = ""
    while index:
        index, rem = divmod(index - 1, 26)
        name = chr(65 + rem) + name
    return name


def _row_height(values: list[str]) -> float:
    lines = 1
    for value in values:
        wrapped = value.count("\n") + 1
        longest = max((len(part) for part in value.split("\n")), default=0)
        wrapped += longest // 70
        lines = max(lines, wrapped)
    return float(min(400, max(18, 15 * lines)))


def worksheet_xml(cases: list[dict]) -> str:
    cols = []
    for index, width in enumerate(COLUMN_WIDTHS, start=1):
        cols.append(f'<col min="{index}" max="{index}" width="{width}" customWidth="1"/>')

    rows = []
    header_cells = []
    for index, header in enumerate(HEADERS, start=1):
        header_cells.append(_cell(f"{_col_letter(index)}1", header, 1))
    rows.append(
        '<row r="1" ht="22" customHeight="1">' + "".join(header_cells) + "</row>"
    )

    for offset, case in enumerate(cases):
        excel_row = offset + 2
        values = [
            case["id"],
            case["type"],
            case["title"],
            case["acceptanceCriteria"],
            case["preconditions"],
            case["steps"],
            case["expectedResult"],
            "",
        ]
        cells = []
        for index, value in enumerate(values, start=1):
            cells.append(_cell(f"{_col_letter(index)}{excel_row}", value, 0))
        height = _row_height(values)
        rows.append(
            f'<row r="{excel_row}" ht="{height:.0f}" customHeight="1">'
            + "".join(cells)
            + "</row>"
        )

    last_row = len(cases) + 1
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetViews>
    <sheetView workbookViewId="0">
      <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <sheetFormatPr defaultRowHeight="15"/>
  <cols>{"".join(cols)}</cols>
  <sheetData>{"".join(rows)}</sheetData>
  <autoFilter ref="A1:H{last_row}"/>
</worksheet>
'''


def styles_xml() -> str:
    return '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF1F4E79"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD0D7DE"/></left>
      <right style="thin"><color rgb="FFD0D7DE"/></right>
      <top style="thin"><color rgb="FFD0D7DE"/></top>
      <bottom style="thin"><color rgb="FFD0D7DE"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1">
      <alignment wrapText="1" vertical="top"/>
    </xf>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment wrapText="1" vertical="center" horizontal="center"/>
    </xf>
  </cellXfs>
</styleSheet>
'''


def workbook_xml(sheet_name: str) -> str:
    return f'''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="{escape(sheet_name)}" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>
'''


def write_xlsx(path: Path, sheet_name: str, cases: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>
'''
    root_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>
'''
    workbook_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>
'''
    with zipfile.ZipFile(path, "w", compression=zipfile.ZIP_DEFLATED) as workbook:
        workbook.writestr("[Content_Types].xml", content_types)
        workbook.writestr("_rels/.rels", root_rels)
        workbook.writestr("xl/workbook.xml", workbook_xml(sheet_name))
        workbook.writestr("xl/_rels/workbook.xml.rels", workbook_rels)
        workbook.writestr("xl/styles.xml", styles_xml())
        workbook.writestr("xl/worksheets/sheet1.xml", worksheet_xml(cases))


def main() -> None:
    parser = argparse.ArgumentParser(description="Write a manual test-case workbook.")
    parser.add_argument("--input", required=True, help="JSON file with sheetName and cases")
    parser.add_argument("--output", required=True, help="Destination .xlsx path")
    args = parser.parse_args()

    output = Path(args.output)
    if output.suffix.lower() != ".xlsx":
        fail("--output must end with .xlsx")

    sheet_name, cases = load_cases(Path(args.input))
    write_xlsx(output, sheet_name, cases)
    print(f"Wrote {len(cases)} cases to {output}")


if __name__ == "__main__":
    main()
