"""
Bug Analyzer — uses Google Gemini API (free tier) to validate a bug
against Confluence documentation and codebase findings.
"""

import json
import requests


SYSTEM_PROMPT = """You are a senior QA engineer and bug validator for the Phoenix project (Java/Spring Boot).
Your job is to analyze a Jira bug report against Confluence documentation and source code using a 5-verdict decision matrix.

MANDATORY WORKFLOW:
1. Extract the expected behavior from the bug report
2. Assess Confluence evidence strength for that expected behavior
3. Analyze if code behavior matches the faulty behavior described in the bug
4. Apply the 5-verdict decision matrix

You MUST return a structured JSON response with the following schema:

{
  "expected_behavior": {
    "bug_claims": "What the bug report says should happen",
    "context": "Relevant business context or user scenario"
  },
  "confluence_validation": {
    "evidence_strength": "exact_match" | "contextual_match" | "no_match" | "contradicts" | "search_failed",
    "explanation": "Detailed explanation of Confluence findings with reasoning for evidence strength",
    "sources": ["List of page titles/URLs found"]
  },
  "code_validation": {
    "behavior_match": "matches_reported_behavior" | "does_not_match_reported_behavior" | "could_not_verify",
    "explanation": "Detailed explanation of actual code behavior vs reported faulty behavior",
    "references": [
      {"file": "path/to/file.java", "lines": "123-145", "implementation": "Description of what code actually does"}
    ]
  },
  "final_verdict": {
    "verdict": "VALID" | "NEEDS_CLARIFICATION" | "NEEDS_APPROVAL" | "NOT_VALID" | "INSUFFICIENT_EVIDENCE",
    "reasoning": "Why this verdict was chosen based on evidence matrix",
    "next_steps": "What should be done next based on verdict"
  }
}

5-VERDICT DECISION MATRIX:
- VALID: Exact Confluence match + code confirms reported faulty behavior → Fix the bug
- NEEDS_CLARIFICATION: Contextual Confluence match + code confirms reported behavior → Get product clarification  
- NEEDS_APPROVAL: No Confluence match + code confirms reported behavior → Get product approval
- NOT_VALID: Confluence contradicts expected behavior + code follows Confluence → Close as "working as designed"
- INSUFFICIENT_EVIDENCE: Cannot access BOTH Confluence AND code, or evidence too weak from BOTH sources → Resolve technical issues

IMPORTANT: If ONLY ONE source is unreachable (e.g. GitLab unreachable but Confluence has data), do NOT default to INSUFFICIENT_EVIDENCE.
Use the available evidence to reach a more specific verdict (NEEDS_APPROVAL, NEEDS_CLARIFICATION, etc.).
INSUFFICIENT_EVIDENCE should be reserved for when NEITHER source provides usable data.

EVIDENCE STRENGTH DEFINITIONS:
- exact_match: Confluence explicitly supports the bug's expected behavior
- contextual_match: Related/similar rules suggest expected behavior but no exact documentation
- no_match: No relevant documentation found for this specific case
- contradicts: Confluence explicitly states different behavior than bug expects
- search_failed: Technical issues accessing Confluence

BEHAVIOR MATCH DEFINITIONS:
- matches_reported_behavior: Code behavior aligns with the faulty behavior described in bug
- does_not_match_reported_behavior: Code behaves differently than bug describes
- could_not_verify: Cannot determine code behavior due to technical issues

Rules:
- Always provide specific file paths and line references when possible
- Be precise and technical in your analysis
- Never use vague verdicts - always choose one of the 5 verdicts
- Separate technical access issues from business validation outcomes
- ALWAYS return valid JSON, nothing else"""

GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent"


class BugAnalyzer:
    def __init__(self, api_key: str, model: str = "gemini-2.5-flash"):
        self.api_key = api_key
        self.model = model

    def analyze(self, bug: dict, confluence_data: dict, code_data: dict) -> dict:
        """Send bug + evidence to Gemini and get structured validation."""
        user_prompt = self._build_prompt(bug, confluence_data, code_data)

        url = GEMINI_API_URL.format(model=self.model)
        payload = {
            "contents": [
                {
                    "parts": [
                        {"text": f"{SYSTEM_PROMPT}\n\n---\n\n{user_prompt}"}
                    ]
                }
            ],
            "generationConfig": {
                "temperature": 0.2,
                "maxOutputTokens": 4096,
                "responseMimeType": "application/json",
            },
        }

        resp = requests.post(
            url,
            params={"key": self.api_key},
            json=payload,
            timeout=60,
        )
        resp.raise_for_status()
        data = resp.json()

        raw = data["candidates"][0]["content"]["parts"][0]["text"].strip()
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
        gitlab_status = code_data.get("status", "")
        if gitlab_status == "unreachable":
            sections.append(
                "**IMPORTANT: GitLab is UNREACHABLE from the CI runner.** "
                f"Reason: {code_data.get('error', 'network unreachable')}. "
                "This is an infrastructure limitation — the runner cannot access the internal GitLab server. "
                "Code analysis COULD NOT be performed. "
                "When determining your verdict, treat the code_validation as 'could_not_verify' "
                "due to infrastructure, NOT due to the code being absent or irrelevant. "
                "If Confluence provides sufficient evidence, you may still issue a verdict "
                "other than INSUFFICIENT_EVIDENCE (e.g. NEEDS_APPROVAL or NEEDS_CLARIFICATION)."
            )
        else:
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
        """Parse Gemini's JSON response."""
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
