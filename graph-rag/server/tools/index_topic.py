"""
graph_index_topic tool — Index a specific topic across all sources on demand.
"""

import os

from ...core.graph_client import GraphClient
from ...core.llm_client import LLMClient
from ...ingest.pipeline import run_ingest


WORKSPACE_ROOT = os.environ.get(
    "GRAPH_RAG_WORKSPACE",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
)


def _topic_key(topic: str) -> str:
    return (topic or "").strip().lower().replace("_", "-").replace(" ", "-")


def graph_index_topic(topic: str, zones: list[str] | None = None) -> dict:
    """
    Index an engine topic. Does not run ingest_zip_codes.py or Swagger ingest.
    """
    _topic_key(topic)
    target_zones = zones or [
        "rules", "skills_and_agents", "memory_and_handoff", "sdlc_docs",
    ]

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
        "note": (
            "Engine ingest only. Phoenix, EnergoTS, and Swagger are not indexed."
        ),
    }
