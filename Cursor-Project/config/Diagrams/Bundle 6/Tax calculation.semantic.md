# Tax Calculation

**Domain:** Payments / Receivables
**Source:** Tax calculation.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 0 |
| User Actions | 0 |
| Process Steps | 10 |
| Save Operations | 2 |
| Error States | 2 |

## Process Steps

- Take latest liability for each POD from 2nd tab
- Text
- Take latest liability for each POD from connected request for disconnection of power supply 2nd tab
- Consider which grid operator and supplier type is selected in 1st tab of connected request for disconnection of power supply object
- Consider which grid operator and supplier type is selected in 1st tab of request for disconnection of power supply object
- Calculate amount including vat for each POD according to
- Take all selected POD's type of disconnection field value from disconnection object
- 2.Tax of disconnection of power supply
- Stop process for this POD and continue for rest ones
- 1.Tax of request for disconnection of power supply

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- According those liability and taxes for the grid operator nomenclature create new invoice
- Create liability according new invoice and add this liability in invoice liability sub-object

## Error/Exception States

- after taking liability check does this liability used in the cancelation of request of the disconnection (the object status should executed)
- if liability is used in the cancel reques for disconnection object , charge fee by this liability should be accured

## Flow Connections

Direct relationships between steps:

- Take latest liability for each POD from 2nd tab --> According those liability and taxes for the grid operator nomenclature create new invoice
- Take all selected POD's type of disconnection field value from disconnection object --> Consider which grid operator and supplier type is selected in 1st tab of connected request for disconnection of power supply object
- Take latest liability for each POD from connected request for disconnection of power supply 2nd tab --> According those liability and taxes for the grid operator nomenclature create new invoice

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

