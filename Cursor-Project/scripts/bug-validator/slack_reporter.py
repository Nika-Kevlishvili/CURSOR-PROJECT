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
        expected = report.get("expected_behavior", {})
        confluence = report.get("confluence_validation", {})
        code = report.get("code_validation", {})
        final_verdict = report.get("final_verdict", {})

        verdict_raw = final_verdict.get("verdict", "INSUFFICIENT_EVIDENCE")
        verdict_emojis = {
            "VALID": ":white_check_mark: *VALID*",
            "NEEDS_CLARIFICATION": ":warning: *NEEDS CLARIFICATION*",
            "NEEDS_APPROVAL": ":hourglass_flowing_sand: *NEEDS APPROVAL*", 
            "NOT_VALID": ":x: *NOT VALID*",
            "INSUFFICIENT_EVIDENCE": ":grey_question: *INSUFFICIENT EVIDENCE*"
        }
        verdict = verdict_emojis.get(verdict_raw, f":question: *{verdict_raw}*")

        evidence_strength = confluence.get("evidence_strength", "N/A")
        behavior_match = code.get("behavior_match", "N/A")

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
                    {"type": "mrkdwn", "text": f"*Confluence:*\n{evidence_strength}"},
                    {"type": "mrkdwn", "text": f"*Code Analysis:*\n{behavior_match}"},
                ],
            },
            {"type": "divider"},
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*Reasoning:*\n{self._truncate(final_verdict.get('reasoning', 'No reasoning provided'), 400)}\n\n*Next Steps:*\n{self._truncate(final_verdict.get('next_steps', 'No next steps provided'), 300)}",
                },
            },
        ]

        refs = code.get("references", [])
        if refs:
            ref_lines = [f"• `{r.get('file', '?')}` (L{r.get('lines', '?')}): {r.get('implementation', '')}" for r in refs[:5]]
            blocks.append({
                "type": "section",
                "text": {"type": "mrkdwn", "text": f"*Code References:*\n" + "\n".join(ref_lines)},
            })

        # Next steps are now included in the main reasoning section above

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
