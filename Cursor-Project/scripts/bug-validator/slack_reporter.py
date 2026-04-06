"""
Slack reporter — sends bug validation results via incoming webhook.
"""

import json
import requests


class SlackReporter:
    def __init__(self, webhook_url: str):
        self.webhook_url = webhook_url

    def send_report(self, report: dict):
        """Send a formatted Slack message with bug validation results."""
        payload = {"blocks": self._build_blocks(report)}

        resp = requests.post(
            self.webhook_url,
            data=json.dumps(payload),
            headers={"Content-Type": "application/json"},
            timeout=15,
        )
        resp.raise_for_status()

    def _build_blocks(self, report: dict) -> list[dict]:
        bug = report.get("bug", {})
        analysis = report.get("analysis", {})
        confluence = report.get("confluence_validation", {})
        code = report.get("code_validation", {})

        is_valid = analysis.get("is_valid")
        if is_valid is True:
            verdict = ":white_check_mark: *VALID*"
        elif is_valid is False:
            verdict = ":x: *NOT VALID*"
        else:
            verdict = ":question: *INCONCLUSIVE*"

        conf_status = confluence.get("status", "N/A")
        code_status = code.get("status", "N/A")

        blocks = [
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
            {
                "type": "section",
                "fields": [
                    {"type": "mrkdwn", "text": f"*Confluence:*\n{conf_status}"},
                    {"type": "mrkdwn", "text": f"*Code Analysis:*\n{code_status}"},
                ],
            },
            {"type": "divider"},
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*Analysis:*\n{self._truncate(analysis.get('summary', 'No summary'), 500)}",
                },
            },
        ]

        refs = code.get("references", [])
        if refs:
            ref_lines = [f"• `{r.get('file', '?')}` (L{r.get('lines', '?')}): {r.get('note', '')}" for r in refs[:5]]
            blocks.append({
                "type": "section",
                "text": {"type": "mrkdwn", "text": f"*Code References:*\n{''.join(ref_lines)}"},
            })

        suggestion = analysis.get("suggested_fix")
        if suggestion:
            blocks.append({
                "type": "section",
                "text": {"type": "mrkdwn", "text": f"*Suggested Fix:*\n{self._truncate(suggestion, 300)}"},
            })

        blocks.append({
            "type": "context",
            "elements": [
                {"type": "mrkdwn", "text": f":robot_face: _Bug Validator (automated) | {report.get('timestamp', '')}_"},
            ],
        })

        return blocks

    @staticmethod
    def _truncate(text: str, max_len: int) -> str:
        if len(text) <= max_len:
            return text
        return text[:max_len - 3] + "..."
