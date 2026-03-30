# Bug Validation Report: PDT-2708

## 1. Confluence Validation (Primary Source)

**Bug summary from Jira:** "The system doesn't restrict creation of the Correction data by scales with a difference in the header period with the original data."

### Sources reviewed

1. **Correction flow for product/service contracts**  
   - Page ID: `256114692`  
   - URL: `https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/256114692/Correction+flow+for+product+service+contracts`
2. **Invoice correction - process**  
   - Page ID: `163545113`  
   - URL: `https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/163545113/Invoice+correction+-+process`
3. **Open topics and answers**  
   - Page ID: `17596485`  
   - URL: `https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/17596485/Open+topics+and+answers`
4. **Create Billing data by scales**  
   - Page ID: `11108559`  
   - URL: `https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/11108559/Create+Billing+data+by+scales`

### Findings

- `Invoice correction - process` and `Correction flow for product/service contracts` describe correction as being tied to old invoice data and period context (old invoice billing data period / matching logic in correction recalculation).
- `Open topics and answers` includes guidance for exact range matching to original billing data header context in scale/profile selection logic.
- `Create Billing data by scales` contains a rule that overlap validation is skipped for correction/override records, but this is a different validation (overlap vs original-data-period consistency) and does not explicitly authorize arbitrary header mismatch against original correction base.

### Confluence validation status

**Status: Partially correct (leaning correct for bug claim).**  
Confluence documents indicate correction flows are expected to remain anchored to original invoice/billing-data period context. The bug claim (no restriction on header-period mismatch with original data) is consistent with a likely requirement gap/violation, although wording is distributed across multiple docs and not stated in one single acceptance criterion sentence.

---

## 2. Codebase Validation (Read-only)

### Scope actually available in workspace

- No Phoenix backend source tree was present in this workspace snapshot (no `Cursor-Project/Phoenix/**` files found), so direct backend validation of period-mismatch checks could not be performed.
- Available executable code was EnergoTS tests.

### Relevant code references

1. `Cursor-Project/EnergoTS/tests/billing/correction/correction(volume change).spec.ts`  
   - Lines inspected: roughly `103-106` (correction billing run call) and surrounding flow.
   - Observation: test performs a happy-path correction run and asserts generic successful responses (`CheckResponse()`), but does not validate rejection/error for mismatched correction header period.

2. `Cursor-Project/EnergoTS/tests/billing/forVolumes/forVolumes.spec.ts`  
   - Lines inspected: around `713-718`, `861-866` and nearby billing steps.
   - Observation: scale data creation is tested for successful creation and invoice calculations; there is no explicit negative validation for "correction scale header period differs from original data period."

### Code validation status

**Status: Does not satisfy bug-report case (within available code scope).**  
In available test code, no enforcement/assertion exists for the reported mismatch restriction. Because backend sources are absent locally, final backend-level confirmation is limited; however, existing automation coverage does not demonstrate the required restriction behavior.

---

## 3. Conclusion

- **Confluence validation:** Partially correct, with strong indication that correction logic should stay aligned to original invoice/original data period context.
- **Code validation:** Available code does not demonstrate or test the required restriction; backend verification is blocked by missing backend source in workspace.
- **Final bug validity:** **VALID (with medium confidence due backend-source visibility gap).**

### Rationale

The primary source (Confluence) indicates correction should be period-context-bound to original data/invoice logic. The available code artifacts do not show enforcement or test coverage for preventing mismatched correction header periods. This supports the reported defect as valid.

