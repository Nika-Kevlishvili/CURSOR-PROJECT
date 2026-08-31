"""
Ingest pipeline — orchestrate extractors, compute embeddings, upsert to Neo4j.
"""

import os
import sys
import time

from core.graph_client import GraphClient
from core.llm_client import LLMClient
from ingest.extractors.swagger_extractor import extract_swagger
from ingest.extractors.testcase_extractor import extract_test_cases


def run_ingest(workspace_root: str, zones: list[str] | None = None,
               verbose: bool = True) -> dict:
    """
    Run the ingest pipeline for specified zones (or all if None).
    Returns a summary dict with counts.
    """
    graph = GraphClient()
    llm = LLMClient()
    summary = {"nodes_created": 0, "edges_created": 0, "errors": [], "zones_processed": []}

    try:
        if verbose:
            print("Setting up Neo4j schema...")
        graph.setup_schema()

        target_zones = zones or ["api_and_repo_layout", "test_cases"]

        if "api_and_repo_layout" in target_zones:
            if verbose:
                print("\n--- Ingesting Swagger specs (api_and_repo_layout) ---")
            swagger_paths = [
                "Cursor-Project/config/swagger/dev/swagger-spec.json",
            ]
            for sp in swagger_paths:
                results = extract_swagger(sp, workspace_root)
                for result in results:
                    _upsert_result(graph, llm, result, summary, verbose)
            summary["zones_processed"].append("api_and_repo_layout")

        if "test_cases" in target_zones:
            if verbose:
                print("\n--- Ingesting test cases (test_cases) ---")
            tc_dirs = [
                "Cursor-Project/test_cases/Backend",
                "Cursor-Project/test_cases/Frontend",
            ]
            for td in tc_dirs:
                results = extract_test_cases(td, workspace_root)
                for result in results:
                    _upsert_result(graph, llm, result, summary, verbose)
            summary["zones_processed"].append("test_cases")

    except Exception as e:
        summary["errors"].append(str(e))
        if verbose:
            print(f"ERROR: {e}")
    finally:
        graph.close()

    if verbose:
        print(f"\n=== Ingest complete ===")
        print(f"Nodes: {summary['nodes_created']}, Edges: {summary['edges_created']}")
        print(f"Zones: {summary['zones_processed']}")
        if summary["errors"]:
            print(f"Errors: {len(summary['errors'])}")

    return summary


def _upsert_result(graph: GraphClient, llm: LLMClient,
                   result: dict, summary: dict, verbose: bool) -> None:
    from core.display import enrich_display

    nodes = result.get("nodes", [])
    edges = result.get("edges", [])

    for node in nodes:
        enrich_display(node)

    # Compute embeddings in batches (human title + description)
    texts_to_embed = [
        f"{n.get('title', n['name'])}: {n['description']}" for n in nodes
    ]

    if verbose:
        print(f"  Computing embeddings for {len(texts_to_embed)} nodes...")

    try:
        embeddings = llm.embed_batch(texts_to_embed)
    except Exception as e:
        summary["errors"].append(f"Embedding error: {e}")
        embeddings = [[] for _ in texts_to_embed]

    for node, embedding in zip(nodes, embeddings):
        try:
            graph.upsert_node(
                uid=node["uid"],
                node_type=node["node_type"],
                zone=node["zone"],
                name=node["name"],
                description=node["description"],
                properties={
                    "source_path": node.get("source_path", ""),
                    "source_hash": node.get("source_hash", ""),
                    "title": node.get("title"),
                    **(node.get("properties", {})),
                },
                embedding=embedding if embedding else None,
            )
            summary["nodes_created"] += 1
        except Exception as e:
            summary["errors"].append(f"Node upsert error ({node['uid']}): {e}")

    if verbose and nodes:
        print(f"  Upserted {len(nodes)} nodes")

    for edge in edges:
        try:
            graph.upsert_edge(
                from_uid=edge["from_uid"],
                to_uid=edge["to_uid"],
                edge_type=edge["edge_type"],
                properties=edge.get("properties"),
            )
            summary["edges_created"] += 1
        except Exception as e:
            summary["errors"].append(f"Edge upsert error: {e}")

    if verbose and edges:
        print(f"  Upserted {len(edges)} edges")


def ingest_topic(topic: str, workspace_root: str) -> dict:
    """Ingest a specific topic by running all extractors and filtering."""
    return run_ingest(workspace_root, zones=None, verbose=True)


if __name__ == "__main__":
    workspace = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
    print(f"Workspace root: {workspace}")
    run_ingest(workspace)
