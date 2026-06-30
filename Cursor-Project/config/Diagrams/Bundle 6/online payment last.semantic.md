# Online Payment Last

**Domain:** Payments / Receivables
**Source:** online payment last.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 8 |
| User Actions | 1 |
| Process Steps | 23 |
| Save Operations | 5 |
| Error States | 0 |

## User Actions (Entry Points)

- Start

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Check confirm Payment amount

- **then** --> (continues...)

### 2. Is liability was overdue?

- **then** --> (continues...)

### 3. should cover liability and save information about rest of amount for Create Receivable

- **then** --> (continues...)

### 4. should cover liability fully (fully cover date =check request date)

- **then** --> (continues...)

### 5. Check Payment

- **then** --> (continues...)

### 6. should cover liability and save information about rest of amount for Create Receivable

- **then** --> (continues...)

### 7. should cover liability fully (fully cover date =check request date)

- **then** --> (continues...)

### 8. All liability was covered?

- **then** --> (continues...)

## Process Steps

- Process One
- Process Two
- Get Confirm Payment Request
- After creating LPF, liability will be automatically generated
- End
- Customer Number (UIC or PN) & Billing Group
- Records the sent emails in the Customer Relationship Management module
- Pending confirmation
- Scheme
- Send Request for sending Email to client (for information about generated LPF)
- Find all customer liabilities which current amount is more then 0 and aren't blocked for payment
- Customer Number (UIC or PN)
- Generates and signs the late payment fine PDF document
- Customer Input
- Find all customer liabilities for inputted billing group which current amount is more then 0 and aren't blocked for payment
- Online Payment (Full Process)
- EASY PAY Sends Liabilities Check Request With Input Parameters
- find all liabilities which is covered by Payment
- compare row by row (take each liability + fine by its order)
- Get request
- Fully cover date=check request date
- Amount is always main currency
- Sends the late payment fine PDF via emails

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- shouldn't cover liability and save amount for Create Receivable
- Create LPF with date of check request date
- Create Receivable object (from all rest of amounts)
- Update current amounts of connected liabilities and add in
- Create Payment in our system

## Flow Connections

Direct relationships between steps:

- Start --> Get Confirm Payment Request
- Start --> find all liabilities which is covered by Payment
- Create LPF with date of check request date --> After creating LPF, liability will be automatically generated
- Update current amounts of connected liabilities and add in --> Create Receivable object (from all rest of amounts)
- Create Receivable object (from all rest of amounts) --> Send Request for sending Email to client (for information about generated LPF)
- Start --> Get request
- Get request --> Generates and signs the late payment fine PDF document
- Sends the late payment fine PDF via emails --> Records the sent emails in the Customer Relationship Management module
- Generates and signs the late payment fine PDF document --> Sends the late payment fine PDF via emails

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

