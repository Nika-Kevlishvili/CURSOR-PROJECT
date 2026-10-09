# Automatic Offsetting Occurrence Date Payment

**Domain:** Payments / Receivables
**Source:** automatic offsetting-Occurrence Date Payment.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 24 |
| User Actions | 0 |
| Process Steps | 26 |
| Save Operations | 0 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Is billing group added?

- **then** --> (continues...)

### 2. Check is payment current amount 0?

- **then** --> (continues...)
- **then** --> (continues...)

### 3. Check collection channel is priority by prefix field fiilled?

- **then** --> (continues...)
- **then** --> (continues...)

### 4. Check for this liabilities is offsetting process already running?

- **then** --> Wait until process end
- **then** --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)

### 5. Check Payment current amount is reached 0?

- **then** --> (continues...)
- **then** --> (continues...)

### 6. check payment object, is

- **then** --> Invoice
- **then** --> Debit note
- **then** --> (continues...)

### 7. Check Payment current amount is reached 0?

- **then** --> (continues...)
- **then** --> (continues...)

### 8. Is billing group added?

- **then** --> (continues...)

### 9. Check billing group separate invoice for each POD is marked?

- **then** --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)
- **then** --> Take only liabilities/receivables for which invoice exists with separate POD

### 10. Check collection channel is priority by prefix field fiilled?

- **then** --> (continues...)
- **then** --> (continues...)

### 11. Check billing group separate invoice for each POD is marked?

- **then** --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)
- **then** --> Take only liabilities/receivables for which invoice exists with separate POD

### 12. Check for this liabilities is offsetting process already running?

- **then** --> Wait until process end
- **then** --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)

### 13. Check is payment current amount 0?

- **then** --> (continues...)
- **then** --> (continues...)

### 14. Check collection channel is priority by prefix field fiilled?

- **then** --> (continues...)
- **then** --> (continues...)

### 15. Check if separate invoice is selected in billing group of payment and in payment outgoing document is filled

- **then** --> Take only liabilities for respective billing group and respective POD
- **then** --> Take liabilities respective for billing group

### 16. Check for this liabilities is offsetting process already running?

- **then** --> Wait until process end
- **then** --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)

### 17. Check for this liabilities is offsetting process already running?

- **then** --> Wait until process end
- **then** --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)

### 18. Check collection channel is priority by prefix field fiilled?

- **then** --> (continues...)
- **then** --> (continues...)

### 19. check liability object, is

- **then** --> Invoice
- **then** --> Debit note
- **then** --> (continues...)

### 20. check liability object, is

- **then** --> Invoice
- **then** --> Debit note
- **then** --> (continues...)

### 21. Check for this liabilities is offsetting process already running?

- **then** --> Wait until process end
- **then** --> Check collection channel is priority by prefix field fiilled?

### 22. Check if separate invoice is selected in billing group of payment and in payment outgoing document is filled

- **then** --> Take only liabilities for respective billing group and respective POD
- **then** --> Take liabilities respective for billing group

### 23. Check for this liabilities is offsetting process already running?

- **then** --> Wait until process end
- **then** --> Check collection channel is priority by prefix field fiilled?

### 24. check payment object, is

- **then** --> Invoice
- **then** --> Debit note
- **then** --> (continues...)

## Process Steps

- For liabilities and receivables find PODs from related invoice or credit note or debit note
- make deduction from payment for each liability until payment current amount reaches 0 or until all liabilities are covered
- liabilities exists
- Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)
- Liability is created
- Find related invoices
- Debit note
- Find all related credit note and debit note
- Wait until process end
- all receivables current amount reached 0
- Take only liabilities for respective billing group and respective POD
- Invoice
- Filter only liabilities and receivables which current amount more then 0
- take related credit note from receivable object
- Order them by POD identifier and then by due date asc(incrementing) and then by ID asc(incrementing)
- Liability mass import
- The process must work even if Occurence Date > Payment Date
- automatic offsetting can happen for liabilities /receivables for which occurrence date is in future
- Take liabilities respective for billing group
- Find all related debit note
- Receivable is created
- For each receivable make deduction for each liability until receivable current amount reaches 0 or until all liabilities are covered
- all liabilities current amount reached 0
- For each receivable make deduction for each liability according billing group until receivable current amount reaches 0 or until all liabilities are covered
- liabilities aren't exist
- Take only liabilities/receivables for which invoice exists with separate POD

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
- Order liabilities by due date asc(incrementing) and then by ID asc(incrementing) --> For each receivable make deduction for each liability according billing group until receivable current amount reaches 0 or until all liabilities are covered
- Receivable is created --> take related credit note from receivable object
- take related credit note from receivable object --> Find related invoices
- take related credit note from receivable object --> Receivable is created
- Liability mass import --> Liability is created
- Check billing group separate invoice for each POD is marked? --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)
- For liabilities and receivables find PODs from related invoice or credit note or debit note --> Order them by POD identifier and then by due date asc(incrementing) and then by ID asc(incrementing)
- Take only liabilities/receivables for which invoice exists with separate POD --> For liabilities and receivables find PODs from related invoice or credit note or debit note
- Check billing group separate invoice for each POD is marked? --> Take only liabilities/receivables for which invoice exists with separate POD
- Check for this liabilities is offsetting process already running? --> Wait until process end
- Check for this liabilities is offsetting process already running? --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)
- Wait until process end --> Order liabilities by due date asc(incrementing) and then by ID asc(incrementing)
- Order liabilities by due date asc(incrementing) and then by ID asc(incrementing) --> Check collection channel is priority by prefix field fiilled?
- check payment object, is --> Invoice
- check payment object, is --> Debit note
- Invoice --> Find all related debit note
- Wait until process end --> Check collection channel is priority by prefix field fiilled?
- Check for this liabilities is offsetting process already running? --> Check collection channel is priority by prefix field fiilled?
- Check if separate invoice is selected in billing group of payment and in payment outgoing document is filled --> Take only liabilities for respective billing group and respective POD
- Check if separate invoice is selected in billing group of payment and in payment outgoing document is filled --> Take liabilities respective for billing group
- Take only liabilities for respective billing group and respective POD --> Check for this liabilities is offsetting process already running?
- Take liabilities respective for billing group --> Check for this liabilities is offsetting process already running?
- Order liabilities by due date asc(incrementing) and then by ID asc(incrementing) --> make deduction from payment for each liability until payment current amount reaches 0 or until all liabilities are covered

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

