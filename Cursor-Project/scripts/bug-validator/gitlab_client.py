"""
GitLab REST API client — read-only code search and file retrieval.
"""

import re
import urllib.parse
import requests


class GitLabClient:
    def __init__(self, base_url: str, token: str, project_ids: list[str]):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.project_ids = project_ids
        self.headers = {"PRIVATE-TOKEN": token}

    def search_for_bug(self, summary: str, description: str) -> dict:
        """
        Search across all configured projects for code related to the bug.
        Returns dict with 'files' (list of relevant file hits) and 'snippets'.
        """
        keywords = self._extract_search_terms(summary, description)
        all_files = []
        all_snippets = []

        for pid in self.project_ids:
            for term in keywords[:10]:
                hits = self._search_blobs(pid, term)
                for hit in hits:
                    entry = {
                        "project_id": pid,
                        "file": hit.get("filename", ""),
                        "ref": hit.get("ref", "main"),
                        "startline": hit.get("startline", 0),
                        "lines": hit.get("data", ""),
                        "search_term": term,
                    }
                    if not any(f["file"] == entry["file"] and f["project_id"] == pid for f in all_files):
                        all_files.append(entry)

        for f in all_files[:15]:
            content = self._read_file(f["project_id"], f["file"], f["ref"])
            if content:
                all_snippets.append({
                    "project_id": f["project_id"],
                    "file": f["file"],
                    "content": content[:5000],
                })

        return {
            "files": all_files,
            "snippets": all_snippets,
            "keywords_used": keywords,
        }

    def _search_blobs(self, project_id: str, query: str) -> list[dict]:
        """Search for code blobs in a project."""
        url = f"{self.base_url}/api/v4/projects/{project_id}/search"
        params = {"scope": "blobs", "search": query, "per_page": 5}
        try:
            resp = requests.get(url, headers=self.headers, params=params, timeout=15)
            resp.raise_for_status()
            return resp.json()
        except requests.RequestException as e:
            print(f"  WARNING: GitLab search failed for '{query}' in project {project_id}: {e}")
            return []

    def _read_file(self, project_id: str, file_path: str, ref: str = "main") -> str | None:
        """Read a file's raw content from GitLab."""
        encoded = urllib.parse.quote(file_path, safe="")
        url = f"{self.base_url}/api/v4/projects/{project_id}/repository/files/{encoded}/raw"
        params = {"ref": ref}
        try:
            resp = requests.get(url, headers=self.headers, params=params, timeout=15)
            resp.raise_for_status()
            return resp.text
        except requests.RequestException:
            return None

    def _extract_search_terms(self, summary: str, description: str) -> list[str]:
        """
        Extract meaningful search terms: CamelCase class names, method names,
        technical terms from bug summary and description.
        """
        combined = f"{summary} {description}"

        camel_case = re.findall(r"\b[A-Z][a-z]+(?:[A-Z][a-z]+)+\b", combined)

        dotted = re.findall(r"\b\w+\.\w+(?:\.\w+)*\b", combined)

        technical = re.findall(r"\b(?:service|controller|handler|repository|entity|dto|mapper|"
                               r"endpoint|api|invoice|contract|payment|liability|receivable|"
                               r"deposit|customer|pod|cancel|create|update|delete|reverse|"
                               r"offset|amount|status|validation|scheduler)\b",
                               combined, re.IGNORECASE)

        all_terms = list(dict.fromkeys(camel_case + dotted + technical))
        return all_terms[:15] if all_terms else [summary.split()[0]] if summary else ["bug"]
