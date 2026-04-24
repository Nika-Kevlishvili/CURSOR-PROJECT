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
CURSOR_WORKSPACE = "C:\Users\N.kevlishvili\Cursor"
MAX_RESULTS = 100  # max per page; script paginates automatically
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


def run_cursor(issues: list[dict], email: str, token: str) -> None:
    print(f"\nRunning cursor agent /bug-validate for {len(issues)} new bug(s)...\n")
    for issue in issues:
        key = issue["key"]
        url = issue["url"]

        print(f"  Fetching full details for {key}...")
        details = fetch_issue_details(email, token, key)

        prompt = (
            f"/bug-validate\n\n"
            f"Ticket: {key}\n"
            f"URL: {url}\n"
            f"Summary: {details.get('summary', issue['summary'])}\n"
            f"Status: {details.get('status', '')}\n"
            f"Priority: {details.get('priority', '')}\n"
            f"Description:\n{details.get('description', '(no description retrieved)')}\n"
        )

        cmd = [
            "cursor", "agent",
            "--print",
            "--trust",
            "--workspace", CURSOR_WORKSPACE,
            prompt,
        ]
        print(f"  Running cursor agent for {key}")
        result = subprocess.run(cmd, capture_output=False)
        if result.returncode != 0:
            print(f"  Warning: cursor agent exited with code {result.returncode} for {key}")


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

    run_cursor(new_issues, email, token)


if __name__ == "__main__":
    main()
