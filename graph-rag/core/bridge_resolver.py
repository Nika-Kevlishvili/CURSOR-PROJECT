"""Format retrieved graph nodes so agents open source_path first."""

from .graph_client import GraphClient


def resolve_bridges(graph: GraphClient, seed_nodes: list[dict],
                    max_depth: int = 1) -> list[dict]:
    """Follow neighbors from seed nodes (deduplicated)."""
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


def _source_index(nodes: list[dict]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for n in nodes:
        path = (n.get("source_path") or "").strip()
        if not path or path in seen:
            continue
        seen.add(path)
        page_id = ""
        props = n.get("properties") if isinstance(n.get("properties"), dict) else {}
        cid = n.get("confluence_page_id") or (props or {}).get("confluence_page_id")
        if cid:
            page_id = f" (Confluence page ID {cid})"
        out.append(f"- `{path}`{page_id}")
    return out


def format_context(seed_nodes: list[dict], bridge_nodes: list[dict]) -> str:
    """Pointer block first (files / wiki URLs), then short node summaries."""
    combined = list(seed_nodes) + list(bridge_nodes)
    lines: list[str] = [
        "### Open these sources (do not treat this graph text as evidence)",
        "Read each path or Confluence page ID below, then answer from those live sources.",
    ]
    index = _source_index(combined)
    if index:
        lines.extend(index)
    else:
        lines.append("- (no source_path on retrieved nodes)")

    if seed_nodes:
        lines.append("\n### Primary Results")
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
                f"  {n.get('description', 'No description')}\n"
                f"  Source: `{n.get('source_path', 'unknown')}`"
            )

    return "\n".join(lines)
