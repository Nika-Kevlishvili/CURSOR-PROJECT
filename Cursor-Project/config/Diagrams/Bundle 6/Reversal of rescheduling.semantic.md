# Reversal Of Rescheduling

**Domain:** Payments / Receivables
**Source:** Reversal of rescheduling.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 2 |
| User Actions | 1 |
| Process Steps | 15 |
| Save Operations | 1 |
| Error States | 0 |

## User Actions (Entry Points)

- Start

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Is liability connected to Payment? (Automatical offset)

- **then** --> (continues...)

### 2. Is (each) liability Connected to manual liabilities offsetting?

- **then** --> (continues...)

## Process Steps

- Reversal of Rescheduling
- Cancal all installment's late payment fine
- Add in created receivable's
- add in
- Liability
- Automatically start Reversal of Manual liability offsetting
- system should Select each installment liability which created after Rescheduling
- Initiate Reversal of Rescheduling in [Backend]
- LPF
- Receivable
- Make offsetting betwween rescheduling and installment liabilities
- M L Offset
- Filter receivables which current amount more then 0 from restored and created receivables
- run standard automatic offsetting for all liabilties/receivables
- Rescheduling

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- For each payment create customer receivable

## Flow Connections

Direct relationships between steps:

- Initiate Reversal of Rescheduling in [Backend] --> system should Select each installment liability which created after Rescheduling
- Make offsetting betwween rescheduling and installment liabilities --> Filter receivables which current amount more then 0 from restored and created receivables
- add in --> run standard automatic offsetting for all liabilties/receivables
- Automatically start Reversal of Manual liability offsetting --> Is liability connected to Payment? (Automatical offset)
- For each payment create customer receivable --> Add in created receivable's

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

