# Invoice Cancelation New

**Domain:** Billing / Invoicing
**Source:** Invoice cancelation -new.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 5 |
| User Actions | 0 |
| Process Steps | 22 |
| Save Operations | 2 |
| Error States | 5 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Check connected receivable

- **then** --> (continues...)

### 2. Check invoice document type

- **then** --> (continues...)

### 3. Billing Run Type

- **then** --> Invoice Document Type
- **then** --> (continues...)
- **then** --> (continues...)
- **then** --> (continues...)

### 4. Liability is connected to payment

- **then** --> Service for Payment Reverse

### 5. Invoice Document Type

- **then** --> (continues...)
- **then** --> (continues...)
- **then** --> (continues...)

## Process Steps

- Liability is fully payed
- Proforma
- receivable initial amount equal to current amount
- Invoice/Debit Note
- receivable current amount is 0
- Document type is credit note
- Service for Reverse manual offsetting
- Manual Debit/Credit Note
- Billing Data By Scale
- Interim
- For Volumes
- Reverse Late Payment Fine
- Credit Note
- Document type is invoice or debit note
- Billing Data By Profile
- Liability is partly paid
- Liability isn't payed
- Service for Reverse Automatic offsetting
- Reverse late payment fine (Use already described process)
- Other Billing Runs
- receivable current amount is more then 0 and less then initial amount
- Service for Payment Reverse

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Create Receivable with deducted amount (Service for Creating Receivable due to debit note)
- Delete Billing Data By Profile in DB (which was made from Scales)

## Error/Exception States

- Liability cancelation
- Service For Creating Liability due to Credit Note Cancelation
- Processes from where should start cancelation
- Service For Creating Receivable due to debit Note Cancelation
- Process Triger should be invoice status change to cancelled

## Flow Connections

Direct relationships between steps:

- Process Triger should be invoice status change to cancelled --> Check invoice document type
- Document type is credit note --> Check connected receivable
- Liability cancelation --> Processes from where should start cancelation
- Billing Run Type --> Invoice Document Type
- Reverse Late Payment Fine --> Service For Creating Liability due to Credit Note Cancelation
- Service for Reverse manual offsetting --> Liability is connected to payment
- Reverse Late Payment Fine --> Service For Creating Receivable due to debit Note Cancelation
- Liability is connected to payment --> Service for Payment Reverse

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

