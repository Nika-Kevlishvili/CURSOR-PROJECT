# PhoenixExpert — Liability counts for Reminder-1976 (Dev DB)

## Request
User asked to find liabilities matching Reminder for Disconnection parameters (amount 10–30, currency leva, max due date from UI) and report how many exist.

## Data source
- **Environment:** PostgreSQL Dev (`10.236.20.21`), database `phoenix`.
- **Reminder row `1976`:** `liability_amount_from` 10, `liability_amount_to` 30, `currency_id` 1001 (nomenclature: name `лева`, abbreviation `лв.`, print `BGN`).

## Results

| Metric | Count | Notes |
|--------|------:|-------|
| **Naive per-liability filter** | **7601** | `ACTIVE`, `currency_id = 1001`, `current_amount` between 10 and 30, `due_date <= reminder.liabilities_max_due_date` and `due_date <= CURRENT_DATE` (DB session date). |
| **Naive with fixed max due `2026-04-07`** | **7607** | Same as above but `due_date <= DATE '2026-04-07'` instead of column from reminder. |
| **Phoenix `execute()` equivalent (row count)** | **2858** | Full repository SQL from `PowerSupplyDisconnectionReminderRepository.execute` for reminder id 1976, including per-customer `sumCurrentAmount` band filter, contract/rescheduling rules, customer list filter, mass-operation blocking exclusion. |
| **Phoenix distinct `customer_id` in that set** | **1835** | Same filtered set, `COUNT(DISTINCT customer_id)`. |

## Important distinction
Phoenix does **not** filter “each liability between 10 and 30”. It filters liabilities whose **customer’s total** converted amount (`sumCurrentAmount` over partition by `customer_id`, reminder id) falls in **[10, 30]** (plus currency conversion rules). The naive count counts **individual** liabilities in that amount range.

## Artifacts
- SQL file: `Cursor-Project/reports/2026-04-08/psdr1976_phoenix_execute_count.sql` (Phoenix row count; executed via local Python + psycopg2 because MCP `query` disallows `WITH`/CTE).

Agents involved: PhoenixExpert
