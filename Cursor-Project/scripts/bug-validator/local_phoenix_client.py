"""
Local Phoenix filesystem client — scan local Cursor-Project/Phoenix directory for code.

Replaces GitLab API client for bug validation. Searches local Phoenix projects
for code related to bug reports using the same output format as gitlab_client.py.
"""

import os
import re
from pathlib import Path
from typing import List, Dict, Optional


class LocalPhoenixClient:
    def __init__(self, phoenix_root: Optional[str] = None):
        """
        Initialize local Phoenix scanner.
        
        Args:
            phoenix_root: Optional absolute path to Phoenix directory. 
                         If None, defaults to Cursor-Project/Phoenix relative to this script.
        """
        # Default: Cursor-Project/Phoenix relative to this script location
        # From: Cursor-Project/scripts/bug-validator/local_phoenix_client.py
        # To:   Cursor-Project/Phoenix
        script_dir = Path(__file__).resolve().parent  # bug-validator/
        default_root = script_dir.parent.parent / "Phoenix"  # Cursor-Project/Phoenix

        self.path_resolution_note = None
        self.path_resolution_warning = None

        env_root = self._normalize_root(phoenix_root) if phoenix_root else None
        default_root = self._normalize_root(str(default_root))

        if env_root and env_root.exists() and env_root.is_dir():
            resolved_root, root_note = self._resolve_scan_root(env_root)
            self.phoenix_root = resolved_root
            self.path_resolution_note = root_note
        elif env_root:
            self.phoenix_root = default_root
            self.path_resolution_warning = (
                f"PHOENIX_LOCAL_ROOT path '{env_root}' is unavailable. "
                f"Falling back to default '{default_root}'."
            )
        else:
            self.phoenix_root = default_root
            self.path_resolution_note = "Using default Cursor-Project/Phoenix path."

    def _normalize_root(self, raw_path: str) -> Path:
        """
        Normalize user-provided path values from environment variables.
        Handles quotes, env vars and ~ expansion.
        """
        cleaned = raw_path.strip().strip('"').strip("'")
        expanded = os.path.expandvars(os.path.expanduser(cleaned))
        return Path(expanded).resolve()

    def _resolve_scan_root(self, env_root: Path) -> tuple[Path, str]:
        """
        Resolve best scan root from PHOENIX_LOCAL_ROOT.
        If a single Phoenix repo path is provided, use its parent Phoenix folder
        when that parent clearly contains multiple repos.
        """
        # Case: env points to a single repo (e.g. .../Phoenix/phoenix-core)
        parent = env_root.parent
        if parent.exists() and parent.is_dir() and parent.name.lower() == "phoenix":
            siblings = [p for p in parent.iterdir() if p.is_dir() and p.name.startswith("phoenix-")]
            if len(siblings) >= 2:
                return parent, (
                    f"PHOENIX_LOCAL_ROOT pointed to single repo '{env_root.name}'. "
                    f"Using parent '{parent}' to scan all Phoenix repos."
                )

        return env_root, "Using PHOENIX_LOCAL_ROOT environment override."
        
    def _check_phoenix_availability(self) -> Optional[str]:
        """
        Check if Phoenix directory exists and is readable.
        Returns None on success, or error message on failure.
        """
        if not self.phoenix_root.exists():
            return (
                f"Phoenix directory not found at '{self.phoenix_root}'. "
                f"Ensure Cursor-Project/Phoenix exists or set PHOENIX_LOCAL_ROOT environment variable."
            )
        
        if not self.phoenix_root.is_dir():
            return f"Phoenix path '{self.phoenix_root}' exists but is not a directory."
            
        try:
            # Test readability
            list(self.phoenix_root.iterdir())
        except PermissionError:
            return f"Cannot read Phoenix directory '{self.phoenix_root}' - permission denied."
        except Exception as e:
            return f"Cannot access Phoenix directory '{self.phoenix_root}' - {e}"
            
        return None

    def search_for_bug(self, summary: str, description: str) -> dict:
        """
        Search local Phoenix codebase for code related to the bug.
        Returns dict with 'files', 'snippets', 'keywords_used', and 'status'.
        
        Compatible with gitlab_client.py output format.
        """
        availability_error = self._check_phoenix_availability()
        if availability_error:
            print(f"  ERROR: Phoenix unavailable — {availability_error}")
            return {
                "files": [],
                "snippets": [],
                "keywords_used": [],
                "status": "unreachable",
                "error": availability_error,
                "source": "local_phoenix"
            }

        if self.path_resolution_warning:
            print(f"  WARNING: {self.path_resolution_warning}")

        keywords = self._extract_search_terms(summary, description)
        all_files = []
        all_snippets = []

        print(f"  Scanning Phoenix directory: {self.phoenix_root}")
        print(f"  Search keywords: {keywords[:5]}...")

        # Search for relevant files using keywords
        for keyword in keywords[:10]:  # Limit keywords like gitlab_client
            matching_files = self._search_files_for_keyword(keyword)
            for file_info in matching_files:
                # Avoid duplicates
                if not any(f["file"] == file_info["file"] for f in all_files):
                    all_files.append(file_info)
                    
        print(f"  Found {len(all_files)} potentially relevant files")
        
        # Read file contents for the most relevant matches (limit like gitlab_client)
        for file_info in all_files[:15]:
            content = self._read_file_content(file_info["file"])
            if content:
                all_snippets.append({
                    "file": str(file_info["file"].relative_to(self.phoenix_root)),
                    "content": content[:5000],  # Same limit as gitlab_client
                    "project": file_info.get("project", "unknown")
                })

        return {
            "files": all_files,
            "snippets": all_snippets,
            "keywords_used": keywords,
            "status": "ok" if all_files else "no_results",
            "source": "local_phoenix",
            "note": self.path_resolution_warning or self.path_resolution_note,
        }

    def _search_files_for_keyword(self, keyword: str) -> List[Dict]:
        """
        Search for files containing the keyword in their content.
        Returns list of file info dicts.
        """
        matching_files = []
        
        # Define file extensions to scan (Java, Kotlin, config files, etc.)
        allowed_extensions = {
            '.java', '.kt', '.xml', '.properties', '.yml', '.yaml', 
            '.ts', '.tsx', '.js', '.json', '.sql'
        }
        
        # Directories to skip (build artifacts, dependencies, etc.)
        skip_dirs = {
            'node_modules', 'target', 'build', '.git', 'dist', '.idea', 
            '.vscode', '__pycache__', '.gradle', 'out', 'bin'
        }
        
        try:
            for root, dirs, files in os.walk(self.phoenix_root):
                # Skip excluded directories
                dirs[:] = [d for d in dirs if d not in skip_dirs]
                
                root_path = Path(root)
                
                # Determine project name from path
                try:
                    relative_to_phoenix = root_path.relative_to(self.phoenix_root)
                    project_name = str(relative_to_phoenix.parts[0]) if relative_to_phoenix.parts else "unknown"
                except ValueError:
                    project_name = "unknown"
                
                for file_name in files:
                    file_path = root_path / file_name
                    
                    # Only scan allowed file types
                    if file_path.suffix.lower() not in allowed_extensions:
                        continue
                        
                    # Skip very large files
                    try:
                        if file_path.stat().st_size > 1024 * 1024:  # 1MB limit
                            continue
                    except (OSError, ValueError):
                        continue
                    
                    # Search for keyword in file content
                    if self._file_contains_keyword(file_path, keyword):
                        matching_files.append({
                            "file": file_path,
                            "project": project_name,
                            "search_term": keyword,
                            "relative_path": str(file_path.relative_to(self.phoenix_root))
                        })
                        
                        # Limit results per keyword
                        if len(matching_files) >= 5:
                            break
                            
        except Exception as e:
            print(f"  WARNING: Error scanning for keyword '{keyword}': {e}")
            
        return matching_files

    def _file_contains_keyword(self, file_path: Path, keyword: str) -> bool:
        """
        Check if file contains the keyword (case-insensitive).
        """
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()
                return keyword.lower() in content.lower()
        except Exception:
            return False

    def _read_file_content(self, file_path: Path) -> Optional[str]:
        """
        Read file content safely with encoding handling.
        """
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                return f.read()
        except Exception:
            return None

    def _extract_search_terms(self, summary: str, description: str) -> List[str]:
        """
        Extract meaningful search terms from bug summary and description.
        
        Reuses the same logic as gitlab_client.py for consistency.
        """
        combined = f"{summary} {description}"

        # Extract URL paths
        url_paths = re.findall(r"/[\w/.-]+", combined)
        path_segments = []
        for path in url_paths:
            segments = [s for s in path.strip("/").split("/") if len(s) > 2]
            path_segments.extend(segments)

        # Extract CamelCase class/method names
        camel_case = re.findall(r"\b[A-Z][a-z]+(?:[A-Z][a-z]+)+\b", combined)

        # Extract dotted notation (e.g., com.example.service)
        dotted = re.findall(r"\b\w+\.\w+(?:\.\w+)*\b", combined)

        # Extract technical terms relevant to Phoenix
        technical = re.findall(r"\b(?:service|controller|handler|repository|entity|dto|mapper|"
                               r"endpoint|api|invoice|contract|payment|liability|receivable|"
                               r"deposit|customer|pod|cancel|create|update|delete|reverse|"
                               r"offset|amount|status|validation|scheduler|health|actuator|"
                               r"monitoring|configuration|indicator)\b",
                               combined, re.IGNORECASE)

        # Extract HTTP status codes
        http_codes = re.findall(r"\b[1-5]\d{2}\b", combined)

        # Combine and deduplicate
        all_terms = list(dict.fromkeys(
            url_paths + path_segments + camel_case + dotted + technical + http_codes
        ))
        
        # Return top terms, fallback to summary word if no technical terms found
        return all_terms[:15] if all_terms else [summary.split()[0]] if summary else ["bug"]


def create_client() -> LocalPhoenixClient:
    """
    Factory function to create LocalPhoenixClient with environment override.
    
    Checks PHOENIX_LOCAL_ROOT environment variable for custom path.
    """
    phoenix_root = os.environ.get("PHOENIX_LOCAL_ROOT")
    return LocalPhoenixClient(phoenix_root)