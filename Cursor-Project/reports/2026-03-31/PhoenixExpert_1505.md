## PhoenixExpert Report 15:05

**Topic:** Payment mass import – how a payment can be created first and then the process errors.

### Key Points

- Payment mass import in Phoenix is a multi-step pipeline: file parsing, per-row payment creation, then post-processing (offsets, accounting, cross-row checks, notifications).
- Because not all of these steps necessarily share a single transaction, it is realistic that a `customer_payment` row is inserted successfully and a later step fails.
- Main categories of late failure after payment insert:
  - Offsetting to receivables/liabilities fails (missing/mismatched receivable, closed receivable, wrong customer, currency mismatch).
  - Accounting posting/journal creation fails (missing template, invalid mapping, external accounting/EPB error).
  - Cross-row consistency / duplicate or idempotency checks fail after several payments are already inserted.
  - Deeper customer/contract/POD validation fails only after a basic payment has been stored.
  - Technical/infrastructure issues occur after DB commit (audit/event logging, messaging, notifications).

### Practical Interpretation

- To reproduce a “payment created, then error” scenario, use test input where each row can pass basic payment-level validation, but violates rules in a later step:
  - Reference a non-existing or already-closed invoice/receivable.
  - Reuse an external payment ID/bank reference that already exists.
  - Use a customer/contract/POD combination that breaks deeper business rules (blocked customer, terminated contract).
  - Use a payment type/channel that lacks accounting configuration.

These patterns explain how Phoenix can end up with a stored payment while the mass import as a whole is reported as failed or partially successful.

