# Missing interim invoice – portal visibility and billing run UI (PDT-2750)

**Jira:** PDT-2750  
**Type:** Bug  
**Summary:** Portal users must **see** interim invoices for standard billing runs when interim/advance payment applies, with labels and links consistent with **REAL parent invoice** rules (**same contract first**, else **fallback REAL invoice with ≥1 POD**). UI must **not** contradict backend state (ticket: misleading audit log vs missing interim).

**Scope:** Frontend (Sales Portal / Energo-Pro portal per Test **`BASE_URL`**) — billing run workspace, invoice lists, invoice detail, customer billing history, document download, and audit/history widgets touched by billing errors.

---

## Jira comments interpreted into checks

| ID | User-visible expectation |
|----|---------------------------|
| **J1** | Invoice detail / billing run line items must show interim invoice **linked** or **derived from** the **latest previous REAL standard invoice on the same contract** when that invoice exists (match backend **`prev_invoice_id`** / parent reference). |
| **J2** | If same-contract parent is absent in UI history, interim must still appear when backend used **fallback REAL invoice with ≥1 POD** — parent hint text or “calculated from invoice #…” must reflect that fallback invoice number. |
| **J3** | When parent invoice is **outside** the current run list but already **REAL**, document generation UI must **not** show a permanent failure badge **only** because the parent row is not in the same run table (align with backend REAL gate). |
| **J4** | For churn scenarios (**old** contract POD deactivated **28.02**, **new** contract POD **01.03**), March billing run screens must list **both** standard and interim invoice types when backend created them — **no** “only standard” display gap. |
| **J5** | Audit / activity messages must not state interim success if **no** interim invoice row exists for that run (ticket complaint). |

---

## Test data (preconditions)

- **Environment:** Test portal (`BASE_URL` from EnergoTS `.env` for Test). Use credentials with **billing run view**, **invoice view**, **customer view** permissions (exact permission names per portal docs).

1. **Backend prerequisite (mandatory):** Execute **TC-BE-1** backend setup **or** equivalent API seeding so **billing run R** exists with: standard invoice **S1**, interim invoice **INT1**, parent REAL invoice **PARENT** per **J1/J2**. Record **invoice numbers** and **billing run number** (e.g. `BILLING…`).
2. Log into the portal with a user that can open **customer UIC** / identifier matching seeded customer.
3. Navigate bookmark: **Billing → Billing runs** (or equivalent menu path per Test portal).

---

## Frontend Test Cases

### TC-FE-1 (Positive): Billing run detail lists interim invoice alongside standard

**Description:** Verify the billing run detail grid shows **`INTERIM_AND_ADVANCE_PAYMENT`** (or localized label “Interim / Advance”) row when backend created **INT1**.

**Preconditions:**
1. Complete **Test data** steps 1–3 with seeded run **R** containing **INT1**.

**Test steps:**
1. Open billing run search; filter by run **number** / **date** for **R**.
2. Open run detail view listing generated invoices.
3. Locate row with invoice type = interim/advance.

**Expected test case results:** Interim row visible with correct **invoice number**, **amount summary**, **customer**, **contract** reference matching **INT1**; status consistent with backend (**DRAFT** / **REAL** per workflow stage under test).

---

### TC-FE-2 (Positive): Invoice detail shows parent / “calculated from” reference matching J1/J2

**Description:** UI exposes which **REAL** invoice anchors percent-based interim — must match **PARENT** id or fallback invoice per backend.

**Preconditions:**
1. **INT1** persisted with known **`interimCalculatedFromInvoiceId`** → maps to invoice number **PARENT_NUM**.

**Test steps:**
1. Open **INT1** from billing run or customer invoice list.
2. Read field / section describing source invoice (number, date, contract).

**Expected test case results:** Displayed parent invoice **number** = **PARENT_NUM**; contract label matches **same contract** path (**J1**) or fallback contract (**J2**) exactly as backend — **no** stale reference to unrelated runs.

---

### TC-FE-3 (Positive): Customer invoices tab shows March interim after month-boundary churn (J4)

**Description:** UI regression for prod-like churn — customer history must show interim for March run.

**Preconditions:**
1. Backend scenario **TC-BE-1** (Feb deactivate old contract POD, Mar activate new contract POD) completed with visible invoices.

**Test steps:**
1. Open **customer** profile → **Invoices** / **Billing documents**.
2. Filter by **March 2026** (or matching test period).
3. Identify standard invoice **S1** and interim **INT1**.

**Expected test case results:** **Both** appear; dates/order consistent with billing period; POD/contract columns reflect **new** contract for March charges where applicable.

---

### TC-FE-4 (Positive): PDF / document download available when backend REAL gate passes (J3)

**Description:** From portal, user downloads interim PDF after generation phase — should succeed when backend **`getValidInvoices`** included interim.

**Preconditions:**
1. Backend finished PDF generation for **INT1** (status indicates document present).

**Test steps:**
1. On **INT1** detail, click **Download PDF** / preview action.
2. Verify HTTP **200** / file opens (browser download).

**Expected test case results:** Document downloads without error toast referencing missing parent when parent is REAL (**J3**).

---

### TC-FE-5 (Negative): User without billing permission cannot open billing run invoices

**Description:** Permission-negative UI path.

**Preconditions:**
1. User role **without** billing-run view permission (create restricted Test user).

**Test steps:**
1. Attempt deep link to billing run **R** URL (from privileged user copy).
2. Observe redirect / error message.

**Expected test case results:** Access denied message; **no** invoice rows leaked.

---

### TC-FE-6 (Negative): Misleading status — UI must not show “interim generated” if list empty (J5)

**Description:** Align with ticket — notification / audit widget must match invoice list.

**Preconditions:**
1. Controlled backend state: billing run **R2** with **no** interim invoice (simulate bug or pre-fix env).

**Test steps:**
1. Open billing run **R2** invoice list (empty interim).
2. Read banner / audit / last operation message area.

**Expected test case results:** **No** message claiming interim invoice creation success; any error banner matches backend **`billing_run_error`** entries.

**Actual result (if bug):** Toast/history says success while grid lacks interim — fail TC.

---

### TC-FE-7 (Edge): Filtering invoice list by type “Interim” includes INT1 only

**Description:** List filters regression.

**Preconditions:**
1. Customer has standard **S1** and interim **INT1** visible.

**Test steps:**
1. Open invoice list with **type** filter = Interim / Advance.
2. Confirm rows.

**Expected test case results:** **INT1** included; **S1** excluded unless multi-type bug.

---

### TC-FE-8 (Negative): Stale cache after regeneration — force refresh shows interim

**Description:** Soft reload / cache bust shows newly appeared interim (if SPA caches lists).

**Preconditions:**
1. Interim **INT1** added after initial page load in same session (via parallel backend completion).

**Test steps:**
1. Hard refresh browser on billing run page (`Ctrl+F5`).

**Expected test case results:** Interim row appears — **no** requirement to re-login for consistency.

---

## References

- **Jira:** PDT-2750 — Missing interim invoice.
- **Backend file:** `Cursor-Project/test_cases/Backend/Missing_Interim_Invoice_PDT_2750.md` (paired TC-BE-*).
- **Playwright bridge:** Steps align with `test.step`, `CheckResponse` for any API-backed UI checks per `Cursor-Project/config/playwright_generation/playwright instructions/`.
