"""
Graph RAG MCP Server for Cursor.

Bridges the Graph RAG knowledge base (Neo4j + Local LLM) to Cursor agents
using the MCP protocol. Provides 4 tools:

  graph_query        — Ask a question, get graph-enriched answer
  graph_update       — Save chat learnings back to the graph
  graph_index_topic  — Index a specific topic across all sources
  graph_status       — Show zone statistics, node counts, staleness

Environment variables:
    NEO4J_URI           - Neo4j bolt URI (default: bolt://localhost:7687)
    NEO4J_USER          - Neo4j username (default: neo4j)
    NEO4J_PASSWORD      - Neo4j password (default: graphrag)
    NEO4J_BROWSER_URL   - Neo4j Browser URL shown in graph_status
    LLM_BASE_URL        - OpenAI-compatible API URL (LM Studio or Ollama)
    LLM_MODEL           - Chat model name
    LLM_EMBED_MODEL     - Embedding model name (documented; embeddings use MiniLM in-process)
    GRAPH_RAG_WORKSPACE - Workspace root path
    MCP_TRANSPORT       - stdio (default) | sse | streamable-http
    MCP_HOST            - Bind host for sse/streamable-http (default: 127.0.0.1)
    MCP_PORT            - Bind port for sse/streamable-http (default: 8100)
"""

import os
import sys

_this_dir = os.path.dirname(os.path.abspath(__file__))
_graph_rag_root = os.path.dirname(_this_dir)
_workspace_root = os.path.abspath(os.path.join(_graph_rag_root, "..", ".."))

sys.path.insert(0, _workspace_root)
sys.path.insert(0, _graph_rag_root)

os.environ.setdefault("GRAPH_RAG_WORKSPACE", _workspace_root)

print("GraphRAG: preloading embedding model...", file=sys.stderr, flush=True)
from core.llm_client import preload_embed_model
try:
    preload_embed_model()
    print("GraphRAG: embedding model ready", file=sys.stderr, flush=True)
except Exception as e:
    print(f"GraphRAG: embedding preload failed: {e}", file=sys.stderr, flush=True)

from mcp.server.fastmcp import FastMCP

_mcp_host = os.environ.get("MCP_HOST", "127.0.0.1")
_mcp_port = int(os.environ.get("MCP_PORT", "8100"))

mcp = FastMCP("GraphRAG", host=_mcp_host, port=_mcp_port)


def _get_graph():
    from core.graph_client import GraphClient
    return GraphClient()


def _get_llm():
    from core.llm_client import LLMClient
    return LLMClient()


@mcp.tool()
def graph_query(question: str, zone: str = "", top_k: int = 10,
                use_llm_synthesis: bool = False) -> str:
    """
    Return source_path pointers (files and Confluence URLs) for a Phoenix question.

    Default: no local-LLM essay. Agents must open the listed sources and answer
    from live code / wiki. Set use_llm_synthesis true only for a short digest.

    Args:
        question: The question to answer
        zone: Optional GRAPH.1 layer (phoenix_domain, api_and_repo_layout, test_cases, playwright_automation). Legacy business names are mapped to domain + API.
        top_k: Number of results to retrieve per zone (default 10)
        use_llm_synthesis: If true, Qwen summarizes the pointer list (not evidence)
    """
    from core.graph_client import GraphClient
    from core.llm_client import LLMClient
    from core.graph_search import retrieve
    from core.bridge_resolver import resolve_bridges, format_context
    from core.staleness import filter_stale_nodes

    graph = GraphClient()
    llm = LLMClient()
    workspace = os.environ.get("GRAPH_RAG_WORKSPACE", _workspace_root)

    try:
        all_nodes, target_zones = retrieve(
            graph, llm, question, zone=zone or None, top_k=top_k
        )

        if not all_nodes:
            all_nodes, _ = retrieve(graph, llm, question, zone="all", top_k=top_k)
            if all_nodes:
                target_zones = ["phoenix_domain", "api_and_repo_layout"]

        if not all_nodes:
            return f"No relevant information found in the graph for: {question}"

        fresh, stale = filter_stale_nodes(all_nodes, workspace)
        bridge_nodes = resolve_bridges(graph, all_nodes, max_depth=1)
        context = format_context(all_nodes, bridge_nodes)

        if use_llm_synthesis:
            answer = llm.synthesize_answer(question, context)
        else:
            answer = context

        parts = [answer]
        parts.append(f"\n\n---\n**Graph RAG metadata:**")
        parts.append(f"- Zones queried: {', '.join(target_zones)}")
        parts.append(f"- Nodes found: {len(all_nodes)}")
        parts.append(f"- Bridge nodes: {len(bridge_nodes)}")
        parts.append(f"- Stale nodes: {len(stale)}")
        sources = list({
            n.get("source_path", "")
            for n in list(all_nodes) + list(bridge_nodes)
            if n.get("source_path")
        })
        if sources:
            parts.append(f"- Sources: {', '.join(sources[:5])}")

        return "\n".join(parts)

    except Exception as e:
        return f"Graph RAG query error: {e}"
    finally:
        graph.close()


@mcp.tool()
def graph_update(context_summary: str, zone: str = "",
                 question: str = "", answer: str = "") -> str:
    """
    Save chat learnings back to the knowledge graph.

    Extracts entities from the context and upserts to the graph.
    Logs Q/A pairs for future LoRA fine-tuning.

    Args:
        context_summary: Summary of what was discussed/learned
        zone: Optional target zone
        question: Original question (for training data)
        answer: Answer given (for training data)
    """
    import json
    from datetime import datetime, timezone
    from core.graph_client import GraphClient
    from core.llm_client import LLMClient
    from core.staleness import compute_content_hash

    graph = GraphClient()
    llm = LLMClient()

    try:
        extraction_prompt = (
            "Extract knowledge entities from the following context.\n"
            "Return a JSON array of objects, each with:\n"
            '  {"name": "...", "type": "Domain|Entity|BusinessProcess|Validation|Endpoint|DTO", '
            '"description": "...", "zone": "phoenix_domain|api_and_repo_layout|test_cases|playwright_automation"}\n\n'
            f"Context:\n{context_summary}\n\nJSON array:"
        )
        raw = llm.generate(extraction_prompt, temperature=0.0, max_tokens=1500)

        entities = []
        start = raw.find("[")
        end = raw.rfind("]")
        if start != -1 and end != -1:
            try:
                entities = [
                    e for e in json.loads(raw[start:end + 1])
                    if isinstance(e, dict) and "name" in e and "description" in e
                ]
            except json.JSONDecodeError:
                pass

        nodes_added = 0
        for entity in entities:
            entity_zone = zone or entity.get("zone", "phoenix_domain")
            uid = f"chat:{entity_zone}:{entity['name'].lower().replace(' ', '_')}"
            embedding = llm.embed(f"{entity['name']}: {entity['description']}")
            graph.upsert_node(
                uid=uid,
                node_type=entity.get("type", "Domain"),
                zone=entity_zone,
                name=entity["name"],
                description=entity["description"],
                properties={
                    "source_path": "chat_context",
                    "source_hash": compute_content_hash(context_summary),
                },
                embedding=embedding,
            )
            nodes_added += 1

        qa_logged = False
        if question and answer:
            qa_path = os.path.join(_graph_rag_root, "training_data", "qa_pairs.jsonl")
            os.makedirs(os.path.dirname(qa_path), exist_ok=True)
            entry = {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "question": question,
                "context_summary": context_summary[:500],
                "answer": answer[:2000],
            }
            with open(qa_path, "a", encoding="utf-8") as f:
                f.write(json.dumps(entry, ensure_ascii=False) + "\n")
            qa_logged = True

        parts = [f"Updated graph with {nodes_added} new nodes."]
        if entities:
            parts.append(f"Entities: {', '.join(e['name'] for e in entities)}")
        if qa_logged:
            parts.append("Q/A pair logged for future training.")
        return "\n".join(parts)

    except Exception as e:
        return f"Graph update error: {e}"
    finally:
        graph.close()


@mcp.tool()
def graph_index_topic(topic: str, zones: str = "") -> str:
    """
    Index a specific topic by running the full ingest pipeline.

    Args:
        topic: Topic name (e.g., "billing run", "invoice cancellation")
        zones: Comma-separated zone names to target (empty = all available)
    """
    from ingest.pipeline import run_ingest

    zone_list = [z.strip() for z in zones.split(",") if z.strip()] if zones else None
    workspace = os.environ.get("GRAPH_RAG_WORKSPACE", _workspace_root)

    summary = run_ingest(workspace_root=workspace, zones=zone_list, verbose=False)

    parts = [f"Indexed topic '{topic}'."]
    parts.append(f"Zones: {', '.join(summary.get('zones_processed', []))}")
    parts.append(f"Nodes created: {summary.get('nodes_created', 0)}")
    parts.append(f"Edges created: {summary.get('edges_created', 0)}")
    if summary.get("errors"):
        parts.append(f"Errors: {len(summary['errors'])}")
        for err in summary["errors"][:3]:
            parts.append(f"  - {err[:200]}")
    return "\n".join(parts)


@mcp.tool()
def graph_status() -> str:
    """
    Show current state of the Graph RAG system.

    Returns zone statistics, node/edge counts, and Neo4j Browser URL
    for visual exploration (spider-web view of the knowledge graph).
    """
    from core.graph_client import GraphClient

    graph = GraphClient()
    try:
        zone_stats = graph.get_zone_stats()
        total = graph.count_nodes()

        zones_summary = {}
        for row in zone_stats:
            z = row.get("zone", "unknown")
            nt = row.get("node_type", "unknown")
            cnt = row.get("count", 0)
            if z not in zones_summary:
                zones_summary[z] = {"total": 0, "types": {}}
            zones_summary[z]["total"] += cnt
            zones_summary[z]["types"][nt] = cnt

        edge_result = graph.run_query("MATCH ()-[r]->() RETURN count(r) AS cnt")
        edge_count = edge_result[0]["cnt"] if edge_result else 0

        parts = ["**Graph RAG Status: running**"]
        parts.append(f"Total nodes: {total}")
        parts.append(f"Total edges: {edge_count}")
        browser_url = os.environ.get("NEO4J_BROWSER_URL", "http://localhost:7474")
        parts.append(f"Neo4j Browser: {browser_url}")

        if zones_summary:
            parts.append("\n**Per-zone breakdown:**")
            for zn, zd in zones_summary.items():
                parts.append(f"\n  {zn}: {zd['total']} nodes")
                for nt, cnt in zd.get("types", {}).items():
                    parts.append(f"    - {nt}: {cnt}")
        else:
            parts.append("\nNo data ingested yet. Run graph_index_topic to populate.")

        return "\n".join(parts)

    except Exception as e:
        return f"**Graph RAG Status: error**\n{e}"
    finally:
        graph.close()


if __name__ == "__main__":
    transport = os.environ.get("MCP_TRANSPORT", "stdio").strip().lower()
    if transport not in {"stdio", "sse", "streamable-http"}:
        raise SystemExit(
            f"Unsupported MCP_TRANSPORT={transport!r}. "
            "Use stdio, sse, or streamable-http."
        )
    print(
        f"GraphRAG: starting transport={transport} host={_mcp_host} port={_mcp_port}",
        file=sys.stderr,
        flush=True,
    )
    mcp.run(transport=transport)
