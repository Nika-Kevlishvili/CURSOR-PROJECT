"""
Route questions to engine layer labels stored on nodes (`n.zone`).

Nodes use: rules, skills_and_agents, memory_and_handoff, sdlc_docs.
The graph is not Phoenix or EnergoTS evidence. Legacy product zone names
map back to these engine layers.
"""

GRAPH_LAYERS = (
    "rules",
    "skills_and_agents",
    "memory_and_handoff",
    "sdlc_docs",
)
DEFAULT_LAYERS = GRAPH_LAYERS

LEGACY_BUSINESS_ZONES = frozenset({
    "contracts", "billing", "invoicing", "payments", "customers",
    "products", "service_operations", "communications", "reference_data",
    "phoenix_domain", "api_and_repo_layout", "test_cases", "playwright_automation",
})


def _dedupe(items: list[str]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for item in items:
        if item and item not in seen:
            seen.add(item)
            out.append(item)
    return out


def to_graph_layers(zones: list[str] | None) -> list[str]:
    """Map any zone name the caller/LLM might send to GRAPH.1 labels."""
    out: list[str] = []
    for raw in zones or []:
        zone = (raw or "").strip()
        if zone in GRAPH_LAYERS:
            out.append(zone)
            continue
        if zone in LEGACY_BUSINESS_ZONES or zone in ("all", "all (fallback)"):
            out.extend(DEFAULT_LAYERS)
            continue
        out.extend(DEFAULT_LAYERS)
    mapped = _dedupe(out)
    return mapped or list(DEFAULT_LAYERS)


def route_by_keywords(question: str) -> list[str]:
    """Search engine layers. Product words do not add a Phoenix or EnergoTS zone."""
    return list(DEFAULT_LAYERS)


def route_question(question: str, llm_client=None) -> list[str]:
    """Return GRAPH.1 layers to search. Does not use the nine legacy names."""
    return route_by_keywords(question)


def resolve_search_zones(zone: str | None, question: str, llm_client=None) -> list[str]:
    """Explicit `zone` (comma-separated) or keyword route, then map to GRAPH.1."""
    if zone and str(zone).strip():
        raw = [part.strip() for part in str(zone).split(",") if part.strip()]
        return to_graph_layers(raw)
    return route_question(question, llm_client=llm_client)
