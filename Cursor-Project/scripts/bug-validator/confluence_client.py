"""
Confluence REST API client — read-only documentation search.
"""

import requests
from requests.auth import HTTPBasicAuth


class ConfluenceClient:
    def __init__(self, base_url: str, email: str, api_token: str):
        self.base_url = base_url.rstrip("/")
        self.auth = HTTPBasicAuth(email, api_token)
        self.headers = {"Accept": "application/json"}

    def search_for_bug(self, summary: str, description: str, space_keys: list[str] | None = None) -> dict:
        """
        Search Confluence for pages related to the bug.
        Returns dict with status, explanation, and sources.
        """
        keywords = self._extract_keywords(summary, description)
        pages = []

        for kw in keywords[:5]:
            results = self._cql_search(kw, space_keys)
            for page in results:
                if not any(p["id"] == page["id"] for p in pages):
                    pages.append(page)

        if not pages:
            return {
                "status": "no_results",
                "explanation": f"No Confluence pages found for keywords: {', '.join(keywords[:5])}",
                "sources": [],
                "page_contents": [],
            }

        page_contents = []
        for page in pages[:5]:
            content = self._get_page_content(page["id"])
            if content:
                page_contents.append({
                    "id": page["id"],
                    "title": page["title"],
                    "url": page.get("url", ""),
                    "excerpt": content[:3000],
                })

        return {
            "status": "found",
            "explanation": f"Found {len(pages)} related Confluence pages.",
            "sources": [f"[{p['title']}]({p.get('url', '')})" for p in pages[:5]],
            "page_contents": page_contents,
        }

    def _cql_search(self, query: str, space_keys: list[str] | None = None) -> list[dict]:
        """Search Confluence using CQL."""
        cql = f'text ~ "{query}"'
        if space_keys:
            spaces = " OR ".join(f'space = "{s}"' for s in space_keys)
            cql = f'({cql}) AND ({spaces})'

        url = f"{self.base_url}/wiki/rest/api/content/search"
        params = {"cql": cql, "limit": 5}

        try:
            resp = requests.get(url, headers=self.headers, auth=self.auth, params=params, timeout=15)
            resp.raise_for_status()
            data = resp.json()
            results = []
            for item in data.get("results", []):
                results.append({
                    "id": item.get("id", ""),
                    "title": item.get("title", ""),
                    "url": f"{self.base_url}/wiki{item.get('_links', {}).get('webui', '')}",
                })
            return results
        except requests.RequestException as e:
            print(f"  WARNING: Confluence search failed for '{query}': {e}")
            return []

    def _get_page_content(self, page_id: str) -> str | None:
        """Get page body content as plain-ish text."""
        url = f"{self.base_url}/wiki/rest/api/content/{page_id}"
        params = {"expand": "body.storage"}

        try:
            resp = requests.get(url, headers=self.headers, auth=self.auth, params=params, timeout=15)
            resp.raise_for_status()
            body = resp.json().get("body", {}).get("storage", {}).get("value", "")
            return self._strip_html(body)
        except requests.RequestException:
            return None

    @staticmethod
    def _strip_html(html: str) -> str:
        """Rough HTML tag removal for readable text."""
        import re
        text = re.sub(r"<[^>]+>", " ", html)
        text = re.sub(r"\s+", " ", text)
        return text.strip()

    @staticmethod
    def _extract_keywords(summary: str, description: str) -> list[str]:
        import re
        combined = f"{summary} {description}"
        words = re.findall(r"[A-Za-z_][A-Za-z0-9_]{3,}", combined)
        stop = {"the", "that", "this", "with", "from", "should", "would", "could",
                "have", "been", "when", "after", "before", "does", "into"}
        seen = set()
        result = []
        for w in words:
            low = w.lower()
            if low not in stop and low not in seen:
                seen.add(low)
                result.append(w)
        return result[:10]
