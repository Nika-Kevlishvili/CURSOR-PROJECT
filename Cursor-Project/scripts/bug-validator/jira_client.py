"""
Jira REST API client — fetches bug issue details.
"""

import re
import requests
from requests.auth import HTTPBasicAuth


class JiraClient:
    def __init__(self, base_url: str, email: str, api_token: str):
        self.base_url = base_url.rstrip("/")
        self.auth = HTTPBasicAuth(email, api_token)
        self.headers = {"Accept": "application/json"}

    def get_issue(self, issue_key: str) -> dict:
        """Fetch a Jira issue and return normalised bug dict."""
        url = f"{self.base_url}/rest/api/3/issue/{issue_key}"
        params = {"fields": "summary,description,status,priority,issuetype,labels,components,created,updated,assignee,reporter"}

        resp = requests.get(url, headers=self.headers, auth=self.auth, params=params, timeout=30)
        resp.raise_for_status()
        data = resp.json()

        fields = data.get("fields", {})
        return {
            "key": data.get("key", issue_key),
            "summary": fields.get("summary", ""),
            "description": self._extract_text(fields.get("description")),
            "status": self._nested(fields, "status", "name"),
            "priority": self._nested(fields, "priority", "name"),
            "issue_type": self._nested(fields, "issuetype", "name"),
            "labels": fields.get("labels", []),
            "components": [c.get("name", "") for c in fields.get("components", [])],
            "created": fields.get("created", ""),
            "updated": fields.get("updated", ""),
            "assignee": self._nested(fields, "assignee", "displayName"),
            "reporter": self._nested(fields, "reporter", "displayName"),
        }

    def _extract_text(self, description) -> str:
        """
        Jira Cloud uses Atlassian Document Format (ADF) for descriptions.
        Extract plain text recursively.
        """
        if description is None:
            return ""
        if isinstance(description, str):
            return description

        parts = []
        self._walk_adf(description, parts)
        return "\n".join(parts).strip()

    def _walk_adf(self, node: dict, parts: list):
        if not isinstance(node, dict):
            return
        if node.get("type") == "text":
            parts.append(node.get("text", ""))
        for child in node.get("content", []):
            self._walk_adf(child, parts)

    @staticmethod
    def _nested(fields: dict, key: str, subkey: str) -> str:
        obj = fields.get(key)
        if isinstance(obj, dict):
            return obj.get(subkey, "")
        return ""

    @staticmethod
    def extract_keywords(text: str) -> list[str]:
        """Pull meaningful keywords from bug text for code search."""
        stop = {
            "the", "a", "an", "is", "are", "was", "were", "be", "been",
            "to", "of", "in", "for", "on", "at", "by", "with", "from",
            "and", "or", "but", "not", "this", "that", "it", "as", "if",
            "when", "should", "must", "can", "does", "do", "has", "have",
            "will", "would", "could", "after", "before", "into", "than",
        }
        words = re.findall(r"[A-Za-z_][A-Za-z0-9_]{2,}", text)
        seen = set()
        keywords = []
        for w in words:
            low = w.lower()
            if low not in stop and low not in seen:
                seen.add(low)
                keywords.append(w)
        return keywords[:20]
