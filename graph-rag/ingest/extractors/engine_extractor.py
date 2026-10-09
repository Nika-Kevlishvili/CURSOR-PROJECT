"""Engine document extractor.

Indexes Cursor rules, skills, agents, memory, and SDLC docs.
Does not read Phoenix/, EnergoTS/, or Swagger.
"""

import os

from core.staleness import compute_file_hash

ENGINE_ZONES = (
    "rules",
    "skills_and_agents",
    "memory_and_handoff",
    "sdlc_docs",
)

_ZONE_SOURCES = {
    "rules": {
        "paths": [".cursor/rules"],
        "suffixes": (".mdc", ".md"),
        "node_type": "Rule",
    },
    "skills_and_agents": {
        "paths": [".cursor/skills", ".cursor/agents"],
        "suffixes": (".md",),
        "node_type": "",
    },
    "memory_and_handoff": {
        "paths": ["memory"],
        "suffixes": (".md",),
        "node_type": "MemoryNote",
    },
    "sdlc_docs": {
        "paths": ["docs", "sdlc/design"],
        "suffixes": (".md",),
        "node_type": "Document",
    },
}

_BLOCKED_ROOTS = {"phoenix", "energots"}


def _blocked(rel: str) -> bool:
    norm = rel.replace("\\", "/").strip("/")
    lower = norm.lower()
    if not lower:
        return True
    first = lower.split("/", 1)[0]
    if first in _BLOCKED_ROOTS:
        return True
    if "/phoenix/" in f"/{lower}/" or "/energots/" in f"/{lower}/":
        return True
    if lower.endswith("swagger-spec.json") or "/swagger/" in f"/{lower}/":
        return True
    return False


def _node_type(zone: str, rel: str) -> str:
    configured = _ZONE_SOURCES[zone]["node_type"]
    if configured:
        return configured
    lower = rel.replace("\\", "/").lower()
    if "/.cursor/agents/" in f"/{lower}" or lower.startswith(".cursor/agents/"):
        return "Agent"
    return "Skill"


def _description(text: str) -> str:
    chunks: list[str] = []
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped or stripped == "---":
            continue
        if stripped.startswith("#"):
            stripped = stripped.lstrip("#").strip()
        if stripped:
            chunks.append(stripped)
        if len(" ".join(chunks)) > 480:
            break
    summary = " ".join(chunks).strip()
    return (summary[:500] or "Engine document.")


def extract_engine(zone: str, workspace_root: str) -> list[dict]:
    """Return one result dict of nodes for an engine zone. Edges stay empty."""
    spec = _ZONE_SOURCES.get(zone)
    if spec is None:
        return []

    nodes: list[dict] = []
    for relative_dir in spec["paths"]:
        abs_dir = os.path.join(workspace_root, relative_dir)
        if not os.path.isdir(abs_dir):
            continue
        for root, dirs, files in os.walk(abs_dir):
            dirs[:] = [d for d in dirs if d.lower() not in _BLOCKED_ROOTS and d not in {".git", "node_modules", "__pycache__"}]
            for fname in files:
                if not fname.endswith(spec["suffixes"]):
                    continue
                abs_path = os.path.join(root, fname)
                rel = os.path.relpath(abs_path, workspace_root).replace("\\", "/")
                if _blocked(rel):
                    continue
                try:
                    with open(abs_path, encoding="utf-8") as handle:
                        text = handle.read()
                except (OSError, UnicodeDecodeError):
                    continue
                if not text.strip():
                    continue
                title = os.path.splitext(fname)[0]
                nodes.append({
                    "uid": f"{zone}:{rel}",
                    "node_type": _node_type(zone, rel),
                    "zone": zone,
                    "name": title,
                    "title": title,
                    "description": _description(text),
                    "source_path": rel,
                    "source_hash": compute_file_hash(abs_path) or "",
                    "properties": {"topic": zone},
                })
    return [{"nodes": nodes, "edges": []}] if nodes else []
