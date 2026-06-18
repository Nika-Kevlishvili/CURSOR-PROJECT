# PDT-2971 — Confluence Evidence & Specification Mapping Report

**Ticket:** [PDT-2971](https://oppa-support.atlassian.net/browse/PDT-2971) — *Request for disconnection - a POD with 2 contracts for different customers is listed on 1 row*  
**Report date:** 2026-06-12  
**Environment context (validation):** PROD (ticket); Playwright repro on Dev  
**Verdict (prior bug validation):** **VALID** (cross-customer contract/liability merge defect)  
**Confluence source:** MCP search + REST read of cached page `72155868` (Phase 1 decision basis)

> **Link note:** Phoenix **Confluence** lives on **`asterbit.atlassian.net`**. Do **not** use `oppa-support.atlassian.net/wiki/...` — those URLs return a *null* error. **Jira** tickets (PDT-*) remain on **`oppa-support.atlassian.net/browse/...`**.

---

## 1. Executive summary

PDT-2971 is **not documented as a standalone Confluence page**. The reproduction scenario lives in **Jira**. Product behaviour for **Request for Disconnection → Point of Delivery (2nd) tab** is specified on **[Confluence page 72155868](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72155868/Request+for+disconnection+of+the+power+supply+-+Create)** (Phase 1).

| Source | What it defines |
|--------|-----------------|
| **Jira PDT-2971** | Concrete prod repro: shared POD, two customers, two reminder rows, one merged request row |
| **Confluence 72155868** | **Expected** request POD tab rules: one row per POD showing the customer with the **latest liability**; contracts/liabilities scoped to **that** customer |
| **Code (prod)** | `pod_agg GROUP BY podid` merges contracts/liabilities across all customers on the POD |
| **Prod DB (request 1066)** | Liabilities owned by customer `7501091095` persisted under request_pod row for customer `6205111469` |

**Key product-quality finding:** Reporter expects **two rows** on the request (like the reminder). Confluence specifies **one row per POD** (latest customer only). The **VALID** defect is the **cross-customer data merge** on that single row — not the row count alone.

---

## 2. Where the case is written (source map)

### 2.1 Jira (primary repro narrative)

| Field | Value |
|-------|-------|
| **Key** | PDT-2971 |
| **Summary** | Request for disconnection - a POD with 2 contracts for different customers is listed on 1 row |
| **Environment (field)** | PROD |
| **Linked work** | [PDT-2957](https://oppa-support.atlassian.net/browse/PDT-2957) (referenced as possible related fix) |

**Reporter expected result (Jira):**

> We should have 2 separate rows for each of the customers in the request for disconnection and when the liabilities for some of them are paid, this customer should be included in the cancellation request with a check.

**Prod entities cited in ticket:**

| Entity | ID |
|--------|-----|
| POD | `32Z4101080010651` |
| Reminder for Disconnection | 1015 |
| Request for Disconnection | 1066 |
| Customer A | `7501091095` (contract `МПОК-2112000371`, terminated) |
| Customer B | `6205111469` (contract `EPES2603000418`, active) |
| Liabilities | `Liability-153387`, `Liability-110944` (A); `Liability-195210` (B, later paid) |

**Note:** PDT-2971 has **no Confluence URL** in the Jira payload; specification is found by **domain search**, not ticket link.

---

### 2.2 Confluence — primary specification (Phase 1)

| Attribute | Value |
|-----------|-------|
| **Title** | Request for disconnection of the power supply - Create |
| **Page ID** | **72155868** |
| **Space** | Phoenix |
| **Canonical wiki URL** | [Request for disconnection of the power supply - Create](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72155868/Request+for+disconnection+of+the+power+supply+-+Create) |
| **Parent page** | [Request for disconnection of the power supply](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72221276/Request+for+disconnection+of+the+power+supply) (72221276) |
| **Phase 2 excluded** | **yes** (prod/preprod decision basis) |
| **Local cache** | `Cursor-Project/config/confluence/pages/72155868.json` |

#### Section: **Point of delivery tab** (2nd tab — listing after *Load customer for disconnection of the power supply*)

**Load / filter rules (summary):**

- Table is built from the selected **Reminder for power supply disconnection** and filters (customer condition, liability amount range, grid operator).
- PODs for a **different grid operator** than selected → not displayed.
- PODs whose **sum of liability current amounts** is outside the object range → excluded.

**Multi-customer / same POD rule (verbatim from wiki storage, page 72155868):**

> If in the reminders list there are more than one customers with Liabilities from the same POD, only the latest customer should be Displayed in the POD tab of Request for disconnection. display only the customer with the latest Liability for this POD.

**Worked example on the same page:**

> Example:  
> Customer-1 Liability-1, Liability-2, Liability-3, POD-1  
> Customer-2 Liability-4, Liability-5, Liability-6, POD-1  
> We should find the latest created liability from the Liability-1 … Liability-6.  
> If Latest liability is 1,2 or 3, The Customer-1 should be shown in this list.  
> if latest liability is 4,5 or 6, The customer-2 should be shown in this list.

**Column rules relevant to PDT-2971:**

| Column | Spec rule |
|--------|-----------|
| **Customer** | Each respective customer displayed separately in the listing (identifier + name + legal form) |
| **Contract** | Product contracts only; **displayed according to the customer** — if customer has 3 contracts but only 2 match configuration, show those 2 only |
| **Billing Group** | All billing groups for displayed product contracts |
| **Point of Delivery** | POD number displayed |
| **Liabilities** | Scoped to displayed customer / billing group / POD per configuration |

**Search dimensions:** ALL, Customer identifier, Customer number, Contract number, Billing group number, POD identifier, Liability number, Outgoing document number.

**Attachments & diagrams on page 72155868:**

| Asset | Purpose |
|-------|---------|
| `request for disconnection of power supply example.xlsx` | Example data |
| diagrams.net — *Load 2nd tab for Request for disconnection of power supply* | Process flow (pageId `QYkEY6YkH-_MGkGUIUQI`) |
| diagrams.net — Charge fee process | Related fee flow |

**Local diagram (Bundle 6):**  
`Cursor-Project/config/Diagrams/Bundle 6/Request for disconnection of power supply-Load 2nd tab for Request for disconnection of power supply.drawio.svg`

---

### 2.3 Confluence — supporting pages

| Page ID | Title | URL | Role for PDT-2971 |
|---------|-------|-----|-------------------|
| **429031426** | Request for disconnection liabilities selecting logic | [Open page](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/429031426/Request+for+disconnection+liabilities+selecting+logic) | Request takes customer list from Reminder; charge-fee / liability context |
| **393248776** | Reminder for disconnection / Request for disconnection liabilities selecting logic | [Open page](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/393248776/Reminder+for+disconnection+%2F+Request+for+disconnection+liabilities+selecting+logic) | How liabilities map to PODs (invoice detailed data vs billing group) |
| **88277057** | Reminder for disconnection of power supply - Create | [Open page](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/88277057/Reminder+for+disconnection+of+power+supply+-+Create) | Reminder customer list generation (why **two rows** appear on reminder) |
| **72188398** | Request for disconnection of the power supply - Listing | [Open page](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72188398/Request+for+disconnection+of+the+power+supply+-+Listing) | Listing/search (secondary) |
| **75923634** | Disconnection of the power supply - Create | [Open page](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/75923634/Disconnection+of+the+power+supply+-+Create) | Downstream disconnection object (out of scope for POD tab merge) |

**429031426 — key quote:**

> (The Request for Disconnection takes its list of customers from the Reminder for Disconnection.)

---

### 2.4 Confluence pages excluded from prod decision basis (Phase 2)

Do **not** use these as authoritative for prod behaviour (Rule 32 Phase 2 exclusion):

| Page ID | Title | URL (Phase 2 — not prod basis) |
|---------|-------|--------------------------------|
| 585697986 | Phase 2 - Request for disconnection of the power supply - Create (2) | [Open page](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/585697986/Phase+2+-+Request+for+disconnection+of+the+power+supply+-+Create+2) |
| 880151237 | Experiment Phase 2 - Request for disconnection of the power supply - Create (2) | [Open page](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/880151237/Experiment+Phase+2+-+Request+for+disconnection+of+the+power+supply+-+Create+2) |

These duplicate POD-tab wording but are **Phase 2 / experimental** trees.

---

## 3. Dual-track: documented product vs runtime today

### 3.1 Documented product (Confluence 72155868)

1. **Reminder** can include multiple customers with liabilities on the **same POD** (separate reminder rows — matches ticket).
2. **Request POD tab:** **one row per POD** — show only the customer with the **latest liability** on that POD.
3. **Contracts, billing groups, liabilities** on that row must be **for the displayed customer only** (“according to the customer”).

### 3.2 Runtime today (code + prod DB — PDT-2971 validation)

| Layer | Behaviour |
|-------|-----------|
| **SQL `customersForDPS`** | `pod_agg` uses `GROUP BY podid`; `distinct_pods` uses `DISTINCT ON (podid)` — aggregates across **all** customers on POD |
| **Prod request 1066** | One `power_supply_disconnection_request_pods` row for POD 24490 (customer `6205111469` only) |
| **Prod pod liabilities** | `Liability-110944` and `Liability-153387` (owner `7501091095`) stored under customer `6205111469` row |
| **Dev Playwright (2026-06-12)** | Reminder second-tab: 2 customers; load/view-pod-tab: 1 row with both contract numbers merged |

### 3.3 Jira reporter vs Confluence

| Topic | Jira reporter | Confluence 72155868 |
|-------|---------------|---------------------|
| Row count on **Request** POD tab | **2 rows** (one per customer) | **1 row** (latest customer per POD) |
| Contracts on row | Each customer's own | **Only displayed customer's** contracts |
| Cross-customer merge | **Not expected** | **Forbidden** by column rules |

---

## 4. Quality findings (Senior QA)

### Finding: Jira expected row count vs Confluence POD tab rule

- **Type:** Code↔Doc mismatch (ticket vs Confluence)
- **User impact:** Medium (PO clarification on row count)
- **Spec / doc says:** [Confluence 72155868](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72155868/Request+for+disconnection+of+the+power+supply+-+Create) — one row per POD, latest liability customer
- **Ticket says:** Two separate rows per customer on request
- **Recommendation:** PO clarification — adopt Confluence “latest customer” rule vs reminder-style multi-row UI

### Finding: Cross-customer contract/liability merge on request POD tab

- **Type:** Code defect (confirmed code + prod DB + dev Playwright)
- **User impact:** High
- **Spec / doc says:** Contracts/liabilities “according to the customer” on displayed row ([72155868](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72155868/Request+for+disconnection+of+the+power+supply+-+Create))
- **Code / runtime does:** Merges all customers' contracts/liabilities sharing `podid`
- **Recommendation:** Fix code — scope aggregation by `(podId, customerId)` or filter to displayed customer

---

## 5. Playwright automation mapping

| Artifact | Path |
|----------|------|
| Spec | `Cursor-Project/EnergoTS/tests/cursor/PDT-2971-rfd-shared-pod-multi-customer-merge.spec.ts` |
| Fixtures | `Cursor-Project/EnergoTS/tests/cursor/pdt-2971-rfd-shared-pod-multi-customer-merge.fixtures.ts` |
| Pattern | AS-IS repro (passes while bug open); references Confluence 72155868 in header comment |
| Last run | Dev — **PASSED** (~4 min); reminder 2656, request draft 2201 |

**Assertions aligned to Confluence + defect:**

1. Reminder `second-tab` — both customers present (baseline).
2. `load-customer-for-disconnection` — **one** merged row (same `customersForDPS` query).
3. `view-pod-tab` — **one** row; `contracts` contains **both** contract numbers (merge defect).

---

## 6. API contract (Swagger — supporting)

| Operation | Path | Schema |
|-----------|------|--------|
| `viewPodTab` | `GET /disconnection-of-power-supply-requests/view-pod-tab/{id}` | `PageCustomersForDPSResponse` |
| `loadCustomerForDisconnectionPowerSupply` | `GET .../load-customer-for-disconnection-power-supply` | Same page schema |
| Row fields | `customers`, `contracts`, `liabilitiesInPod`, `liabilitiesInBillingGroup`, `podId`, `customerId` | Strings / IDs per row |

Swagger supports UI-level merged string fields; does not define cross-customer merge — business rules are Confluence + code.

---

## 7. Recommendations

1. **Development:** Fix `DisconnectionPowerSupplyRequestRepository.customersForDPS` — do not `GROUP BY podid` across customers when building contracts/liabilities for the displayed customer row.
2. **Product:** Clarify with PO whether request POD tab should show **one** row (Confluence) or **two** (Jira reporter); merge defect is **VALID** either way.
3. **QA:** Keep PDT-2971 Playwright as AS-IS repro; flip assertions when fix ships (per-customer scoping per 72155868).
4. **Documentation:** Consider adding explicit “same POD / multiple customers” example to 72155868 if PO confirms single-row rule.

---

## 8. Evidence checklist

| Evidence | Status |
|----------|--------|
| Confluence 72155868 (Phase 1) | Read (cached JSON + MCP search) |
| Confluence 429031426 | Read (cached) |
| Jira PDT-2971 | Read (REST) |
| Phoenix code | Cited (`customersForDPS`, `pod_agg`) |
| Prod DB | Queried (request 1066, reminder 1015) |
| Dev Playwright | Executed PASS |
| Backend TC `.md` | Not created |

---

**Confidence: 90% (GO)**  
Evidence: [+Confluence 72155868 verbatim extract, +Jira ticket, +prior validation code/DB/Playwright, -88277057 not cached locally]  
Reason: Phase 1 Confluence POD tab rules are directly quoted from page storage; mapping to PDT-2971 is evidence-backed.

**Agents involved:** PhoenixExpert, Senior QA Tester, BugFinderAgent
