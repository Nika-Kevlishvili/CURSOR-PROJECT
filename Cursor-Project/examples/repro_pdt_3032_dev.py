"""Reproduce PDT-3032 on Dev: manual liability create with open receivable on same billing group."""
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
    "https://devapps.energo-pro.bg/backend/portal/rest/v2/login",
    data=login_body,
    headers={"Content-Type": "application/json"},
)
token = json.load(urllib.request.urlopen(login_req, timeout=30))["jwt"]
base = "http://10.236.20.11:8091"
headers = {"Content-Type": "application/json", "Authorization": f"Bearer {token}"}

payload = {
    "accountingPeriodId": 1042,
    "dueDate": "07-07-2026",
    "initialAmount": 12.00,
    "currencyId": 1001,
    "basisForIssuing": "PDT-3032 repro",
    "customerId": 6075881,
    "billingGroupId": 90190,
    "occurrenceDate": "26-06-2026",
    "directDebit": False,
    "blockedForPayment": False,
    "blockedForReminderLetters": False,
    "blockedForCalculationOfLatePayment": False,
    "blockedForLiabilitiesOffsetting": False,
    "blockedForSupplyTermination": False,
}

create_req = urllib.request.Request(
    f"{base}/customer-liability",
    data=json.dumps(payload).encode(),
    headers=headers,
    method="POST",
)
try:
    with urllib.request.urlopen(create_req, timeout=60) as resp:
        liability_id = resp.read().decode().strip().strip('"')
        print(f"CREATE_STATUS={resp.status}")
        print(f"LIABILITY_ID={liability_id}")
except urllib.error.HTTPError as e:
    print(f"CREATE_STATUS={e.code}")
    print(f"CREATE_BODY={e.read().decode()}")
    raise SystemExit(1)

get_req = urllib.request.Request(f"{base}/customer-liability/{liability_id}", headers=headers)
try:
    with urllib.request.urlopen(get_req, timeout=30) as resp:
        body = resp.read().decode()
        print(f"GET_STATUS={resp.status}")
        print(f"GET_OK=true")
        print(body[:500])
except urllib.error.HTTPError as e:
    print(f"GET_STATUS={e.code}")
    print(f"GET_OK=false")
    print(e.read().decode())
