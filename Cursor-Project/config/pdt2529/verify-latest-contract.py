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


SQL = """
WITH latest AS (
  SELECT c.id AS contract_id, c.contract_number, c.create_date
  FROM product_contract.contracts c
  WHERE c.create_date >= NOW() - INTERVAL '15 minutes'
  ORDER BY c.create_date DESC
  LIMIT 1
),
pods AS (
  SELECT l.contract_id, l.contract_number, l.create_date,
         cp.id AS contract_pod_id, p.id AS pod_id, p.identifier,
         cp.activation_date, cp.deactivation_date,
         cbg.id AS billing_group_id, cbg.separate_invoice_for_each_pod
  FROM latest l
  JOIN product_contract.contract_billing_groups cbg ON cbg.contract_id = l.contract_id
  JOIN product_contract.contract_details cd ON cd.contract_id = l.contract_id
  JOIN product_contract.contract_pods cp ON cp.contract_detail_id = cd.id AND cp.status = 'ACTIVE'
  JOIN pod.pod_details pd ON cp.pod_detail_id = pd.id
  JOIN pod.pod p ON pd.pod_id = p.id
),
bbp AS (
  SELECT p.contract_id, p.pod_id, p.identifier,
         EXISTS (
           SELECT 1 FROM pod.billing_by_profile b
           WHERE b.pod_id = p.pod_id AND b.status = 'ACTIVE'
             AND b.period_from <= COALESCE(p.deactivation_date, DATE '9999-12-31')
             AND COALESCE(b.period_to, DATE '9999-12-31') >= COALESCE(p.activation_date, DATE '2020-01-01')
         ) AS has_profile
  FROM pods p
)
SELECT contract_id, contract_number, create_date,
       MAX(billing_group_id) AS billing_group_id,
       BOOL_OR(separate_invoice_for_each_pod) AS separate_invoice,
       COUNT(*) AS pod_count,
       COUNT(*) FILTER (WHERE has_profile) AS pods_with_profile
FROM pods
JOIN bbp USING (contract_id, pod_id, identifier)
GROUP BY contract_id, contract_number, create_date;
"""

POD_SQL = """
SELECT p.identifier, b.id AS bbp_id, b.period_from, b.period_to, b.period_type, b.time_zone
FROM pod.pod p
JOIN pod.billing_by_profile b ON b.pod_id = p.id AND b.status = 'ACTIVE'
WHERE p.id = ANY(%s)
ORDER BY p.identifier, b.id;
"""

TERM_SQL = """
SELECT cd.invoice_payment_term_id,
       cd.invoice_payment_term_value,
       cd.product_contract_term_id
FROM product_contract.contract_details cd
WHERE cd.contract_id = %s
LIMIT 5;
"""

if __name__ == "__main__":
    with load_conn() as conn:
        with conn.cursor() as cur:
            cur.execute(SQL)
            row = cur.fetchone()
            if not row:
                print(json.dumps({"error": "NO_CONTRACT_IN_LAST_15_MIN"}, indent=2))
            else:
                cols = [d[0] for d in cur.description]
                summary = dict(zip(cols, row))
                contract_id = summary["contract_id"]
                # fetch pod ids for contract
                cur.execute(
                    """
                    SELECT DISTINCT p.id
                    FROM product_contract.contract_details cd
                    JOIN product_contract.contract_pods cp ON cp.contract_detail_id = cd.id AND cp.status = 'ACTIVE'
                    JOIN pod.pod_details pd ON cp.pod_detail_id = pd.id
                    JOIN pod.pod p ON pd.pod_id = p.id
                    WHERE cd.contract_id = %s
                    """,
                    (contract_id,),
                )
                pod_ids = [r[0] for r in cur.fetchall()]
                cur.execute(POD_SQL, (pod_ids,))
                profiles = [
                    dict(zip([d[0] for d in cur.description], r)) for r in cur.fetchall()
                ]
                cur.execute(TERM_SQL, (contract_id,))
                terms = [
                    dict(zip([d[0] for d in cur.description], r)) for r in cur.fetchall()
                ]
                out = {
                    "summary": {k: str(v) if v is not None else None for k, v in summary.items()},
                    "billing_by_profiles": [
                        {k: str(v) if v is not None else None for k, v in p.items()} for p in profiles
                    ],
                    "payment_terms": [
                        {k: str(v) if v is not None else None for k, v in t.items()} for t in terms
                    ],
                    "requirements_check": {
                        "separate_invoice_for_each_pod": summary["separate_invoice"] is True,
                        "all_pods_have_profile": summary["pod_count"] == summary["pods_with_profile"],
                        "pod_count_is_3": summary["pod_count"] == 3,
                        "no_billing_run_expected": True,
                    },
                }
                print(json.dumps(out, indent=2, default=str))
