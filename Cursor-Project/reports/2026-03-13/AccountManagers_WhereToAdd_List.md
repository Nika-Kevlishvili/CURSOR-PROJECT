# Where to Add Account Managers – Objects, Nomenclatures, and Entry Points

**Date:** 2026-03-13  
**Summary:** List of all places in Phoenix where Account Managers can be added, configured, or assigned.

---

## 1. Account Manager (person) – source of the list

- **Entity:** `AccountManager`  
- **Schema/table:** `customer.account_managers`  
- **How the list is filled:** **Not created in Phoenix.** Data is **synced from Portal every day at 00:00** (`CustomerAccountManagerService.updateAccountManagersFromPortal()`).  
- **API:** **Read-only** – `GET /account-managers` (filter by statuses).  
- **Conclusion:** You do **not** “add” Account Managers in Phoenix; they come from Portal. To have new Account Managers in the list, they must exist in Portal.

---

## 2. Account Manager Type (nomenclature) – add/edit in Phoenix

- **Entity:** `AccountManagerType`  
- **Schema/table:** `nomenclature.account_manager_types`  
- **Nomenclature key in UI/config:** `account-manager-type`  
- **API (master-data):**
  - `GET /account-manager-types` – list/filter  
  - `GET /account-manager-types/{id}` – get one  
  - **`POST /account-manager-types`** – **add** new type  
  - **`PUT /account-manager-types/{id}`** – **edit** existing type  
- **Request body (add/edit):** `AccountManagerTypeRequest` (e.g. name, status, defaultSelection).  
- **Conclusion:** **This is the nomenclature where you add new “types” of Account Managers** (e.g. Primary, Secondary) in Phoenix (backend/API or any admin UI that uses these endpoints).

---

## 3. Customer – assigning Account Managers to a customer

- **Entity:** `CustomerAccountManager`  
- **Schema/table:** `customer.customer_account_managers`  
- **Links:** `CustomerDetails` + `AccountManager` (person) + `AccountManagerType` (nomenclature).  
- **Where you “add” an Account Manager to a customer:**

| Entry point | How Account Manager is added |
|-------------|------------------------------|
| **Customer Create** | In create-customer flow: send `CreateCustomerAccountManagerRequest` (e.g. in `accountManagers` array) with `accountManagerId` and `accountManagerTypeId`. |
| **Customer Edit** | In edit-customer-details flow: send `EditCustomerAccountManagerRequest` (create/edit/delete assignments) with `accountManagerId`, `accountManagerTypeId`, and optional `id` for existing row. |
| **UI – Customer form** | Modal **Account Managers** (`AccountManagersComponent`): dropdowns use nomenclatures `account-managers` and `account-manager-types`; user picks person + type and saves. |
| **Mass Import (Excel)** | Excel columns: Account Manager identifier (username) and Account Manager Type (name). Mapped via `ExcelMapper` to `CreateCustomerAccountManagerRequest` (e.g. `addCustomerAccountManagerRequest`). |

- **Conclusion:** **Adding an Account Manager** in the sense of “assigning to a customer” is done on the **Customer** (create, edit, or mass import).

---

## 4. Nomenclature list (reference)

From `backend-architecture.json`, account-manager-related nomenclatures:

- `account-manager-type` – types (e.g. Primary, Secondary); **add/edit via API above.**  
- `account-manager` – list of people; **read-only from Portal**, no add in Phoenix.

---

## 5. Short checklist – “Where can I add Account Managers?”

| What you want to do | Where |
|----------------------|--------|
| Add a **new type** of Account Manager (nomenclature) | **Account Manager Type** – `POST /account-manager-types` or admin UI that uses it. |
| Add an **Account Manager (person)** to the system list | **Portal** – Phoenix only syncs the list; no add in Phoenix. |
| Assign an Account Manager **to a customer** | **Customer** – create customer, edit customer, or Customer mass import (Excel). |

---

## 6. Code references (read-only)

- **Entities:**  
  - `phoenix-core-lib/.../entity/customer/AccountManager.java`  
  - `phoenix-core-lib/.../entity/nomenclature/customer/AccountManagerType.java`  
  - `phoenix-core-lib/.../entity/customer/CustomerAccountManager.java`  
- **Service:** `phoenix-core-lib/.../service/customer/CustomerAccountManagerService.java` (create/edit customer account managers, sync from Portal).  
- **UI modal:** `phoenix-ui/.../modals/customer/account-managers/account-managers.component.ts` (and `.html`).  
- **API:** `AccountManagerController` (GET only), Account Manager Type controller (CRUD) – see `config/backend-architecture.json` for paths.
