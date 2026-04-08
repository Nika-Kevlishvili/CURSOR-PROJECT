# PhoenixExpert — Reminder for Disconnection stuck IN_PROGRESS (id 1976, Dev)

## Request
User asked why Reminder for Disconnection record **1976** on **Dev** has been stuck in **IN_PROGRESS** for a long time.

## Findings (codebase)
- Entity: `receivable.power_supply_disconnection_reminders` (`PowerSupplyDisconnectionReminder`).
- Scheduled-style processing: `PowerSupplyDisconnectionReminderService.documentGenerationJob()`:
  - Selects only rows with `reminderStatus = DRAFT`, `status = ACTIVE`, and `customerSendDate` in **(now − 1 hour, now]** (`findByStatusAndSendTimePessimisticLock`).
  - Immediately sets selected rows to **IN_PROGRESS**, then runs customer selection, document/communication generation (including async tasks).
  - On failure in the outer block, reminders are reset to **DRAFT**; per-reminder failures in async can reset to **DRAFT**; successful paths set **EXECUTED** or intermediate **EMAIL_IN_PROGRESS** / **SMS_IN_PROGRESS** (combined channel flow).

## Why IN_PROGRESS can persist
1. **No automatic retry for IN_PROGRESS**: The job query requires **DRAFT**. A row left in **IN_PROGRESS** is **never picked again** by `documentGenerationJob`, so it can remain indefinitely without manual/ops intervention.
2. **Process interruption**: If the application node stops, is killed, or loses the DB session **after** flushing **IN_PROGRESS** but **before** the final status update is committed, the row can stay **IN_PROGRESS** (no rollback of an already committed transaction).
3. **Narrow send window**: Selection uses a **1-hour** window around `customer_send_date`; this does not fix stuck **IN_PROGRESS** but explains why a missed run while still **DRAFT** could also cause operational issues.

## Database verification (not performed)
PostgreSQL Dev MCP returned `Not connected` in this session. Recommended Dev SQL:
`SELECT id, reminder_number, reminder_status, status, customer_send_date, status_assigned_at, communication_channels FROM receivable.power_supply_disconnection_reminders WHERE id = 1976;`
Also check related rows in `receivable.power_supply_disconnection_reminder_customers` and application logs for reminder id 1976 around the original send time.

## Suggested next steps (operational, not implemented here)
- Confirm row state and `communication_channels` on Dev.
- If processing never completed: typical recovery is **DRAFT** (to allow controlled re-run in the send window) or **EXECUTED** only after business confirmation — follow internal Phoenix/DB change procedures.

Agents involved: PhoenixExpert
