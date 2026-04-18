"""
Bug Validator — CI entry point.

Triggered by GitHub Actions when a Jira bug reaches a specific status.
Flow: Jira → Confluence → Local Phoenix code → Gemini analysis → Slack report.
"""

import argparse
import io
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

# Ensure UTF-8 output on Windows (cp1252 can't handle Georgian/Cyrillic chars)
if sys.stdout and hasattr(sys.stdout, "buffer"):
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
if sys.stderr and hasattr(sys.stderr, "buffer"):
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

from jira_client import JiraClient
from local_phoenix_client import create_client as create_local_phoenix_client
from confluence_client import ConfluenceClient
from analyzer import BugAnalyzer
from slack_reporter import SlackReporter
from branch_resolver import (
    format_switch_summary,
    resolve_environment,
    switch_phoenix_repos_to_env,
)
from slack_report_template import (
    build_confluence_basis_markdown,
    build_sources_and_alignment_markdown,
    humanize_behavior_match,
    humanize_evidence_strength,
    merge_confluence_validation,
)

OUTPUT_DIR = Path(__file__).parent / "output"


def load_env():
    """Load and validate required environment variables."""
    required = {
        "JIRA_BASE_URL": "Jira base URL (e.g. https://yourorg.atlassian.net)",
        "JIRA_EMAIL": "Jira account email",
        "JIRA_API_TOKEN": "Jira API token",
        "GEMINI_API_KEY": "Google Gemini API key (free tier)",
        "SLACK_WEBHOOK_URL": "Slack incoming webhook URL bound to the target channel",
    }
    optional = {
        "CONFLUENCE_BASE_URL": None,
        "CONFLUENCE_EMAIL": None,
        "CONFLUENCE_API_TOKEN": None,
        "CONFLUENCE_SPACE_KEYS": None,
    }

    config = {}
    missing = []
    for key, description in required.items():
        val = os.environ.get(key)
        if not val:
            missing.append(f"  {key} — {description}")
        config[key] = val

    if missing:
        print("ERROR: Missing required environment variables:")
        print("\n".join(missing))
        sys.exit(1)

    for key in optional:
        config[key] = os.environ.get(key)

    if config.get("CONFLUENCE_SPACE_KEYS"):
        config["CONFLUENCE_SPACE_KEYS"] = [
            s.strip() for s in config["CONFLUENCE_SPACE_KEYS"].split(",") if s.strip()
        ]
    else:
        config["CONFLUENCE_SPACE_KEYS"] = []

    return config


def save_report(report: dict, jira_key: str):
    """Save validation report as JSON and Markdown."""
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    json_path = OUTPUT_DIR / f"{jira_key}_report.json"
    json_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    md_path = OUTPUT_DIR / f"{jira_key}_report.md"
    md_path.write_text(format_markdown_report(report), encoding="utf-8")

    print(f"Reports saved: {json_path}, {md_path}")
    return md_path


def format_markdown_report(report: dict) -> str:
    """Format the validation report as readable Markdown."""
    bug = report.get("bug", {})
    expected = report.get("expected_behavior", {})
    confluence = report.get("confluence_validation", {})
    code = report.get("code_validation", {})
    verdict = report.get("final_verdict", {})
    environment = report.get("environment", {})
    branch_switch = report.get("branch_switch", {})
    ts = report.get("timestamp", datetime.now(timezone.utc).isoformat())

    lines = [
        f"# Bug Validation Report: {bug.get('key', 'N/A')}",
        f"**Date:** {ts}",
        "",
        "---",
        "",
        "## Bug Details",
        f"**Key:** {bug.get('key', 'N/A')}",
        f"**Summary:** {bug.get('summary', 'N/A')}",
        f"**Status:** {bug.get('status', 'N/A')}",
        f"**Priority:** {bug.get('priority', 'N/A')}",
        "",
        "**Description:**",
        bug.get("description", "_No description provided._"),
        "",
        "---",
        "",
        "## 1. Expected Behavior",
        f"**Bug Claims:** {expected.get('bug_claims', 'N/A')}",
        f"**Context:** {expected.get('context', 'N/A')}",
        "",
        "---",
        "",
        build_sources_and_alignment_markdown(report),
        "",
        "---",
        "",
        "## 2. Confluence Validation",
        f"**Evidence Strength:** {humanize_evidence_strength(confluence.get('evidence_strength'))}",
        "",
        confluence.get("explanation", "_No Confluence analysis performed._"),
        "",
    ]

    sources = confluence.get("sources", [])
    if sources:
        lines.append("**Sources found:**")
        for src in sources:
            lines.append(f"- {src}")
        lines.append("")

    basis_md = build_confluence_basis_markdown({"confluence_validation": confluence})
    if basis_md:
        lines.append(basis_md)
        lines.append("")

    lines.extend([
        "---",
        "",
        "## 3. Code Analysis",
        f"**Behavior Match:** {humanize_behavior_match(code.get('behavior_match'))}",
        "",
    ])

    scan = report.get("code_scan") or {}
    if isinstance(scan, dict) and scan.get("status"):
        lines.append(f"**Local Phoenix scan:** `{scan.get('status')}`")
        if scan.get("phoenix_root"):
            lines.append(f"**Phoenix path:** `{scan['phoenix_root']}`")
        if scan.get("files_found") is not None:
            lines.append(f"**Files matched:** {scan['files_found']}")
        if scan.get("error"):
            lines.append(f"**Scan note:** {scan['error']}")
        elif scan.get("note"):
            lines.append(f"**Scan note:** {scan['note']}")
        lines.append("")

    if environment or branch_switch:
        env_name = environment.get("detected") or branch_switch.get("env") or "?"
        env_origin = "from bug text" if environment.get("explicit") else "default (no env keyword found)"
        lines.append(f"**Environment scanned:** `{env_name}` ({env_origin})")
        repos = branch_switch.get("repos") if isinstance(branch_switch, dict) else None
        if repos:
            lines.append("**Repository branches:**")
            for repo in repos:
                before = repo.get("before") or "?"
                after = repo.get("after") or "?"
                status = repo.get("status", "?")
                lines.append(
                    f"- `{repo.get('repo', '?')}`: `{before}` -> `{after}` ({status})"
                )
        elif isinstance(branch_switch, dict) and branch_switch.get("error"):
            lines.append(f"**Branch switch note:** {branch_switch['error']}")
        lines.append("")

    lines.extend([
        code.get("explanation", "_No code analysis performed._"),
        "",
    ])

    refs = code.get("references", [])
    if refs:
        lines.append("**Code References:**")
        for ref in refs:
            lines.append(f"- `{ref.get('file', '?')}` (lines {ref.get('lines', '?')}): {ref.get('implementation', '')}")
        lines.append("")

    lines.extend([
        "---",
        "",
        "## 4. Final Verdict",
        f"**Verdict:** {verdict.get('verdict', 'INSUFFICIENT_EVIDENCE')}",
        "",
        f"**Reasoning:** {verdict.get('reasoning', 'No reasoning provided.')}",
        "",
        f"**Next Steps:** {verdict.get('next_steps', 'No next steps provided.')}",
        "",
    ])

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Bug Validator — CI pipeline")
    parser.add_argument("--jira-key", required=True, help="Jira issue key (e.g. REG-456)")
    args = parser.parse_args()

    jira_key = args.jira_key.strip().upper()
    print(f"=== Bug Validator started for {jira_key} ===")

    config = load_env()
    timestamp = datetime.now(timezone.utc).isoformat()

    # --- Step 1: Fetch bug from Jira ---
    print(f"\n[1/5] Fetching bug details from Jira: {jira_key}")
    jira = JiraClient(
        base_url=config["JIRA_BASE_URL"],
        email=config["JIRA_EMAIL"],
        api_token=config["JIRA_API_TOKEN"],
    )
    bug = jira.get_issue(jira_key)
    print(f"  Bug: {bug['summary']}")
    print(f"  Status: {bug['status']}, Priority: {bug['priority']}")

    # --- Step 2: Validate against Confluence ---
    print("\n[2/5] Validating against Confluence documentation...")
    confluence_result = {"status": "skipped", "explanation": "Confluence not configured.", "sources": []}

    if config.get("CONFLUENCE_BASE_URL") and config.get("CONFLUENCE_API_TOKEN"):
        _c_url = config['CONFLUENCE_BASE_URL']
        print(f"  Confluence URL: {_c_url}")
        print(f"  Confluence URL length: {len(_c_url)}, ends_with_wiki: {_c_url.rstrip('/').endswith('/wiki')}, last_10_chars: '{_c_url[-10:]}'")
        print(f"  Confluence spaces: {config['CONFLUENCE_SPACE_KEYS'] or '(all)'}")
        print(f"  Confluence email set: {bool(config.get('CONFLUENCE_EMAIL'))}, token length: {len(config.get('CONFLUENCE_API_TOKEN', ''))}")
        confluence = ConfluenceClient(
            base_url=config["CONFLUENCE_BASE_URL"],
            email=config["CONFLUENCE_EMAIL"],
            api_token=config["CONFLUENCE_API_TOKEN"],
        )
        confluence_result = confluence.search_for_bug(
            summary=bug["summary"],
            description=bug["description"],
            space_keys=config["CONFLUENCE_SPACE_KEYS"],
        )
    else:
        print(f"  Confluence NOT configured (BASE_URL={bool(config.get('CONFLUENCE_BASE_URL'))}, TOKEN={bool(config.get('CONFLUENCE_API_TOKEN'))})")
    print(f"  Confluence status: {confluence_result['status']}")
    if confluence_result.get("sources"):
        print(f"  Confluence pages found: {len(confluence_result['sources'])}")

    # --- Step 3: Search local Phoenix codebase ---
    print("\n[3/5] Analyzing local Phoenix codebase...")
    phoenix_client = create_local_phoenix_client()

    # 3a. Resolve target environment from bug text and switch every Phoenix repo
    #     under PHOENIX_LOCAL_ROOT to the matching branch BEFORE scanning.
    bug_text = f"{bug.get('summary', '')}\n{bug.get('description', '')}"
    env, env_was_explicit = resolve_environment(bug_text)
    print(
        f"  Environment detected: '{env}' "
        f"({'from bug text' if env_was_explicit else 'default — no env keyword in bug'})"
    )

    # By default, fetch + merge from origin to get latest code for the target env.
    # Set BUG_VALIDATOR_SKIP_FETCH=1 to disable fetching (use only local refs).
    skip_fetch = os.environ.get("BUG_VALIDATOR_SKIP_FETCH", "").lower() in (
        "1", "true", "yes",
    )
    branch_switch = switch_phoenix_repos_to_env(
        phoenix_root=phoenix_client.phoenix_root,
        env=env,
        fetch=not skip_fetch,
    )
    for line in format_switch_summary(branch_switch):
        print(f"  {line}")

    code_results = phoenix_client.search_for_bug(
        summary=bug["summary"],
        description=bug["description"],
    )
    if code_results.get("status") == "unreachable":
        print(f"  Phoenix UNAVAILABLE — code analysis skipped ({code_results.get('error', 'unknown')})")
    elif code_results.get("status") == "no_results":
        print(f"  No relevant Phoenix code found for this bug")
    else:
        print(f"  Found {len(code_results['files'])} relevant files in local Phoenix codebase")

    # --- Step 4: Analyze with Gemini ---
    print("\n[4/5] Running AI analysis (Gemini)...")
    analyzer = BugAnalyzer(api_key=config["GEMINI_API_KEY"])
    analysis = analyzer.analyze(
        bug=bug,
        confluence_data=confluence_result,
        code_data=code_results,
    )
    verdict = analysis.get('final_verdict', {}).get('verdict', 'INSUFFICIENT_EVIDENCE')
    print(f"  Verdict: {verdict}")

    # --- Step 5: Build report and send to Slack ---
    confluence_for_report = merge_confluence_validation(confluence_result, analysis)
    report = {
        "timestamp": timestamp,
        "bug": bug,
        "environment": {
            "detected": env,
            "explicit": env_was_explicit,
            "default_used": not env_was_explicit,
        },
        "branch_switch": branch_switch,
        "expected_behavior": analysis.get("expected_behavior", {}),
        "confluence_validation": confluence_for_report,
        "code_validation": analysis.get("code_validation", {}),
        "code_scan": {
            "status": code_results.get("status"),
            "error": code_results.get("error"),
            "note": code_results.get("note"),
            "files_found": len(code_results.get("files") or []),
            "snippets_sent": len(code_results.get("snippets") or []),
            "phoenix_root": str(phoenix_client.phoenix_root),
        },
        "final_verdict": analysis.get("final_verdict", {"verdict": "INSUFFICIENT_EVIDENCE", "reasoning": "Analysis failed", "next_steps": "Check technical issues"}),
    }

    md_path = save_report(report, jira_key)

    print("\n[5/5] Sending report to Slack...")
    slack = SlackReporter(webhook_url=config["SLACK_WEBHOOK_URL"])
    slack.send_report(report)
    print("  Slack notification sent.")

    print(f"\n=== Bug Validator completed for {jira_key} ===")


if __name__ == "__main__":
    main()
