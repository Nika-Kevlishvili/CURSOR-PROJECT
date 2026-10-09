# Create One Time Billing Run

**Domain:** Billing / Invoicing
**Source:** Create One-Time Billing Run.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 1 |
| User Actions | 0 |
| Process Steps | 6 |
| Save Operations | 3 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Check what is the type of Process Periodicity object

- **then** --> (continues...)

## Process Steps

- If type is Periodical
- X
- System should check what is the type of One-time Process Periodicity object
- If type is One-time
- One-time billing run will be created on the previous day at 11:59 p.m. by the periodic billing run JOB
- System should check each Process Periodicity object connected to the Periodic Billing Run

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- If defined date is today, then system should create one-time billing run and billing run process will be started when the time comes (Exact Date JOB)
- After user creates new Periodic Billing Run (Clicking on Save button), it is mandatory to check if it is possible to create one-time billing run immediately
- If defined date is in the future (compared to the current day), system should ignore such Process Periodicity object and shouldn't create one-time billing run. It will be created by the Periodic Billing Run JOB

## Flow Connections

Direct relationships between steps:

- After user creates new Periodic Billing Run (Clicking on Save button), it is mandatory to check if it is possible to create one-time billing run immediately --> System should check each Process Periodicity object connected to the Periodic Billing Run
- If type is Periodical --> One-time billing run will be created on the previous day at 11:59 p.m. by the periodic billing run JOB
- If type is One-time --> System should check what is the type of One-time Process Periodicity object
- If defined date is today, then system should create one-time billing run and billing run process will be started when the time comes (Exact Date JOB) --> X
- If defined date is in the future (compared to the current day), system should ignore such Process Periodicity object and shouldn't create one-time billing run. It will be created by the Periodic Billing Run JOB --> X

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

