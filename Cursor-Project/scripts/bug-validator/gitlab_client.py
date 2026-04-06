"""
GitLab REST API client — read-only code search and file retrieval.

Includes a connectivity pre-check so that when the GitLab host is
unreachable (e.g. internal network, VPN required) the validator gets
a clear "unreachable" status instead of dozens of timeout warnings.
"""

import re
import socket
import urllib.parse
import requests


class GitLabClient:
    def __init__(self, base_url: str, token: str, project_ids: list[str]):
        self.base_url = base_url.rstrip("/")
        self.token = token
        self.project_ids = project_ids
        self.headers = {"PRIVATE-TOKEN": token}

    def _check_connectivity(self) -> str | None:
        """
        Verify that the GitLab host is reachable.
        Returns None on success, or a human-readable error string on failure.
        """
        try:
            from urllib.parse import urlparse
            parsed = urlparse(self.base_url)
            host = parsed.hostname or ""
            port = parsed.port or (443 if parsed.scheme == "https" else 80)
            socket.create_connection((host, port), timeout=5).close()
            return None
        except socket.gaierror:
            return (
                f"DNS resolution failed for '{host}'. "
                f"The GitLab server appears to be on an internal/private network. "
                f"A self-hosted runner with VPN/network access is required."
            )
        except (socket.timeout, OSError) as e:
            return (
                f"Cannot connect to GitLab at {host}:{port} — {e}. "
                f"The server may be behind a firewall or VPN."
            )

    def search_for_bug(self, summary: str, description: str) -> dict:
        """
        Search across all configured projects for code related to the bug.
        Returns dict with 'files', 'snippets', 'keywords_used', and 'status'.
        """
        connectivity_error = self._check_connectivity()
        if connectivity_error:
            print(f"  ERROR: GitLab unreachable — {connectivity_error}")
            return {
                "files": [],
                "snippets": [],
                "keywords_used": [],
                "status": "unreachable",
                "error": connectivity_error,
            }

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
            "status": "ok" if all_files else "no_results",
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
        Extract meaningful search terms: URL paths, CamelCase class names,
        method names, technical terms from bug summary and description.
        """
        combined = f"{summary} {description}"

        url_paths = re.findall(r"/[\w/.-]+", combined)
        path_segments = []
        for path in url_paths:
            segments = [s for s in path.strip("/").split("/") if len(s) > 2]
            path_segments.extend(segments)

        camel_case = re.findall(r"\b[A-Z][a-z]+(?:[A-Z][a-z]+)+\b", combined)

        dotted = re.findall(r"\b\w+\.\w+(?:\.\w+)*\b", combined)

        technical = re.findall(r"\b(?:service|controller|handler|repository|entity|dto|mapper|"
                               r"endpoint|api|invoice|contract|payment|liability|receivable|"
                               r"deposit|customer|pod|cancel|create|update|delete|reverse|"
                               r"offset|amount|status|validation|scheduler|health|actuator|"
                               r"monitoring|configuration|indicator)\b",
                               combined, re.IGNORECASE)

        http_codes = re.findall(r"\b[1-5]\d{2}\b", combined)

        all_terms = list(dict.fromkeys(
            url_paths + path_segments + camel_case + dotted + technical + http_codes
        ))
        return all_terms[:15] if all_terms else [summary.split()[0]] if summary else ["bug"]
