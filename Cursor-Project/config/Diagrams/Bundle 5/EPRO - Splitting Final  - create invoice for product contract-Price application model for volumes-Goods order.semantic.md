# Epro Splitting Final Create Invoice For Product Contract Price Application Model For Volumes Goods Order

**Domain:** Billing / Invoicing
**Source:** EPRO - Splitting Final  - create invoice for product contract-Price application model for volumes-Goods order.drawio.svg
**Extracted:** 2026-06-30

## Overview

| Metric | Count |
|--------|-------|
| Decision Points | 41 |
| User Actions | 2 |
| Process Steps | 117 |
| Save Operations | 16 |
| Error States | 16 |

## User Actions (Entry Points)

- User pays full amount based on Pro forma invoice
- Start Billing

## Decision Points (Business Logic)

These are the branching points in the process. Each decision leads to different outcomes.

### 1. is there more then 1 profile selected in price component-> application model?

- **then** --> Check which profile are selected in application model and are this profile matches our filtered billing data by profile?
- **then** --> (continues...)

### 2. User should see 4th tab to see generated PDF document. Does the user confirm to create real pro forma invocie?

- **then** --> (continues...)

### 3. check all filtered price components. application model type is price application model over time and application type is one time

- **then** --> Is already created invoice for selected price component for selected contract?

### 4. Check which level is selected in application Model

- **then** --> POD
- **then** --> Contract

### 5. Is in filtered billing data by scales override?

- **then** --> take all corrections after respective standard billing data by scale
- **then** --> (continues...)

### 6. User should see 5th tab to see generated PDF document. Does the user confirm to create real pro forma invocie?

- **then** --> (continues...)

### 7. Should Update Service order status as Paid and should generate real invoice based on Pro forma invoice

- **then** --> (continues...)

### 8. check all filtered price components. application model type is price application model per piece

- **then** --> (continues...)

### 9. check price formula in price component is price profile or price parameter used ?

- **then** --> price include only fixed variable
- **then** --> (continues...)

### 10. Should Update Goods order status as Paid and should generate real invoice based on Pro forma invoice

- **then** --> (continues...)

### 11. check invoice correction 1st page Is price change checkbox marked?

- **then** --> (continues...)
- **then** --> return an error: in order to have correction for service contract invoice price change should be marked

### 12. Check Permission to create an invoice. If user doesn't have permission - disable 'Issue an invoice' button

- **then** --> Check if goods order status is Awaiting Payment and Payment term is filled

### 13. Is Filled (Listing Or Mass Import)

- **then** --> Take Invoice
- **then** --> Take All PODs from Invoice
- **then** --> Take Invoice

### 14. Billing Data Type

- **then** --> Billing Data By Scale
- **then** --> Billing Data By Profile

### 15. Check which level is selected in application Model

- **then** --> POD
- **then** --> Contract

### 16. Check if service order status is Awaiting Payment and Payment term is filled

- **then** --> (continues...)

### 17. Check if goods order status is Awaiting Payment and Payment term is filled

- **then** --> (continues...)

### 18. User should see 4th tab to see generated PDF document. Does the user confirm to create real pro forma invocie?

- **then** --> (continues...)

### 19. Check Permission to create Pro forma invoice. If user doesn't have permission - disable 'Issue a pro-forma invoice' button

- **then** --> (continues...)

### 20. check filtered billing data by scales, Is any of them correction or override

- **then** --> (continues...)
- **then** --> (continues...)

### 21. Does such billing data exist?

- **then** --> (continues...)
- **then** --> Check which profile are selected in application model and are all this profile matches our filtered billing data by profile for calculated period?

### 22. Validate New Billing Datas

- **then** --> (continues...)
- **then** --> Ignore all not useful billing data for Reversal

### 23. Check which profile are selected in application model and are all this profile matches our filtered billing data by profile for calculated period?

- **then** --> error for invoice slot: billing data by profile is missing for period () for POD:XXX for contract with number

### 24. Is contract type combined and at least one selected profile is

- **then** --> Check which profile are selected in application model and are all this profile matches our filtered billing data by profile for calculated period?
- **then** --> is there more then 1 profile selected in price component-> application model?

### 25. Check which level is selected in application Model

- **then** --> POD
- **then** --> Contract

### 26. Check Permission to create Pro forma invoice. If user doesn't have permission - disable 'Issue a pro-forma invoice' button

- **then** --> (continues...)

### 27. Should Update Goods order status as Paid and should generate real invoice based on Pro forma invoice

- **then** --> (continues...)

### 28. Check Permission to create an invoice. If user doesn't have permission - disable 'Issue an invoice' button

- **then** --> Check if service order status is Awaiting Payment and Payment term is filled

### 29. Is Empty

- **then** --> (continues...)
- **then** --> (continues...)

### 30. Is already created invoice for selected price component for selected contract?

- **then** --> (continues...)

### 31. is price profile or/and price parameter same or bigger dimension then billing data by profile?

- **then** --> (continues...)
- **then** --> (continues...)

### 32. Check which level is selected in application Model

- **then** --> POD
- **then** --> Contract

### 33. Check if goods order status is Awaiting Payment and Payment term is filled

- **then** --> (continues...)

### 34. check invoice correction 1st page Is price change checkbox marked?

- **then** --> (continues...)
- **then** --> Take contract number all distinct PODs identifiers from selected standard invoice

### 35. Check Permission to create an invoice. If user doesn't have permission - disable 'Issue an invoice' button

- **then** --> Check if goods order status is Awaiting Payment and Payment term is filled

### 36. check if restriction is selected in application model

- **then** --> (continues...)
- **then** --> check price component 1st tab, is discount checkbox marked?

### 37. check price component 1st tab, is discount checkbox marked?

- **then** --> (continues...)
- **then** --> (continues...)

### 38. Check which profile are selected in application model and are this profile matches our filtered billing data by profile?

- **then** --> profile is
- **then** --> (continues...)
- **then** --> error for invoice slot: billing data by profile is missing for period () for POD:XXX for contract with number

### 39. Check Permission to create Pro forma invoice. If user doesn't have permission - disable 'Issue a pro-forma invoice' button

- **then** --> (continues...)

### 40. is in price component, in price formula, price profile/price parameter used?

- **then** --> (continues...)
- **then** --> price formula should return one numeric result.

### 41. Reversal

- **then** --> Reverse PODs from original invoice
- **then** --> Invoice Reversal (Debit > Credit; Credit > Debit)

## Process Steps

- Calculate VAT amount=total amount without VAT *VAT rate /100
- Send invoice to customer
- Reverse PODs from original invoice
- Case 2: Date of the month
- The field Exact amount to be renamed to Exact amount without VAT
- Same corrections from One time billing run
- Price component
- Case 1: Type is Match the invoice date
- Use Volume Price Components Only from original invoice
- Find Billing Data
- Exclude all interim groups which is related to this interim object from the list
- Product contract
- POD
- Generate PDF documents based on selected template in Billing run or take from respective product if in billing run template isn't added
- Generate PDF document
- Not Found
- On termination of a contract
- Do not generate interim for this inteirm group and record in the report. interim ID is not generated for contract XXX
- application model type is price application model over time and application type is Periodically
- price include only fixed variable
- When billing run condition is POD and application model is contract
- Exact amount
- Finalize Process and Not Generate any new Invoice
- Send to customer generated PDF document
- Step 4 check value type
- Use Billing Datas from original invoice
- Step 3.3.1 To check the number of days which is written in contract or product/service.
- take all corrections after respective standard billing data by scale
- Scales flow - normalize data
- If doesn't exist do not generate anything
- price formula should return one numeric result.
- Take contract number all distinct PODs identifiers from selected standard invoice
- After creating invoice should be generated liability according invoice amount
- Generate debit note
- Invoice amount is more than interim amount
- interim and advance payment
- Select all service orders which status is Awaiting Payment and prepayment term in calendar days is less than current day
- Over time one time
- Max end date and time zone question
- system should check all active in term, active in perpetuity service contracts and take respective service version
- Text
- Generate liability
- Do not generate anything
- Ignore all not useful billing data for Reversal
- Generate credit note note
- Together with the first invoice
- Negative
- If there is <1 Same Data, Choose Last (bigger ID)
- Take Invoice
- Contract
- Identify Invoice where This billing Data-s are invoiced
- For Volumes
- Full
- Take All PODs from Invoice
- For each split check application model type in price component
- Per piece
- amount = Calculated price
- Warning message: Missing billing data for POD:XXXX for contract with number: XXX
- 09/01 10:03
- Positive
- MEETING 2
- Find and use Respective Price components (Original Invoice Date Contract Version > Product/Service > Price Component)
- Separate version check box separate invoice shouldbe added
- Application model per piece
- MEETING 7
- MEETING 3
- Example doesn't match description
- Price Change
- MEETING 4
- contract status is terminated
- Service contract
- Billing Data By Profile
- Ignore such price component
- Find old and new Billing Datas for Invoice
- POD is Potentially Not Reversed
- profile is
- MEETING 8
- Take Last Override + All Corrections
- All interims which was used in calculation of invoice should be marked as Deducted
- Case 3: Calendar days after invoice date
- application model type is Price application model for volumes and application type is by scales
- With standard invoice
- system should check all active in term, active in perpetuity product/service contracts and take respective product/service version
- Generate debit note with final amount - 0
- Half
- Application model over time - periodical
- % from previous invoice
- warning message: invoiced data for tariff/scale doesn't match with the invoiced data from the grid operator and continue process
- Price & Volume Change
- With every invoice
- POD is Potentially Reversed
- Find old and new Billing Datas for PODs
- Volume Change
- Only interims
- Do not generate interim for this inteirm and advance payment
- Upon signing a contract
- Step 3.1.1 To check billing run type
- Case 4: Periodical
- MEETING 5
- Calculate Invoice Amount with standard Process with Price components
- Invoice deduction check
- Invoice amount is less than interim amount
- Process should start from invoice correction type billing run manually or automatically
- 7:39 8 jan
- Same for Service order
- Invoice Reversal (Debit > Credit; Credit > Debit)
- Evaluate price amount according this price component
- Invoice amount is equal to interim amount
- MEETING 11
- Step 3: For each inteirm and advance payment check date of issue
- 10 jan 2024 09:00 - whole day
- Round after percentage calculation
- With electricity invoice
- Billing Data By Scale
- Generate already calculated invoice
- For each price component check application model type
- contract status is active in terms, active in perpetuity

## Save Operations (Outcomes)

These are the possible end states where data is persisted:

- Manually can be deleted draft pro forma invoice. 'Create pro forma invoice button is enabled'
- Create Draft interim and advance payment
- Generate real invoice (change invoice status as real update invoice number with real invoice number counter)
- if at least correction is received - Remove new meter reading, old meter reading, difference multiplier, correction, deduct . Apply only updated sub period
- Select goods order to create invoice
- create draft invoice
- Create new debit note
- Create records in CRM module for each invoice and start directly email sending process
- Create new credit note
- Create Draft invoice
- Delete Pro forma invoice (actual delete from DB) and change goods order status as cancelled
- Select goods order to create pro forma invoice
- Generate real invoice ( change invoice status as real update invoice number with real invoice number counter)
- Select service order to create invoice
- Select service order to create pro forma invoice
- Delete Pro forma invoice (actual delete from DB) and change service order status as cancelled

## Error/Exception States

- Error if price parmeter value is empty
- Error if price parameter value is empty
- error for invoice slot: billing data by profile is missing for period () for POD:XXX for contract with number
- return an error: in order to have correction for service contract invoice price change should be marked
- return an error and do not generate invoice (standard invoice shouldn't be generated as well)
- Error for invoice slot. billing data by scales with ID doesn't match validations
- error in invoice slot: Price can't be calculated for selected dimension and continue process for other slots
- error in invoice slot: billing data by profile /scale with IDs doesn't cover requirements for combined flow (also record type in errors with its IDs)
- error for invoice slot: correction/override billing data with IDs(...) doesn't match to its standard billing data continue process for other slots
- error for invoice slot: data is missing in billing data by profile with ID for billing data by profile
- Error message: Price can't be calculated for interim XXXXX for contract XXXX
- Return an error: Payment term is missing and Can't generate real invoice
- error for invoice slot: correction billing data with IDs(...) doesn't match to its override billing data record with ID and continue process for other slots
- Display error for this invoice:
- error for invoice slot: billing data with ID: XXX periodicity doesn't match with application model for price component with ID: XXX
- error 1for invoice slot: price parameter/price profile doesn't exist for selected period () for billing data with ID

## Flow Connections

Direct relationships between steps:

- For each split check application model type in price component --> application model type is Price application model for volumes and application type is by scales
- Check which profile are selected in application model and are all this profile matches our filtered billing data by profile for calculated period? --> error for invoice slot: billing data by profile is missing for period () for POD:XXX for contract with number
- check price formula in price component is price profile or price parameter used ? --> price include only fixed variable
- is in price component, in price formula, price profile/price parameter used? --> price formula should return one numeric result.
- Is contract type combined and at least one selected profile is --> Check which profile are selected in application model and are all this profile matches our filtered billing data by profile for calculated period?
- Is contract type combined and at least one selected profile is --> is there more then 1 profile selected in price component-> application model?
- is there more then 1 profile selected in price component-> application model? --> Check which profile are selected in application model and are this profile matches our filtered billing data by profile?
- Check which profile are selected in application model and are this profile matches our filtered billing data by profile? --> profile is
- Check which profile are selected in application model and are this profile matches our filtered billing data by profile? --> error for invoice slot: billing data by profile is missing for period () for POD:XXX for contract with number
- check if restriction is selected in application model --> check price component 1st tab, is discount checkbox marked?
- Does such billing data exist? --> Check which profile are selected in application model and are all this profile matches our filtered billing data by profile for calculated period?
- Create Draft invoice --> Generate PDF documents based on selected template in Billing run or take from respective product if in billing run template isn't added
- Generate PDF documents based on selected template in Billing run or take from respective product if in billing run template isn't added --> Generate real invoice ( change invoice status as real update invoice number with real invoice number counter)
- Is in filtered billing data by scales override? --> take all corrections after respective standard billing data by scale
- check all filtered price components. application model type is price application model over time and application type is one time --> Is already created invoice for selected price component for selected contract?
- create draft invoice --> Generate PDF documents based on selected template in Billing run or take from respective product if in billing run template isn't added
- Check which level is selected in application Model --> POD
- Check which level is selected in application Model --> Contract
- After creating invoice should be generated liability according invoice amount --> Create records in CRM module for each invoice and start directly email sending process
- Generate PDF documents based on selected template in Billing run or take from respective product if in billing run template isn't added --> Generate real invoice (change invoice status as real update invoice number with real invoice number counter)
- Generate real invoice (change invoice status as real update invoice number with real invoice number counter) --> After creating invoice should be generated liability according invoice amount
- Contract --> amount = Calculated price
- amount = Calculated price --> create draft invoice
- Evaluate price amount according this price component --> Check which level is selected in application Model
- Only interims --> Exclude all interim groups which is related to this interim object from the list
- With standard invoice --> Step 4 check value type
- Step 3: For each inteirm and advance payment check date of issue --> Case 1: Type is Match the invoice date
- Step 3: For each inteirm and advance payment check date of issue --> Case 2: Date of the month
- Step 3: For each inteirm and advance payment check date of issue --> Case 3: Calendar days after invoice date
- Step 3: For each inteirm and advance payment check date of issue --> Case 4: Periodical
- Case 1: Type is Match the invoice date --> Step 3.1.1 To check billing run type
- Case 3: Calendar days after invoice date --> Step 3.3.1 To check the number of days which is written in contract or product/service.
- Step 3.1.1 To check billing run type --> Only interims
- Step 3.1.1 To check billing run type --> With standard invoice
- Step 4 check value type --> Exact amount
- Step 4 check value type --> % from previous invoice
- Step 4 check value type --> Price component
- Invoice amount is more than interim amount --> Generate debit note
- Invoice amount is less than interim amount --> Generate credit note note
- Invoice amount is equal to interim amount --> Generate debit note with final amount - 0
- Generate debit note --> All interims which was used in calculation of invoice should be marked as Deducted
- Generate credit note note --> All interims which was used in calculation of invoice should be marked as Deducted
- Generate debit note with final amount - 0 --> All interims which was used in calculation of invoice should be marked as Deducted
- Select service order to create pro forma invoice --> Check Permission to create Pro forma invoice. If user doesn't have permission - disable 'Issue a pro-forma invoice' button
- For each price component check application model type --> Application model per piece
- For each price component check application model type --> Application model over time - periodical
- Application model over time - periodical --> Check which level is selected in application Model
- Generate PDF document --> User should see 5th tab to see generated PDF document. Does the user confirm to create real pro forma invocie?
- Generate liability --> Send invoice to customer
- Select all service orders which status is Awaiting Payment and prepayment term in calendar days is less than current day --> Delete Pro forma invoice (actual delete from DB) and change service order status as cancelled
- ... and 34 more connections

---

## How to Use This for Test Cases

1. **Happy Path**: Follow decisions with 'Yes' conditions to Save Operations
2. **Alternative Paths**: Follow 'No' branches to see different outcomes
3. **Error Scenarios**: Check Error States section for exception cases
4. **Preconditions**: User Actions show what triggers this process

*This is supplementary evidence - always verify against code and Confluence.*

