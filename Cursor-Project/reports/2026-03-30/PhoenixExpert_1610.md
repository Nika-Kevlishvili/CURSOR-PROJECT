# PhoenixExpert – Payment mass import vs. automatic payment offsetting error

## Question (summary)

When can payment mass import surface **"Automatic payment offsetting out failed;"** and no payment appear? Hypothesis: payment might be created first, then offsetting fails and the payment is "lost".

## Findings (code)

### Flow in `PaymentService.create`

1. Payment is built and validated.
2. `paymentRepository.saveAndFlush(payment)` persists the payment **within the same `@Transactional` method**.
3. For normal create (not `fromPaymentCancel`), `executePaymentOffsetting(paymentId)` runs, which calls `AutomaticOffsettingService.offsetOfPayments` → DB procedure `receivable.automatic_payment_offsetting_out`.

### When `ClientException("Automatic payment offsetting out failed;")` is thrown

In `AutomaticOffsettingService.offsetOfPayments`, this exact message is thrown **only** in the `catch (Exception exception)` around the JDBC `CallableStatement` work — i.e. a **Java-level failure** (e.g. SQL/driver/connection/procedure call exception). If the procedure completes but returns a message other than `"OK"`, the code **logs** and does **not** throw this particular exception from that branch.

### Transaction outcome

`create` is `@Transactional`. If offsetting throws `ClientException` (extends `RuntimeException`), the **entire transaction rolls back**, including the payment `saveAndFlush`. So the user should **not** see a committed payment for that failed row; it is not "created then deleted" in the sense of a committed row — the uncommitted work is rolled back.

### Mass import (`PaymentMassImportProcessService`)

Each row calls `paymentService.create(...)`. `AbstractTxtMassImportProcessService.processRecordBatch` catches exceptions and sets `ProcessedRecordInfo` error message from `e.getMessage()`. On success, the third-column-style value is `Payment_id: <id>, ...`; on failure there is **no** payment id returned, which matches empty ID columns in import logs when create fails after the tracking row is started.

## Conclusion

- Order: **yes** — persist (flush) is attempted **before** automatic payment offsetting in the same transaction.
- "Payment exists then offset fails": **partially misleading** — if this exception is thrown, default Spring behaviour is **rollback**, so no **committed** payment from that call.
- Practical causes for the message: **exceptions during** `automatic_payment_offsetting_out` (DB/procedure/connectivity), not the "procedure returned non-OK" path for this specific client message.

## References

- `PaymentService.java` — `create`, `saveAndFlush`, `executePaymentOffsetting`
- `AutomaticOffsettingService.java` — `offsetOfPayments`, `ClientException`
- `PaymentMassImportProcessService.java` — `paymentService.create`
- `AbstractTxtMassImportProcessService.java` — `processRecordBatch` error handling
