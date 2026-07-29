"""
graph_index_topic tool — Index a specific topic across all sources on demand.
"""

import os

from ...core.graph_client import GraphClient
from ...core.llm_client import LLMClient
from ...ingest.pipeline import run_ingest


WORKSPACE_ROOT = os.environ.get(
    "GRAPH_RAG_WORKSPACE",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
)


def graph_index_topic(topic: str, zones: list[str] | None = None) -> dict:
    """
    Run full ingest pipeline for a specific topic.
    If zones are specified, only ingest those zones.
    Otherwise, ingest all available zones.

    Args:
        topic: Topic name (e.g., "billing run", "invoice cancellation")
        zones: Optional list of zones to target

    Returns dict with ingest summary
    """
    target_zones = zones or ["api_and_repo_layout", "test_cases"]

    summary = run_ingest(
        workspace_root=WORKSPACE_ROOT,
        zones=target_zones,
        verbose=False,
    )

    return {
        "topic": topic,
        "zones_ingested": summary.get("zones_processed", []),
        "nodes_created": summary.get("nodes_created", 0),
        "edges_created": summary.get("edges_created", 0),
        "errors": summary.get("errors", []),
    }
