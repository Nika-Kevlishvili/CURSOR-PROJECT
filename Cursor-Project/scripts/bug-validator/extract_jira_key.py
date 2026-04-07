#!/usr/bin/env python3
"""
GitHub Actions helper: write steps.jira.outputs.key from env (no bash required).

Reads:
  EVENT_NAME — github.event_name
  KEY_DISPATCH — github.event.client_payload.jira_key (repository_dispatch)
  KEY_INPUT — github.event.inputs.jira_key (workflow_dispatch)

Writes one line to GITHUB_OUTPUT: key=<value>
"""

import os
import sys


def main() -> None:
    event = os.environ.get("EVENT_NAME", "")
    if event == "repository_dispatch":
        key = (os.environ.get("KEY_DISPATCH") or "").strip()
    else:
        key = (os.environ.get("KEY_INPUT") or "").strip()

    out = os.environ.get("GITHUB_OUTPUT", "")
    if not out:
        print("ERROR: GITHUB_OUTPUT not set", file=sys.stderr)
        sys.exit(1)

    with open(out, "a", encoding="utf-8") as f:
        f.write(f"key={key}\n")


if __name__ == "__main__":
    main()
