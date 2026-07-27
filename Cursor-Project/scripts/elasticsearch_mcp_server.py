"""
Elasticsearch MCP Server for Cursor.

Bridges Elasticsearch (via REST API) to Cursor agents using the MCP protocol.
Supports environment-aware Phoenix log searching, SQL queries (like Kibana
Dev Tools), native Query DSL, index discovery, and field mapping inspection.

Each MCP server instance serves TWO environments that share a cluster:
  ElasticsearchDev  → Dev + Dev2   (HTTPS, API key auth)
  ElasticsearchTest → Test + PreProd (HTTP, no auth)

Environment filtering uses the `app_name` field:
  Primary env (dev/test)     → phoenix, phoenix-scheduler, ...
  Secondary env (dev2/preprod) → phoenix2, phoenix-scheduler2, ...

Environment variables (set in mcp.json env block):
    ES_HOST          - Elasticsearch URL
    ES_API_KEY       - Base64-encoded API key (empty = no auth)
    ES_VERIFY_SSL    - "true" or "false" (default: false)
    ES_SERVER_NAME   - MCP server display name (default: Elasticsearch)
    ES_ENVIRONMENTS  - Comma-separated pair of env names this instance serves
                       (default: "dev,dev2"). E.g. "test,preprod".
"""

import json
import os
import urllib3

import requests
from mcp.server.fastmcp import FastMCP

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

ES_HOST = os.environ.get("ES_HOST", "https://localhost:9200").rstrip("/")
ES_API_KEY = os.environ.get("ES_API_KEY", "")
ES_VERIFY_SSL = os.environ.get("ES_VERIFY_SSL", "false").lower() == "true"
ES_SERVER_NAME = os.environ.get("ES_SERVER_NAME", "Elasticsearch")

_env_pair = os.environ.get("ES_ENVIRONMENTS", "dev,dev2").lower().split(",")
ES_PRIMARY_ENV = _env_pair[0].strip()
ES_SECONDARY_ENV = _env_pair[1].strip() if len(_env_pair) > 1 else ""
SUPPORTED_ENVS = {ES_PRIMARY_ENV, ES_SECONDARY_ENV} - {""}

ES_READ_ONLY = os.environ.get("ES_READ_ONLY", "false").lower() == "true"

HEADERS: dict[str, str] = {"Content-Type": "application/json"}
if ES_API_KEY:
    HEADERS["Authorization"] = f"ApiKey {ES_API_KEY}"

BLOCKED_PATH_PREFIXES = (
    "_bulk", "_delete_by_query", "_update_by_query", "_reindex",
    "_aliases", "_template", "_index_template", "_component_template",
    "_ingest", "_snapshot", "_ilm", "_rollover",
)
WRITE_METHODS = {"PUT", "DELETE", "PATCH"}

BASE_APP_NAMES = [
    "phoenix", "phoenix-scheduler", "phoenix-billing-run",
    "phoenix-mass-import", "phoenix-sales-portal", "phoenix-payment-api",
]

mcp = FastMCP(
    ES_SERVER_NAME,
    instructions=(
        f"Elasticsearch MCP server for Phoenix {'/'.join(SUPPORTED_ENVS)} environments. "
        f"Two envs share this cluster — filter by environment using the app_name field "
        f"({ES_PRIMARY_ENV}: phoenix, phoenix-scheduler, ... | "
        f"{ES_SECONDARY_ENV}: phoenix2, phoenix-scheduler2, ...). "
        "Use es_search_logs for environment-aware Phoenix log searching during bug validation. "
        "Use es_sql_query for SQL-style queries (like Kibana). "
        "Use es_list_indices to discover available indices. "
        "Use es_index_mapping to see fields before querying."
    ),
)


def _request(method: str, path: str, body: dict | None = None, params: dict | None = None) -> dict | list | str:
    if ES_READ_ONLY:
        normalized = path.lstrip("/").split("?")[0]
        if method.upper() in WRITE_METHODS:
            return {"error": f"BLOCKED: {method.upper()} is a write operation. This server is read-only (ES_READ_ONLY=true)."}
        for prefix in BLOCKED_PATH_PREFIXES:
            if normalized == prefix or normalized.startswith(f"{prefix}/") or f"/{prefix}" in normalized:
                return {"error": f"BLOCKED: path '{path}' is a write operation. This server is read-only (ES_READ_ONLY=true)."}

    url = f"{ES_HOST}/{path.lstrip('/')}"
    try:
        resp = requests.request(
            method, url,
            headers=HEADERS,
            json=body,
            params=params,
            verify=ES_VERIFY_SSL,
            timeout=30,
        )
        resp.raise_for_status()
        return resp.json()
    except requests.exceptions.HTTPError as e:
        error_body = ""
        try:
            error_body = e.response.text[:2000]
        except Exception:
            pass
        return {"error": str(e), "status_code": e.response.status_code, "detail": error_body}
    except requests.exceptions.ConnectionError as e:
        return {"error": f"Connection failed: {e}"}
    except requests.exceptions.Timeout:
        return {"error": "Request timed out (30s)"}
    except Exception as e:
        return {"error": str(e)}


def _build_env_filter(environment: str) -> list[dict]:
    """Build Elasticsearch bool filter clauses for app_name based on environment.

    Primary env (dev, test) uses base app names.
    Secondary env (dev2, preprod) uses base app names with '2' suffix.
    """
    env = environment.strip().lower()
    if env == ES_SECONDARY_ENV:
        names = [f"{base}2" for base in BASE_APP_NAMES]
    elif env == ES_PRIMARY_ENV:
        names = list(BASE_APP_NAMES)
    else:
        return []
    return [{"terms": {"app_name.keyword": names}}]


@mcp.tool()
def es_cluster_health() -> str:
    """Check Elasticsearch cluster health. Use this first to verify connectivity."""
    result = _request("GET", "_cluster/health")
    return json.dumps(result, indent=2)


@mcp.tool()
def es_search_logs(
    environment: str,
    search_text: str = "",
    level: str = "",
    app_name: str = "",
    logger_name: str = "",
    days_back: int = 7,
    size: int = 30,
    extra_filters: str = "",
) -> str:
    """Search Phoenix application logs filtered by environment.

    This is the primary tool for bug validation log analysis. Two environments
    share each cluster — this tool filters by app_name automatically
    (primary env: phoenix, phoenix-scheduler, ... vs secondary env: phoenix2,
    phoenix-scheduler2, ...).

    Args:
        environment: REQUIRED. One of the supported environments for this server
                    (e.g. "dev"/"dev2" or "test"/"preprod").
        search_text: Free-text search across message and stack_trace fields.
                    Supports wildcards. Example: "NullPointerException",
                    "invoice cancellation", "timeout".
        level: Log level filter. One of: ERROR, WARN, INFO, DEBUG.
              Leave empty for all levels.
        app_name: Filter to a specific app. Example: "phoenix-scheduler".
                 For the secondary env, the "2" suffix is added automatically
                 if not present. Leave empty to search across all Phoenix apps.
        logger_name: Filter by Java logger/class name pattern.
                    Example: "ge.halcom.phoenix.billing".
        days_back: How many days of logs to search (1-30). Default 7.
        size: Max log entries to return (1-200). Default 30.
        extra_filters: Optional JSON string with additional field:value filters.
                      Example: {"invoiceId": "123", "billingId": "456"}
    """
    env = environment.strip().lower()
    if env not in SUPPORTED_ENVS:
        return json.dumps({"error": f"Unsupported environment '{environment}'. This server supports: {', '.join(sorted(SUPPORTED_ENVS))}."})

    must_clauses: list[dict] = []
    filter_clauses: list[dict] = _build_env_filter(env)

    days_back = max(1, min(30, days_back))
    filter_clauses.append({"range": {"@timestamp": {"gte": f"now-{days_back}d", "lte": "now"}}})

    if search_text:
        must_clauses.append({
            "multi_match": {
                "query": search_text,
                "fields": ["message", "stack_trace"],
                "type": "phrase_prefix",
            }
        })

    if level:
        filter_clauses.append({"term": {"level.keyword": level.upper()}})

    if app_name:
        resolved = app_name.strip()
        if env == ES_SECONDARY_ENV and not resolved.endswith("2"):
            resolved += "2"
        filter_clauses.append({"term": {"app_name.keyword": resolved}})

    if logger_name:
        must_clauses.append({"match_phrase_prefix": {"logger_name": logger_name}})

    if extra_filters:
        try:
            extras = json.loads(extra_filters)
            for field, value in extras.items():
                filter_clauses.append({"term": {f"{field}.keyword": str(value)}})
        except json.JSONDecodeError as e:
            return json.dumps({"error": f"Invalid JSON in extra_filters: {e}"})

    body = {
        "query": {
            "bool": {
                "must": must_clauses if must_clauses else [{"match_all": {}}],
                "filter": filter_clauses,
            }
        },
        "sort": [{"@timestamp": "desc"}],
        "size": max(1, min(200, size)),
        "_source": [
            "@timestamp", "message", "level", "app_name", "logger_name",
            "stack_trace", "thread_name", "environment",
            "invoiceId", "billingId", "billingRunId", "contractPodDetailId",
            "processId", "schedulerId", "RequestURI", "User", "operation",
        ],
    }

    result = _request("POST", "logstash-*/_search", body=body)
    if isinstance(result, dict) and "error" in result:
        return json.dumps(result, indent=2)

    hits = result.get("hits", {})
    total = hits.get("total", {})
    docs = []
    for hit in hits.get("hits", []):
        doc = hit.get("_source", {})
        doc["_index"] = hit.get("_index", "")
        docs.append(doc)

    return json.dumps({
        "environment_filter": env,
        "total_matching": total.get("value", 0) if isinstance(total, dict) else total,
        "returned": len(docs),
        "logs": docs,
    }, indent=2)


@mcp.tool()
def es_error_summary(
    environment: str,
    search_text: str = "",
    days_back: int = 7,
    top_n: int = 15,
) -> str:
    """Get a summary of ERROR-level logs grouped by logger_name — useful for
    identifying the most frequent errors related to a bug's domain.

    Args:
        environment: REQUIRED. One of the supported environments for this server.
        search_text: Optional text to narrow errors (e.g. "invoice", "billing").
        days_back: How many days to cover (1-30). Default 7.
        top_n: How many top error groups to return. Default 15.
    """
    env = environment.strip().lower()
    if env not in SUPPORTED_ENVS:
        return json.dumps({"error": f"Unsupported environment '{environment}'. This server supports: {', '.join(sorted(SUPPORTED_ENVS))}."})

    must_clauses: list[dict] = []
    filter_clauses: list[dict] = _build_env_filter(env)
    filter_clauses.append({"term": {"level.keyword": "ERROR"}})
    filter_clauses.append({"range": {"@timestamp": {"gte": f"now-{max(1, min(30, days_back))}d", "lte": "now"}}})

    if search_text:
        must_clauses.append({
            "multi_match": {
                "query": search_text,
                "fields": ["message", "stack_trace", "logger_name"],
                "type": "phrase_prefix",
            }
        })

    body = {
        "size": 0,
        "query": {
            "bool": {
                "must": must_clauses if must_clauses else [{"match_all": {}}],
                "filter": filter_clauses,
            }
        },
        "aggs": {
            "by_logger": {
                "terms": {"field": "logger_name.keyword", "size": max(1, min(50, top_n))},
                "aggs": {
                    "sample_messages": {
                        "top_hits": {
                            "size": 2,
                            "_source": ["@timestamp", "message", "stack_trace", "app_name"],
                            "sort": [{"@timestamp": "desc"}],
                        }
                    }
                }
            }
        },
    }

    result = _request("POST", "logstash-*/_search", body=body)
    if isinstance(result, dict) and "error" in result:
        return json.dumps(result, indent=2)

    buckets = result.get("aggregations", {}).get("by_logger", {}).get("buckets", [])
    groups = []
    for b in buckets:
        samples = [h["_source"] for h in b.get("sample_messages", {}).get("hits", {}).get("hits", [])]
        groups.append({
            "logger_name": b["key"],
            "error_count": b["doc_count"],
            "recent_samples": samples,
        })

    return json.dumps({
        "environment_filter": env,
        "total_error_groups": len(groups),
        "groups": groups,
    }, indent=2)


@mcp.tool()
def es_list_indices(pattern: str = "*", sort_by: str = "index") -> str:
    """List available Elasticsearch indices.

    Args:
        pattern: Index name pattern with wildcards (e.g. "logstash-*"). Default "*".
        sort_by: Sort by "index" (name), "docs.count", or "store.size". Default "index".
    """
    result = _request("GET", f"_cat/indices/{pattern}", params={
        "format": "json",
        "h": "index,health,status,docs.count,store.size,creation.date.string",
        "s": sort_by,
    })
    if isinstance(result, dict) and "error" in result:
        return json.dumps(result, indent=2)

    if isinstance(result, list) and len(result) > 100:
        return json.dumps(result[:100], indent=2) + f"\n\n... truncated ({len(result)} total indices). Use a narrower pattern."
    return json.dumps(result, indent=2)


@mcp.tool()
def es_index_mapping(index: str) -> str:
    """Get field mappings for an index so you know which fields/columns are available.

    Args:
        index: Index name or pattern (e.g. "logstash-2024.07.27", "logstash-*").
              For patterns, returns the mapping of the first matching index.
    """
    result = _request("GET", f"{index}/_mapping")
    if isinstance(result, dict) and "error" in result:
        return json.dumps(result, indent=2)

    output = json.dumps(result, indent=2)
    if len(output) > 15000:
        flat_fields = []
        idx_name = ""
        for idx_name, idx_data in result.items():
            props = idx_data.get("mappings", {}).get("properties", {})
            flat_fields.extend(_flatten_properties(props))
            break
        return json.dumps({
            "index": idx_name,
            "fields": flat_fields[:200],
            "note": f"Showing flattened field list ({len(flat_fields)} fields). Full mapping was too large."
        }, indent=2)
    return output


def _flatten_properties(props: dict, prefix: str = "") -> list[str]:
    fields = []
    for name, meta in props.items():
        full = f"{prefix}{name}"
        fields.append(f"{full} ({meta.get('type', 'object')})")
        if "properties" in meta:
            fields.extend(_flatten_properties(meta["properties"], f"{full}."))
    return fields


@mcp.tool()
def es_sql_query(query: str, fetch_size: int = 50) -> str:
    """Run an Elasticsearch SQL query — same syntax as Kibana Dev Tools SQL.

    Examples:
        SELECT * FROM "logstash-*" WHERE level = 'ERROR' ORDER BY @timestamp DESC LIMIT 20
        SELECT @timestamp, message, logger_name FROM "logstash-*" WHERE message LIKE '%timeout%' LIMIT 10
        DESCRIBE "logstash-*"
        SHOW TABLES LIKE 'logstash%'

    Args:
        query: Elasticsearch SQL query string.
        fetch_size: Max rows to return (1-500). Default 50.
    """
    fetch_size = max(1, min(500, fetch_size))
    result = _request("POST", "_sql", body={
        "query": query,
        "fetch_size": fetch_size,
    }, params={"format": "json"})

    if isinstance(result, dict) and "error" in result:
        return json.dumps(result, indent=2)

    if isinstance(result, dict) and "columns" in result and "rows" in result:
        columns = [c["name"] for c in result["columns"]]
        rows = result["rows"]
        table = [dict(zip(columns, row)) for row in rows]
        return json.dumps({
            "total_rows": len(rows),
            "columns": columns,
            "data": table,
        }, indent=2)

    return json.dumps(result, indent=2)


@mcp.tool()
def es_search(index: str, query_body: str, size: int = 20) -> str:
    """Run a native Elasticsearch Query DSL search (for complex queries that SQL can't express).

    Args:
        index: Index name or pattern (e.g. "logstash-*").
        query_body: JSON string of the Elasticsearch query body.
                   Example: {"query": {"bool": {"must": [{"match": {"level": "ERROR"}}]}}, "sort": [{"@timestamp": "desc"}]}
        size: Max documents to return (1-200). Default 20.
    """
    size = max(1, min(200, size))
    try:
        body = json.loads(query_body) if isinstance(query_body, str) else query_body
    except json.JSONDecodeError as e:
        return json.dumps({"error": f"Invalid JSON in query_body: {e}"})

    body["size"] = size

    result = _request("POST", f"{index}/_search", body=body)
    if isinstance(result, dict) and "error" in result:
        return json.dumps(result, indent=2)

    hits = result.get("hits", {})
    total = hits.get("total", {})
    docs = []
    for hit in hits.get("hits", []):
        doc = hit.get("_source", {})
        doc["_index"] = hit.get("_index", "")
        doc["_id"] = hit.get("_id", "")
        docs.append(doc)

    return json.dumps({
        "total": total.get("value", 0) if isinstance(total, dict) else total,
        "returned": len(docs),
        "documents": docs,
    }, indent=2)


@mcp.tool()
def es_count(index: str, query_body: str = '{"query": {"match_all": {}}}') -> str:
    """Count documents matching a query in an index.

    Args:
        index: Index name or pattern (e.g. "logstash-*").
        query_body: JSON string with the query clause. Default counts all documents.
                   Example: {"query": {"match": {"level": "ERROR"}}}
    """
    try:
        body = json.loads(query_body) if isinstance(query_body, str) else query_body
    except json.JSONDecodeError as e:
        return json.dumps({"error": f"Invalid JSON in query_body: {e}"})

    result = _request("POST", f"{index}/_count", body=body)
    return json.dumps(result, indent=2)


@mcp.tool()
def es_field_values(index: str, field: str, size: int = 20, query: str = "") -> str:
    """Get unique values (top terms) for a specific field — useful for discovering enum-like values.

    Args:
        index: Index name or pattern.
        field: Field name (e.g. "level.keyword", "app_name.keyword", "logger_name.keyword").
        size: How many top values to return. Default 20.
        query: Optional filter query string (e.g. "level:ERROR"). Leave empty for no filter.
    """
    body: dict = {
        "size": 0,
        "aggs": {
            "top_values": {
                "terms": {
                    "field": field,
                    "size": min(100, max(1, size)),
                }
            }
        }
    }
    if query:
        body["query"] = {"query_string": {"query": query}}

    result = _request("POST", f"{index}/_search", body=body)
    if isinstance(result, dict) and "error" in result:
        return json.dumps(result, indent=2)

    buckets = result.get("aggregations", {}).get("top_values", {}).get("buckets", [])
    return json.dumps({
        "field": field,
        "values": [{"value": b["key"], "count": b["doc_count"]} for b in buckets],
    }, indent=2)


if __name__ == "__main__":
    mcp.run(transport="stdio")
