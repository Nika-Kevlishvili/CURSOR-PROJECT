# Objection To A Change Balancing Group Coordinator

**Domain:** Payments / Receivables
**Source:** objection to a change balancing group coordinator.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 1 |
| User Actions | 0 |
| Process Steps | 12 |
| Save Operations | 1 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. exist in system Objection to change of a CBG with status send and same creation date which include any imported POD and also this POD is checked?

- **then** --> (continues...)

## Process Steps

- Find all overdue liabilities for this customer.
- Sum all liabilities current amount
- Display value in result from the processed file in overdue amount for contract field
- Display POD and customer from contract's version in result from the processed file in POD and customer fields
- Find all overdue liabilities which are connected to this invoices
- Find all overdue liabilities for this billing group
- Display value in result from the processed file in overdue amount for point of delivery field
- Find all standard or debit note type invoice for this contract which include this POD
- Display value in result from the processed file in overdue amount for billing group field
- For each POD find contract's version where POD is active today
- Read imported file and get POD identifiers
- Take Billing group where this POD is added from contract's version

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Process will start save draft button

## Flow Connections

Direct relationships between steps:

- Process will start save draft button --> Read imported file and get POD identifiers
- Sum all liabilities current amount --> Display value in result from the processed file in overdue amount for contract field
- Read imported file and get POD identifiers --> exist in system Objection to change of a CBG with status send and same creation date which include any imported POD and also this POD is checked?
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

