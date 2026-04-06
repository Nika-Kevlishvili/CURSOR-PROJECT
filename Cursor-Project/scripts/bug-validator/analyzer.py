"""
Bug Analyzer — uses Claude API to validate a bug against
Confluence documentation and codebase findings.
"""

import json
import anthropic


SYSTEM_PROMPT = """You are a senior QA engineer and bug validator for the Phoenix project (Java/Spring Boot).
Your job is to analyze a Jira bug report against:
1. Confluence documentation (if provided)
2. Source code from GitLab (if provided)

You MUST return a structured JSON response with the following schema:

{
  "confluence_validation": {
    "status": "correct" | "incorrect" | "partially_correct" | "no_data",
    "explanation": "Detailed explanation of how bug description matches or mismatches documentation"
  },
  "code_validation": {
    "status": "satisfies" | "does_not_satisfy" | "inconclusive",
    "explanation": "Detailed explanation of code analysis",
    "references": [
      {"file": "path/to/file.java", "lines": "123-145", "note": "Brief note about this reference"}
    ]
  },
  "analysis": {
    "is_valid": true | false | null,
    "summary": "One-paragraph summary of the validation result",
    "details": "Multi-paragraph detailed analysis",
    "suggested_fix": "Description of potential fix (text only, no code modification)"
  }
}

Rules:
- is_valid = true: Bug is confirmed valid (code has the issue described)
- is_valid = false: Bug is NOT valid (code works correctly, or bug description is wrong)
- is_valid = null: Inconclusive (not enough data to determine)
- Always provide specific file paths and line references when possible
- Be precise and technical in your analysis
- If Confluence data is missing, note it but continue with code analysis
- ALWAYS return valid JSON, nothing else"""


class BugAnalyzer:
    def __init__(self, api_key: str, model: str = "claude-sonnet-4-20250514"):
        self.client = anthropic.Anthropic(api_key=api_key)
        self.model = model

    def analyze(self, bug: dict, confluence_data: dict, code_data: dict) -> dict:
        """
        Send bug + evidence to Claude and get structured validation.
        """
        user_prompt = self._build_prompt(bug, confluence_data, code_data)

        message = self.client.messages.create(
            model=self.model,
            max_tokens=4096,
            system=SYSTEM_PROMPT,
            messages=[{"role": "user", "content": user_prompt}],
        )

        raw = message.content[0].text.strip()
        return self._parse_response(raw)

    def _build_prompt(self, bug: dict, confluence_data: dict, code_data: dict) -> str:
        sections = []

        sections.append("## Bug Report")
        sections.append(f"**Key:** {bug.get('key', 'N/A')}")
        sections.append(f"**Summary:** {bug.get('summary', 'N/A')}")
        sections.append(f"**Priority:** {bug.get('priority', 'N/A')}")
        sections.append(f"**Description:**\n{bug.get('description', 'No description')}")

        sections.append("\n## Confluence Documentation")
        if confluence_data.get("page_contents"):
            for page in confluence_data["page_contents"]:
                sections.append(f"\n### {page.get('title', 'Untitled')}")
                sections.append(page.get("excerpt", "No content"))
        elif confluence_data.get("sources"):
            sections.append("Pages found but no content retrieved:")
            for src in confluence_data["sources"]:
                sections.append(f"- {src}")
        else:
            sections.append("_No Confluence documentation found._")

        sections.append("\n## Source Code (from GitLab)")
        snippets = code_data.get("snippets", [])
        if snippets:
            for snippet in snippets[:10]:
                sections.append(f"\n### File: {snippet.get('file', '?')}")
                sections.append(f"```\n{snippet.get('content', '')}\n```")
        else:
            files = code_data.get("files", [])
            if files:
                sections.append("Files found (no content retrieved):")
                for f in files[:15]:
                    sections.append(f"- {f.get('file', '?')} (line {f.get('startline', '?')})")
            else:
                sections.append("_No relevant code found._")

        sections.append(f"\nKeywords used for search: {', '.join(code_data.get('keywords_used', []))}")
        sections.append("\n---\nAnalyze the above and return your validation as JSON.")

        return "\n".join(sections)

    def _parse_response(self, raw: str) -> dict:
        """Parse Claude's JSON response, handling markdown code blocks."""
        text = raw
        if "```json" in text:
            text = text.split("```json", 1)[1]
            text = text.split("```", 1)[0]
        elif "```" in text:
            text = text.split("```", 1)[1]
            text = text.split("```", 1)[0]

        try:
            return json.loads(text.strip())
        except json.JSONDecodeError:
            return {
                "confluence_validation": {"status": "error", "explanation": "Failed to parse AI response"},
                "code_validation": {"status": "error", "explanation": raw[:500]},
                "analysis": {
                    "is_valid": None,
                    "summary": "AI analysis returned unparseable response.",
                    "details": raw,
                    "suggested_fix": None,
                },
            }
