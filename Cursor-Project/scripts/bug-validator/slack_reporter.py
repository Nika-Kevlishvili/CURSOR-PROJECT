"""
Slack reporter — sends bug validation results via incoming webhook.

Layout and labels live in slack_report_template.py (edit that file to change structure).
"""

import json
import requests

from slack_report_template import build_slack_blocks


class SlackReporter:
    def __init__(self, webhook_url: str, channel: str | None = None):
        self.webhook_url = webhook_url
        self.channel = channel

    def send_report(self, report: dict):
        """Send a formatted Slack message with bug validation results."""
        payload = {"blocks": build_slack_blocks(report)}
        if self.channel:
            payload["channel"] = self.channel

        resp = requests.post(
            self.webhook_url,
            data=json.dumps(payload),
            headers={"Content-Type": "application/json"},
            timeout=15,
        )
        resp.raise_for_status()
