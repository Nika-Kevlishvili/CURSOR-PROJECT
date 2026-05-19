"""Export PDT-2529 bulk contracts needing remediation (PostgreSQL Dev, read-only)."""
import json
from pathlib import Path

import psycopg2
import psycopg2.extras

OUT_DIR = Path(r"d:\Cursor\cursor-project\Cursor-Project\config\pdt2529")
JSON_PATH = OUT_DIR / "contracts-needing-remediation.json"
SUMMARY_PATH = OUT_DIR / "remediation-summary.txt"
MCP_PATH = Path(r"C:\Users\nikak\.cursor\mcp.json")

SQL = """
WITH bulk_contracts AS (
  SELECT c.id AS contract_id
  FROM product_contract.contracts c
  WHERE c.create_date::date = DATE '2026-05-18'
  GROUP BY c.id
  HAVING (
    SELECT COUNT(DISTINCT cp.id)
    FROM product_contract.contract_details cd
    JOIN product_contract.contract_pods cp
      ON cp.contract_detail_id = cd.id AND cp.status = 'ACTIVE'
    JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
    JOIN pod.pod p ON p.id = pd.pod_id
    WHERE cd.contract_id = c.id
      AND p.identifier LIKE '32X%%'
  ) = 3
),
latest_detail AS (
  SELECT DISTINCT ON (cd.contract_id)
         cd.contract_id,
         cd.id AS contract_detail_id
  FROM product_contract.contract_details cd
  JOIN bulk_contracts bc ON bc.contract_id = cd.contract_id
  ORDER BY cd.contract_id, cd.version_id DESC NULLS LAST, cd.id DESC
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
  JOIN latest_detail ld ON ld.contract_id = bc.contract_id
  JOIN product_contract.contract_pods cp
    ON cp.contract_detail_id = ld.contract_detail_id
   AND cp.status = 'ACTIVE'
  JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
  JOIN pod.pod p ON p.id = pd.pod_id
  WHERE p.identifier LIKE '32X%%'
),
pod_profile AS (
  SELECT ap.*,
         EXISTS (
           SELECT 1
           FROM pod.billing_by_profile bbp
           WHERE bbp.pod_id = ap.pod_id
             AND bbp.status = 'ACTIVE'
             AND bbp.period_from::date <= COALESCE(ap.deactivation_date, DATE '9999-12-31')
             AND COALESCE(bbp.period_to::date, DATE '9999-12-31')
                 >= COALESCE(ap.activation_date, DATE '1900-01-01')
         ) AS has_profile
  FROM active_pods ap
),
billing_groups AS (
  SELECT bc.contract_id,
         cbg.id AS billing_group_id,
         COALESCE(cbg.separate_invoice_for_each_pod, false) AS separate_invoice
  FROM bulk_contracts bc
  JOIN product_contract.contract_billing_groups cbg
    ON cbg.contract_id = bc.contract_id
   AND cbg.status = 'ACTIVE'
),
qualifying_contracts AS (
  SELECT DISTINCT bc.contract_id
  FROM bulk_contracts bc
  WHERE EXISTS (
    SELECT 1
    FROM billing_groups bg
    WHERE bg.contract_id = bc.contract_id
      AND bg.separate_invoice IS NOT TRUE
  )
  OR EXISTS (
    SELECT 1 FROM pod_profile pp
    WHERE pp.contract_id = bc.contract_id
      AND NOT pp.has_profile
  )
),
rows AS (
  SELECT
    qc.contract_id,
    bg.billing_group_id,
    (bg.separate_invoice IS NOT TRUE) AS needs_separate_invoice,
    pp.pod_id,
    pp.contract_pod_id,
    pp.identifier,
    (NOT pp.has_profile) AS needs_profile
  FROM qualifying_contracts qc
  JOIN billing_groups bg ON bg.contract_id = qc.contract_id
  JOIN pod_profile pp
    ON pp.contract_id = qc.contract_id
   AND pp.billing_group_id = bg.billing_group_id
)
SELECT
  contract_id,
  billing_group_id,
  needs_separate_invoice,
  json_agg(
    json_build_object(
      'podId', pod_id,
      'contractPodId', contract_pod_id,
      'identifier', identifier,
      'needsProfile', needs_profile
    )
    ORDER BY contract_pod_id
  ) AS pods
FROM rows
GROUP BY contract_id, billing_group_id, needs_separate_invoice
ORDER BY contract_id, billing_group_id;
"""

SQL_COUNTS = """
WITH bulk_contracts AS (
  SELECT c.id AS contract_id
  FROM product_contract.contracts c
  WHERE c.create_date::date = DATE '2026-05-18'
  GROUP BY c.id
  HAVING (
    SELECT COUNT(DISTINCT cp.id)
    FROM product_contract.contract_details cd
    JOIN product_contract.contract_pods cp
      ON cp.contract_detail_id = cd.id AND cp.status = 'ACTIVE'
    JOIN pod.pod_details pd ON pd.id = cp.pod_detail_id
    JOIN pod.pod p ON p.id = pd.pod_id
    WHERE cd.contract_id = c.id
      AND p.identifier LIKE '32X%%'
  ) = 3
)
SELECT COUNT(*) AS count FROM bulk_contracts;
"""


def load_conn():
    j = json.loads(MCP_PATH.read_text(encoding="utf-8"))
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


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    with load_conn() as conn:
        with conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor) as cur:
            cur.execute(SQL_COUNTS)
            bulk_total = int(cur.fetchone()["count"])
            cur.execute(SQL)
            db_rows = cur.fetchall()

    result = []
    needs_sep_entries = 0
    pods_needing_profile = 0
    contracts = set()

    for r in db_rows:
        pods = r["pods"]
        if isinstance(pods, str):
            pods = json.loads(pods)
        contracts.add(r["contract_id"])
        if r["needs_separate_invoice"]:
            needs_sep_entries += 1
        for p in pods:
            if p.get("needsProfile"):
                pods_needing_profile += 1
        result.append(
            {
                "contractId": int(r["contract_id"]),
                "billingGroupId": int(r["billing_group_id"]),
                "pods": [
                    {
                        "podId": int(p["podId"]),
                        "contractPodId": int(p["contractPodId"]),
                        "identifier": p["identifier"],
                        "needsProfile": bool(p["needsProfile"]),
                    }
                    for p in pods
                ],
                "needsSeparateInvoice": bool(r["needs_separate_invoice"]),
            }
        )

    JSON_PATH.write_text(
        json.dumps(result, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )

    summary_lines = [
        "PDT-2529 remediation export (PostgreSQL Dev, read-only)",
        "Filter date: 2026-05-18",
        "Bulk-like: exactly 3 ACTIVE pods with identifier LIKE '32X%'",
        "",
        f"bulk_contracts_total: {bulk_total}",
        f"qualifying_contracts: {len(contracts)}",
        f"export_rows (contract x billing group): {len(result)}",
        f"rows_with_needsSeparateInvoice: {needs_sep_entries}",
        f"pod_slots_needing_profile: {pods_needing_profile}",
        "",
        f"json: {JSON_PATH}",
    ]
    SUMMARY_PATH.write_text("\n".join(summary_lines) + "\n", encoding="utf-8")
    print(f"bulk_total={bulk_total} export_rows={len(result)} contracts={len(contracts)}")


if __name__ == "__main__":
    main()
