import difflib
from pathlib import Path

import psycopg2

PROCS = (
    "automatic_liability_offsetting",
    "automatic_liability_offsetting_pair",
    "check_liability_offestting_allowed",
    "check_receivable_offestting_allowed",
)

q = """
SELECT pg_get_functiondef(p.oid)
FROM pg_proc p
JOIN pg_namespace n ON p.pronamespace = n.oid
WHERE n.nspname = 'receivable' AND p.proname = %s
"""

dev = psycopg2.connect(
    host="10.236.20.21",
    port=5432,
    user="postgres",
    password="rakX5thB2qO3",
    dbname="phoenix",
    connect_timeout=30,
)
test = psycopg2.connect(
    host="10.236.20.24",
    port=5432,
    user="postgres",
    password="U&Vd2Ge@nyM1",
    dbname="phoenix",
    connect_timeout=30,
)

for proc in PROCS:
    d = dev.cursor()
    d.execute(q, (proc,))
    drow = d.fetchone()
    t = test.cursor()
    t.execute(q, (proc,))
    trow = t.fetchone()
    if not drow or not trow:
        print(f"{proc}: MISSING dev={bool(drow)} test={bool(trow)}")
        continue
    ddef, tdef = drow[0], trow[0]
    print(f"{proc}: DEV_LEN={len(ddef)} TEST_LEN={len(tdef)} EQUAL={ddef == tdef}")
    if proc == "automatic_liability_offsetting" and ddef != tdef:
        out = Path(__file__).resolve().parent / "offsetting_proc.diff"
        out.write_text(
            "\n".join(
                difflib.unified_diff(
                    ddef.splitlines(), tdef.splitlines(), fromfile="dev", tofile="test", n=3
                )
            ),
            encoding="utf-8",
        )
        print(f"WROTE={out}")
