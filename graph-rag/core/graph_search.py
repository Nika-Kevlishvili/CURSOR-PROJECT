"""Shared graph retrieval for graph_query (MCP + tools)."""

from core.zone_router import resolve_search_zones


def search_nodes(graph, embedding: list[float], question: str,
                 zones: list[str], top_k: int) -> list[dict]:
    """Vector + token keyword search per GRAPH.1 layer; dedupe by uid."""
    all_nodes: list[dict] = []
    seen: set[str] = set()
    for zone in zones:
        vector_results = graph.vector_search(embedding, zone=zone, top_k=top_k)
        keyword_results = graph.keyword_search(question, zone=zone, limit=top_k)
        for node in list(vector_results) + list(keyword_results):
            uid = node.get("uid")
            if not uid or uid in seen:
                continue
            seen.add(uid)
            all_nodes.append(node)
    return all_nodes


def retrieve(graph, llm, question: str, zone: str | None = None,
             top_k: int = 10) -> tuple[list[dict], list[str]]:
    """Route, embed, search. Returns (nodes, layers searched)."""
    layers = resolve_search_zones(zone, question, llm_client=llm)
    embedding = llm.embed(question)
    nodes = search_nodes(graph, embedding, question, layers, top_k)
    return nodes, layers
