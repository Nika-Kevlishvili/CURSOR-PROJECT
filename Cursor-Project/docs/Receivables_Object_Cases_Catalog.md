# Receivables Object Cases — Index

**Workbook:** [`Receivables_Object_Cases_with_billing.xlsx`](Receivables_Object_Cases_with_billing.xlsx)

`Receivables_Object_Cases.xlsx` was open when the billing rows were added, so the merged workbook is the file above. It contains the cleaned object sheets, the 13 generated offsetting cases, and sheet `23_Billing_Regression`.

One sheet per receivables object. Listing, permissions, translations, and preview are excluded. Each row cites Jira, Confluence, a diagram, or a Playwright test.

- Object rows: 181 (duplicate and covered rows removed)
- Jira cases on the object sheets: 63
- Automation Yes / Partial / No: 59 / 19 / 103
- Billing regression rows on `23_Billing_Regression`: 153 (report wave was 111 first-wave and 42 entrance; 76 on the REG board, 77 not)
- Generated offsetting cases kept: 7 automatic (`21_TC_Automatic`) and 6 direct (`22_TC_Direct`). None of the 153 billing cases covers them.

## Regression scope

Every data row has `RegressionScope`, `ScopeRank`, and `ScopeReason`. The rank follows [`regression-scope-method.md`](regression-scope-method.md). The billing report's own Version 1 / Version 1.1 labels were not copied.

| Scope | Rank | Rows |
|---|---|---|
| Version 1 | HIGHEST | 227 |
| Version 1.1 | HIGH | 86 |
| Version 1.2 | MEDIUM | 14 |
| Out of scope | Out of scope | 20 |

Version 1 is posted money, who is billed, or supply. Version 1.1 is the entrance (invoice correction, manual credit or debit note, collection channel, payment package, reminder, request). Version 1.2 is a catalog, a read, or customer assessment. Screen, listing, sort, and field format stay out of scope. Standard cancellation and standard reversal stay Version 1.

Sheets `21_TC_Automatic` and `22_TC_Direct` hold the automatic and direct offsetting test cases. They are not extra objects.

Jira source: REST search. Confluence source: REST fallback (`asterbit.atlassian.net/wiki`).
