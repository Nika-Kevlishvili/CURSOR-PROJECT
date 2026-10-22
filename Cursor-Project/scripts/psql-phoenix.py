#!/usr/bin/env python3
"""Phoenix PostgreSQL access through the official psql client.

Agents call this script. They do not call PostgreSQL MCP servers and they do
not put passwords on the command line. Passwords stay in the environment or in
Cursor-Project/config/postgres/secrets.env. This script never prints them.

psql flags follow https://www.postgresql.org/docs/current/app-psql.html :
-X (ignore ~/.psqlrc), -w (never prompt), -v ON_ERROR_STOP=1, --csv, -c.
Reads also set default_transaction_read_only via libpq PGOPTIONS.
"""

from __future__ import annotations

import argparse
import csv
import io
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / "config" / "postgres" / "contexts.json"
SECRETS_FILE = ROOT / "config" / "postgres" / "secrets.env"

PSQL_CANDIDATES = (
    "/opt/homebrew/opt/libpq/bin/psql",
    "/usr/local/opt/libpq/bin/psql",
    "/Applications/Postgres.app/Contents/Versions/latest/bin/psql",
)

READ_START = re.compile(r"^(SELECT|WITH|EXPLAIN|SHOW|VALUES|TABLE)\b", re.IGNORECASE)
WRITE_KEYWORD = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|GRANT|REVOKE|COPY|CALL|DO|"
    r"VACUUM|REINDEX|CLUSTER|COMMENT|MERGE|REFRESH|LOCK|REASSIGN|IMPORT|LISTEN|"
    r"NOTIFY|UNLISTEN|LOAD|SECURITY)\b",
    re.IGNORECASE,
)
DANGEROUS_FUNC = re.compile(
    r"\b(pg_terminate_backend|pg_cancel_backend|pg_reload_conf|pg_rotate_logfile|"
    r"lo_import|lo_export|lo_unlink|dblink_exec|dblink_connect)\b",
    re.IGNORECASE,
)
IDENT = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
COPY_PROGRAM = re.compile(r"\bCOPY\b[\s\S]{0,200}\bPROGRAM\b", re.IGNORECASE)


def emit(payload: dict, code: int = 0) -> None:
    sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
    raise SystemExit(code)


def fail(message: str, code: int = 1, **extra: object) -> None:
    emit({"ok": False, "source": "psql", "error": message, **extra}, code)


def clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def trim(value: str, limit: int = 400) -> str:
    if len(value) <= limit:
        return value
    return value[:limit] + f"... [truncated {len(value) - limit} chars]"


def load_secrets_file() -> None:
    if not SECRETS_FILE.is_file():
        return
    for raw in SECRETS_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        name, value = line.split("=", 1)
        name = name.strip()
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {'"', "'"}:
            value = value[1:-1]
        if name and value and not os.environ.get(name):
            os.environ[name] = value


def load_targets() -> dict:
    if not CONFIG.is_file():
        fail(f"Missing PostgreSQL config: {CONFIG}")
    try:
        data = json.loads(CONFIG.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        fail(f"Invalid contexts.json: {exc}")
    environments = data.get("environments")
    if not isinstance(environments, dict) or not environments:
        fail("contexts.json has no environments")
    return environments


def target_for(name: str, targets: dict) -> dict:
    key = name.strip().lower()
    target = targets.get(key)
    if not target:
        known = ", ".join(sorted(targets))
        fail(f"Unknown environment '{name}'. Known: {known}", environment=name)
    password_env = str(target.get("password_env") or "")
    password = os.environ.get(password_env, "")
    if not password:
        fail(
            f"{password_env} is not set. Add it to Cursor-Project/config/postgres/secrets.env "
            "or export it, then re-run. Do not put the password in mcp.json or in chat.",
            environment=key,
        )
    return {
        "name": key,
        "host": str(target["host"]),
        "port": str(target["port"]),
        "database": str(target["database"]),
        "user": str(target["user"]),
        "password": password,
        "read_only": bool(target.get("read_only")),
    }


def locate_psql() -> str:
    override = os.environ.get("PSQL_BIN", "").strip()
    if override and Path(override).is_file():
        return override
    found = shutil.which("psql")
    if found:
        return found
    for candidate in PSQL_CANDIDATES:
        if Path(candidate).is_file():
            return candidate
    return ""


def find_psql() -> str:
    found = locate_psql()
    if found:
        return found
    fail(
        "psql is not installed. On macOS: brew install libpq "
        "(binary at /opt/homebrew/opt/libpq/bin/psql). "
        "On Windows: install the PostgreSQL client tools and put psql on PATH.",
    )
    return ""


def mask_literals(sql: str) -> str:
    """Blank string and dollar-quote contents so keyword checks ignore literals."""
    out: list[str] = []
    i = 0
    n = len(sql)
    while i < n:
        ch = sql[i]
        if ch == "'":
            j = i + 1
            while j < n:
                if sql[j] == "'":
                    if j + 1 < n and sql[j + 1] == "'":
                        j += 2
                        continue
                    j += 1
                    break
                j += 1
            out.append("''")
            i = j
            continue
        if ch == "$":
            match = re.match(r"\$[A-Za-z0-9_]*\$", sql[i:])
            if match:
                tag = match.group(0)
                j = sql.find(tag, i + len(tag))
                if j < 0:
                    out.append(sql[i:])
                    break
                out.append(tag + tag)
                i = j + len(tag)
                continue
        out.append(ch)
        i += 1
    return "".join(out)


def strip_sql_comments(sql: str) -> str:
    out: list[str] = []
    i = 0
    n = len(sql)
    while i < n:
        ch = sql[i]
        if ch == "'":
            j = i + 1
            while j < n:
                if sql[j] == "'":
                    if j + 1 < n and sql[j + 1] == "'":
                        j += 2
                        continue
                    j += 1
                    break
                j += 1
            out.append(sql[i:j])
            i = j
            continue
        if ch == '"':
            j = i + 1
            while j < n and sql[j] != '"':
                if sql[j] == "\\":
                    j += 2
                    continue
                j += 1
            j = min(n, j + 1)
            out.append(sql[i:j])
            i = j
            continue
        if sql.startswith("--", i):
            j = sql.find("\n", i)
            i = n if j < 0 else j
            out.append(" ")
            continue
        if sql.startswith("/*", i):
            j = sql.find("*/", i + 2)
            i = n if j < 0 else j + 2
            out.append(" ")
            continue
        if ch == "$":
            match = re.match(r"\$[A-Za-z0-9_]*\$", sql[i:])
            if match:
                tag = match.group(0)
                j = sql.find(tag, i + len(tag))
                if j < 0:
                    out.append(sql[i:])
                    break
                out.append(sql[i : j + len(tag)])
                i = j + len(tag)
                continue
        out.append(ch)
        i += 1
    return "".join(out)


def single_statement(sql: str) -> str:
    text = sql.strip()
    if not text:
        fail("SQL is empty")
    if text.startswith("\\"):
        fail("psql meta-commands are not accepted. Use query, list-schemas, list-tables, or describe.")
    stripped = strip_sql_comments(text).strip()
    if stripped.endswith(";"):
        stripped = stripped[:-1].strip()
    if ";" in stripped:
        fail("Only one SQL statement is accepted. psql -c sends that statement as a single server request.")
    if COPY_PROGRAM.search(mask_literals(stripped)):
        fail("COPY ... PROGRAM is rejected.")
    return stripped


def redact(text: str, secret: str) -> str:
    if secret and secret in text:
        text = text.replace(secret, "[redacted]")
    return trim(text, 2000)


def run_psql(target: dict, sql: str, read_only: bool) -> subprocess.CompletedProcess[str]:
    binary = find_psql()
    command = [
        binary,
        "-X",
        "-w",
        "-q",
        "-v",
        "ON_ERROR_STOP=1",
        "--csv",
        "-P",
        "pager=off",
        "-P",
        "null=[null]",
        "-c",
        sql,
    ]
    options = ["-c statement_timeout=60000"]
    if read_only:
        options.append("-c default_transaction_read_only=on")
    env = os.environ.copy()
    env.update(
        {
            "PGHOST": target["host"],
            "PGPORT": target["port"],
            "PGDATABASE": target["database"],
            "PGUSER": target["user"],
            "PGPASSWORD": target["password"],
            "PGCONNECT_TIMEOUT": "10",
            "PGCLIENTENCODING": "UTF8",
            "PGOPTIONS": " ".join(options),
        }
    )
    try:
        return subprocess.run(
            command,
            capture_output=True,
            text=True,
            timeout=90,
            env=env,
        )
    except subprocess.TimeoutExpired:
        fail("psql timed out after 90 seconds", environment=target["name"])
    except OSError as exc:
        fail(f"psql failed to start: {exc}", environment=target["name"])
    raise AssertionError("unreachable")


def parse_csv(stdout: str, limit: int) -> tuple[list[str], list[dict[str, str]], bool]:
    if not stdout.strip():
        return [], [], False
    reader = csv.DictReader(io.StringIO(stdout))
    columns = list(reader.fieldnames or [])
    rows: list[dict[str, str]] = []
    truncated = False
    for index, row in enumerate(reader):
        if index >= limit:
            truncated = True
            break
        rows.append({key: trim("" if value is None else str(value)) for key, value in row.items()})
    return columns, rows, truncated


def result_payload(target: dict, command: str, proc: subprocess.CompletedProcess[str], limit: int) -> dict:
    stderr = redact(proc.stderr or "", target["password"])
    if proc.returncode != 0:
        message = stderr or redact(proc.stdout or "", target["password"]) or f"psql exited {proc.returncode}"
        fail(message.strip(), environment=target["name"], command=command)
    columns, rows, truncated = parse_csv(proc.stdout or "", limit)
    payload = {
        "ok": True,
        "source": "psql",
        "environment": target["name"],
        "database": target["database"],
        "read_only": target["read_only"],
        "session_read_only": True,
        "command": command,
        "columns": columns,
        "row_count": len(rows),
        "truncated": truncated,
        "rows": rows,
    }
    if stderr.strip():
        payload["notices"] = stderr.strip()
    return payload


def guard_read(stripped: str) -> None:
    if not READ_START.match(stripped):
        fail("query accepts SELECT, WITH, EXPLAIN, SHOW, VALUES, or TABLE. Use execute --allow-write for changes.")
    scanned = mask_literals(stripped)
    if WRITE_KEYWORD.search(scanned) or re.search(r"\bINTO\b", scanned, re.IGNORECASE):
        fail("query is read-only. The statement contains a write keyword.")
    if DANGEROUS_FUNC.search(scanned):
        fail("query rejected an administrative function.")


def cmd_query(target: dict, sql: str, limit: int) -> None:
    stripped = single_statement(sql)
    guard_read(stripped)
    limited = stripped
    if not stripped.upper().startswith("EXPLAIN") and not stripped.upper().startswith("SHOW"):
        limited = f"SELECT * FROM (\n{stripped}\n) AS psql_phoenix_q\nLIMIT {limit}"
    proc = run_psql(target, limited, read_only=True)
    emit(result_payload(target, "query", proc, limit))


def cmd_execute(target: dict, sql: str, allow_write: bool) -> None:
    if target["read_only"]:
        fail(
            f"{target['name']} is read-only. execute is rejected.",
            environment=target["name"],
        )
    if not allow_write:
        fail("execute requires --allow-write. A shell hook asks for approval before the write runs.")
    stripped = single_statement(sql)
    scanned = mask_literals(stripped)
    if not WRITE_KEYWORD.search(scanned):
        fail("execute is for data changes. Use query for SELECT, WITH, EXPLAIN, SHOW, VALUES, or TABLE.")
    if DANGEROUS_FUNC.search(scanned):
        fail("execute rejected an administrative function.")
    proc = run_psql(target, stripped, read_only=False)
    stderr = redact(proc.stderr or "", target["password"])
    if proc.returncode != 0:
        message = stderr or redact(proc.stdout or "", target["password"]) or f"psql exited {proc.returncode}"
        fail(message.strip(), environment=target["name"], command="execute")
    status = redact(proc.stdout or "", target["password"]).strip() or "completed"
    payload = {
        "ok": True,
        "source": "psql",
        "environment": target["name"],
        "database": target["database"],
        "command": "execute",
        "status": status,
    }
    if stderr.strip():
        payload["notices"] = stderr.strip()
    emit(payload)


def split_table(table: str) -> tuple[str, str]:
    parts = table.split(".")
    if len(parts) == 1:
        schema, name = "public", parts[0]
    elif len(parts) == 2:
        schema, name = parts
    else:
        fail("Pass a table as schema.name or name.")
    if not IDENT.match(schema) or not IDENT.match(name):
        fail("Schema and table must be unquoted identifiers (letters, digits, underscore).")
    return schema, name


def cmd_list_schemas(target: dict, limit: int) -> None:
    sql = """
SELECT nspname AS schema_name
FROM pg_namespace
WHERE nspname <> 'information_schema'
  AND nspname NOT LIKE 'pg\\_%' ESCAPE '\\'
ORDER BY 1
"""
    proc = run_psql(target, sql, read_only=True)
    emit(result_payload(target, "list-schemas", proc, limit))


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def cmd_list_tables(target: dict, schema: str, limit: int) -> None:
    if schema and not IDENT.match(schema):
        fail("Schema must be an unquoted identifier.")
    schema_filter = f"\n  AND table_schema = {sql_literal(schema)}" if schema else ""
    sql = f"""
SELECT table_schema, table_name, table_type
FROM information_schema.tables
WHERE table_schema NOT IN ('pg_catalog', 'information_schema'){schema_filter}
ORDER BY 1, 2
"""
    proc = run_psql(target, sql, read_only=True)
    emit(result_payload(target, "list-tables", proc, limit))


def cmd_describe(target: dict, table: str) -> None:
    schema, name = split_table(table)
    sql = f"""
SELECT column_name, data_type, is_nullable, column_default, character_maximum_length
FROM information_schema.columns
WHERE table_schema = {sql_literal(schema)}
  AND table_name = {sql_literal(name)}
ORDER BY ordinal_position
"""
    proc = run_psql(target, sql, read_only=True)
    emit(result_payload(target, "describe", proc, 500))


def cmd_health(target: dict) -> None:
    sql = """
SELECT current_database() AS database,
       current_user AS user_name,
       inet_server_port() AS server_port
"""
    proc = run_psql(target, sql, read_only=True)
    emit(result_payload(target, "health", proc, 5))


def cmd_check(targets: dict) -> None:
    binary = locate_psql()
    version = ""
    if binary:
        proc = subprocess.run([binary, "--version"], capture_output=True, text=True, timeout=10)
        version = (proc.stdout or proc.stderr or "").strip()
    environments = {}
    for name, target in sorted(targets.items()):
        password_env = str(target.get("password_env") or "")
        environments[name] = {
            "host": target.get("host"),
            "port": target.get("port"),
            "database": target.get("database"),
            "user": target.get("user"),
            "read_only": bool(target.get("read_only")),
            "password_set": bool(os.environ.get(password_env)),
        }
    ready = bool(binary) and all(item["password_set"] for item in environments.values())
    emit(
        {
            "ok": ready,
            "source": "psql",
            "psql": binary or None,
            "psql_version": version or None,
            "secrets_file": SECRETS_FILE.is_file(),
            "environments": environments,
        },
        0 if ready else 1,
    )


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Phoenix PostgreSQL via the official psql client.")
    sub = parser.add_subparsers(dest="command", required=True)

    def add_env(command: argparse.ArgumentParser) -> None:
        command.add_argument("--environment", required=True)

    query = sub.add_parser("query", help="Read-only SQL (SELECT, WITH, EXPLAIN, SHOW, VALUES, TABLE).")
    add_env(query)
    query.add_argument("--sql", required=True)
    query.add_argument("--limit", type=int, default=200)

    execute = sub.add_parser("execute", help="One write statement. Refused on read-only environments.")
    add_env(execute)
    execute.add_argument("--sql", required=True)
    execute.add_argument("--allow-write", action="store_true")

    schemas = sub.add_parser("list-schemas")
    add_env(schemas)
    schemas.add_argument("--limit", type=int, default=200)

    tables = sub.add_parser("list-tables")
    add_env(tables)
    tables.add_argument("--schema", default="")
    tables.add_argument("--limit", type=int, default=500)

    describe = sub.add_parser("describe")
    add_env(describe)
    describe.add_argument("--table", required=True)

    health = sub.add_parser("health")
    add_env(health)

    sub.add_parser("check", help="Show psql and which passwords are set. Prints no secrets.")
    return parser


def main() -> None:
    load_secrets_file()
    args = build_parser().parse_args()
    targets = load_targets()
    if args.command == "check":
        cmd_check(targets)
        return
    target = target_for(args.environment, targets)
    if args.command == "query":
        cmd_query(target, args.sql, clamp(args.limit, 1, 500))
    elif args.command == "execute":
        cmd_execute(target, args.sql, args.allow_write)
    elif args.command == "list-schemas":
        cmd_list_schemas(target, clamp(args.limit, 1, 500))
    elif args.command == "list-tables":
        cmd_list_tables(target, args.schema, clamp(args.limit, 1, 1000))
    elif args.command == "describe":
        cmd_describe(target, args.table)
    elif args.command == "health":
        cmd_health(target)
    else:
        fail(f"Unknown command {args.command}")


if __name__ == "__main__":
    main()
