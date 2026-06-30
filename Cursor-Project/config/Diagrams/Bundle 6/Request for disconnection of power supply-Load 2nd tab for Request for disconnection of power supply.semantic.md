# Request For Disconnection Of Power Supply Load Nd Tab For Request For Disconnection Of Power Supply

**Domain:** Payments / Receivables
**Source:** Request for disconnection of power supply-Load 2nd tab for Request for disconnection of power supply.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 2 |
| User Actions | 0 |
| Process Steps | 19 |
| Save Operations | 2 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Check conditions for left liabilities and filter it (conditions should be checked liked it's described in export liabilities topic)

- **then** --> (continues...)

### 2. Check are selected liabilities blocked for supply disconnection in liability object and in general blocking object for process running day

- **then** --> (continues...)

## Process Steps

- Take latest liability for each POD from 2nd tab
- Take latest liability for each POD from connected request for disconnection of power supply 2nd tab
- Consider which grid operator and supplier type is selected in 1st tab of connected request for disconnection of power supply object
- Customer will selected desired rows and this rows should be saved in Database
- Take POD's type of disconnection field value from disconnection object , for which express reconnection checkbox is checked
- Take all liabilities which amount is more then 0 from connected reminder for disconnection
- 3.Tax for express reconnection
- If exist in system Request for disconnection of power supply with status executed which include any filter POD and also this POD is checked, then don't load this POD in row data
- For left liabilities find all connected invoice
- Filter left liabilities by amount (use amount from and amount to fields)
- If there is more then one customer for any POD, then use again connection and for this POD find customer which is from latest created liability
- Use connection and find customers for each POD from liability
- Take all selected POD's type of disconnection field value from disconnection object
- 2.Tax of disconnection of power supply
- Consider which grid operator and supplier type is selected in 1st tab of request for disconnection of power supply object
- 1.Tax of request for disconnection of power supply
- Calculate amount including vat for each POD according to
- Use again connection and for this POD exclude liabilities which is for another customer
- Stop process for this POD and continue for rest ones

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Create liability according new invoice and add this liability in invoice liability sub-object
- According those liability and taxes for the grid operator nomenclature create new invoice

## Flow Connections

Direct relationships between steps:

- Take all liabilities which amount is more then 0 from connected reminder for disconnection --> Check are selected liabilities blocked for supply disconnection in liability object and in general blocking object for process running day
- Filter left liabilities by amount (use amount from and amount to fields) --> For left liabilities find all connected invoice
- For left liabilities find all connected invoice --> Check conditions for left liabilities and filter it (conditions should be checked liked it's described in export liabilities topic)
- If exist in system Request for disconnection of power supply with status executed which include any filter POD and also this POD is checked, then don't load this POD in row data --> Customer will selected desired rows and this rows should be saved in Database
- Use connection and find customers for each POD from liability --> If there is more then one customer for any POD, then use again connection and for this POD find customer which is from latest created liability
- If there is more then one customer for any POD, then use again connection and for this POD find customer which is from latest created liability --> Use again connection and for this POD exclude liabilities which is for another customer
- Take latest liability for each POD from 2nd tab --> According those liability and taxes for the grid operator nomenclature create new invoice
- Take all selected POD's type of disconnection field value from disconnection object --> Consider which grid operator and supplier type is selected in 1st tab of connected request for disconnection of power supply object
- Take latest liability for each POD from connected request for disconnection of power supply 2nd tab --> According those liability and taxes for the grid operator nomenclature create new invoice
- Take POD's type of disconnection field value from disconnection object , for which express reconnection checkbox is checked --> Consider which grid operator and supplier type is selected in 1st tab of connected request for disconnection of power supply object

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

