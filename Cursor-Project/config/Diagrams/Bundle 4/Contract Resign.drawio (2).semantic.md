# Contract Resign

**Domain:** POD Management / Contracts
**Source:** Contract Resign.drawio (2).svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 7 |
| User Actions | 0 |
| Process Steps | 3 |
| Save Operations | 0 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Check all filtered contract's initial term end date, if it is empty skip this contract

- **then** --> (continues...)

### 2. Is in system any other contract with status Active in term or Active in perpetuity, which include same customer in respective version(depends on new contract signing date) and any combination of PODs any version, which are added in new contract?

- **then** --> (continues...)

### 3. Is this POD has activation date in any future version and has any gap?

- **then** --> (continues...)

### 4. Is this POD has future activation date in any other contract?

- **then** --> (continues...)

### 5. In new contract exist versions where this POD can be activated?

- **then** --> (continues...)

### 6. Check new contract's filtered version 3rd tab Wait for old contract term to expire(for resigning)

- **then** --> (continues...)
- **then** --> (continues...)

### 7. Is perpetuity date empty or greater then new contract's signed date?

- **then** --> (continues...)

## Process Steps

- Take contract's version, which start date is less then contract signed date and next version's start date is more then contract signed date
- Send information to xEnergy
- Mark new contract as Continiued

## Flow Connections

Direct relationships between steps:

- Take contract's version, which start date is less then contract signed date and next version's start date is more then contract signed date --> Is In product contract's 3rd tab Supply activation after contract resigning marked as Manual?
- Mark new contract as Continiued --> Send information to xEnergy

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

