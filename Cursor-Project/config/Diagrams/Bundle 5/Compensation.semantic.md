# Compensation

**Domain:** Billing / Invoicing
**Source:** Compensation.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 3 |
| User Actions | 0 |
| Process Steps | 49 |
| Save Operations | 7 |
| Error States | 0 |

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. Compensation Currency

- **then** --> (continues...)
- **then** --> Convert compensation amount to invoice currency

### 2. Compensation Currency

- **then** --> Convert compensation amount to invoice currency
- **then** --> (continues...)

### 3. Offset Result

- **then** --> all the receivables were spent fully for liability
- **then** --> all the receivables were spent partly (for 1) for liability
- **then** --> not all the receivables were spent for liability

## Process Steps

- Search for compensations which has this invoice attached and their index is previous of current compensations (current index - 1)
- same as system
- Group Compensations with same customer and invoice PODs by period and sum them
- Offset current compensations receivable(s) for customer (index 1) with liabilities for customer which can be found in compensations > invoice
- compensation Mass import
- Liability for Customer
- Compensations index = 0
- Compensation index > 0
- 0 pods had compensations
- Created Customer Receivable and Recipient Liability Initial and Current Amounts
- Offset current compensations receivable(s) (current index) for customer with liabilities for customer which can be found in compensations with previous index > liability for customer
- Automatic Creation standard process - Fields and sub objects (receivable)
- Liability for Recipient
- Receivable for Customer
- Leva
- different from system
- all the receivables were spent partly (for 1) for liability
- Standard billing process
- Convert compensation amount to invoice currency
- Change receivable current amount to zero and change liability current amount to deducted amount
- Compensation Identifier
- Search with customer identifier AND PoD in compensations
- Search Receivable for customer and Liability for Recipient (any) and see their initial amount
- Take Receivable for Customer and Liability for Customer and offset them with each other (repeat this for every period group for this index)
- Receivable for Recipient
- Add compensation manually
- not all pods had compensations
- Just for information
- Exchange amount according to system if needed
- Add customer liability in customer receivables' offsetting sub object AND add receivable(s) in customer liability's offsetting sub object
- Invoice
- Eur
- On invoice: Regenerate Invoice
- Selected by user
- Standard Billing Run starts
- Customer
- Automatic Creation standard process - Fields and sub objects (liabilities)
- Standard calculation of the invoice
- not all the receivables were spent for liability
- Take currency converted compensation amounts
- all the receivables were spent fully for liability
- Make compensations Index as 1
- Add Compensations in Invoice object sub object area: Current Compensation
- all pods had compensations
- Search for compensations which has previous index (current index - 0)
- Takes Customer Identifier and PoD Identifier(s) and searches compensations listing to match with PoD Identifier and Customer Identifier(s)
- Change receivable current amount to zero or what was left from the receivable after completed offsetting and change liability current amount to deducted amount and leave it for next billing
- PoD
- Liability Recipient

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Update Compensation Index to +1 than it is now
- Grouping and summed different POD amounts by the same period to show them in summary data in invoice and create receivable liabilities
- Update invoice doc with regenerated invoice
- Create liability for recipient for each created receivable for customer (customer which will mean government). outgoing document should be the invoice
- Update Invoice sub object Current Compensation with new compensations
- Create Receivable(s) as many as period groups for the summed amounts. outgoing document should be the invoice
- Create Liability for Customer and Receivable for Recipient with same initial amount + current amount should be same as initial amount

## Flow Connections

Direct relationships between steps:

- Standard Billing Run starts --> Standard calculation of the invoice
- all pods had compensations --> Compensation Currency
- not all pods had compensations --> Compensation Currency
- 0 pods had compensations --> Standard billing process
- Create Receivable(s) as many as period groups for the summed amounts. outgoing document should be the invoice --> Create liability for recipient for each created receivable for customer (customer which will mean government). outgoing document should be the invoice
- all the receivables were spent fully for liability --> Change receivable current amount to zero and change liability current amount to deducted amount
- all the receivables were spent partly (for 1) for liability --> Change receivable current amount to zero or what was left from the receivable after completed offsetting and change liability current amount to deducted amount and leave it for next billing
- Change receivable current amount to zero and change liability current amount to deducted amount --> Add customer liability in customer receivables' offsetting sub object AND add receivable(s) in customer liability's offsetting sub object
- Change receivable current amount to zero or what was left from the receivable after completed offsetting and change liability current amount to deducted amount and leave it for next billing --> Add customer liability in customer receivables' offsetting sub object AND add receivable(s) in customer liability's offsetting sub object
- Standard calculation of the invoice --> Takes Customer Identifier and PoD Identifier(s) and searches compensations listing to match with PoD Identifier and Customer Identifier(s)
- Take currency converted compensation amounts --> Group Compensations with same customer and invoice PODs by period and sum them
- Offset Result --> all the receivables were spent fully for liability
- Offset Result --> all the receivables were spent partly (for 1) for liability
- Offset Result --> not all the receivables were spent for liability
- On invoice: Regenerate Invoice --> Search with customer identifier AND PoD in compensations
- Search with customer identifier AND PoD in compensations --> Compensation Currency
- Selected by user --> Customer
- Selected by user --> PoD
- Selected by user --> Liability Recipient
- Compensation Currency --> Convert compensation amount to invoice currency
- Group Compensations with same customer and invoice PODs by period and sum them --> Create Receivable(s) as many as period groups for the summed amounts. outgoing document should be the invoice
- Add Compensations in Invoice object sub object area: Current Compensation --> Make compensations Index as 1
- Update invoice doc with regenerated invoice --> Update Compensation Index to +1 than it is now
- Make compensations Index as 1 --> Take currency converted compensation amounts
- Search for compensations which has this invoice attached and their index is previous of current compensations (current index - 1) --> Search Receivable for customer and Liability for Recipient (any) and see their initial amount
- Update Compensation Index to +1 than it is now --> Update Invoice sub object Current Compensation with new compensations
- Search Receivable for customer and Liability for Recipient (any) and see their initial amount --> Create Liability for Customer and Receivable for Recipient with same initial amount + current amount should be same as initial amount
- Create Liability for Customer and Receivable for Recipient with same initial amount + current amount should be same as initial amount --> Take currency converted compensation amounts
- Search for compensations which has previous index (current index - 0) --> Take Receivable for Customer and Liability for Customer and offset them with each other (repeat this for every period group for this index)
- Take Receivable for Customer and Liability for Customer and offset them with each other (repeat this for every period group for this index) --> Offset current compensations receivable(s) (current index) for customer with liabilities for customer which can be found in compensations with previous index > liability for customer
- Offset current compensations receivable(s) (current index) for customer with liabilities for customer which can be found in compensations with previous index > liability for customer --> Offset Result
- Offset current compensations receivable(s) for customer (index 1) with liabilities for customer which can be found in compensations > invoice --> Offset Result
- Update Invoice sub object Current Compensation with new compensations --> Search for compensations which has this invoice attached and their index is previous of current compensations (current index - 1)

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

