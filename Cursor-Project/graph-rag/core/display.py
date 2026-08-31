"""Layer colors and human captions for graph nodes (GRAPH.1)."""

import os

import yaml

_CONFIG = os.path.join(os.path.dirname(__file__), "..", "config", "layer_display.yaml")

_FALLBACK_LAYER = "api_and_repo_layout"

_cache: dict | None = None


def load_layers() -> dict:
    global _cache
    if _cache is None:
        with open(_CONFIG, encoding="utf-8") as f:
            _cache = yaml.safe_load(f).get("layers", {})
    return _cache


def neo4j_layer_label(zone: str) -> str:
    layers = load_layers()
    layer = zone if zone in layers else _FALLBACK_LAYER
    return layers[layer]["neo4j_label"]


def enrich_display(node: dict) -> dict:
    """Add title, color, layer_title; map unknown zones to API layer."""
    layers = load_layers()
    zone = node.get("zone") or _FALLBACK_LAYER
    if zone not in layers:
        zone = _FALLBACK_LAYER
        node["zone"] = zone
    cfg = layers[zone]
    raw_title = (node.get("title") or "").strip() or node.get("name", "Untitled")
    badge = (cfg.get("badge") or cfg["title"]).strip()
    if raw_title.startswith(badge) or raw_title.startswith(cfg["title"]):
        title = raw_title
    else:
        title = f"{badge} · {raw_title}"
    description = (node.get("description") or "").strip() or cfg["description"]
    node["title"] = title
    node["description"] = description
    props = dict(node.get("properties") or {})
    props["title"] = title
    props["color"] = cfg["color"]
    props["layer_title"] = cfg["title"]
    props["layer_description"] = cfg["description"]
    node["properties"] = props
    return node
