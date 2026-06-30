# Reversal Of Payment V

**Domain:** Payments / Receivables
**Source:** Reversal of payment V3.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 5 |
| User Actions | 0 |
| Process Steps | 6 |
| Save Operations | 6 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Check any non reversed offsettings from this reeivable and reverse all for current date; for each liabiblity which was reversed check if LPF exists and reverse them also

- **No** --> For receivable restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between receivable and payment; offseting date= current date; mark original offsetting as reversed

### 2. Check if LPF is created for this liabilty?

- **then** --> Restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between liability and payment; offseting date= current date; mark original offsetting as reversed
- **then** --> Reverse LPF

### 3. Is (each) receivable connected to Manual liabilities offsetting?

- **then** --> (continues...)

### 4. Is receivable created from payment?

- **then** --> (continues...)

### 5. Is blocking for offsetting selected?

- **then** --> (continues...)

## Process Steps

- Ask user to select customer for the new payment and select checkbox should the payment be blocked for offsetting
- Receivable should not go to automatic offsetting process and should be blocked for offsetting
- Reverse LPF
- Automatically start Reversal of Manual liability offsetting
- Initiate Reversal of Payment in [Backend]
- Make direct offsetting between negative payment and receivable with offsetting date today

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Create new payment for the selected customer with or without blocking for offsetting, depending the selection in the checkbox
- From payment create customer receivable
- For receivable restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between receivable and payment; offseting date= current date; mark original offsetting as reversed
- From new payment create customer receivable
- Create negative payment
- Restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between liability and payment; offseting date= current date; mark original offsetting as reversed

## Flow Connections

Direct relationships between steps:

- For receivable restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between receivable and payment; offseting date= current date; mark original offsetting as reversed --> From payment create customer receivable
- Check any non reversed offsettings from this reeivable and reverse all for current date; for each liabiblity which was reversed check if LPF exists and reverse them also [No] --> For receivable restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between receivable and payment; offseting date= current date; mark original offsetting as reversed
- Automatically start Reversal of Manual liability offsetting --> Check any non reversed offsettings from this reeivable and reverse all for current date; for each liabiblity which was reversed check if LPF exists and reverse them also
- Restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between liability and payment; offseting date= current date; mark original offsetting as reversed --> Is receivable created from payment?
- From payment create customer receivable --> Create negative payment
- Create negative payment --> Make direct offsetting between negative payment and receivable with offsetting date today
- Make direct offsetting between negative payment and receivable with offsetting date today --> Create new payment for the selected customer with or without blocking for offsetting, depending the selection in the checkbox
- From new payment create customer receivable --> Receivable should not go to automatic offsetting process and should be blocked for offsetting
- Check if LPF is created for this liabilty? --> Restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between liability and payment; offseting date= current date; mark original offsetting as reversed
- Check if LPF is created for this liabilty? --> Reverse LPF
- Reverse LPF --> Restore payment current amount from offsetting sub-object with deducted amount. Create reversal offsetting sub-object between liability and payment; offseting date= current date; mark original offsetting as reversed

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

