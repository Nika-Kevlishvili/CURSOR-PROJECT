## Summary Report 15:05

**Agent:** PhoenixExpert  
**Topic:** Payment mass import – payment created, then error.

### Overview

The Phoenix payment mass import pipeline can create `customer_payment` records early (after row-level validation) and only then perform later steps such as offsets, accounting posting, cross-row checks, deep contract/customer/POD validation, and infrastructure actions (events/notifications).

Because these later steps may run in separate service calls or outside the main DB transaction, failures there do not always roll back the already-inserted payments.

### Main Late-Failure Scenarios

- Offsetting to receivables/liabilities fails due to missing/mismatched/closed receivables or currency/customer mismatch.
- Accounting posting or accounting template resolution fails, or external accounting integration errors occur.
- Cross-row/duplicate/idempotency checks (e.g. duplicate external payment ID, wrong totals) fail after several payments have been inserted.
- Deep validation on customer/contract/POD state fails (blocked customer, terminated contract, invalid POD state).
- Technical/infrastructure issues (audit/event logs, messaging, notifications) fail after the DB commit.

These scenarios explain how a mass import can report an error while some payments from the file still exist in the database.

