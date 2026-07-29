"""
Swagger/OpenAPI extractor — parse spec JSON into Endpoint, DTO, and EnumType nodes.
"""

import json
import os
import re

from core.staleness import compute_file_hash

ZONE = "api_and_repo_layout"


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

    # --- Endpoints ---
    for path, methods in spec.get("paths", {}).items():
        for method, details in methods.items():
            if method in ("parameters", "servers", "summary", "description", "$ref"):
                continue

            op_id = details.get("operationId", f"{method}_{path}")
            summary = details.get("summary", "")
            description = details.get("description", summary)
            tags = details.get("tags", [])

            uid = f"endpoint:{env_name}:{method.upper()}:{path}"
            nodes.append({
                "uid": uid,
                "node_type": "Endpoint",
                "zone": ZONE,
                "name": f"{method.upper()} {path}",
                "description": _truncate(f"{summary}. {description}".strip(". "), 500),
                "source_path": rel_path,
                "source_hash": source_hash,
                "properties": {
                    "http_method": method.upper(),
                    "path": path,
                    "operation_id": op_id,
                    "tags": ", ".join(tags),
                    "environment": env_name,
                },
            })

            # Link request/response DTOs
            req_ref = _extract_request_schema_ref(details)
            if req_ref:
                dto_uid = f"dto:{env_name}:{req_ref}"
                edges.append({
                    "from_uid": uid,
                    "to_uid": dto_uid,
                    "edge_type": "ACCEPTS",
                })

            resp_refs = _extract_response_schema_refs(details)
            for ref_name in resp_refs:
                dto_uid = f"dto:{env_name}:{ref_name}"
                edges.append({
                    "from_uid": uid,
                    "to_uid": dto_uid,
                    "edge_type": "RETURNS",
                })

    # --- DTOs (schemas/components) ---
    schemas = spec.get("components", {}).get("schemas", {})
    for schema_name, schema_def in schemas.items():
        props = schema_def.get("properties", {})
        required = schema_def.get("required", [])
        field_names = list(props.keys())

        is_enum = "enum" in schema_def
        node_type = "EnumType" if is_enum else "DTO"

        uid = f"{'enum' if is_enum else 'dto'}:{env_name}:{schema_name}"
        desc_parts = []
        if is_enum:
            values = schema_def.get("enum", [])
            desc_parts.append(f"Enum values: {', '.join(str(v) for v in values[:20])}")
        else:
            if required:
                desc_parts.append(f"Required: {', '.join(required[:10])}")
            if field_names:
                desc_parts.append(f"Fields: {', '.join(field_names[:15])}")

        nodes.append({
            "uid": uid,
            "node_type": node_type,
            "zone": ZONE,
            "name": schema_name,
            "description": _truncate(". ".join(desc_parts), 500),
            "source_path": rel_path,
            "source_hash": source_hash,
            "properties": {
                "fields": ", ".join(field_names[:30]),
                "required_fields": ", ".join(required[:20]),
                "environment": env_name,
            },
        })

        # DTO -> Enum edges
        for field_name, field_def in props.items():
            if "enum" in field_def:
                enum_uid = f"enum:{env_name}:{schema_name}_{field_name}"
                enum_values = field_def["enum"]
                nodes.append({
                    "uid": enum_uid,
                    "node_type": "EnumType",
                    "zone": ZONE,
                    "name": f"{schema_name}.{field_name}",
                    "description": f"Enum values: {', '.join(str(v) for v in enum_values[:20])}",
                    "source_path": rel_path,
                    "source_hash": source_hash,
                    "properties": {"environment": env_name},
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


def _extract_request_schema_ref(operation: dict) -> str | None:
    rb = operation.get("requestBody", {})
    content = rb.get("content", {})
    for media_type in ("application/json", "*/*"):
        schema = content.get(media_type, {}).get("schema", {})
        ref = schema.get("$ref", "")
        if ref:
            return ref.rsplit("/", 1)[-1]
    return None


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
