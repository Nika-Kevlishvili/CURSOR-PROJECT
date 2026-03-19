# POD update existing – Test cases overview (PHN-2160)

**Jira:** PHN-2160 (Phoenix)  
**Type:** Task  
**Summary:** This folder contains exhaustive test cases for updating an existing POD (Point of Delivery). Coverage includes API PUT update behaviour, UI edit flow, permissions and validation, locking/concurrency behaviour, and regression risks for list/filter views, `/pod/{identifier}/exists`, and contract–POD related flows.

---

## Files in this flow

| File | What it covers |
|------|----------------|
| `Comprehensive_POD_update_existing_PHN-2160.md` | Canonical exhaustive suite aligned to Confluence page `740229121`: Sales Portal trigger, name-only input (`podParameters.Name`), update in existing POD version only (no new version), plus route mismatch, lock/permission, over-posting, bound-object, and concurrency regressions. |
| `API_PUT_Update_existing_POD.md` | API-level behaviour for PUT update: happy path updates, idempotency, optimistic concurrency, field validation, uniqueness, and error handling. |
| `UI_Edit_flow_POD_update.md` | UI edit flow: edit form, save/cancel, dirty state, error display, list/detail refresh, and navigation behaviours. |
| `Permissions_and_validation_POD_update.md` | Permission/role matrix, forbidden updates, field-level restrictions, audit requirements, and security validation. |
| `Concurrency_and_locking_POD_update.md` | Concurrent updates, locking/ordering risks, conflict resolution, and stability under load. |
| `Regression_List_filters_exists_and_contract_POD_flows.md` | Regression suite: POD list and filters, `/pod/{identifier}/exists` behaviour, contract POD flows impact (create/update/attach/detach), and API consumers’ assumptions. |

---

## General notes (apply to all scenarios)

- **Terminology:** POD = Point of Delivery. “Identifier” refers to the public/business identifier used by external callers (e.g. `POD-123...`), not the internal numeric ID, unless explicitly stated in the ticket.
- **Expected invariants:** Updating an existing POD must not create a new POD record, must not break identifier-based lookups, and must preserve referential integrity for linked objects such as contract–POD associations.
- **Regression focus:** Any change in update logic must be validated against dependent flows: UI list/filter, `/pod/{identifier}/exists`, and contract–POD flows that rely on stable identifiers and consistent POD state.

---

## References

- **Jira:** PHN-2160 – Put: Update existing POD.

