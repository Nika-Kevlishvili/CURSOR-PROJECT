import json
from pathlib import Path
import psycopg2

def load_conn():
    j = json.loads(Path(r"C:\Users\nikak\.cursor\mcp.json").read_text(encoding="utf-8"))
    env = j["mcpServers"]["PostgreSQLDev"]["env"]
    cs = env.get("POSTGRES_CONNECTION_STRING")
    if cs:
        return psycopg2.connect(cs)
    return psycopg2.connect(
        host=env["POSTGRES_HOST"],
        port=env.get("POSTGRES_PORT", 5432),
        dbname=env["POSTGRES_DB"],
        user=env["POSTGRES_USER"],
        password=env["POSTGRES_PASSWORD"],
    )

SQL_PROBE = """
SELECT COUNT(*) FROM product_contract.contracts c
WHERE c.create_date::date = DATE '2026-05-18';
"""

with load_conn() as conn:
    with conn.cursor() as cur:
        cur.execute(SQL_PROBE)
        print("contracts_on_date", cur.fetchone()[0])
