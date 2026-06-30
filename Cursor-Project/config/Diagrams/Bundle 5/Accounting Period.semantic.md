# Accounting Period

**Domain:** Billing / Invoicing
**Source:** Accounting Period.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 0 |
| Process Steps | 3 |
| Save Operations | 1 |
| Error States | 0 |

## Process Steps

- System should automatically select newly created accounting period as Open
- Already created accounting period should be displayed in listing with its start-end dates and status - Opened
- Accounting Period

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- On the first day of the month system should automatically create an accounting period

## Flow Connections

Direct relationships between steps:

- Accounting Period --> On the first day of the month system should automatically create an accounting period

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

