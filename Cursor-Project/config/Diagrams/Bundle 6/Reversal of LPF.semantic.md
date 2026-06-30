# Reversal Of Lpf

**Domain:** Payments / Receivables
**Source:** Reversal of LPF.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 1 |
| Process Steps | 10 |
| Save Operations | 0 |
| Error States | 1 |

## User Actions (Entry Points)

- Start

## Process Steps

- Offset
- Customer Relationships
- Reversal of Late Payment Fine
- If this receivable amount equal more than 0 should start automatically offsetting
- Late Payment Fine
- Receivable
- Partly or not Covered LPF
- Successfull message in the interface
- Reversal of all successful operations
- Fully Covered LPF

## Error/Exception States

- Failed message in the interface

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

