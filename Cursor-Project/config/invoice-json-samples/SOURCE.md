# Invoice JSON empty structure

Empty key skeleton from live `GET /billing-run/generate-invoice-data` (TEST 1 + End Supplier shapes merged). No sample values.

| File | Content |
|------|---------|
| `invoice-json-test1-epres-new-billing.json` | Empty invoice document structure |
| `invoice-json-phoenix2-end-supplier.json` | Same empty structure |

Strings are `""`, numbers/booleans are `null`, nested object arrays keep one empty item so keys stay visible.
