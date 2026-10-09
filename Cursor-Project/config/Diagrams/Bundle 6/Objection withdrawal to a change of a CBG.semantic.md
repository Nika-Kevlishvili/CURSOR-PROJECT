# Objection Withdrawal To A Change Of A Cbg

**Domain:** Payments / Receivables
**Source:** Objection withdrawal to a change of a CBG.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 0 |
| Process Steps | 10 |
| Save Operations | 1 |
| Error States | 0 |

## Process Steps

- Sum all liabilities current amount
- Display value in result from the processed file in overdue amount for contract field
- Display value in result from the processed file in overdue amount for point of delivery field
- Find all overdue liabilities for this customer.
- Display POD and customer from contract's version in result from the processed file in POD and customer fields
- Find all standard or debit note type invoice for this contract which include this POD
- Find all overdue liabilities which are connected to this invoices
- Take Billing group where this POD is added from contract's version
- Display value in result from the processed file in overdue amount for billing group field
- Find all overdue liabilities for this billing group

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Process will start save draft button

## Flow Connections

Direct relationships between steps:

- Sum all liabilities current amount --> Display value in result from the processed file in overdue amount for contract field
- Take Billing group where this POD is added from contract's version --> Find all overdue liabilities for this billing group
- Find all overdue liabilities for this billing group --> Sum all liabilities current amount
- Sum all liabilities current amount --> Display value in result from the processed file in overdue amount for billing group field
- Find all standard or debit note type invoice for this contract which include this POD --> Find all overdue liabilities which are connected to this invoices
- Find all overdue liabilities which are connected to this invoices --> Sum all liabilities current amount
- Sum all liabilities current amount --> Display value in result from the processed file in overdue amount for point of delivery field
- Display value in result from the processed file in overdue amount for contract field --> Display POD and customer from contract's version in result from the processed file in POD and customer fields
- Display value in result from the processed file in overdue amount for point of delivery field --> Display POD and customer from contract's version in result from the processed file in POD and customer fields
- Display value in result from the processed file in overdue amount for billing group field --> Display POD and customer from contract's version in result from the processed file in POD and customer fields

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

