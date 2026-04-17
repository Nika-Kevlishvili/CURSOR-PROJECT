"""
Slack reporter — sends bug validation results via Slack Web API.

Layout and labels live in slack_report_template.py (edit that file to change structure).
"""

import requests

from slack_report_template import build_slack_blocks


class SlackReporter:
    def __init__(self, bot_token: str, channel: str):
        self.bot_token = bot_token
        self.channel = channel

    def send_report(self, report: dict):
        """Send a formatted Slack message with bug validation results."""
        payload = {
            "channel": self.channel,
            "text": f"Bug Validation: {report.get('bug', {}).get('key', 'N/A')}",
            "blocks": build_slack_blocks(report),
        }

        resp = requests.post(
            "https://slack.com/api/chat.postMessage",
            json=payload,
            headers={
                "Authorization": f"Bearer {self.bot_token}",
                "Content-Type": "application/json; charset=utf-8",
            },
            timeout=15,
        )
        resp.raise_for_status()
        data = resp.json()
        if not data.get("ok"):
            raise RuntimeError(f"Slack API error: {data.get('error', 'unknown_error')}")
