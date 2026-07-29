"""
Zone router — classify user questions to relevant graph zone(s).

Uses keyword heuristics first (fast, no LLM call).
Falls back to LLM classification for ambiguous queries.
"""

import re

ZONE_KEYWORDS: dict[str, list[str]] = {
    "phoenix_domain": [
        "billing", "invoice", "payment", "receivable", "liability", "contract",
        "deposit", "scheduler", "validation", "business", "process", "domain",
        "entity", "service", "phoenix", "cancellation", "reversal", "rescheduling",
        "mass import", "supply", "activation", "disconnection", "reconnection",
        "settlement", "compensation", "interim", "final", "splitting",
        "product contract", "service contract", "service order",
    ],
    "api_and_repo_layout": [
        "endpoint", "api", "swagger", "dto", "rest", "http", "post", "get",
        "put", "delete", "patch", "controller", "repository", "module",
        "request", "response", "status code", "enum", "field", "required",
        "openapi", "path", "url", "method",
    ],
    "test_cases": [
        "test case", "tc-be", "tc-fe", "precondition", "expected result",
        "test step", "coverage", "test scenario", "acceptance criteria",
        "test plan", "priority", "test data",
    ],
    "playwright_automation": [
        "playwright", "spec", "fixture", "spec.ts", "automation",
        "energots", "selector", "locator", "page object", "test run",
        "beforeall", "beforeeach", "assertion", "screenshot",
        "writing rule", "pattern",
    ],
}


def route_by_keywords(question: str) -> list[str]:
    q_lower = question.lower()
    scores: dict[str, int] = {}
    for zone, keywords in ZONE_KEYWORDS.items():
        score = sum(1 for kw in keywords if kw in q_lower)
        if score > 0:
            scores[zone] = score

    if not scores:
        return []

    max_score = max(scores.values())
    threshold = max(1, max_score // 2)
    return [zone for zone, score in scores.items() if score >= threshold]


def route_question(question: str, llm_client=None) -> list[str]:
    """Route question to zone(s). Keyword-first, LLM fallback."""
    zones = route_by_keywords(question)
    if zones:
        return zones

    if llm_client:
        return llm_client.classify_zone(question)

    return ["phoenix_domain"]
