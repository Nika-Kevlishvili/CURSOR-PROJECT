"""
graph_query tool — Ask a question, get graph-enriched answer via local LLM.
"""

import os

from ...core.graph_client import GraphClient
from ...core.llm_client import LLMClient
from ...core.zone_router import route_question
from ...core.bridge_resolver import resolve_bridges, format_context
from ...core.staleness import filter_stale_nodes


WORKSPACE_ROOT = os.environ.get(
    "GRAPH_RAG_WORKSPACE",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
)


def graph_query(question: str, zone: str | None = None, top_k: int = 10,
                use_llm_synthesis: bool = True) -> dict:
    """
    Query the Graph RAG system.

    1. Route question to zone(s)
    2. Vector + keyword search in Neo4j
    3. Check staleness of results
    4. Follow bridge edges for cross-zone context
    5. Synthesize answer via local LLM

    Returns dict with: answer, sources, zones_queried, nodes_found, stale_count
    """
    graph = GraphClient()
    llm = LLMClient()

    try:
        # 1. Route to zone(s)
        if zone:
            target_zones = [zone]
        else:
            target_zones = route_question(question, llm_client=llm)

        # 2. Search graph
        question_embedding = llm.embed(question)
        all_nodes: list[dict] = []

        for z in target_zones:
            vector_results = graph.vector_search(question_embedding, zone=z, top_k=top_k)
            keyword_results = graph.keyword_search(question, zone=z, limit=top_k)

            seen = {n["uid"] for n in vector_results}
            combined = list(vector_results)
            for kr in keyword_results:
                if kr["uid"] not in seen:
                    combined.append(kr)
                    seen.add(kr["uid"])

            all_nodes.extend(combined)

        if not all_nodes:
            return {
                "answer": f"No relevant information found in the graph for: {question}",
                "sources": [],
                "zones_queried": target_zones,
                "nodes_found": 0,
                "stale_count": 0,
                "graph_context": "",
            }

        # 3. Check staleness
        fresh, stale = filter_stale_nodes(all_nodes, WORKSPACE_ROOT)

        # 4. Bridge resolution
        bridge_nodes = resolve_bridges(graph, all_nodes, max_depth=1)

        # 5. Format context
        context = format_context(all_nodes, bridge_nodes)

        # 6. Synthesize answer
        answer = context
        if use_llm_synthesis:
            answer = llm.synthesize_answer(question, context)

        sources = list({
            n.get("source_path", "unknown")
            for n in all_nodes + bridge_nodes
            if n.get("source_path")
        })

        return {
            "answer": answer,
            "sources": sources,
            "zones_queried": target_zones,
            "nodes_found": len(all_nodes),
            "bridge_nodes": len(bridge_nodes),
            "stale_count": len(stale),
            "graph_context": context,
        }

    finally:
        graph.close()
