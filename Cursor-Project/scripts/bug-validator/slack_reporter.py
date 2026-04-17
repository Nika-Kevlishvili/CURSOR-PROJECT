"""
Slack reporter — sends bug validation results via incoming webhook.

Layout and labels live in slack_report_template.py (edit that file to change structure).
"""

import requests

from slack_report_template import build_slack_blocks


class SlackReporter:
    def __init__(self, webhook_url: str | None, channel: str, bot_token: str | None = None):
        self.webhook_url = webhook_url
        self.channel = channel
        self.bot_token = bot_token

    def send_report(self, report: dict):
        """Send a formatted Slack message with bug validation results."""
        payload = {
            "channel": self.channel,
            "text": f"Bug Validation: {report.get('bug', {}).get('key', 'N/A')}",
            "blocks": build_slack_blocks(report),
        }

        # Preferred path: Web API with bot token (reliably honors channel ID).
        if self.bot_token:
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
            return

        # Fallback: incoming webhook send (channel override depends on app settings).
        if not self.webhook_url:
            raise RuntimeError("Slack notifier is not configured: provide SLACK_BOT_TOKEN or SLACK_WEBHOOK_URL.")

        resp = requests.post(self.webhook_url, json=payload, timeout=15)
        resp.raise_for_status()
