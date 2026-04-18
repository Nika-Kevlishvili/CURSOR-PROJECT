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


def _yes_no_token(used: bool, slack: bool) -> str:
    if slack:
        return ":white_check_mark: *Yes*" if used else ":x: *No*"
    return "**Yes**" if used else "**No**"


def describe_confluence_input(cv: dict, *, slack: bool = True) -> str:
    """Factual: was Confluence queried and did we get pages/excerpts."""
    status = (cv.get("status") or "").strip()
    pages = cv.get("page_contents") or []
    sources = cv.get("sources") or []

    if status == "skipped":
        return f"{_yes_no_token(False, slack)} — not configured (missing Confluence URL/token)."

    if status == "no_results":
        return f"{_yes_no_token(True, slack)} — search ran; **0** pages matched."

    if pages:
        return (
            f"{_yes_no_token(True, slack)} — **{len(pages)}** page excerpt(s); "
            f"**{len(sources)}** link(s) in search results."
        )

    if sources:
        return (
            f"{_yes_no_token(True, slack)} — **{len(sources)}** page link(s); "
            "excerpts were not loaded."
        )

    if status == "found":
        return f"{_yes_no_token(True, slack)} — search reported hits but no excerpt payload."

    return f"{_yes_no_token(True, slack)} — API status `{status or 'unknown'}`."


def describe_code_input(scan: dict | None, *, slack: bool = True) -> str:
    """Factual: did we scan local Phoenix and how many hits/snippets."""
    if not scan:
        return "_Scan metadata missing._"

    st = (scan.get("status") or "").strip()
    root = (scan.get("phoenix_root") or "").strip()
    nf = scan.get("files_found")
    ns = scan.get("snippets_sent")
    err = (scan.get("error") or "").strip()

    root_short = _truncate(root, 100) if root else "n/a"

    if st == "unreachable":
        return (
            f"{_yes_no_token(False, slack)} — Phoenix path not available. "
            f"{_truncate(err, 200)}"
        )

    if st == "no_results":
        return (
            f"{_yes_no_token(True, slack)} — scan ran; **0** files matched keywords. "
            f"Path: `{root_short}`"
        )

    if st == "ok":
        snip_part = f", **{ns}** snippet(s) sent to the model" if ns is not None else ""
        return (
            f"{_yes_no_token(True, slack)} — **{nf}** file(s) matched{snip_part}. "
            f"Path: `{root_short}`"
        )

    return f"_Unknown scan status:_ `{st or 'n/a'}` Path: `{root_short}`"


def documentation_alignment_line(evidence_strength: str | None) -> str:
    """
    AI layer: how documentation supports the bug's expected behavior.
    Not a percentage — ordinal strength from the model's evidence_strength enum.
    """
    if not evidence_strength or str(evidence_strength).strip() in ("", "N/A"):
        return "_Not assessed_ — the model did not return `evidence_strength`."

    key = str(evidence_strength).strip().lower().replace(" ", "_")
    label = humanize_evidence_strength(evidence_strength)
    tier = {
        "exact_match": "Strong alignment",
        "contextual_match": "Partial alignment",
        "no_match": "Weak / no supporting documentation for this case",
        "contradicts": "Contradicts the bug's expectation",
        "search_failed": "Documentation check failed (access/search error)",
    }.get(key, "See label")

    return f"*{tier}* — {label}"


def code_alignment_line(behavior_match: str | None) -> str:
    """AI layer: does code behavior match what the bug claims is wrong."""
    if not behavior_match or str(behavior_match).strip() in ("", "N/A"):
        return "_Not assessed_ — the model did not return `behavior_match`."

    key = str(behavior_match).strip().lower().replace(" ", "_")
    label = humanize_behavior_match(behavior_match)
    tier = {
        "matches_reported_behavior": "Supports the bug report",
        "does_not_match_reported_behavior": "Does not support the bug report",
        "could_not_verify": "Inconclusive from available code evidence",
    }.get(key, "See label")

    return f"*{tier}* — {label}"


def build_sources_and_alignment_mrkdwn(report: dict) -> str:
    """Slack mrkdwn: factual inputs + AI match summary."""
    confluence = report.get("confluence_validation") or {}
    code = report.get("code_validation") or {}
    code_scan = report.get("code_scan") if isinstance(report.get("code_scan"), dict) else {}

    lines = [
        "*1) What was actually used*",
        f"• Confluence API: {describe_confluence_input(confluence, slack=True)}",
        f"• Local Phoenix scan: {describe_code_input(code_scan, slack=True)}",
        "",
        "*2) Match vs bug description (AI assessment)*",
        f"• Documentation ↔ bug: {documentation_alignment_line(confluence.get('evidence_strength'))}",
        f"• Code ↔ bug: {code_alignment_line(code.get('behavior_match'))}",
    ]

    extra = _format_code_analysis_field(code, code_scan if code_scan else None)
    if "\n" in extra.strip():
        lines.extend(["", "*Code scan / model notes:*", extra])

    return "\n".join(lines)


def build_sources_and_alignment_markdown(report: dict) -> str:
    """Saved .md report section (no Slack emoji)."""
    confluence = report.get("confluence_validation") or {}
    code = report.get("code_validation") or {}
    code_scan = report.get("code_scan") if isinstance(report.get("code_scan"), dict) else {}

    return "\n".join(
        [
            "## Input sources and match vs bug report",
            "",
            "### 1) What was actually used",
            "",
            f"- **Confluence API:** {describe_confluence_input(confluence, slack=False)}",
            f"- **Local Phoenix scan:** {describe_code_input(code_scan, slack=False)}",
            "",
            "### 2) Match vs bug description (AI assessment)",
            "",
            f"- **Documentation ↔ bug:** {documentation_alignment_line(confluence.get('evidence_strength'))}",
            f"- **Code ↔ bug:** {code_alignment_line(code.get('behavior_match'))}",
            "",
        ]
    ).rstrip() + "\n"


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


def _format_environment_block(report: dict) -> str:
    """
    Compact summary of which environment was scanned and which branch was
    chosen per Phoenix repo. Returns an empty string when no env data exists.
    """
    env_info = report.get("environment") or {}
    branch_switch = report.get("branch_switch") or {}
    env_name = env_info.get("detected") or branch_switch.get("env")
    if not env_name:
        return ""

    explicit = bool(env_info.get("explicit"))
    origin = "from bug text" if explicit else "default — no env keyword in bug"

    lines = [f"*Environment scanned:* `{env_name}` _({origin})_"]
    repos = branch_switch.get("repos") if isinstance(branch_switch, dict) else None
    if repos:
        repo_lines = []
        for repo in repos[:12]:
            name = repo.get("repo", "?")
            after = repo.get("after") or "?"
            status = repo.get("status", "?")
            marker = {
                "switched": ":arrows_counterclockwise:",
                "updated": ":arrow_down:",
                "already_on_branch": ":white_check_mark:",
                "skipped": ":heavy_minus_sign:",
                "failed": ":x:",
            }.get(status, ":grey_question:")
            repo_lines.append(f"{marker} `{name}` -> `{after}`")
        if repo_lines:
            lines.append("\n".join(repo_lines))
    elif isinstance(branch_switch, dict) and branch_switch.get("error"):
        lines.append(f"_Branch switch note:_ {_truncate(branch_switch['error'], 200)}")

    return "\n".join(lines)


def build_slack_blocks(report: dict) -> list[dict]:
    """Assemble Slack Block Kit payload for the bug validation report."""
    bug = report.get("bug", {})
    final_verdict = report.get("final_verdict", {})

    verdict_raw = final_verdict.get("verdict", "INSUFFICIENT_EVIDENCE")
    verdict = VERDICT_EMOJIS.get(verdict_raw, f":question: *{verdict_raw}*")

    usage_text = build_sources_and_alignment_mrkdwn(report)

    env_block_text = _format_environment_block(report)

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
    ]

    if env_block_text:
        blocks.append(
            {
                "type": "section",
                "text": {"type": "mrkdwn", "text": env_block_text},
            }
        )

    blocks.append(
        {
            "type": "section",
            "text": {
                "type": "mrkdwn",
                "text": _truncate(usage_text, 2950),
            },
        }
    )

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

    code = report.get("code_validation", {}) or {}
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
