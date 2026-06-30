import difflib
import psycopg2

q = """
SELECT pg_get_functiondef(p.oid)
FROM pg_proc p
JOIN pg_namespace n ON p.pronamespace = n.oid
WHERE n.nspname = 'receivable' AND p.proname = 'automatic_liability_offsetting_pair'
"""

dev = psycopg2.connect(host="10.236.20.21", port=5432, user="postgres", password="rakX5thB2qO3", dbname="phoenix", connect_timeout=30)
test = psycopg2.connect(host="10.236.20.24", port=5432, user="postgres", password="U&Vd2Ge@nyM1", dbname="phoenix", connect_timeout=30)
d = dev.cursor(); d.execute(q); ddef = d.fetchone()[0]
t = test.cursor(); t.execute(q); tdef = t.fetchone()[0]
for line in difflib.unified_diff(ddef.splitlines(), tdef.splitlines(), fromfile="dev", tofile="test", n=5):
    print(line)
