"""
Staleness checker — compare source_hash to detect outdated graph nodes.
"""

import hashlib
import os
from datetime import datetime, timezone


def compute_file_hash(file_path: str) -> str | None:
    if not os.path.isfile(file_path):
        return None
    hasher = hashlib.sha256()
    with open(file_path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            hasher.update(chunk)
    return hasher.hexdigest()


def is_stale(node: dict, workspace_root: str) -> bool:
    """Check if a graph node's source has changed since last ingest."""
    source_path = node.get("source_path")
    stored_hash = node.get("source_hash")

    if not source_path or not stored_hash:
        return True

    abs_path = os.path.join(workspace_root, source_path)
    current_hash = compute_file_hash(abs_path)

    if current_hash is None:
        return True

    return current_hash != stored_hash


def compute_content_hash(content: str) -> str:
    return hashlib.sha256(content.encode("utf-8")).hexdigest()


def hours_since_update(node: dict) -> float | None:
    last_updated = node.get("last_updated")
    if not last_updated:
        return None
    try:
        if isinstance(last_updated, str):
            updated_dt = datetime.fromisoformat(last_updated)
        else:
            updated_dt = last_updated
        if updated_dt.tzinfo is None:
            updated_dt = updated_dt.replace(tzinfo=timezone.utc)
        delta = datetime.now(timezone.utc) - updated_dt
        return delta.total_seconds() / 3600
    except (ValueError, TypeError):
        return None


def filter_stale_nodes(nodes: list[dict], workspace_root: str,
                       max_age_hours: float = 24.0) -> tuple[list[dict], list[dict]]:
    """Split nodes into (fresh, stale) lists."""
    fresh, stale = [], []
    for node in nodes:
        age = hours_since_update(node)
        if age is not None and age > max_age_hours:
            stale.append(node)
        elif is_stale(node, workspace_root):
            stale.append(node)
        else:
            fresh.append(node)
    return fresh, stale
