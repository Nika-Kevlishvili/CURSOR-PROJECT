"""
Swagger/OpenAPI extractor — parse spec JSON into Endpoint, DTO, and EnumType nodes.
`n.zone` is the GRAPH.1 API layer (`api_and_repo_layout`). Business area is `properties.domain`.
"""

import json
import os
import re

import yaml

from core.staleness import compute_file_hash

_ZONES_PATH = os.path.join(os.path.dirname(__file__), "..", "..", "config", "zones.yaml")

FALLBACK_ZONE = "reference_data"
API_LAYER_ZONE = "api_and_repo_layout"

_METHOD_VERB = {
    "GET": "Get",
    "POST": "Create",
    "PUT": "Update",
    "PATCH": "Change",
    "DELETE": "Delete",
}

_OP_VERB = {
    "view": "View",
    "edit": "Edit",
    "add": "Create",
    "create": "Create",
    "delete": "Delete",
    "list": "List",
    "treeview": "Tree view",
    "detailedview": "Detailed view",
}


def _human_ident(name: str) -> str:
    text = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", name or "")
    text = text.replace("_", " ").replace(".", " ").replace("-", " ")
    text = re.sub(r"\s+", " ", text).strip()
    if not text:
        return name or "Untitled"
    return text[0].upper() + text[1:]


def _path_phrase(path: str) -> str:
    parts = []
    for raw in (path or "").strip("/").split("/"):
        if not raw:
            continue
        if raw.startswith("{") and raw.endswith("}"):
            parts.append("by id")
            continue
        parts.append(raw.replace("-", " "))
    return " — ".join(parts) if parts else "this resource"


def _endpoint_title(method: str, path: str, summary: str, op_id: str) -> str:
    summary = (summary or "").strip()
    if summary and summary.lower() not in ("ok", "default"):
        return summary
    phrase = _path_phrase(path)
    op_key = re.sub(r"[^a-z0-9]", "", (op_id or "").lower())
    if op_key in _OP_VERB and not re.match(r"^[a-z]+_\d+$", op_id or ""):
        return f"{_OP_VERB[op_key]} ({phrase})"
    verb = _METHOD_VERB.get((method or "").upper(), (method or "").upper())
    return f"{verb} {phrase}"


def _endpoint_description(title: str, method: str, path: str, summary: str,
                          description: str, env_name: str) -> str:
    extra = " ".join(
        part.strip()
        for part in (summary, description)
        if part and part.strip() and part.strip().lower() not in ("ok", title.lower())
    )
    base = (
        f"{title}. HTTP {method.upper()} {path} "
        f"(Swagger environment: {env_name})."
    )
    if extra:
        return _truncate(f"{base} {extra}", 500)
    return base


def _dto_title(schema_name: str, is_enum: bool) -> str:
    if is_enum:
        return f"{_human_ident(schema_name)} (allowed values)"
    lower = schema_name.lower()
    if lower.endswith("filterrequest"):
        return f"{_human_ident(schema_name[:-13])} search filter"
    if lower.endswith("request"):
        return f"{_human_ident(schema_name[:-7])} request body"
    if lower.endswith("response"):
        stem = schema_name[:-8]
        if stem.lower().startswith("page"):
            return f"{_human_ident(stem[4:])} list page"
        return f"{_human_ident(stem)} API result"
    return _human_ident(schema_name)


def _dto_description(schema_name: str, is_enum: bool, required: list,
                     field_names: list, enum_values: list | None = None) -> str:
    label = _human_ident(schema_name)
    if is_enum:
        values = ", ".join(str(v) for v in (enum_values or [])[:20])
        return f"{label}: allowed values are {values}." if values else f"{label} is a fixed list of values."
    parts = [f"{label} is an API data type."]
    if required:
        parts.append(
            "Must include: " + ", ".join(_human_ident(x) for x in required[:10]) + "."
        )
    if field_names:
        parts.append(
            "Fields: " + ", ".join(_human_ident(x) for x in field_names[:15]) + "."
        )
    return _truncate(" ".join(parts), 500)


def _build_prefix_to_zone() -> dict[str, str]:
    """Build a mapping from API path prefix to zone using zones.yaml."""
    try:
        with open(_ZONES_PATH, encoding="utf-8") as f:
            cfg = yaml.safe_load(f)
    except FileNotFoundError:
        return {}

    mapping: dict[str, str] = {}
    for zone_name, zone_def in cfg.get("zones", {}).items():
        for prefix in zone_def.get("prefixes", []):
            mapping[prefix.lower()] = zone_name
    return mapping


PREFIX_TO_ZONE = _build_prefix_to_zone()


def _resolve_zone_for_path(api_path: str) -> str:
    """Determine the zone for an API path based on its first path segment."""
    clean = api_path.strip("/")
    first_segment = clean.split("/")[0] if clean else ""

    if not first_segment:
        return FALLBACK_ZONE

    first_lower = first_segment.lower()
    if first_lower in PREFIX_TO_ZONE:
        return PREFIX_TO_ZONE[first_lower]

    for prefix, zone in PREFIX_TO_ZONE.items():
        if first_lower.startswith(prefix) or prefix.startswith(first_lower):
            return zone

    return FALLBACK_ZONE


def _resolve_zone_for_dto(schema_name: str, endpoint_dto_zones: dict[str, str]) -> str:
    """Determine zone for a DTO based on which endpoints reference it."""
    if schema_name in endpoint_dto_zones:
        return endpoint_dto_zones[schema_name]

    name_lower = schema_name.lower()
    for prefix, zone in PREFIX_TO_ZONE.items():
        prefix_camel = prefix.replace("-", "")
        if prefix_camel in name_lower:
            return zone

    return FALLBACK_ZONE


def extract_swagger(swagger_path: str, workspace_root: str) -> list[dict]:
    """
    Parse an OpenAPI 3.x JSON spec and return a list of node dicts
    ready for upserting into Neo4j.
    """
    abs_path = os.path.join(workspace_root, swagger_path)
    if not os.path.isfile(abs_path):
        return []

    with open(abs_path, encoding="utf-8") as f:
        spec = json.load(f)

    source_hash = compute_file_hash(abs_path)
    rel_path = swagger_path

    nodes: list[dict] = []
    edges: list[dict] = []

    env_name = _extract_env_from_path(swagger_path)

    endpoint_dto_zones: dict[str, str] = {}

    # --- Endpoints ---
    for path, methods in spec.get("paths", {}).items():
        for method, details in methods.items():
            if method in ("parameters", "servers", "summary", "description", "$ref"):
                continue

            domain = _resolve_zone_for_path(path)
            op_id = details.get("operationId", f"{method}_{path}")
            summary = details.get("summary", "")
            description = details.get("description", summary)
            tags = details.get("tags", [])
            title = _endpoint_title(method, path, summary, op_id)

            uid = f"endpoint:{env_name}:{method.upper()}:{path}"
            nodes.append({
                "uid": uid,
                "node_type": "Endpoint",
                "zone": API_LAYER_ZONE,
                "name": f"{method.upper()} {path}",
                "title": title,
                "description": _endpoint_description(
                    title, method, path, summary, description, env_name
                ),
                "source_path": rel_path,
                "source_hash": source_hash,
                "properties": {
                    "http_method": method.upper(),
                    "path": path,
                    "operation_id": op_id,
                    "tags": ", ".join(tags),
                    "environment": env_name,
                    "domain": domain,
                },
            })

            for req_ref in _extract_accepted_schema_refs(details):
                dto_uid = f"dto:{env_name}:{req_ref}"
                edges.append({
                    "from_uid": uid,
                    "to_uid": dto_uid,
                    "edge_type": "ACCEPTS",
                })
                endpoint_dto_zones[req_ref] = domain

            resp_refs = _extract_response_schema_refs(details)
            for ref_name in resp_refs:
                dto_uid = f"dto:{env_name}:{ref_name}"
                edges.append({
                    "from_uid": uid,
                    "to_uid": dto_uid,
                    "edge_type": "RETURNS",
                })
                if ref_name not in endpoint_dto_zones:
                    endpoint_dto_zones[ref_name] = domain

    # --- DTOs (schemas/components) ---
    schemas = spec.get("components", {}).get("schemas", {})
    for schema_name, schema_def in schemas.items():
        props = schema_def.get("properties", {})
        required = schema_def.get("required", [])
        field_names = list(props.keys())

        is_enum = "enum" in schema_def
        node_type = "EnumType" if is_enum else "DTO"

        domain = _resolve_zone_for_dto(schema_name, endpoint_dto_zones)
        enum_values = schema_def.get("enum", []) if is_enum else []

        uid = f"{'enum' if is_enum else 'dto'}:{env_name}:{schema_name}"
        nodes.append({
            "uid": uid,
            "node_type": node_type,
            "zone": API_LAYER_ZONE,
            "name": schema_name,
            "title": _dto_title(schema_name, is_enum),
            "description": _dto_description(
                schema_name, is_enum, required, field_names, enum_values
            ),
            "source_path": rel_path,
            "source_hash": source_hash,
            "properties": {
                "fields": ", ".join(field_names[:30]),
                "required_fields": ", ".join(required[:20]),
                "environment": env_name,
                "domain": domain,
            },
        })

        for field_name, field_def in props.items():
            if "enum" in field_def:
                enum_uid = f"enum:{env_name}:{schema_name}_{field_name}"
                enum_values = field_def["enum"]
                field_title = f"{_human_ident(schema_name)} — {_human_ident(field_name)}"
                nodes.append({
                    "uid": enum_uid,
                    "node_type": "EnumType",
                    "zone": API_LAYER_ZONE,
                    "name": f"{schema_name}.{field_name}",
                    "title": field_title,
                    "description": (
                        f"Allowed values for {_human_ident(field_name)} on "
                        f"{_human_ident(schema_name)}: "
                        f"{', '.join(str(v) for v in enum_values[:20])}."
                    ),
                    "source_path": rel_path,
                    "source_hash": source_hash,
                    "properties": {
                        "environment": env_name,
                        "domain": domain,
                    },
                })
                edges.append({
                    "from_uid": uid,
                    "to_uid": enum_uid,
                    "edge_type": "HAS_ENUM",
                })

    return [{"nodes": nodes, "edges": edges}]


def _extract_env_from_path(swagger_path: str) -> str:
    parts = swagger_path.replace("\\", "/").split("/")
    for known in ("dev", "dev2", "test", "preprod", "prod", "experiment"):
        if known in parts:
            return known
    return "unknown"


def _schema_ref_name(schema: dict | None) -> str | None:
    if not schema:
        return None
    ref = schema.get("$ref") or ""
    if ref:
        return ref.rsplit("/", 1)[-1]
    items_ref = (schema.get("items") or {}).get("$ref") or ""
    if items_ref:
        return items_ref.rsplit("/", 1)[-1]
    return None


def _extract_request_schema_ref(operation: dict) -> str | None:
    rb = operation.get("requestBody", {})
    content = rb.get("content", {})
    for media_type in ("application/json", "*/*"):
        name = _schema_ref_name(content.get(media_type, {}).get("schema"))
        if name:
            return name
    return None


def _extract_accepted_schema_refs(operation: dict) -> list[str]:
    """DTOs the operation accepts: JSON body and query/path parameter $refs."""
    names: list[str] = []
    body = _extract_request_schema_ref(operation)
    if body:
        names.append(body)
    for param in operation.get("parameters") or []:
        name = _schema_ref_name(param.get("schema"))
        if name:
            names.append(name)
        content = param.get("content") or {}
        for media in content.values():
            name = _schema_ref_name((media or {}).get("schema"))
            if name:
                names.append(name)
    seen: set[str] = set()
    ordered: list[str] = []
    for name in names:
        if name not in seen:
            seen.add(name)
            ordered.append(name)
    return ordered


def _extract_response_schema_refs(operation: dict) -> list[str]:
    refs = []
    for code, response in operation.get("responses", {}).items():
        content = response.get("content", {})
        for media_type in ("application/json", "*/*"):
            schema = content.get(media_type, {}).get("schema", {})
            ref = schema.get("$ref", "")
            if ref:
                refs.append(ref.rsplit("/", 1)[-1])
            items_ref = schema.get("items", {}).get("$ref", "")
            if items_ref:
                refs.append(items_ref.rsplit("/", 1)[-1])
    return list(set(refs))


def _truncate(text: str, max_len: int) -> str:
    if len(text) <= max_len:
        return text
    return text[: max_len - 3] + "..."
