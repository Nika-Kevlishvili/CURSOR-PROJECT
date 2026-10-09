"""
graph_status tool — Show zone statistics, node counts, staleness report.
"""

from ...core.graph_client import GraphClient


def graph_status() -> dict:
    """
    Return current state of the Graph RAG system.
    Includes per-zone node counts, total nodes, and edge counts.
    """
    graph = GraphClient()

    try:
        zone_stats = graph.get_zone_stats()
        total = graph.count_nodes()

        zones_summary: dict[str, dict] = {}
        for row in zone_stats:
            zone = row.get("zone", "unknown")
            node_type = row.get("node_type", "unknown")
            count = row.get("count", 0)
            if zone not in zones_summary:
                zones_summary[zone] = {"total": 0, "types": {}}
            zones_summary[zone]["total"] += count
            zones_summary[zone]["types"][node_type] = count

        edge_count_result = graph.run_query(
            "MATCH ()-[r]->() RETURN count(r) AS cnt"
        )
        edge_count = edge_count_result[0]["cnt"] if edge_count_result else 0

        return {
            "total_nodes": total,
            "total_edges": edge_count,
            "zones": zones_summary,
            "neo4j_browser": "http://localhost:7474",
            "status": "running" if total >= 0 else "error",
        }

    except Exception as e:
        return {
            "total_nodes": 0,
            "total_edges": 0,
            "zones": {},
            "status": f"error: {e}",
        }
    finally:
        graph.close()
