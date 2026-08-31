import json
import os
import psycopg2
import psycopg2.extras

conn = psycopg2.connect(
    host=os.environ["PGHOST"],
    port=os.environ["PGPORT"],
    user=os.environ["PGUSER"],
    password=os.environ["PGPASSWORD"],
    dbname=os.environ["PGDATABASE"],
    connect_timeout=30,
)
cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

cur.execute(
    """
    SELECT table_schema, table_name
    FROM information_schema.tables
    WHERE table_name IN ('customer','customers','pod')
       OR table_name ILIKE 'customer%'
    ORDER BY 1,2
    LIMIT 40
    """
)
print("customer-ish:", json.dumps(cur.fetchall(), default=str))

# Case A: separate=false, invoice has pod_id
sql_a = """
SELECT
  cd.id AS customer_detail_id,
  cl.customer_id,
  cl.id AS liability_id,
  cl.liability_number,
  cl.invoice_id,
  cbg.group_number AS bg_number,
  cbg.separate_invoice_for_each_pod AS separate_flag,
  COUNT(DISTINCT isdd.pod_id) FILTER (WHERE isdd.pod_id IS NOT NULL) AS invoice_pod_count,
  MIN(p.identifier) FILTER (WHERE isdd.pod_id IS NOT NULL) AS sample_pod
FROM receivable.customer_liabilities cl
JOIN product_contract.contract_billing_groups cbg
  ON cbg.id = cl.contract_billing_group_id
JOIN invoice.invoice_standard_detailed_data isdd
  ON isdd.invoice_id = cl.invoice_id
LEFT JOIN pod.pod p ON p.id = isdd.pod_id
LEFT JOIN LATERAL (
  SELECT x.id
  FROM customer.customer_details x
  WHERE x.customer_id = cl.customer_id
  ORDER BY x.version_id DESC NULLS LAST, x.id DESC
  LIMIT 1
) cd ON TRUE
WHERE cl.invoice_id IS NOT NULL
  AND cbg.separate_invoice_for_each_pod IS DISTINCT FROM TRUE
  AND cl.status::text NOT IN ('DELETED', 'Cancelled', 'CANCELLED')
GROUP BY cd.id, cl.customer_id, cl.id, cl.liability_number, cl.invoice_id,
         cbg.group_number, cbg.separate_invoice_for_each_pod
HAVING COUNT(DISTINCT isdd.pod_id) FILTER (WHERE isdd.pod_id IS NOT NULL) = 1
ORDER BY cl.id DESC
LIMIT 12
"""

sql_b = """
SELECT
  cd.id AS customer_detail_id,
  cl.customer_id,
  cl.id AS liability_id,
  cl.liability_number,
  cl.invoice_id,
  cbg.group_number AS bg_number,
  cbg.separate_invoice_for_each_pod AS separate_flag,
  COUNT(DISTINCT isdd.pod_id) FILTER (WHERE isdd.pod_id IS NOT NULL) AS invoice_pod_count,
  MIN(p.identifier) FILTER (WHERE isdd.pod_id IS NOT NULL) AS sample_pod
FROM receivable.customer_liabilities cl
JOIN product_contract.contract_billing_groups cbg
  ON cbg.id = cl.contract_billing_group_id
JOIN invoice.invoice_standard_detailed_data isdd
  ON isdd.invoice_id = cl.invoice_id
LEFT JOIN pod.pod p ON p.id = isdd.pod_id
LEFT JOIN LATERAL (
  SELECT x.id
  FROM customer.customer_details x
  WHERE x.customer_id = cl.customer_id
  ORDER BY x.version_id DESC NULLS LAST, x.id DESC
  LIMIT 1
) cd ON TRUE
WHERE cl.invoice_id IS NOT NULL
  AND cbg.separate_invoice_for_each_pod = TRUE
GROUP BY cd.id, cl.customer_id, cl.id, cl.liability_number, cl.invoice_id,
         cbg.group_number, cbg.separate_invoice_for_each_pod
HAVING COUNT(DISTINCT isdd.pod_id) FILTER (WHERE isdd.pod_id IS NOT NULL) >= 1
ORDER BY cl.id DESC
LIMIT 10
"""

print("=== A: separate=false + exactly 1 invoice pod (UI POD EMPTY) ===")
cur.execute(sql_a)
print(json.dumps(cur.fetchall(), default=str, indent=2))

print("=== B: separate=true + has invoice pod (UI POD SHOWN) ===")
cur.execute(sql_b)
print(json.dumps(cur.fetchall(), default=str, indent=2))

# Pick one customer from A and summarize all their invoice liabilities separate flags
cur.execute(sql_a)
rows = cur.fetchall()
if rows:
    cust = rows[0]["customer_id"]
    detail = rows[0]["customer_detail_id"]
    print(f"=== summary for best demo customer_id={cust} detail={detail} ===")
    cur.execute(
        """
        SELECT
          cbg.separate_invoice_for_each_pod AS separate_flag,
          COUNT(*) AS liab_cnt,
          COUNT(*) FILTER (
            WHERE EXISTS (
              SELECT 1 FROM invoice.invoice_standard_detailed_data d
              WHERE d.invoice_id = cl.invoice_id AND d.pod_id IS NOT NULL
            )
          ) AS with_invoice_pod
        FROM receivable.customer_liabilities cl
        JOIN product_contract.contract_billing_groups cbg
          ON cbg.id = cl.contract_billing_group_id
        WHERE cl.customer_id = %s
          AND cl.invoice_id IS NOT NULL
        GROUP BY cbg.separate_invoice_for_each_pod
        ORDER BY 1
        """,
        (cust,),
    )
    print(json.dumps(cur.fetchall(), default=str, indent=2))

    # customer identifier if exists
    cur.execute(
        """
        SELECT column_name FROM information_schema.columns
        WHERE table_schema='customer' AND table_name='customers'
        ORDER BY ordinal_position
        """
    )
    cols = [r["column_name"] for r in cur.fetchall()]
    print("customer.customers cols sample:", cols[:30])
    if cols:
        id_col = "identifier" if "identifier" in cols else ("customer_number" if "customer_number" in cols else None)
        if id_col:
            cur.execute(f"SELECT id, {id_col} AS ident FROM customer.customers WHERE id=%s", (cust,))
            print(json.dumps(cur.fetchall(), default=str, indent=2))

cur.close()
conn.close()
