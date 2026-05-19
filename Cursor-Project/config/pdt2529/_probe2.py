import json
from pathlib import Path
import psycopg2

def load_conn():
    j = json.loads(Path(r"C:\Users\nikak\.cursor\mcp.json").read_text(encoding="utf-8"))
    env = j["mcpServers"]["PostgreSQLDev"]["env"]
    return psycopg2.connect(env["POSTGRES_CONNECTION_STRING"])

SQL = """
WITH contract_pods_active AS (
  SELECT cd.contract_id,
         cp.id AS contract_pod_id,
         p.id AS pod_id,
         p.identifier
  FROM product_contract.contract_details cd
  JOIN product_contract.contract_pods cp ON cp.contract_detail_id = cd.id
  JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
  JOIN pod.pod p ON p.id = pd.pod_id
  WHERE cp.status = 'ACTIVE'
    AND p.identifier LIKE '32X%'
)
SELECT pod_count, COUNT(*) 
FROM (
  SELECT contract_id, COUNT(*) AS pod_count
  FROM contract_pods_active cpa
  JOIN product_contract.contracts c ON c.id = cpa.contract_id
  WHERE c.create_date::date = DATE '2026-05-18'
  GROUP BY contract_id
) x
GROUP BY pod_count
ORDER BY pod_count;
"""

with load_conn() as conn:
    with conn.cursor() as cur:
        cur.execute(SQL)
        for row in cur.fetchall():
            print(row)
