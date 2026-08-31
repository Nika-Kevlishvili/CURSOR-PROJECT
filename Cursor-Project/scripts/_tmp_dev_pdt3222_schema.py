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

# Discover schemas/tables if needed
cur.execute(
    """
    SELECT table_schema, table_name
    FROM information_schema.tables
    WHERE table_name ILIKE '%liabilit%'
       OR table_name ILIKE '%billing_group%'
       OR table_name ILIKE '%invoice_standard_detailed%'
    ORDER BY 1,2
    LIMIT 80
    """
)
print("=== candidate tables ===")
print(json.dumps(cur.fetchall(), default=str, indent=2))

cur.close()
conn.close()
