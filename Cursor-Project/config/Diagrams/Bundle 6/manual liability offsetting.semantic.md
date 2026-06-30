# Manual Liability Offsetting

**Domain:** Payments / Receivables
**Source:** manual liability offsetting.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 1 |
| Process Steps | 9 |
| Save Operations | 0 |
| Error States | 0 |

## User Actions (Entry Points)

- Start

## Process Steps

- receivable
- Reversal Of Manual Liability Offset
- Liability
- System should take all deducted Liability, Receivable, Late payment fine, Deposit from Manual liability offsetting object
- This box is for Giga
- Successfull message in the interface
- not paid or partly paid
- End
- LPF

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

