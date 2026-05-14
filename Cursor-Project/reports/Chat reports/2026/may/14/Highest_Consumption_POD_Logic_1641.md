# Highest Consumption POD Logic — Request For Disconnection

**Date:** 2026-05-14  
**Requested by:** Nika Kevlishvili  

---

## Summary

This report documents how the **"Point of delivery with highest consumption" (YES/NO)** column is determined and how the **"Select POD's with highest consumption"** button works on the **Request For Disconnection** page.

---

## 1. How YES/NO Is Determined

The YES/NO value is calculated on the **backend** using the following logic:

1. **For each billing group**, the system looks at all PODs and compares their **`estimated_monthly_avg_consumption`** value (estimated monthly average consumption — a field manually entered on the POD entity when creating/editing a POD).

2. The POD with the **highest** `estimated_monthly_avg_consumption` within its **billing group** gets marked as **YES**.

3. **Tie-break rule:** If two or more PODs in the same billing group have the same consumption value, the one with the **more recent creation date** wins.

4. All other PODs in the same billing group are marked as **NO**.

5. If a POD participates in **multiple billing groups** and is the highest consumer in **any** of them, it gets **YES**.

> **Note:** This is NOT based on real-time invoice consumption data. It uses the `estimated_monthly_avg_consumption` field from `pod.pod_details`, which is manually entered by the user when creating or editing a POD (form field: "Monthly Average Consumption"). Required field, integer type, minimum value 1.

---

## 2. "Select POD's with highest consumption" Button

When the user clicks this button:

- The system **auto-selects (checks)** all PODs that have **YES** in the "highest consumption" column
- All other PODs (with **NO**) remain **unchecked**
- The button works as a **toggle** — clicking again deselects all
- It is **mutually exclusive** with the "Select All PODs" button — activating one deactivates the other
- After auto-selection, the user can still **manually check/uncheck** individual PODs
- The button is **disabled** when the request is in preview or executed status

---

## 3. Saving Behavior

When saving a request with "highest consumption" selection mode:

- A header-level flag `podWithHighestConsumption = true` is saved on the request
- Any manually **excluded** PODs are tracked via `excludePodIds`
- Any manually **added** non-highest PODs are sent in the `pods` array
- **Validation:** If no PODs are explicitly listed, either "Select All" or "Highest Consumption" mode must be active

---

## 4. Persistence (DB tables)

| Table | Column | Purpose |
|---|---|---|
| `receivable.power_supply_disconnection_requests` | `pods_with_highest_consumption` | Was "highest consumption" selection mode used |
| `receivable.power_supply_disconnection_request_results` | `is_highest_consumption` | Per-POD YES/NO snapshot at execution time |
| `pod.pod_details` | `estimated_monthly_avg_consumption` | Source field — estimated monthly average consumption |

---

## 5. Confluence References

- [Request for disconnection of the power supply - Create (Page 72155868)](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/72155868/Request+for+disconnection+of+the+power+supply+-+Create)
- [Phase 2 - Request for disconnection of the power supply - Create (Page 585697986)](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/585697986)
- [Request for disconnection liabilities selecting logic (Page 429031426)](https://asterbit.atlassian.net/wiki/spaces/Phoenix/pages/429031426) — covers charge-fee liability types, not the highest consumption metric

---

## 6. Confluence vs Code Discrepancy

The Confluence specification states that highest consumption is determined by **"total volumes"** from the invoice's detailed data tab. However, the **actual code implementation** uses `pod_details.estimated_monthly_avg_consumption` — a manually entered field on the POD. Code is the primary source of truth.

---

**Source:** Phoenix codebase analysis (backend: `DisconnectionPowerSupplyRequestRepository.java`, `DisconnectionPowerSupplyRequestsService.java`; frontend: `pod.component.ts`, `create-edit.component.ts`)

**Agents involved:** PhoenixExpert
