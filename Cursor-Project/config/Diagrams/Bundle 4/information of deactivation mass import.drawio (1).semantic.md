# Information Of Deactivation Mass Import

**Domain:** POD Management / Contracts
**Source:** information of deactivation mass import.drawio (1).svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 5 |
| User Actions | 0 |
| Process Steps | 5 |
| Save Operations | 3 |
| Error States | 1 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Is contract's term end date equal to imported deactivation date?

- **then** --> (continues...)

### 2. POD exists in system?

- **then** --> (continues...)

### 3. POD has activation date?

- **then** --> (continues...)

### 4. Check for each action are all penalties covered?

- **then** --> (continues...)

### 5. Exists any contract version, where POD is activated and activation date is more then imported deactivation date?

- **then** --> (continues...)

## Process Steps

- Penalty automatic calculation
- For each action duplicate action with missing penalty
- Import file from supply deactivation page
- Other
- Group this imported PODs by contract

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- create new action with uncovered left PODs and any of respective penalty. other parameters are same
- Shouldn't create action
- shouldn't create action

## Error/Exception States

- display error in process: POD doesn’t exist in system

## Flow Connections

Direct relationships between steps:

- Import file from supply deactivation page --> POD exists in system?
- Other --> Group this imported PODs by contract
- Group this imported PODs by contract --> Is contract's term end date equal to imported deactivation date?
- create new action with uncovered left PODs and any of respective penalty. other parameters are same --> For each action duplicate action with missing penalty

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

