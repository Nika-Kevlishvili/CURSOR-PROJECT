import json
from pathlib import Path
import psycopg2

def load_conn():
    j = json.loads(Path(r"C:\Users\nikak\.cursor\mcp.json").read_text(encoding="utf-8"))
    return psycopg2.connect(j["mcpServers"]["PostgreSQLDev"]["env"]["POSTGRES_CONNECTION_STRING"])

SQL = """
WITH bulk_contracts AS (
  SELECT c.id AS contract_id
  FROM product_contract.contracts c
  WHERE c.create_date::date = DATE '2026-05-18'
  GROUP BY c.id
  HAVING (
    SELECT COUNT(*)
    FROM product_contract.contract_details cd
    JOIN product_contract.contract_pods cp ON cp.contract_detail_id = cd.id AND cp.status = 'ACTIVE'
    JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
    JOIN pod.pod p ON p.id = pd.pod_id
    WHERE cd.contract_id = c.id AND p.identifier LIKE '32X%'
  ) = 3
),
active_pods AS (
  SELECT bc.contract_id,
         cp.id AS contract_pod_id,
         cp.contract_billing_group_id AS billing_group_id,
         p.id AS pod_id,
         p.identifier,
         cp.activation_date,
         cp.deactivation_date
  FROM bulk_contracts bc
  JOIN product_contract.contract_details cd ON cd.contract_id = bc.contract_id
  JOIN product_contract.contract_pods cp ON cp.contract_detail_id = cd.id AND cp.status = 'ACTIVE'
  JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
  JOIN pod.pod p ON p.id = pd.pod_id
  WHERE p.identifier LIKE '32X%'
),
pod_profile AS (
  SELECT ap.*,
         EXISTS (
           SELECT 1 FROM pod.billing_by_profile bbp
           WHERE bbp.pod_id = ap.pod_id
             AND bbp.status = 'ACTIVE'
             AND bbp.period_from::date <= COALESCE(ap.deactivation_date, DATE '9999-12-31')
             AND COALESCE(bbp.period_to::date, DATE '9999-12-31') >= COALESCE(ap.activation_date, DATE '1900-01-01')
         ) AS has_profile
  FROM active_pods ap
),
contract_flags AS (
  SELECT bc.contract_id,
         BOOL_OR(COALESCE(cbg.separate_invoice_for_each_pod, false) IS NOT TRUE) AS needs_sep_any,
         BOOL_OR(NOT pp.has_profile) AS needs_prof_any
  FROM bulk_contracts bc
  LEFT JOIN product_contract.contract_billing_groups cbg
    ON cbg.contract_id = bc.contract_id AND cbg.status = 'ACTIVE'
  LEFT JOIN pod_profile pp ON pp.contract_id = bc.contract_id
  GROUP BY bc.contract_id
)
SELECT
  (SELECT COUNT(*) FROM bulk_contracts) AS bulk_total,
  (SELECT COUNT(*) FROM contract_flags WHERE needs_sep_any OR needs_prof_any) AS needing,
  (SELECT COUNT(*) FROM contract_flags WHERE needs_sep_any) AS need_sep,
  (SELECT COUNT(*) FROM contract_flags WHERE needs_prof_any) AS need_prof;
"""

with load_conn() as conn:
    with conn.cursor() as cur:
        cur.execute(SQL)
        print(cur.fetchone())
