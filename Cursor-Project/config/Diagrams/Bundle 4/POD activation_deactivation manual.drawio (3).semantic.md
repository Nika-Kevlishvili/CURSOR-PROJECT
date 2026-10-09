# Pod Activation Deactivation Manual

**Domain:** POD Management / Contracts
**Source:** POD activation_deactivation manual.drawio (3).svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 6 |
| User Actions | 1 |
| Process Steps | 12 |
| Save Operations | 5 |
| Error States | 0 |

## User Actions (Entry Points)

- Click on POD edit icon from 4th tab of contract and open modal window

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Is deactivation date selected?

- **then** --> (continues...)

### 2. Check POD exists contract's future versions(start date more then this version start date)

- **then** --> POD isn't add in any future version
- **then** --> POD is add in any future version without deactivation and activation date
- **then** --> POD is add in any future version with deactivation and activation date

### 3. Is deactivation date selected?

- **then** --> (continues...)

### 4. Is selected activation date more then all exist deactivation date?

- **then** --> (continues...)

### 5. Check POD exists contract's future versions(start date more then this version start date)

- **then** --> POD isn't add in any future version
- **then** --> POD is add in any future version without deactivation and activation date
- **then** --> POD is add in any future version with deactivation and activation date

### 6. Exists POD's version in any other contract with activation date?

- **then** --> (continues...)

## Process Steps

- All POD hasn't activation date
- Selected activation date is equal any exist activation date
- POD is add in any future version with deactivation and activation date
- Periodical
- POD isn't add in any future version
- Selected activation date is less then all exist activation date
- change contract status to Entry into force
- POD is add in any future version without deactivation and activation date
- All exist activation date have deactivation date
- Selected activation date is more then all exist activation date
- Any of activation date hasn't deactivation date
- Selected activation date is between exist activation dates

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- After clicking Save button
- Save activation date
- Save without activation and deactivation date
- Save activation and deactivation date
- System should remove date of activation in contract

## Flow Connections

Direct relationships between steps:

- Click on POD edit icon from 4th tab of contract and open modal window --> After clicking Save button
- After clicking Save button --> Exists POD's version in any other contract with activation date?
- Selected activation date is less then all exist activation date --> Is deactivation date selected?
- Selected activation date is more then all exist activation date --> All exist activation date have deactivation date
- Selected activation date is more then all exist activation date --> Any of activation date hasn't deactivation date
- All exist activation date have deactivation date --> Is selected activation date more then all exist deactivation date?
- Save activation date --> Check POD exists contract's future versions(start date more then this version start date)
- Check POD exists contract's future versions(start date more then this version start date) --> POD isn't add in any future version
- Check POD exists contract's future versions(start date more then this version start date) --> POD is add in any future version without deactivation and activation date
- Check POD exists contract's future versions(start date more then this version start date) --> POD is add in any future version with deactivation and activation date

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

