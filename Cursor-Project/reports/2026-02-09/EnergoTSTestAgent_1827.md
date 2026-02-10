# EnergoTSTestAgent Report

**Date:** 2026-02-09  
**Time:** 18:27  
**Agent:** EnergoTSTestAgent  
**Task:** Create test for REG-1026

## Task Summary

Created a new test case for Jira ticket REG-1026: "For volumes - SLP With 2 Tariff and 2 Scale code - 15 minute price parameter"

## Jira Ticket Information

- **Ticket ID:** REG-1026
- **Title:** For volumes - SLP With 2 Tariff and 2 Scale code - 15 minute price parameter
- **Type:** Test Case
- **Status:** To Do
- **Priority:** High

## Test Created

### Test Details

- **Test Name:** `[REG-1026]: For volumes - SLP With 2 Tariff and 2 Scale code - 15 minute price parameter`
- **File:** `Cursor-Project/EnergoTS/tests/billing/forVolumes/SLP.spec.ts`
- **Domain:** Billing / For Volumes
- **Test Timeout:** 10 minutes

### Test Structure

The test follows the standard EnergoTS test pattern and includes the following steps:

1. **Generate Customer** - Creates a legal customer
2. **POD Create** - Creates SLP (Settlement Level Point) POD with ЕСО ЕАД grid operator
3. **Meters Create** - Creates meter with 2 tariff scales + 2 scale code scales (4 scales total)
4. **Generate Term** - Creates term for the contract
5. **Price Parameter 15 Minute** - Creates and uploads 15-minute price parameter
6. **Generate Price Component with Scales** - Creates price component using scale codes
7. **Generate Product** - Creates product
8. **Generate Contract** - Creates product contract
9. **Activate POD** - Activates POD for the contract
10. **Data by Scales** - Creates billing data with 2 tariff rows + 2 scale code rows (4 rows total)
11. **Billing Run** - Executes billing run
12. **Invoice Generation** - Waits for invoice generation
13. **Data by Profiles Check** - Validates that profiles were created

### Key Features

- **SLP POD Type:** Uses Settlement Level Point POD type
- **Multiple Scales:** Meter contains 4 scales (2 tariffs + 2 scale codes)
- **15-Minute Price Parameter:** Implements 15-minute interval price parameter
- **Scale-Based Pricing:** Price component uses scale codes for volume-based pricing
- **Complex Data Structure:** Creates billing data with 4 rows (2 tariff rows + 2 scale code rows)

### Test Implementation Details

- Uses existing payload generators and fixtures
- Follows EnergoTS test patterns from similar tests (REG-1008, REG-1030)
- Properly handles meter scales array with 4 scales
- Creates custom data by scales payload with 4 rows
- Includes proper validation and response checking

## Files Modified

- `Cursor-Project/EnergoTS/tests/billing/forVolumes/SLP.spec.ts` - Added new test case

## Compliance

- ✅ Follows Rule 0.8.1 - Test file modification allowed in EnergoTS/tests/ directory
- ✅ Uses exact Jira ticket title as test name
- ✅ Follows EnergoTS test patterns and conventions
- ✅ Includes proper test steps and validations
- ✅ Uses appropriate fixtures and payload generators

## Next Steps

The test is ready for execution. It can be run using:
```bash
npx playwright test --grep "REG-1026"
```

## Notes

- The test creates a meter with 4 scales (2 tariffs + 2 scale codes) as required
- The data by scales payload includes 4 rows: 2 tariff rows and 2 scale code rows
- The test uses 15-minute price parameter as specified in the ticket title
- All test steps follow the established patterns from similar volume tests
