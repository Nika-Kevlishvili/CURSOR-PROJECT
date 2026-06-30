# Updated Automatic Offsetting

**Domain:** Payments / Receivables
**Source:** Updated 28012026 - Automatic offsetting.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 3 |
| User Actions | 0 |
| Process Steps | 22 |
| Save Operations | 0 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. check liability object, is

- **then** --> Invoice
- **then** --> Debit note
- **then** --> (continues...)

### 2. Check billing group separate invoice for each POD is marked?

- **then** --> Order liabilities and receavables by due date asc(incrementing) and then by ID asc(incrementing)
- **then** --> Take only liabilities/receivables for which invoice exists with separate POD

### 3. Check billing group separate invoice for each POD is marked?

- **then** --> (continues...)
- **then** --> Take only liabilities/receivables for which invoice exists with separate POD

## Process Steps

- Receivable is created
- Invoice
- receivables aren't exist
- liabilities exists
- receivables exist
- Liability mass import
- Filter only liabilities and receivables which current amount more then 0
- Find related invoices
- Order them by POD identifier and then by due date asc(incrementing) and then by ID asc(incrementing)
- take related credit note from receivable object
- Debit note
- Take only liabilities/receivables for which invoice exists with separate POD
- For liabilities and receivables find PODs from related invoice or credit note or debit note
- all liabilities current amount reached 0
- all receivables current amount reached 0
- Find all related credit note and debit note
- For each receivable make deduction for each liability until receivable current amount reaches 0 or until all liabilities are covered
- For each receivable make deduction for each liability according billing group until receivable current amount reaches 0 or until all liabilities are covered
- Liability is created
- liabilities aren't exist
- automatic offsetting can happen for liabilities /receivables for which occurrence date is in future
- Order liabilities and receavables by due date asc(incrementing) and then by ID asc(incrementing)

## Flow Connections

Direct relationships between steps:

- check liability object, is --> Invoice
- check liability object, is --> Debit note
- Invoice --> Find all related credit note and debit note
- Debit note --> Find related invoices
- Find related invoices --> Invoice
- Filter only liabilities and receivables which current amount more then 0 --> all liabilities current amount reached 0
- Filter only liabilities and receivables which current amount more then 0 --> all receivables current amount reached 0
- liabilities exists --> Check billing group separate invoice for each POD is marked?
- take related credit note from receivable object --> Find related invoices
- Liability mass import --> Liability is created
- For liabilities and receivables find PODs from related invoice or credit note or debit note --> Order them by POD identifier and then by due date asc(incrementing) and then by ID asc(incrementing)
- Take only liabilities/receivables for which invoice exists with separate POD --> For liabilities and receivables find PODs from related invoice or credit note or debit note
- Check billing group separate invoice for each POD is marked? --> Take only liabilities/receivables for which invoice exists with separate POD
- receivables exist --> Check billing group separate invoice for each POD is marked?
- Order liabilities and receavables by due date asc(incrementing) and then by ID asc(incrementing) --> For each receivable make deduction for each liability according billing group until receivable current amount reaches 0 or until all liabilities are covered
- Check billing group separate invoice for each POD is marked? --> Order liabilities and receavables by due date asc(incrementing) and then by ID asc(incrementing)

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

