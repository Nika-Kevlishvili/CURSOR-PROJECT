"""
Bridge resolver — follow cross-zone edges to enrich retrieved context.
"""

from .graph_client import GraphClient


def resolve_bridges(graph: GraphClient, seed_nodes: list[dict],
                    max_depth: int = 1) -> list[dict]:
    """
    Given seed nodes from a primary zone query, follow bridge edges
    into neighboring zones to collect related context.

    Returns additional nodes discovered via bridges (deduplicated).
    """
    seen_uids = {n["uid"] for n in seed_nodes}
    bridge_nodes: list[dict] = []

    for node in seed_nodes:
        neighbors = graph.get_neighbors(node["uid"], max_depth=max_depth)
        for neighbor in neighbors:
            if neighbor["uid"] not in seen_uids:
                seen_uids.add(neighbor["uid"])
                neighbor["_bridge_from"] = node["uid"]
                bridge_nodes.append(neighbor)

    return bridge_nodes


def format_context(seed_nodes: list[dict], bridge_nodes: list[dict]) -> str:
    """Format seed + bridge nodes into a readable context string for LLM."""
    lines: list[str] = []

    if seed_nodes:
        lines.append("### Primary Results")
        for n in seed_nodes:
            score = n.get("score", "")
            score_str = f" (relevance: {score:.3f})" if isinstance(score, float) else ""
            lines.append(
                f"- **[{n.get('zone', '?')}] {n.get('node_type', '?')}: "
                f"{n.get('name', '?')}**{score_str}\n"
                f"  {n.get('description', 'No description')}\n"
                f"  Source: `{n.get('source_path', 'unknown')}`"
            )

    if bridge_nodes:
        lines.append("\n### Related (via bridges)")
        for n in bridge_nodes:
            lines.append(
                f"- **[{n.get('zone', '?')}] {n.get('node_type', '?')}: "
                f"{n.get('name', '?')}**\n"
                f"  {n.get('description', 'No description')}"
            )

    return "\n".join(lines) if lines else "No relevant nodes found in the graph."
