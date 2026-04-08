"""
Slack Bug Validation report layout and copy.

Edit this file to change how the automated Bug Validator formats Slack messages:
section order, labels, truncation limits, and human-readable enum labels.

Block structure follows Slack Block Kit (mrkdwn).
"""

from __future__ import annotations


def merge_confluence_validation(confluence_result: dict, analysis: dict) -> dict:
    """
    Overlay Gemini Confluence fields onto API results while preserving
    page_contents (and other fetch metadata) from confluence_result.
    """
    ai_cv = analysis.get("confluence_validation") or {}
    merged = {**confluence_result, **ai_cv}
    if confluence_result.get("page_contents"):
        merged["page_contents"] = confluence_result["page_contents"]
    return merged


# --- Human-readable labels (replaces snake_case in the UI) ---

EVIDENCE_STRENGTH_LABELS: dict[str, str] = {
    "exact_match": "Exact match",
    "contextual_match": "Contextual match",
    "no_match": "No match",
    "contradicts": "Contradicts expected behavior",
    "search_failed": "Search failed",
}

BEHAVIOR_MATCH_LABELS: dict[str, str] = {
    "matches_reported_behavior": "Matches reported behavior",
    "does_not_match_reported_behavior": "Does not match reported behavior",
    "could_not_verify": "Could not verify",
}

VERDICT_EMOJIS: dict[str, str] = {
    "VALID": ":white_check_mark: *VALID*",
    "NEEDS_CLARIFICATION": ":warning: *NEEDS CLARIFICATION*",
    "NEEDS_APPROVAL": ":hourglass_flowing_sand: *NEEDS APPROVAL*",
    "NOT_VALID": ":x: *NOT VALID*",
    "INSUFFICIENT_EVIDENCE": ":grey_question: *INSUFFICIENT EVIDENCE*",
}

# Truncation (Slack section text limit is 3000; stay under)
MAX_CONFLUENCE_BASIS_TOTAL = 2600
MAX_EXCERPT_PER_PAGE = 900
MAX_PAGES_IN_BASIS = 3


def humanize_evidence_strength(value: str | None) -> str:
    if not value or value == "N/A":
        return "N/A"
    key = value.strip().lower().replace(" ", "_")
    if key in EVIDENCE_STRENGTH_LABELS:
        return EVIDENCE_STRENGTH_LABELS[key]
    return _fallback_title(value)


def humanize_behavior_match(value: str | None) -> str:
    if not value or value == "N/A":
        return "N/A"
    key = value.strip().lower().replace(" ", "_")
    if key in BEHAVIOR_MATCH_LABELS:
        return BEHAVIOR_MATCH_LABELS[key]
    return _fallback_title(value)


def _fallback_title(raw: str) -> str:
    """Turn unknown_enum_values into Title Case words without underscores."""
    cleaned = raw.replace("_", " ").strip()
    if not cleaned:
        return "N/A"
    return cleaned.title()


def _format_code_analysis_field(code: dict, code_scan: dict | None) -> str:
    """
    Human-readable code verdict plus concrete reason when local scan failed or found nothing.
    """
    label = humanize_behavior_match(code.get("behavior_match"))
    scan = code_scan or {}
    status = (scan.get("status") or "").strip()
    err = (scan.get("error") or "").strip()
    n_files = scan.get("files_found")
    root = (scan.get("phoenix_root") or "").strip()

    notes: list[str] = []
    if status == "unreachable":
        detail = _truncate(err or "Phoenix directory missing or not readable on runner.", 220)
        notes.append(f"_Local code scan skipped:_ {detail}")
    elif status == "no_results":
        notes.append("_Local code scan:_ no matching source files for extracted keywords.")
    elif status == "ok" and (n_files is not None) and n_files == 0:
        notes.append("_Local code scan:_ no files collected.")

    if root and status in ("unreachable", "no_results", "ok"):
        notes.append(f"_Phoenix path:_ `{_truncate(root, 120)}`")

    # Model said it could not verify despite snippets — nudge reader to Reasoning
    if (
        (code.get("behavior_match") or "").strip().lower().replace(" ", "_") == "could_not_verify"
        and status == "ok"
        and isinstance(n_files, int)
        and n_files > 0
    ):
        notes.append(
            "_Note:_ snippets were sent to the model, but it did not confirm the reported faulty behavior vs code — see Reasoning."
        )

    if notes:
        return label + "\n" + "\n".join(notes)
    return label


def _truncate(text: str, max_len: int) -> str:
    text = text.strip()
    if len(text) <= max_len:
        return text
    return text[: max_len - 3].rstrip() + "..."


def build_confluence_basis_mrkdwn(report: dict) -> str | None:
    """
    Build the 'documentation basis' section: Confluence titles, links, and excerpts.

    Returns None if there is nothing meaningful to show (caller may skip the block).
    """
    cv = report.get("confluence_validation") or {}
    pages = cv.get("page_contents") or []
    status = cv.get("status", "")

    lines: list[str] = ["*Confluence basis for validation*"]

    if status == "skipped" or (not pages and not cv.get("sources")):
        lines.append(
            "_No Confluence documentation was retrieved — the validation could not be anchored to specific pages._"
        )
        return "\n".join(lines)

    if not pages:
        sources = cv.get("sources") or []
        if sources:
            lines.append("_Pages matched by search (content excerpts not available):_")
            for src in sources[:8]:
                lines.append(f"• {src}")
            return "\n".join(lines)
        lines.append("_No Confluence pages were found for this bug._")
        return "\n".join(lines)

    total_budget = MAX_CONFLUENCE_BASIS_TOTAL
    for i, page in enumerate(pages[:MAX_PAGES_IN_BASIS]):
        title = page.get("title") or "Untitled"
        url = (page.get("url") or "").strip()
        excerpt = (page.get("excerpt") or "").strip()

        if url:
            header = f"*{i + 1}. {title}* — <{url}|Open in Confluence>"
        else:
            header = f"*{i + 1}. {title}*"

        block = [header]
        if excerpt:
            ex = _truncate(excerpt, MAX_EXCERPT_PER_PAGE)
            block.append(f"> {ex}")

        piece = "\n".join(block)
        if len("\n\n".join(lines)) + len(piece) + 20 > total_budget:
            block = [header, "> _(excerpt truncated — see full page in Confluence)_"]
            piece = "\n".join(block)
        if len("\n\n".join(lines)) + len(piece) > total_budget:
            lines.append("_…further pages omitted (length limit)._")
            break
        lines.append(piece)

    return "\n\n".join(lines)


def build_confluence_basis_markdown(report: dict) -> str | None:
    """
    Same content as the Slack basis section, but with standard Markdown links for saved .md reports.
    Returns None if there is nothing to show.
    """
    cv = report.get("confluence_validation") or {}
    pages = cv.get("page_contents") or []
    status = cv.get("status", "")

    lines: list[str] = ["### Documentation basis (excerpts)", ""]

    if status == "skipped" or (not pages and not cv.get("sources")):
        lines.append(
            "_No Confluence documentation was retrieved — the validation could not be anchored to specific pages._"
        )
        return "\n".join(lines)

    if not pages:
        sources = cv.get("sources") or []
        if sources:
            lines.append("_Pages matched by search (content excerpts not available):_")
            for src in sources[:8]:
                lines.append(f"- {src}")
            return "\n".join(lines)
        lines.append("_No Confluence pages were found for this bug._")
        return "\n".join(lines)

    for i, page in enumerate(pages[:MAX_PAGES_IN_BASIS]):
        title = page.get("title") or "Untitled"
        url = (page.get("url") or "").strip()
        excerpt = (page.get("excerpt") or "").strip()
        lines.append(f"### {i + 1}. {title}")
        if url:
            lines.append(f"**Link:** [{url}]({url})")
        if excerpt:
            lines.append("")
            lines.append(f"> {_truncate(excerpt, MAX_EXCERPT_PER_PAGE)}")
        lines.append("")

    return "\n".join(lines).rstrip() + "\n"


def build_slack_blocks(report: dict) -> list[dict]:
    """Assemble Slack Block Kit payload for the bug validation report."""
    bug = report.get("bug", {})
    final_verdict = report.get("final_verdict", {})

    verdict_raw = final_verdict.get("verdict", "INSUFFICIENT_EVIDENCE")
    verdict = VERDICT_EMOJIS.get(verdict_raw, f":question: *{verdict_raw}*")

    confluence = report.get("confluence_validation", {})
    code = report.get("code_validation", {})
    code_scan = report.get("code_scan")

    evidence_label = humanize_evidence_strength(confluence.get("evidence_strength"))
    behavior_label = _format_code_analysis_field(code, code_scan if isinstance(code_scan, dict) else None)

    blocks: list[dict] = [
        {
            "type": "header",
            "text": {
                "type": "plain_text",
                "text": f"Bug Validation: {bug.get('key', 'N/A')}",
            },
        },
        {
            "type": "section",
            "fields": [
                {"type": "mrkdwn", "text": f"*Summary:*\n{bug.get('summary', 'N/A')}"},
                {"type": "mrkdwn", "text": f"*Verdict:*\n{verdict}"},
            ],
        },
        {
            "type": "section",
            "fields": [
                {"type": "mrkdwn", "text": f"*Confluence:*\n{evidence_label}"},
                {"type": "mrkdwn", "text": f"*Code Analysis:*\n{behavior_label}"},
            ],
        },
    ]

    basis = build_confluence_basis_mrkdwn(report)
    if basis:
        blocks.append(
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": _truncate(basis, 2950),
                },
            }
        )

    blocks.append({"type": "divider"})
    blocks.append(
        {
            "type": "section",
            "text": {
                "type": "mrkdwn",
                "text": (
                    f"*Reasoning:*\n{_truncate(final_verdict.get('reasoning', 'No reasoning provided'), 400)}\n\n"
                    f"*Next Steps:*\n{_truncate(final_verdict.get('next_steps', 'No next steps provided'), 300)}"
                ),
            },
        }
    )

    refs = code.get("references", [])
    if refs:
        ref_lines = [
            f"• `{r.get('file', '?')}` (L{r.get('lines', '?')}): {r.get('implementation', '')}"
            for r in refs[:5]
        ]
        blocks.append(
            {
                "type": "section",
                "text": {"type": "mrkdwn", "text": "*Code References:*\n" + "\n".join(ref_lines)},
            }
        )

    blocks.append(
        {
            "type": "context",
            "elements": [
                {
                    "type": "mrkdwn",
                    "text": f":robot_face: _Bug Validator (automated) | {report.get('timestamp', '')}_",
                },
            ],
        }
    )

    return blocks
