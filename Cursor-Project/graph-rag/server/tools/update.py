"""
graph_update tool — Save chat learnings back to the graph.
"""

import json
import os
from datetime import datetime, timezone

from ...core.graph_client import GraphClient
from ...core.llm_client import LLMClient
from ...core.staleness import compute_content_hash


WORKSPACE_ROOT = os.environ.get(
    "GRAPH_RAG_WORKSPACE",
    os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "..", ".."))
)

QA_LOG_PATH = os.path.join(
    os.path.dirname(__file__), "..", "..", "training_data", "qa_pairs.jsonl"
)


def graph_update(context_summary: str, zone: str | None = None,
                 question: str | None = None, answer: str | None = None) -> dict:
    """
    Extract entities/relations from chat context and upsert to graph.
    Also logs Q/A pair for future LoRA training data.

    Args:
        context_summary: What was discussed/learned in the chat session
        zone: Target zone (auto-detected if None)
        question: Original question (for Q/A logging)
        answer: Answer that was given (for Q/A logging)

    Returns dict with: nodes_added, edges_added, qa_logged
    """
    graph = GraphClient()
    llm = LLMClient()

    try:
        # Extract entities from context using LLM
        extraction_prompt = (
            "Extract knowledge entities from the following context.\n"
            "Return a JSON array of objects, each with:\n"
            '  {"name": "...", "type": "Domain|Entity|BusinessProcess|Validation|Endpoint|DTO", '
            '"description": "...", "zone": "phoenix_domain|api_and_repo_layout|test_cases|playwright_automation"}\n\n'
            f"Context:\n{context_summary}\n\n"
            "JSON array:"
        )

        raw = llm.generate(extraction_prompt, temperature=0.0, max_tokens=1500)
        entities = _parse_entities(raw)

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

        # Log Q/A pair for future LoRA training
        qa_logged = False
        if question and answer:
            qa_logged = _log_qa_pair(question, context_summary, answer)

        return {
            "nodes_added": nodes_added,
            "entities_extracted": [e["name"] for e in entities],
            "qa_logged": qa_logged,
        }

    finally:
        graph.close()


def _parse_entities(raw: str) -> list[dict]:
    """Try to parse LLM output as JSON array of entities."""
    raw = raw.strip()
    # Find JSON array in the response
    start = raw.find("[")
    end = raw.rfind("]")
    if start == -1 or end == -1:
        return []
    try:
        entities = json.loads(raw[start : end + 1])
        return [
            e for e in entities
            if isinstance(e, dict) and "name" in e and "description" in e
        ]
    except json.JSONDecodeError:
        return []


def _log_qa_pair(question: str, context: str, answer: str) -> bool:
    """Append Q/A pair to JSONL file for future LoRA training."""
    try:
        os.makedirs(os.path.dirname(QA_LOG_PATH), exist_ok=True)
        entry = {
            "timestamp": datetime.now(timezone.utc).isoformat(),
            "question": question,
            "context_summary": context[:500],
            "answer": answer[:2000],
        }
        with open(QA_LOG_PATH, "a", encoding="utf-8") as f:
            f.write(json.dumps(entry, ensure_ascii=False) + "\n")
        return True
    except Exception:
        return False
