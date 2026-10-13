# Invoice JSON empty structure

Empty key skeleton from Phoenix `BillingRunDocumentModelImpl` + nested document models (not from a live invoice without compensations).

Includes government compensation additional parameters (PDT-3341):

- `DD[].TableCompensations[]`: `Reason`, `DocumentNumber`, `DocumentDate`, `Period`, `Volumes`, `Price`, `Amount`, `Currency`, `AdditionalParameter1`–`AdditionalParameter12`
- `SDCompensations[]`: same except no `Reason` / `Price` (summary model)

| File | Environment |
|------|-------------|
| `invoice-json-test1-epres-new-billing.json` | Phase 1 Test 1 (`phoenix-epres`) |
| `invoice-json-phoenix2-end-supplier.json` | Phase 2 Test 2 End Supplier (`phoenix-test2`) |

Both files currently share the same empty structure. Strings `""`, numbers `null`. Nested arrays keep one empty object so keys stay visible.
