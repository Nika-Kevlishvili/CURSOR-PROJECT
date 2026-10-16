#!/usr/bin/env python3
"""Read-only Phoenix Elasticsearch access through the official Elastic CLI.

Agents call this script. They do not call Elasticsearch MCP servers and they do
not send write requests. API keys stay in the environment, secrets.env, or the
macOS keychain. This script never prints them.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "config" / "elasticsearch" / "contexts.yml"
SECRETS_FILE = ROOT / "config" / "elasticsearch" / "secrets.env"
LOCAL_ELASTIC = ROOT / "config" / "elasticsearch" / "node_modules" / ".bin" / "elastic"

HTTPS_CONTEXTS = {"phoenix-dev", "phoenix-prod"}
KEYCHAIN_ACCOUNTS = {
    "ELASTIC_DEV_API_KEY": "phoenix-dev:elasticsearch.auth.api_key",
    "ELASTIC_PROD_API_KEY": "phoenix-prod:elasticsearch.auth.api_key",
}
CONTEXT_KEYS = {
    "phoenix-dev": "ELASTIC_DEV_API_KEY",
    "phoenix-prod": "ELASTIC_PROD_API_KEY",
}

BASE_APP_NAMES = [
    "phoenix",
    "phoenix-scheduler",
    "phoenix-billing-run",
    "phoenix-mass-import",
    "phoenix-sales-portal",
    "phoenix-payment-api",
]

ENVIRONMENTS = {
    "dev": {"context": "phoenix-dev", "mode": "paired", "role": "primary"},
    "dev2": {"context": "phoenix-dev", "mode": "paired", "role": "secondary"},
    "test": {"context": "phoenix-test", "mode": "paired", "role": "primary"},
    "preprod": {"context": "phoenix-test", "mode": "paired", "role": "secondary"},
    "test2es": {
        "context": "phoenix-test",
        "mode": "keyword",
        "app_name": "phoenix2",
        "environment_keyword": "test",
    },
    "test2slr": {
        "context": "phoenix-test",
        "mode": "keyword",
        "app_name": "phoenix2",
        "environment_keyword": "testi2",
    },
    "prod": {"context": "phoenix-prod", "mode": "single"},
}

LOG_SOURCE = [
    "@timestamp",
    "message",
    "level",
    "app_name",
    "logger_name",
    "stack_trace",
    "thread_name",
    "environment",
    "invoiceId",
    "billingId",
    "billingRunId",
    "contractPodDetailId",
    "processId",
    "schedulerId",
    "RequestURI",
    "User",
    "operation",
]

SQL_FORBIDDEN = (
    "INSERT",
    "UPDATE",
    "DELETE",
    "DROP",
    "ALTER",
    "CREATE",
    "TRUNCATE",
    "GRANT",
    "REVOKE",
    "INTO",
)


def emit(payload: dict, code: int = 0) -> None:
    sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
    raise SystemExit(code)


def fail(message: str, code: int = 1, **extra: object) -> None:
    emit({"ok": False, "error": message, **extra}, code)


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def trim(value: object, limit: int) -> object:
    if not isinstance(value, str) or len(value) <= limit:
        return value
    return value[:limit] + f"... [truncated {len(value) - limit} chars]"


def trim_doc(doc: dict) -> dict:
    out = dict(doc)
    out["message"] = trim(out.get("message"), 600)
    out["stack_trace"] = trim(out.get("stack_trace"), 1200)
    return out


def load_secrets_file() -> None:
    if not SECRETS_FILE.is_file():
        return
    for raw in SECRETS_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        name = name.strip()
        value = value.strip().strip('"').strip("'")
        if name and value and not os.environ.get(name):
            os.environ[name] = value


def keychain_get(account: str) -> str:
    if sys.platform != "darwin":
        return ""
    proc = subprocess.run(
        ["security", "find-generic-password", "-s", "elastic-cli", "-a", account, "-w"],
        capture_output=True,
        text=True,
        timeout=10,
    )
    if proc.returncode != 0:
        return ""
    return (proc.stdout or "").strip()


def ensure_api_key(context: str) -> None:
    env_name = CONTEXT_KEYS.get(context)
    if not env_name:
        return
    load_secrets_file()
    if os.environ.get(env_name):
        return
    found = keychain_get(KEYCHAIN_ACCOUNTS[env_name])
    if found:
        os.environ[env_name] = found
        return
    fail(
        f"{env_name} is not set. Add it to Cursor-Project/config/elasticsearch/secrets.env "
        "or export it, then re-run. Do not put the key in mcp.json or in chat.",
        code=1,
        context=context,
    )


def find_elastic() -> str:
    override = os.environ.get("ELASTIC_BIN", "").strip()
    if override and Path(override).is_file():
        return override
    if os.name == "nt":
        cmd = LOCAL_ELASTIC.parent / "elastic.cmd"
        if cmd.is_file():
            return str(cmd)
    if LOCAL_ELASTIC.is_file():
        return str(LOCAL_ELASTIC)
    found = shutil.which("elastic")
    if found:
        return found
    fail(
        "Elastic CLI is not installed. From Cursor-Project/config/elasticsearch run: npm install",
        code=1,
    )
    return ""


def redact(text: str) -> str:
    redacted = text
    for name in CONTEXT_KEYS.values():
        secret = os.environ.get(name, "")
        if secret:
            redacted = redacted.replace(secret, "[redacted]")
    return redacted


def parse_json_blob(blob: str) -> dict | None:
    text = blob.strip()
    if not text:
        return None
    try:
        value = json.loads(text)
    except json.JSONDecodeError:
        start = text.find("{")
        if start < 0:
            return None
        try:
            value = json.loads(text[start:])
        except json.JSONDecodeError:
            return None
    return value if isinstance(value, dict) else {"value": value}


def run_elastic(context: str, args: list[str], body: dict | None = None) -> dict:
    if not CONFIG.is_file():
        fail(f"Missing Elastic CLI config: {CONFIG}")
    ensure_api_key(context)
    elastic = find_elastic()
    cmd = [
        elastic,
        "--config-file",
        str(CONFIG),
        "--use-context",
        context,
        "--json",
        *args,
    ]
    tmp_name = None
    if body is not None:
        handle = tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8")
        json.dump(body, handle)
        handle.close()
        tmp_name = handle.name
        cmd.extend(["--input-file", tmp_name])
    env = os.environ.copy()
    env["ELASTIC_CLI_TELEMETRY"] = "false"
    env["ELASTIC_NO_BANNER"] = "1"
    env["NODE_NO_WARNINGS"] = "1"
    if context in HTTPS_CONTEXTS:
        env["NODE_TLS_REJECT_UNAUTHORIZED"] = "0"
    try:
        proc = subprocess.run(cmd, capture_output=True, text=True, env=env, timeout=45)
    except subprocess.TimeoutExpired:
        fail("Elastic CLI timed out after 45s", code=1, context=context)
    finally:
        if tmp_name:
            Path(tmp_name).unlink(missing_ok=True)
    parsed = parse_json_blob(proc.stdout) or parse_json_blob(proc.stderr)
    if proc.returncode != 0:
        detail = parsed if parsed is not None else redact((proc.stderr or proc.stdout or "")[-800:])
        fail("Elastic CLI request failed", code=1, context=context, detail=detail)
    if parsed is None:
        fail("Elastic CLI returned non-JSON output", code=1, context=context)
    return parsed


def env_spec(name: str) -> dict:
    key = name.strip().lower()
    if key == "experiments":
        emit(
            {
                "ok": False,
                "elasticsearch_investigation": "not_available",
                "error": "Experiments has no Elasticsearch cluster configured.",
            },
            code=2,
        )
    spec = ENVIRONMENTS.get(key)
    if spec is None:
        fail(
            "Unsupported environment. Use dev, dev2, test, preprod, test2es, test2slr, prod, or experiments.",
            environment=name,
        )
    return {"name": key, **spec}


def env_filter(spec: dict) -> list[dict]:
    if spec["mode"] == "keyword":
        return [
            {"term": {"app_name.keyword": spec["app_name"]}},
            {"term": {"environment.keyword": spec["environment_keyword"]}},
        ]
    if spec["mode"] != "paired":
        return []
    if spec["role"] == "secondary":
        names = [f"{base}2" for base in BASE_APP_NAMES]
    else:
        names = list(BASE_APP_NAMES)
    return [{"terms": {"app_name.keyword": names}}]


def time_filter(days_back: int) -> dict:
    return {"range": {"@timestamp": {"gte": f"now-{days_back}d", "lte": "now"}}}


def bool_query(must: list[dict], filters: list[dict]) -> dict:
    return {
        "bool": {
            "must": must or [{"match_all": {}}],
            "filter": filters,
        }
    }


def search(spec: dict, body: dict, index: str = "logstash-*", extra_args: list[str] | None = None) -> dict:
    args = [
        "es",
        "search",
        "--index",
        index,
        "--ignore-unavailable",
        "true",
        "--allow-no-indices",
        "true",
    ]
    if extra_args:
        args.extend(extra_args)
    return run_elastic(spec["context"], args, body)


def hits_payload(result: dict) -> tuple[object, list[dict]]:
    hits = result.get("hits", {})
    total = hits.get("total", {})
    total_value = total.get("value", total) if isinstance(total, dict) else total
    docs = []
    for hit in hits.get("hits", []):
        doc = trim_doc(hit.get("_source", {}))
        doc["_index"] = hit.get("_index", "")
        docs.append(doc)
    return total_value, docs


def keyword_meta(spec: dict) -> dict:
    if spec["mode"] != "keyword":
        return {}
    return {
        "keyword_filters": {
            "app_name.keyword": spec["app_name"],
            "environment.keyword": spec["environment_keyword"],
        }
    }


def cmd_search_logs(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    days = clamp(args.days_back, 1, 30)
    size = clamp(args.size, 1, 50)
    filters = env_filter(spec)
    filters.append(time_filter(days))
    must: list[dict] = []
    if args.search_text:
        must.append(
            {
                "multi_match": {
                    "query": args.search_text,
                    "fields": ["message", "stack_trace"],
                    "type": "phrase_prefix",
                }
            }
        )
    if args.level:
        filters.append({"term": {"level.keyword": args.level.upper()}})
    if args.app_name and spec["mode"] != "keyword":
        resolved = args.app_name.strip()
        if spec.get("role") == "secondary" and not resolved.endswith("2"):
            resolved += "2"
        filters.append({"term": {"app_name.keyword": resolved}})
    if args.logger_name:
        must.append({"match_phrase_prefix": {"logger_name": args.logger_name}})
    if args.extra_filters:
        try:
            extras = json.loads(args.extra_filters)
        except json.JSONDecodeError as exc:
            fail(f"Invalid JSON in --extra-filters: {exc}")
        if not isinstance(extras, dict):
            fail("--extra-filters must be a JSON object")
        for field, value in extras.items():
            filters.append({"term": {f"{field}.keyword": str(value)}})
    body = {
        "query": bool_query(must, filters),
        "size": size,
        "_source": LOG_SOURCE,
        "track_total_hits": True,
    }
    result = search(spec, body, extra_args=["--sort", "@timestamp:desc"])
    total, docs = hits_payload(result)
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "environment_filter": spec["name"],
            "days_back": days,
            "total_matching": total,
            "returned": len(docs),
            "logs": docs,
            **keyword_meta(spec),
        }
    )


def cmd_error_summary(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    days = clamp(args.days_back, 1, 30)
    top_n = clamp(args.top_n, 1, 20)
    filters = env_filter(spec)
    filters.append({"term": {"level.keyword": "ERROR"}})
    filters.append(time_filter(days))
    must: list[dict] = []
    if args.search_text:
        must.append(
            {
                "multi_match": {
                    "query": args.search_text,
                    "fields": ["message", "stack_trace", "logger_name"],
                    "type": "phrase_prefix",
                }
            }
        )
    body = {
        "size": 0,
        "query": bool_query(must, filters),
        "aggregations": {
            "by_logger": {
                "terms": {"field": "logger_name.keyword", "size": top_n},
                "aggregations": {
                    "sample_messages": {
                        "top_hits": {
                            "size": 2,
                            "_source": ["@timestamp", "message", "stack_trace", "app_name"],
                            "sort": [{"@timestamp": "desc"}],
                        }
                    }
                },
            }
        },
    }
    result = search(spec, body)
    groups = []
    for bucket in result.get("aggregations", {}).get("by_logger", {}).get("buckets", []):
        samples = []
        for hit in bucket.get("sample_messages", {}).get("hits", {}).get("hits", []):
            samples.append(trim_doc(hit.get("_source", {})))
        groups.append(
            {
                "logger_name": bucket.get("key"),
                "error_count": bucket.get("doc_count"),
                "recent_samples": samples,
            }
        )
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "environment_filter": spec["name"],
            "days_back": days,
            "total_error_groups": len(groups),
            "groups": groups,
            **keyword_meta(spec),
        }
    )


def cmd_search(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    try:
        body = json.loads(args.query_body)
    except json.JSONDecodeError as exc:
        fail(f"Invalid JSON in --query-body: {exc}")
    if not isinstance(body, dict):
        fail("--query-body must be a JSON object")
    body["size"] = clamp(args.size, 1, 50)
    result = search(spec, body, index=args.index)
    total, docs = hits_payload(result)
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "environment_filter": spec["name"],
            "total": total,
            "returned": len(docs),
            "documents": docs,
        }
    )


def cmd_count(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    try:
        body = json.loads(args.query_body)
    except json.JSONDecodeError as exc:
        fail(f"Invalid JSON in --query-body: {exc}")
    if not isinstance(body, dict):
        fail("--query-body must be a JSON object")
    result = run_elastic(
        spec["context"],
        ["es", "count", "--index", args.index],
        body,
    )
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "environment_filter": spec["name"],
            "count": result.get("count"),
        }
    )


def cmd_sql(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    query = args.query.strip()
    if re.search(r"\b(" + "|".join(SQL_FORBIDDEN) + r")\b", query.upper()):
        fail("SQL writes are blocked. Use SELECT, DESCRIBE, or SHOW.", code=1)
    result = run_elastic(
        spec["context"],
        [
            "es",
            "sql",
            "query",
            "--query",
            query,
            "--fetch-size",
            str(clamp(args.fetch_size, 1, 100)),
            "--format",
            "json",
        ],
    )
    if "columns" in result and "rows" in result:
        columns = [col.get("name") for col in result["columns"]]
        rows = result["rows"][: clamp(args.fetch_size, 1, 100)]
        emit(
            {
                "ok": True,
                "elasticsearch_source": "official Elastic CLI",
                "context": spec["context"],
                "total_rows": len(rows),
                "columns": columns,
                "data": [dict(zip(columns, row)) for row in rows],
            }
        )
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "result": result,
        }
    )


def cmd_indices(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    result = run_elastic(
        spec["context"],
        [
            "es",
            "cat",
            "indices",
            "--index",
            args.pattern,
            "--format",
            "json",
            "--h",
            "index,health,status,docs.count,store.size",
            "--s",
            "index",
        ],
    )
    rows = result if isinstance(result, list) else result.get("value", result)
    if isinstance(rows, dict):
        emit({"ok": True, "elasticsearch_source": "official Elastic CLI", "context": spec["context"], "result": rows})
    limit = clamp(args.limit, 1, 40)
    shown = rows[:limit] if isinstance(rows, list) else []
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "returned": len(shown),
            "truncated": isinstance(rows, list) and len(rows) > limit,
            "indices": shown,
        }
    )


def cmd_mapping(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    index = args.index
    if "*" in index:
        listing = run_elastic(
            spec["context"],
            ["es", "cat", "indices", "--index", index, "--format", "json", "--h", "index", "--s", "index"],
        )
        rows = listing if isinstance(listing, list) else []
        names = [row.get("index") for row in rows if isinstance(row, dict) and row.get("index")]
        if not names:
            fail(f"No indices matched {index}", context=spec["context"])
        index = sorted(names)[-1]
    result = run_elastic(spec["context"], ["es", "indices", "get-mapping", "--index", index])
    fields: list[str] = []
    index_name = index
    if isinstance(result, dict):
        for name, payload in result.items():
            index_name = name
            props = payload.get("mappings", {}).get("properties", {}) if isinstance(payload, dict) else {}
            fields = flatten_properties(props)[:100]
            break
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "index": index_name,
            "fields": fields,
            "note": "Flattened field list, capped at 100. Prod list/mapping calls can return 403; use search-logs.",
        }
    )


def flatten_properties(props: dict, prefix: str = "") -> list[str]:
    fields: list[str] = []
    for name, meta in props.items():
        full = f"{prefix}{name}"
        kind = meta.get("type", "object") if isinstance(meta, dict) else "object"
        fields.append(f"{full} ({kind})")
        if isinstance(meta, dict) and "properties" in meta:
            fields.extend(flatten_properties(meta["properties"], f"{full}."))
    return fields


def cmd_field_values(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    body: dict = {
        "size": 0,
        "aggregations": {
            "top_values": {"terms": {"field": args.field, "size": clamp(args.size, 1, 30)}}
        },
    }
    if args.query:
        body["query"] = {"query_string": {"query": args.query}}
    result = search(spec, body, index=args.index)
    buckets = result.get("aggregations", {}).get("top_values", {}).get("buckets", [])
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "field": args.field,
            "values": [{"value": bucket.get("key"), "count": bucket.get("doc_count")} for bucket in buckets],
        }
    )


def cmd_health(args: argparse.Namespace) -> None:
    spec = env_spec(args.environment)
    filters = env_filter(spec)
    filters.append(time_filter(clamp(args.days_back, 1, 30)))
    body = {"size": 0, "query": bool_query([], filters), "track_total_hits": True}
    result = search(spec, body)
    total = result.get("hits", {}).get("total", {})
    emit(
        {
            "ok": True,
            "elasticsearch_source": "official Elastic CLI",
            "context": spec["context"],
            "environment_filter": spec["name"],
            "reachable": True,
            "total_matching": total.get("value", total) if isinstance(total, dict) else total,
            "note": "Connectivity check is a size-0 search. Dev and Prod API keys return 403 for cluster health and index listing.",
            **keyword_meta(spec),
        }
    )


def cmd_store_keys(_args: argparse.Namespace) -> None:
    load_secrets_file()
    if sys.platform != "darwin":
        emit(
            {
                "ok": True,
                "stored": [],
                "note": "This OS has no macOS keychain step. Export ELASTIC_DEV_API_KEY and ELASTIC_PROD_API_KEY, or fill secrets.env.",
            }
        )
    stored = []
    for env_name, account in KEYCHAIN_ACCOUNTS.items():
        secret = os.environ.get(env_name, "")
        if not secret:
            continue
        proc = subprocess.run(
            ["security", "add-generic-password", "-U", "-s", "elastic-cli", "-a", account, "-w"],
            input=f"{secret}\n{secret}\n",
            text=True,
            capture_output=True,
            timeout=10,
        )
        if proc.returncode != 0:
            fail(f"Keychain write failed for {env_name}", code=1)
        stored.append(env_name)
    emit({"ok": True, "stored": stored, "note": "Values were not printed."})


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Read-only Phoenix Elasticsearch via the official Elastic CLI.")
    sub = parser.add_subparsers(dest="command", required=True)

    logs = sub.add_parser("search-logs")
    logs.add_argument("--environment", required=True)
    logs.add_argument("--search-text", default="")
    logs.add_argument("--level", default="")
    logs.add_argument("--app-name", default="")
    logs.add_argument("--logger-name", default="")
    logs.add_argument("--days-back", type=int, default=7)
    logs.add_argument("--size", type=int, default=20)
    logs.add_argument("--extra-filters", default="")
    logs.set_defaults(func=cmd_search_logs)

    summary = sub.add_parser("error-summary")
    summary.add_argument("--environment", required=True)
    summary.add_argument("--search-text", default="")
    summary.add_argument("--days-back", type=int, default=7)
    summary.add_argument("--top-n", type=int, default=10)
    summary.set_defaults(func=cmd_error_summary)

    raw = sub.add_parser("search")
    raw.add_argument("--environment", required=True)
    raw.add_argument("--index", default="logstash-*")
    raw.add_argument("--query-body", required=True)
    raw.add_argument("--size", type=int, default=20)
    raw.set_defaults(func=cmd_search)

    count = sub.add_parser("count")
    count.add_argument("--environment", required=True)
    count.add_argument("--index", default="logstash-*")
    count.add_argument("--query-body", default='{"query":{"match_all":{}}}')
    count.set_defaults(func=cmd_count)

    sql = sub.add_parser("sql")
    sql.add_argument("--environment", required=True)
    sql.add_argument("--query", required=True)
    sql.add_argument("--fetch-size", type=int, default=30)
    sql.set_defaults(func=cmd_sql)

    indices = sub.add_parser("indices")
    indices.add_argument("--environment", required=True)
    indices.add_argument("--pattern", default="logstash-*")
    indices.add_argument("--limit", type=int, default=20)
    indices.set_defaults(func=cmd_indices)

    mapping = sub.add_parser("mapping")
    mapping.add_argument("--environment", required=True)
    mapping.add_argument("--index", default="logstash-*")
    mapping.set_defaults(func=cmd_mapping)

    fields = sub.add_parser("field-values")
    fields.add_argument("--environment", required=True)
    fields.add_argument("--index", default="logstash-*")
    fields.add_argument("--field", required=True)
    fields.add_argument("--size", type=int, default=15)
    fields.add_argument("--query", default="")
    fields.set_defaults(func=cmd_field_values)

    health = sub.add_parser("health")
    health.add_argument("--environment", required=True)
    health.add_argument("--days-back", type=int, default=1)
    health.set_defaults(func=cmd_health)

    store = sub.add_parser("store-keys")
    store.set_defaults(func=cmd_store_keys)
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
