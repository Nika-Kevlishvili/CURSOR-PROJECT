"""Create clean PDT-3032 repro receivable on Test (same pattern as Dev setup)."""
import json
import os
import urllib.error
import urllib.request
from pathlib import Path

for line in Path(__file__).resolve().parents[1].joinpath(".env").read_text(encoding="utf-8").splitlines():
    if "=" in line and not line.strip().startswith("#"):
        k, v = line.split("=", 1)
        os.environ.setdefault(k.strip(), v.strip().strip('"'))

login_body = json.dumps(
    {"user": os.environ["PORTAL_USER"], "password": os.environ["PASSWORD"]}
).encode()
login_req = urllib.request.Request(
    "https://testapps.energo-pro.bg/backend/portal/rest/v2/login",
    data=login_body,
    headers={"Content-Type": "application/json"},
)
token = json.load(urllib.request.urlopen(login_req, timeout=30))["jwt"]
base = "https://testapps.energo-pro.bg/backend/phoenix-epres"
headers = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}

CUSTOMER_ID = 6047425
BILLING_GROUP_ID = 43838

payload = {
    "accountingPeriodId": 1010,
    "initialAmount": 148.00,
    "currencyId": 1001,
    "basisForIssuing": "PDT-3032 fresh repro",
    "customerId": CUSTOMER_ID,
    "billingGroupId": BILLING_GROUP_ID,
    "occurrenceDate": "26-06-2026",
    "dueDate": "07-07-2026",
    "directDebit": False,
    "blockedForOffsetting": False,
}

create_req = urllib.request.Request(
    f"{base}/customer-receivable",
    data=json.dumps(payload).encode(),
    headers=headers,
    method="POST",
)
try:
    with urllib.request.urlopen(create_req, timeout=60) as resp:
        receivable_id = resp.read().decode().strip().strip('"')
        print(f"CREATE_STATUS={resp.status}")
        print(f"RECEIVABLE_ID={receivable_id}")
except urllib.error.HTTPError as e:
    print(f"CREATE_STATUS={e.code}")
    print(f"CREATE_BODY={e.read().decode()}")
    raise SystemExit(1)
