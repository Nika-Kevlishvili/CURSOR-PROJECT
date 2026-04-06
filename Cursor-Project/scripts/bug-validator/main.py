"""
Bug Validator — CI entry point.

Triggered by GitHub Actions when a Jira bug reaches a specific status.
Flow: Jira → Confluence → GitLab code → Claude analysis → Slack report.
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

from jira_client import JiraClient
from gitlab_client import GitLabClient
from confluence_client import ConfluenceClient
from analyzer import BugAnalyzer
from slack_reporter import SlackReporter

OUTPUT_DIR = Path(__file__).parent / "output"


def load_env():
    """Load and validate required environment variables."""
    required = {
        "JIRA_BASE_URL": "Jira base URL (e.g. https://yourorg.atlassian.net)",
        "JIRA_EMAIL": "Jira account email",
        "JIRA_API_TOKEN": "Jira API token",
        "GITLAB_URL": "GitLab base URL (e.g. https://gitlab.example.com)",
        "GITLAB_TOKEN": "GitLab read-only access token",
        "GITLAB_PROJECT_IDS": "Comma-separated GitLab project IDs",
        "ANTHROPIC_API_KEY": "Anthropic (Claude) API key",
        "SLACK_WEBHOOK_URL": "Slack incoming webhook URL",
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

    config["GITLAB_PROJECT_IDS"] = [
        pid.strip() for pid in config["GITLAB_PROJECT_IDS"].split(",") if pid.strip()
    ]
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
    confluence = report.get("confluence_validation", {})
    code = report.get("code_validation", {})
    analysis = report.get("analysis", {})
    ts = report.get("timestamp", datetime.now(timezone.utc).isoformat())

    valid_emoji = {True: "VALID", False: "NOT VALID", None: "INCONCLUSIVE"}
    is_valid = analysis.get("is_valid")

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
        "## 1. Confluence Validation",
        f"**Status:** {confluence.get('status', 'N/A')}",
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

    lines.extend([
        "---",
        "",
        "## 2. Code Analysis",
        f"**Status:** {code.get('status', 'N/A')}",
        "",
        code.get("explanation", "_No code analysis performed._"),
        "",
    ])

    refs = code.get("references", [])
    if refs:
        lines.append("**Code References:**")
        for ref in refs:
            lines.append(f"- `{ref.get('file', '?')}` (lines {ref.get('lines', '?')}): {ref.get('note', '')}")
        lines.append("")

    lines.extend([
        "---",
        "",
        "## 3. Conclusion",
        f"**Bug Valid:** {valid_emoji.get(is_valid, 'INCONCLUSIVE')}",
        "",
        analysis.get("summary", "_No analysis summary._"),
        "",
        "### Detailed Analysis",
        analysis.get("details", ""),
        "",
    ])

    suggestion = analysis.get("suggested_fix")
    if suggestion:
        lines.extend([
            "### Suggested Fix (not implemented)",
            suggestion,
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
    print(f"  Confluence status: {confluence_result['status']}")

    # --- Step 3: Search codebase via GitLab API ---
    print("\n[3/5] Analyzing codebase via GitLab API (read-only)...")
    gitlab = GitLabClient(
        base_url=config["GITLAB_URL"],
        token=config["GITLAB_TOKEN"],
        project_ids=config["GITLAB_PROJECT_IDS"],
    )
    code_results = gitlab.search_for_bug(
        summary=bug["summary"],
        description=bug["description"],
    )
    print(f"  Found {len(code_results['files'])} relevant files across {len(config['GITLAB_PROJECT_IDS'])} projects")

    # --- Step 4: Analyze with Claude ---
    print("\n[4/5] Running AI analysis (Claude)...")
    analyzer = BugAnalyzer(api_key=config["ANTHROPIC_API_KEY"])
    analysis = analyzer.analyze(
        bug=bug,
        confluence_data=confluence_result,
        code_data=code_results,
    )
    print(f"  Verdict: {'VALID' if analysis['analysis']['is_valid'] else 'NOT VALID' if analysis['analysis']['is_valid'] is False else 'INCONCLUSIVE'}")

    # --- Step 5: Build report and send to Slack ---
    report = {
        "timestamp": timestamp,
        "bug": bug,
        "confluence_validation": analysis.get("confluence_validation", confluence_result),
        "code_validation": analysis.get("code_validation", {}),
        "analysis": analysis.get("analysis", {}),
    }

    md_path = save_report(report, jira_key)

    print("\n[5/5] Sending report to Slack...")
    slack = SlackReporter(webhook_url=config["SLACK_WEBHOOK_URL"])
    slack.send_report(report)
    print("  Slack notification sent.")

    print(f"\n=== Bug Validator completed for {jira_key} ===")


if __name__ == "__main__":
    main()
