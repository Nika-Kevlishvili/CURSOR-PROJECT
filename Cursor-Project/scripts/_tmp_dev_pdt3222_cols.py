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
    connect_timeout=25,
)
cur = conn.cursor(cursor_factory=psycopg2.extras.RealDictCursor)

for schema, table in [
    ("receivable", "customer_liabilities"),
    ("product_contract", "contract_billing_groups"),
    ("invoice", "invoice_standard_detailed_data"),
    ("customer", "customer"),
    ("customer", "customer_details"),
]:
    cur.execute(
        """
        SELECT column_name, data_type
        FROM information_schema.columns
        WHERE table_schema=%s AND table_name=%s
        ORDER BY ordinal_position
        """,
        (schema, table),
    )
    print(f"=== {schema}.{table} ===")
    print(json.dumps(cur.fetchall(), default=str))

cur.close()
conn.close()
