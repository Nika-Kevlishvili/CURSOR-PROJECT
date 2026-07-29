"""
Test case extractor — parse test case markdown files into TestCase/TestStep nodes.
"""

import os
import re

from core.staleness import compute_file_hash

ZONE = "test_cases"


def extract_test_cases(directory: str, workspace_root: str) -> list[dict]:
    """
    Scan a directory of test case markdown files and extract
    TestCase and TestStep nodes.
    """
    abs_dir = os.path.join(workspace_root, directory)
    if not os.path.isdir(abs_dir):
        return []

    all_results: list[dict] = []

    for root, _dirs, files in os.walk(abs_dir):
        for fname in files:
            if not fname.endswith(".md"):
                continue
            abs_path = os.path.join(root, fname)
            rel_path = os.path.relpath(abs_path, workspace_root).replace("\\", "/")
            result = _parse_test_case_file(abs_path, rel_path)
            if result:
                all_results.append(result)

    return all_results


def _parse_test_case_file(abs_path: str, rel_path: str) -> dict | None:
    source_hash = compute_file_hash(abs_path)
    with open(abs_path, encoding="utf-8") as f:
        content = f.read()

    if not content.strip():
        return None

    nodes: list[dict] = []
    edges: list[dict] = []

    topic = os.path.splitext(os.path.basename(abs_path))[0]
    is_frontend = "/Frontend/" in rel_path or "\\Frontend\\" in rel_path
    tc_prefix = "TC-FE" if is_frontend else "TC-BE"

    test_cases = _split_test_cases(content)

    for idx, tc in enumerate(test_cases, 1):
        tc_id = tc.get("id", f"{tc_prefix}-{idx}")
        tc_name = tc.get("title", f"{topic} - {tc_id}")
        tc_description = tc.get("description", "")
        priority = tc.get("priority", "Medium")
        preconditions = tc.get("preconditions", "")
        jira_key = tc.get("jira_key", "")

        tc_uid = f"testcase:{rel_path}:{tc_id}"
        nodes.append({
            "uid": tc_uid,
            "node_type": "TestCase",
            "zone": ZONE,
            "name": tc_name,
            "description": _truncate(tc_description, 500),
            "source_path": rel_path,
            "source_hash": source_hash,
            "properties": {
                "tc_id": tc_id,
                "priority": priority,
                "jira_key": jira_key,
                "topic": topic,
                "scope": "Frontend" if is_frontend else "Backend",
            },
        })

        if preconditions:
            pre_uid = f"precondition:{rel_path}:{tc_id}"
            nodes.append({
                "uid": pre_uid,
                "node_type": "Precondition",
                "zone": ZONE,
                "name": f"Preconditions for {tc_id}",
                "description": _truncate(preconditions, 500),
                "source_path": rel_path,
                "source_hash": source_hash,
                "properties": {},
            })
            edges.append({
                "from_uid": tc_uid,
                "to_uid": pre_uid,
                "edge_type": "HAS_PRECONDITION",
            })

        for step_idx, step in enumerate(tc.get("steps", []), 1):
            step_uid = f"step:{rel_path}:{tc_id}:S{step_idx}"
            nodes.append({
                "uid": step_uid,
                "node_type": "TestStep",
                "zone": ZONE,
                "name": f"{tc_id} Step {step_idx}",
                "description": _truncate(step.get("action", ""), 300),
                "source_path": rel_path,
                "source_hash": source_hash,
                "properties": {"step_number": step_idx},
            })
            edges.append({
                "from_uid": tc_uid,
                "to_uid": step_uid,
                "edge_type": "HAS_STEP",
            })

            expected = step.get("expected", "")
            if expected:
                er_uid = f"expected:{rel_path}:{tc_id}:S{step_idx}"
                nodes.append({
                    "uid": er_uid,
                    "node_type": "ExpectedResult",
                    "zone": ZONE,
                    "name": f"{tc_id} Step {step_idx} Expected",
                    "description": _truncate(expected, 300),
                    "source_path": rel_path,
                    "source_hash": source_hash,
                    "properties": {},
                })
                edges.append({
                    "from_uid": step_uid,
                    "to_uid": er_uid,
                    "edge_type": "EXPECTS",
                })

    return {"nodes": nodes, "edges": edges} if nodes else None


def _split_test_cases(content: str) -> list[dict]:
    """Parse markdown content into structured test case dicts."""
    test_cases = []
    tc_blocks = re.split(r"(?=^##\s+(?:TC-|Test Case))", content, flags=re.MULTILINE)

    for block in tc_blocks:
        block = block.strip()
        if not block:
            continue

        tc: dict = {"steps": []}

        title_match = re.match(r"^##\s+(.+?)(?:\n|$)", block)
        if title_match:
            tc["title"] = title_match.group(1).strip()
            id_match = re.search(r"(TC-(?:BE|FE)-\d+)", tc["title"])
            if id_match:
                tc["id"] = id_match.group(1)

        prio_match = re.search(r"\*\*Priority\*\*[:\s]*(\w+)", block, re.IGNORECASE)
        if prio_match:
            tc["priority"] = prio_match.group(1)

        jira_match = re.search(r"(PDT-\d+|GB-\d+)", block)
        if jira_match:
            tc["jira_key"] = jira_match.group(1)

        desc_match = re.search(r"\*\*Description\*\*[:\s]*(.*?)(?=\n\*\*|\n##|\Z)",
                               block, re.DOTALL | re.IGNORECASE)
        if desc_match:
            tc["description"] = desc_match.group(1).strip()

        pre_match = re.search(r"\*\*Preconditions?\*\*[:\s]*(.*?)(?=\n\*\*|\n##|\Z)",
                              block, re.DOTALL | re.IGNORECASE)
        if pre_match:
            tc["preconditions"] = pre_match.group(1).strip()

        step_pattern = re.compile(
            r"\|\s*\d+\s*\|\s*(.*?)\s*\|\s*(.*?)\s*\|", re.MULTILINE
        )
        for match in step_pattern.finditer(block):
            action = match.group(1).strip()
            expected = match.group(2).strip()
            if action and action.lower() not in ("action", "step", "test step"):
                tc["steps"].append({"action": action, "expected": expected})

        if tc.get("title") or tc.get("steps"):
            test_cases.append(tc)

    if not test_cases and content.strip():
        test_cases.append({
            "title": "Full document",
            "description": _truncate(content, 500),
            "steps": [],
        })

    return test_cases


def _truncate(text: str, max_len: int) -> str:
    if len(text) <= max_len:
        return text
    return text[: max_len - 3] + "..."
