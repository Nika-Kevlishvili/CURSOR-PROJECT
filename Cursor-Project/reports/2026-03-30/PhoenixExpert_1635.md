# PhoenixExpert – How to reproduce "Automatic payment offsetting out failed;"

## Code reminder

`ClientException("Automatic payment offsetting out failed;")` is thrown only in `AutomaticOffsettingService.offsetOfPayments` when the `try` around `CALL receivable.automatic_payment_offsetting_out(...)` throws **any** `Exception`. Procedure returning `o_message != "OK"` is logged but does **not** throw this exact message from that branch.

## Reproduction approaches

1. **Trigger path (required):** Any successful `PaymentService.create` that is not `fromPaymentCancel` — after `saveAndFlush` it calls `executePaymentOffsetting(paymentId)`. Same for payment mass import per row via `paymentService.create`.

2. **Force JDBC/SQL failure (reliable for exact message):**
   - In a **non-production** DB: temporarily revoke `EXECUTE` on `receivable.automatic_payment_offsetting_out` from the app DB user, or rename/drop the procedure; create payment → expect this `ClientException`.
   - Simulate connectivity failure during the call (harder to time): break connection, firewall, or kill session.

3. **Data-driven (if procedure uses RAISE EXCEPTION):** If `automatic_payment_offsetting_out` raises a PostgreSQL exception for specific invalid state, loading those conditions would surface `SQLException` → same Java catch → same message. Requires reading the procedure body in DB migrations / DBA repo or `\df+` / `pg_get_functiondef` in Test.

4. **Automated test (local):** Unit/integration test with mocked `EntityManager` / `Session` / `Connection` so `prepareCall` or `execute` throws `SQLException` — reproduces message without DB tricks.

## Not sufficient for this exact text

- Procedure completes with non-OK `o_message` only (current Java only logs).

## Agents involved

PhoenixExpert
