# PhoenixExpert Report — Test Case Preconditions Fix

**Date:** 2026-04-03  
**Time:** 13:22  
**Task:** Fix entity creation ordering and remove unnecessary hardcoded values in Backend test cases

## Summary

Fixed two systemic problems across all 60 test cases in `Cursor-Project/test_cases/Backend/Zero_amount_liability_receivable.md`:

### Problem 1: Incorrect Entity Creation Order (FIXED)

**Root cause:** The original test cases had Product created before Terms and Price Component, but the Phoenix codebase (`ProductTermsValidator`, `ProductService.create`) requires Terms (or TermsGroup) and Price Components to exist before Product creation.

**Old order:** Customer → POD → Product → Terms(productId) → PriceComponent(productId) → ProductContract  
**Corrected order:** Customer → POD → Terms → PriceComponent → Product(termId, priceComponentIds) → ProductContract

- Terms: Removed incorrect `productId` parameter (Terms is standalone master data)
- Price Component: Removed incorrect `productId` parameter (standalone master data)
- Product: Now references `termId` and `priceComponentIds` from prior steps
- Product Contract: Updated `productId` reference from step 5 (was step 3)
- TC-BE-53/54 (Disconnection): Added missing Terms and Price Component steps

### Problem 2: Unnecessary Hardcoded Values (FIXED)

Removed irrelevant hardcoded values from preconditions that could cause test failures (format mismatches, uniqueness conflicts):

- **Removed:** Customer `firstName`, `lastName`, `identificationNumber` (e.g., "EGN0000000006")
- **Removed:** POD `identifier` (e.g., "BG-POD-BILL-002")
- **Removed:** Product `name` (e.g., "Zero Consumption Product")
- **Removed:** Deposit `description`
- **Kept:** All values directly relevant to test outcomes (rate, consumptionKwh, amount, vatRate, disconnectionFee, installmentCount, etc.)

## Files Modified

- `Cursor-Project/test_cases/Backend/Zero_amount_liability_receivable.md` — All 60 test cases, shared billing chain, shared customer setup, references section

## Files Reviewed (No Changes Needed)

- `Cursor-Project/test_cases/Frontend/Zero_amount_liability_receivable.md` — Uses browser-based UI testing with clean preconditions; no entity creation chain repeated

## Codebase Evidence

- `ProductTermsValidator`: Requires `termId` or `termGroupId` on `BaseProductRequest`
- `ProductService.create`: Validates Terms/TermsGroup and links PriceComponents after ProductDetails creation
- `TermsService.createTerms`: Terms entity has no FK to Product (standalone)
- `PriceComponentService.create`: PriceComponent entity has no FK to Product (standalone)
