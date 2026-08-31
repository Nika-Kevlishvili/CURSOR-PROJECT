"""
graph_query tool — Ask a question, get graph-enriched answer via local LLM.
"""

import os

from ...core.graph_client import GraphClient
from ...core.llm_client import LLMClient
from ...core.graph_search import retrieve
from ...core.bridge_resolver import resolve_bridges, format_context
from ...core.staleness import filter_stale_nodes


WORKSPACE_ROOT = os.environ.get(
    "GRAPH_RAG_WORKSPACE",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
)


def graph_query(question: str, zone: str | None = None, top_k: int = 10,
                use_llm_synthesis: bool = False) -> dict:
    """
    Return source_path pointers. LLM synthesis is off by default.

    Returns dict with: answer, sources, zones_queried, nodes_found, stale_count
    """
    graph = GraphClient()
    llm = LLMClient()

    try:
        all_nodes, target_zones = retrieve(
            graph, llm, question, zone=zone, top_k=top_k
        )

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
