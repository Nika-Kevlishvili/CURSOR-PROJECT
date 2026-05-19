import json
from pathlib import Path
import psycopg2

def load_conn():
    j = json.loads(Path(r"C:\Users\nikak\.cursor\mcp.json").read_text(encoding="utf-8"))
    return psycopg2.connect(j["mcpServers"]["PostgreSQLDev"]["env"]["POSTGRES_CONNECTION_STRING"])

SQL = """
SELECT column_name FROM information_schema.columns
WHERE table_schema='product_contract' AND table_name='contract_details'
ORDER BY ordinal_position;
"""

with load_conn() as conn:
    with conn.cursor() as cur:
        cur.execute(SQL)
        print([r[0] for r in cur.fetchall()])
