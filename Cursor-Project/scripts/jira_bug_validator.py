#!/usr/bin/env python3
"""
Fetch Jira bugs from project PDT and run cursor /bag-validate for each one.
Designed to run as a background service - skips bugs already processed today.

Required environment variables:
  JIRA_EMAIL      - your Atlassian account email
  JIRA_API_TOKEN  - your Atlassian API token (https://id.atlassian.com/manage-profile/security/api-tokens)

Usage:
  python3 jira_bug_validator.py
"""

import json
import os
import re
import subprocess
import sys
from datetime import date
from pathlib import Path

import requests
from requests.auth import HTTPBasicAuth

# ── Config ──────────────────────────────────────────────────────────────────
JIRA_BASE_URL = "https://oppa-support.atlassian.net"
JIRA_BROWSE_URL = f"{JIRA_BASE_URL}/browse"
PROJECT_KEY = "PDT"
OUTPUT_DIR = Path(__file__).parent / "bug_logs"
CURSOR_WORKSPACE = r"C:\Users\N.kevlishvili\Cursor"
MAX_RESULTS = 100  # max per page; script paginates automatically
MAX_REPRO_STEPS = 8
# ─────────────────────────────────────────────────────────────────────────────


def get_credentials() -> tuple[str, str]:
    email = os.environ.get("JIRA_EMAIL")
    token = os.environ.get("JIRA_API_TOKEN")
    if not email or not token:
        sys.exit(
            "Error: JIRA_EMAIL and JIRA_API_TOKEN environment variables must be set.\n"
            "  export JIRA_EMAIL=you@example.com\n"
            "  export JIRA_API_TOKEN=your_token_here"
        )
    return email, token


def get_todays_file() -> Path:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    today_str = date.today().strftime("%d.%m.%Y")
    return OUTPUT_DIR / f"{today_str}.json"


def load_todays_urls(file_path: Path) -> set[str]:
    """Return the set of URLs already stored in today's file."""
    if not file_path.exists():
        return set()
    data = json.loads(file_path.read_text(encoding="utf-8"))
    return set(data.get("urls", []))


def save_urls(file_path: Path, urls: set[str]) -> None:
    sorted_urls = sorted(urls)
    payload = {"project": PROJECT_KEY, "date": date.today().strftime("%d.%m.%Y"), "bug_count": len(sorted_urls), "urls": sorted_urls}
    file_path.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def fetch_bugs(email: str, token: str) -> list[dict]:
    """Return a list of issue dicts with key, url, summary for all matching bugs."""
    auth = HTTPBasicAuth(email, token)
    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    jql = (
        f'project = "{PROJECT_KEY}" AND issuetype = Bug '
        f'AND status in (Backlog, "Needs Approval") '
        f'AND created >= -7d '
        f'ORDER BY created DESC'
    )
    print(f"  JQL: {jql}")

    issues_out: list[dict] = []
    next_page_token: str | None = None

    while True:
        body: dict = {
            "jql": jql,
            "maxResults": MAX_RESULTS,
            "fields": ["summary", "status", "issuetype"],
        }
        if next_page_token:
            body["nextPageToken"] = next_page_token

        response = requests.post(
            f"{JIRA_BASE_URL}/rest/api/3/search/jql",
            headers=headers,
            auth=auth,
            json=body,
            timeout=30,
        )

        if response.status_code == 401:
            sys.exit("Error: Jira authentication failed. Check JIRA_EMAIL and JIRA_API_TOKEN.")
        if not response.ok:
            sys.exit(f"Error: Jira API returned {response.status_code}: {response.text}")

        data = response.json()
        issues = data.get("issues", [])

        for issue in issues:
            key = issue["key"]
            summary = issue["fields"]["summary"]
            url = f"{JIRA_BROWSE_URL}/{key}"
            issues_out.append({"key": key, "url": url, "summary": summary})
            print(f"  Found: [{key}] {summary}")

        if data.get("isLast", True) or not issues:
            break

        next_page_token = data.get("nextPageToken")
        if not next_page_token:
            break

    return issues_out


def _extract_text(node) -> str:
    """Recursively extract plain text from Jira Atlassian Document Format (ADF)."""
    if node is None:
        return ""
    if isinstance(node, str):
        return node
    if isinstance(node, dict):
        if node.get("type") == "text":
            return node.get("text", "")
        return "".join(_extract_text(child) for child in node.get("content", []))
    if isinstance(node, list):
        return " ".join(_extract_text(item) for item in node)
    return ""


def fetch_issue_details(email: str, token: str, key: str) -> dict:
    """Fetch full issue fields for a single Jira issue key."""
    auth = HTTPBasicAuth(email, token)
    headers = {"Accept": "application/json"}
    response = requests.get(
        f"{JIRA_BASE_URL}/rest/api/3/issue/{key}",
        headers=headers,
        auth=auth,
        params={"fields": "summary,description,status,priority,environment,customfield_10002"},
        timeout=30,
    )
    if not response.ok:
        return {}
    fields = response.json().get("fields", {})

    description_raw = fields.get("description") or ""
    description = _extract_text(description_raw) if isinstance(description_raw, dict) else str(description_raw)

    return {
        "summary": fields.get("summary", ""),
        "status": (fields.get("status") or {}).get("name", ""),
        "priority": (fields.get("priority") or {}).get("name", ""),
        "description": description.strip(),
    }


def extract_urls(text: str) -> list[str]:
    if not text:
        return []
    # Keep links unique while preserving order.
    seen: set[str] = set()
    links: list[str] = []
    for match in re.finditer(r"https?://[^\s)\]}>,]+", text):
        url = match.group(0).rstrip(".,;:")
        if url not in seen:
            seen.add(url)
            links.append(url)
    return links


def extract_log_signals(text: str) -> list[str]:
    """Pull probable log/error lines from free-form description."""
    if not text:
        return []
    patterns = (
        r"error",
        r"exception",
        r"traceback",
        r"stack trace",
        r"\b5\d{2}\b",
        r"\b4\d{2}\b",
        r"failed",
        r"timeout",
    )
    signal_re = re.compile("|".join(patterns), re.IGNORECASE)
    lines: list[str] = []
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            continue
        if signal_re.search(line):
            lines.append(line)
    return lines[:10]


def _clean_step_text(text: str) -> str:
    cleaned = re.sub(r"^[\-\*\u2022]\s*", "", text).strip()
    cleaned = re.sub(r"^\d+[\.\)]\s*", "", cleaned).strip()
    return cleaned


def infer_reproduce_steps(summary: str, description: str) -> list[str]:
    """
    Generate candidate reproduce steps from issue data.
    Works even when exact steps are not explicitly provided.
    """
    steps: list[str] = []
    if description:
        lines = [line.strip() for line in description.splitlines() if line.strip()]
        for line in lines:
            if re.match(r"^\d+[\.\)]\s+", line) or line.startswith(("-", "*", "•")):
                cleaned = _clean_step_text(line)
                if len(cleaned) > 6:
                    steps.append(cleaned)
            elif re.search(r"\b(open|go to|navigate|select|click|create|update|save|submit|send|delete|refresh|search)\b", line, re.IGNORECASE):
                if len(line) > 12:
                    steps.append(line)

    if not steps:
        # Build synthetic baseline flow when explicit steps are missing.
        steps = [
            "Open the relevant module/page described in the ticket summary.",
            "Prepare minimal preconditions that match the bug context (user role, data state, environment).",
            "Perform the primary action implied by the bug summary.",
            "Observe UI/API/log behavior and capture actual response details.",
        ]

    unique_steps: list[str] = []
    seen: set[str] = set()
    for step in steps:
        normalized = step.lower()
        if normalized not in seen:
            seen.add(normalized)
            unique_steps.append(step)
    return unique_steps[:MAX_REPRO_STEPS]


def format_bullet_list(items: list[str], empty_text: str) -> str:
    if not items:
        return f"- {empty_text}"
    return "\n".join(f"- {item}" for item in items)


def run_cursor_command(prompt: str, label: str) -> int:
    cmd = [
        "cursor", "agent",
        "--print",
        "--trust",
        "--workspace", CURSOR_WORKSPACE,
        prompt,
    ]
    print(f"    Running command: {label}")
    result = subprocess.run(cmd, capture_output=False)
    if result.returncode != 0:
        print(f"    Warning: command '{label}' exited with code {result.returncode}")
    return result.returncode


def run_cursor_pipeline(issues: list[dict], email: str, token: str) -> None:
    print(f"\nRunning multi-agent bug pipeline for {len(issues)} new bug(s)...\n")
    for issue in issues:
        key = issue["key"]
        url = issue["url"]

        print(f"  Fetching full details for {key}...")
        details = fetch_issue_details(email, token, key)
        summary = details.get("summary", issue["summary"])
        description = details.get("description", "(no description retrieved)")
        links = extract_urls(description)
        log_signals = extract_log_signals(description)
        inferred_steps = infer_reproduce_steps(summary, description)

        common_context = (
            f"Ticket: {key}\n"
            f"URL: {url}\n"
            f"Summary: {summary}\n"
            f"Status: {details.get('status', '')}\n"
            f"Priority: {details.get('priority', '')}\n"
            f"Description:\n{description}\n\n"
            f"Extracted links:\n{format_bullet_list(links, 'No links found in ticket description.')}\n\n"
            f"Extracted log/error signals:\n{format_bullet_list(log_signals, 'No clear log/error line found in description.')}\n\n"
            f"Inferred reproduce flow candidates:\n{format_bullet_list(inferred_steps, 'Could not infer direct steps from the text.')}\n"
        )

        bug_validate_prompt = (
            f"/bug-validate\n\n"
            f"{common_context}\n"
            "Validation policy for this ticket:\n"
            "- Do NOT invalidate only because exact reproduce steps are missing.\n"
            "- Use all available data (description, links, log signals, inferred flow).\n"
            "- Build a practical reproduce scenario for a similar real-world case.\n"
            "- Final output must state whether bug triggering appears achievable on a realistic scenario, even with incomplete original steps.\n"
        )

        cross_dependency_prompt = (
            f"/cross-dependency-finder\n\n"
            f"{common_context}\n"
            "Focus:\n"
            "- Identify upstream/downstream dependencies and what could break for this bug scope.\n"
            "- Return structured cross-dependency output suitable for test-case generation handoff.\n"
        )

        test_case_prompt = (
            f"/test-case-generate\n\n"
            f"{common_context}\n"
            "Generation policy:\n"
            "- Generate comprehensive Backend and Frontend test cases for this ticket.\n"
            "- Use the dedicated TestCaseGeneratorAgent logic and required folder structure.\n"
            "- Include at least one scenario designed to trigger the reported/similar bug condition.\n"
        )

        playwright_prompt = (
            f"/energo-ts-test\n\n"
            f"{common_context}\n"
            "Generation policy:\n"
            "- Create Playwright test(s) using EnergoTSTestAgent logic.\n"
            "- Build tests from generated test cases for this ticket.\n"
            "- Keep tests focused on reproducing/triggering the bug condition on realistic data.\n"
            "- Do not skip generation because original ticket steps are incomplete.\n"
        )

        validator_prompt = (
            "Validate generated Playwright tests against generated test cases "
            "using playwright-test-validator subagent.\n\n"
            f"{common_context}\n"
            "Validation targets:\n"
            f"- Backend test cases path: Cursor-Project/test_cases/Backend/{key}.md (or matching topic generated for this ticket)\n"
            f"- Frontend test cases path: Cursor-Project/test_cases/Frontend/{key}.md (or matching topic generated for this ticket)\n"
            f"- Playwright spec path: Cursor-Project/EnergoTS/tests/cursor/{key}-*.spec.ts\n"
            "Validation rules:\n"
            "- Check syntax, 1:1 TC coverage, alignment with test cases, and framework usage.\n"
            "- If validation fails, return actionable issues and keep status as failed.\n"
        )

        run_prompt = (
            f"/energo-ts-run\n\n"
            f"Run Playwright test(s) for ticket {key} from EnergoTS cursor branch.\n"
            f"Target: Cursor-Project/EnergoTS/tests/cursor/{key}-*.spec.ts (or tests tagged/named with {key}).\n"
            "Return clear pass/fail output and key failure reasons if any.\n"
        )

        print(f"  Running agent chain for {key}")
        run_cursor_command(bug_validate_prompt, f"{key} /bug-validate")
        run_cursor_command(cross_dependency_prompt, f"{key} /cross-dependency-finder")
        run_cursor_command(test_case_prompt, f"{key} /test-case-generate")
        playwright_rc = run_cursor_command(playwright_prompt, f"{key} /energo-ts-test")
        if playwright_rc != 0:
            print(f"    Skipping validation and run for {key} because Playwright generation failed.")
            continue

        validator_rc = run_cursor_command(validator_prompt, f"{key} playwright-test-validator")
        if validator_rc != 0:
            print(f"    Skipping run step for {key} because Playwright validation failed.")
            continue

        run_cursor_command(run_prompt, f"{key} /energo-ts-run")


def main() -> None:
    email, token = get_credentials()

    today_file = get_todays_file()
    already_processed = load_todays_urls(today_file)

    if already_processed:
        print(f"Today's log exists ({today_file.name}) with {len(already_processed)} bug(s) already processed.")
    else:
        print(f"No log for today yet. Starting fresh → {today_file.name}")

    print(f"\nFetching Bug issues from project '{PROJECT_KEY}'...")
    fetched_issues = fetch_bugs(email, token)

    if not fetched_issues:
        print("No bugs found on Jira. Nothing to process.")
        return

    fetched_urls = {i["url"] for i in fetched_issues}
    new_urls = sorted(fetched_urls - already_processed)

    if not new_urls:
        print(f"\nAll {len(fetched_urls)} fetched bug(s) were already processed today. Nothing to do.")
        return

    new_issues = [i for i in fetched_issues if i["url"] in new_urls]
    print(f"\nNew bugs since last run: {len(new_issues)}")

    # Merge and persist
    updated_urls = already_processed | set(new_urls)
    save_urls(today_file, updated_urls)
    print(f"Saved {len(updated_urls)} total bug(s) to {today_file}")

    run_cursor_pipeline(new_issues, email, token)


if __name__ == "__main__":
    main()
