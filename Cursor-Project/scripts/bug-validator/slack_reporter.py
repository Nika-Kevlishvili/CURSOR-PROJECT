"""
Slack reporter — sends bug validation results via Slack incoming webhook.

The webhook is bound to a specific channel at creation time in Slack,
so no channel id is needed here.

Layout and labels live in slack_report_template.py (edit that file to change structure).
"""
import requests

from slack_report_template import build_slack_blocks


class SlackReporter:
    def __init__(self, webhook_url: str):
        self.webhook_url = webhook_url

    def send_report(self, report: dict):
        """Send a formatted Slack message with bug validation results."""
        payload = {
            "text": f"Bug Validation: {report.get('bug', {}).get('key', 'N/A')}",
            "blocks": build_slack_blocks(report),
        }

        resp = requests.post(self.webhook_url, json=payload, timeout=15)
        resp.raise_for_status()
        if resp.text.strip().lower() != "ok":
            raise RuntimeError(f"Slack webhook error: {resp.status_code} {resp.text}")
