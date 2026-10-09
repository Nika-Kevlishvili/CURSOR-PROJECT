# Supply Activation Mass Import

**Domain:** POD Management / Contracts
**Source:** supply activation mass import.drawio (1).svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 1 |
| User Actions | 0 |
| Process Steps | 9 |
| Save Operations | 0 |
| Error States | 6 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Contract status is valid?

- **then** --> (continues...)
- **then** --> (continues...)

## Process Steps

- replace activation date with imported activation date
- Set activation date
- POD is add in any future version without deactivation and activation date
- POD isn't add in any future version
- POD is add without activation and deactivation date
- version exist
- Import file from supply activation page
- POD is add in any future version with deactivation and activation date
- Periodical

## Error/Exception States

- Display error imported activation date more then POD activation date
- display error in process: POD doesn’t exist in system
- display error in process: POD is add more then 1 contract
- display error in process: Activation date is less then contract's signing date
- display error in process: Valid contract is not found
- display error in process: POD is already active in A contract's a version

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

