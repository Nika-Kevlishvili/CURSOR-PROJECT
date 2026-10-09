# Real Deactivation From Mass Import

**Domain:** POD Management / Contracts
**Source:** Real deactivation from mass import.drawio (1).svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 4 |
| User Actions | 0 |
| Process Steps | 5 |
| Save Operations | 0 |
| Error States | 1 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Exists any contract version, where POD is activated and activation date is more/equal then imported deactivation date?

- **then** --> (continues...)

### 2. POD has activation date?

- **then** --> (continues...)

### 3. POD exists in system?

- **then** --> (continues...)

### 4. Exists any contract version, where POD is activated and activation date is more then imported deactivation date?

- **then** --> (continues...)

## Process Steps

- Exist POD has activation and deactivation date
- Import file from supply activation page
- Set deactivation date
- Exist POD has activation date and hasn't deactivation date
- Replace deactivation date

## Error/Exception States

- display error in process: POD doesn’t exist in system

## Flow Connections

Direct relationships between steps:

- Import file from supply activation page --> POD exists in system?
- Exist POD has activation date and hasn't deactivation date --> Set deactivation date
- Exist POD has activation and deactivation date --> Replace deactivation date
- Set deactivation date --> Exists any contract version, where POD is activated and activation date is more then imported deactivation date?
- Replace deactivation date --> Exists any contract version, where POD is activated and activation date is more then imported deactivation date?

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

