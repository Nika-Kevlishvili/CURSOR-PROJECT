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


def _topic_key(topic: str) -> str:
    return (topic or "").strip().lower().replace("_", "-").replace(" ", "-")


def graph_index_topic(topic: str, zones: list[str] | None = None) -> dict:
    """
    Index a topic. Known scoped topics (zip-codes) use a dedicated ingest.
    Otherwise ingest API only — do not dump every test-case markdown unless
    `zones` explicitly includes `test_cases`.
    """
    key = _topic_key(topic)
    if "zip-code" in key or key in ("zip", "zipcodes", "zip-codes"):
        from ...ingest.ingest_zip_codes import main as ingest_zip

        ingest_zip()
        return {
            "topic": topic,
            "zones_ingested": ["phoenix_domain", "api_and_repo_layout"],
            "nodes_created": None,
            "edges_created": None,
            "errors": [],
            "note": "Scoped zip-codes ingest. Test-case and Playwright layers omitted (no sources).",
        }

    target_zones = zones or ["api_and_repo_layout"]

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
            "Topic string is not a scoped filter except zip-codes. "
            "Full Swagger ingest ran for the requested zones."
        ),
    }
