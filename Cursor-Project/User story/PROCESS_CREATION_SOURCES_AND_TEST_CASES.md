# Process Creation Sources and Test Cases

## Overview
This document lists all Process types that can be created in the system, their creation sources, and test case examples for each.

## Process Types Enum
All Process types are defined in `ProcessType.java`:
- `PROCESS_CUSTOMER_MASS_IMPORT`
- `PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT`
- `PROCESS_POD_MASS_IMPORT`
- `PROCESS_PRODUCT_MASS_IMPORT`
- `PROCESS_SERVICE_MASS_IMPORT`
- `PROCESS_METER_MASS_IMPORT`
- `PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT`
- `PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT`
- `PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT`
- `PRODUCT_CONTRACT_MASS_IMPORT`
- `PROCESS_SERVICE_CONTRACT_MASS_IMPORT`
- `PROCESS_INVOICE_CANCELLATION`
- `X_ENERGIE_EXCEPTION_REPORT`
- `PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT`
- `PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT`
- `PROCESS_PAYMENT_MASS_IMPORT`
- `PROCESS_REMINDER`
- `GOVERNMENT_COMPENSATION_MASS_IMPORT`

---

## 1. Mass Import Processes (via REST API)

### 1.1. PROCESS_CUSTOMER_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `CustomerMassImportService`
**Endpoint:** `POST /mass-import/customers/files/upload`
**DomainType:** `CUSTOMERS`

**Test Case:**
```http
POST /mass-import/customers/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with customer data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/customers/template/download`
2. Fill template with customer data (name, identifier, address, etc.)
3. Upload file via POST endpoint
4. Verify Process created in `process_management.process` table
5. Check Process status is `NOT_STARTED`
6. Verify Process type is `PROCESS_CUSTOMER_MASS_IMPORT`

---

### 1.2. PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `UnwantedCustomerMassImportService`
**Endpoint:** `POST /mass-import/unwanted-customers/files/upload`
**DomainType:** `UNWANTED_CUSTOMERS`

**Test Case:**
```http
POST /mass-import/unwanted-customers/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with unwanted customer data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/unwanted-customers/template/download`
2. Fill template with unwanted customer identifiers
3. Upload file
4. Verify Process created with type `PROCESS_UNWANTED_CUSTOMER_MASS_IMPORT`

---

### 1.3. PROCESS_POD_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `PodMassImportService`
**Endpoint:** `POST /mass-import/pods/files/upload`
**DomainType:** `PODS`

**Test Case:**
```http
POST /mass-import/pods/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with POD data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/pods/template/download`
2. Fill template with POD identifiers and details
3. Upload file
4. Verify Process created with type `PROCESS_POD_MASS_IMPORT`

---

### 1.4. PROCESS_PRODUCT_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `ProductMassImportService`
**Endpoint:** `POST /mass-import/products/files/upload`
**DomainType:** `PRODUCTS`

**Test Case:**
```http
POST /mass-import/products/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with product data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/products/template/download`
2. Fill template with product information
3. Upload file
4. Verify Process created with type `PROCESS_PRODUCT_MASS_IMPORT`

---

### 1.5. PROCESS_SERVICE_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `ServiceMassImportService`
**Endpoint:** `POST /mass-import/services/files/upload`
**DomainType:** `SERVICES`

**Test Case:**
```http
POST /mass-import/services/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with service data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/services/template/download`
2. Fill template with service information
3. Upload file
4. Verify Process created with type `PROCESS_SERVICE_MASS_IMPORT`

---

### 1.6. PROCESS_METER_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `MeterMassImportService`
**Endpoint:** `POST /mass-import/meters/files/upload`
**DomainType:** `METERS`

**Test Case:**
```http
POST /mass-import/meters/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with meter data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/meters/template/download`
2. Fill template with meter serial numbers and details
3. Upload file
4. Verify Process created with type `PROCESS_METER_MASS_IMPORT`

---

### 1.7. PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `SupplyAutomaticActivationMassImportService`
**Endpoint:** `POST /mass-import/supply-automatic-activations/files/upload`
**DomainType:** `SUPPLY_AUTOMATIC_ACTIVATIONS`

**Test Case:**
```http
POST /mass-import/supply-automatic-activations/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with supply activation data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/supply-automatic-activations/template/download`
2. Fill template with POD identifiers and activation dates
3. Upload file
4. Verify Process created with type `PROCESS_SUPPLY_AUTOMATIC_ACTIVATION_MASS_IMPORT`

---

### 1.8. PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `SupplyAutomaticDeactivationMassImportService`
**Endpoint:** `POST /mass-import/supply-automatic-deactivations/files/upload`
**DomainType:** `SUPPLY_AUTOMATIC_DEACTIVATIONS`

**Test Case:**
```http
POST /mass-import/supply-automatic-deactivations/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with supply deactivation data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/supply-automatic-deactivations/template/download`
2. Fill template with POD identifiers and deactivation dates
3. Upload file
4. Verify Process created with type `PROCESS_SUPPLY_AUTOMATIC_DEACTIVATION_MASS_IMPORT`

---

### 1.9. PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `SupplyActionDeactivationMassImportService`
**Endpoint:** `POST /mass-import/supply-action-deactivations/files/upload`
**DomainType:** `SUPPLY_ACTION_DEACTIVATIONS`

**Test Case:**
```http
POST /mass-import/supply-action-deactivations/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with supply action deactivation data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/supply-action-deactivations/template/download`
2. Fill template with POD identifiers and action deactivation details
3. Upload file
4. Verify Process created with type `PROCESS_SUPPLY_ACTION_DEACTIVATION_MASS_IMPORT`

---

### 1.10. PRODUCT_CONTRACT_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `ProductContractMassImportService`
**Endpoint:** `POST /mass-import/product-contracts/files/upload`
**DomainType:** `PRODUCT_CONTRACTS`

**Test Case:**
```http
POST /mass-import/product-contracts/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with product contract data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/product-contracts/template/download`
2. Fill template with product contract details (customer, POD, terms, etc.)
3. Upload file
4. Verify Process created with type `PRODUCT_CONTRACT_MASS_IMPORT`

---

### 1.11. PROCESS_SERVICE_CONTRACT_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `ServiceContractMassImportService`
**Endpoint:** `POST /mass-import/service-contract/files/upload`
**DomainType:** `SERVICE_CONTRACT`

**Test Case:**
```http
POST /mass-import/service-contract/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with service contract data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/service-contract/template/download`
2. Fill template with service contract details
3. Upload file
4. Verify Process created with type `PROCESS_SERVICE_CONTRACT_MASS_IMPORT`

---

### 1.12. PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `CustomerReceivableMassImportService`
**Endpoint:** `POST /mass-import/customer-receivable/files/upload`
**DomainType:** `CUSTOMER_RECEIVABLE`

**Test Case:**
```http
POST /mass-import/customer-receivable/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with customer receivable data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/customer-receivable/template/download`
2. Fill template with customer receivable amounts and details
3. Upload file
4. Verify Process created with type `PROCESS_CUSTOMER_RECEIVABLE_MASS_IMPORT`

---

### 1.13. PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `CustomerLiabilityMassImportService`
**Endpoint:** `POST /mass-import/customer-liability/files/upload`
**DomainType:** `CUSTOMER_LIABILITY`

**Test Case:**
```http
POST /mass-import/customer-liability/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with customer liability data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/customer-liability/template/download`
2. Fill template with customer liability amounts and details
3. Upload file
4. Verify Process created with type `PROCESS_CUSTOMER_LIABILITY_MASS_IMPORT`

---

### 1.14. PROCESS_PAYMENT_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `PaymentMassImportService`
**Endpoint:** `POST /mass-import/payment/files/upload`
**DomainType:** `PAYMENT`

**Test Case:**
```http
POST /mass-import/payment/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Text file with payment data - bank format or payment partner format]
date: 2025-02-03 (required)
collectionChannelId: 1 (required)
```

**Steps:**
1. Download template: `GET /mass-import/payment/template/download`
2. Prepare payment file (text format, starts with `:25:` for bank format)
3. Upload file with date and collectionChannelId
4. Verify Process created with type `PROCESS_PAYMENT_MASS_IMPORT`
5. Verify PaymentPackage is created and linked to Process
6. Verify Process has collectionChannelId set

**Note:** Payment mass import uses text file format, not Excel.

---

### 1.15. GOVERNMENT_COMPENSATION_MASS_IMPORT
**Source:** `MassImportController.uploadFile()` → `CompensationMassImportService`
**Endpoint:** `POST /mass-import/government-compensation/files/upload`
**DomainType:** `GOVERNMENT_COMPENSATION`

**Test Case:**
```http
POST /mass-import/government-compensation/files/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with government compensation data]
date: 2025-02-03 (optional)
```

**Steps:**
1. Download template: `GET /mass-import/government-compensation/template/download`
2. Fill template with government compensation amounts and customer details
3. Upload file
4. Verify Process created with type `GOVERNMENT_COMPENSATION_MASS_IMPORT`

---

## 2. Invoice Cancellation Process

### 2.1. PROCESS_INVOICE_CANCELLATION
**Source:** `InvoiceCancellationController.createInvoiceCancellation()` → `InvoiceCancellationService.createInvoiceCancellation()`
**Endpoint:** `POST /invoice-cancellation`

**Test Case:**
```http
POST /invoice-cancellation
Content-Type: application/json
Authorization: Bearer {token}

{
  "taxEventDate": "2025-02-03",
  "templateId": 1,
  "invoices": "INV-001,INV-002",
  "fileId": null
}
```

**Steps:**
1. Create invoice cancellation request with invoice numbers
2. Optionally upload file with invoice numbers: `POST /invoice-cancellation/upload`
3. Create invoice cancellation via POST endpoint
4. Verify Process created with type `PROCESS_INVOICE_CANCELLATION`
5. Verify Process status is `NOT_STARTED`
6. Verify InvoiceCancellation entity is created and linked to Process
7. Verify Process is started automatically (RabbitMQ event published)

**Alternative Test Case (with file):**
```http
# Step 1: Upload file
POST /invoice-cancellation/upload
Content-Type: multipart/form-data
Authorization: Bearer {token}

file: [Excel file with invoice numbers and dates]

# Step 2: Create cancellation
POST /invoice-cancellation
Content-Type: application/json
Authorization: Bearer {token}

{
  "taxEventDate": "2025-02-03",
  "templateId": 1,
  "invoices": "INV-001",
  "fileId": {fileId from step 1}
}
```

---

## 3. Reminder Process

### 3.1. PROCESS_REMINDER
**Source:** `ReminderController.create()` → `ReminderService.create()` → `ReminderProcessService`
**Endpoint:** `POST /reminder`

**Test Case:**
```http
POST /reminder
Content-Type: application/json
Authorization: Bearer {token}

{
  "number": "REM-001",
  "currencyId": 1,
  "conditions": [
    {
      "field": "LIABILITY_AMOUNT",
      "operator": "GREATER_THAN",
      "value": "100"
    }
  ],
  "listOfCustomers": [1, 2, 3],
  "purposeOfTheContactId": 1,
  "documentTemplateId": 1,
  "emailTemplateId": 2,
  "smsTemplateId": 3,
  "excludeLiabilitiesByPrefixes": ["PREFIX1"],
  "onlyLiabilitiesWithPrefixes": ["PREFIX2"],
  "periodicityIds": [1]
}
```

**Steps:**
1. Create reminder via POST endpoint
2. Verify Reminder entity is created
3. Trigger reminder process execution (usually via scheduled job or manual trigger)
4. Verify Process created with type `PROCESS_REMINDER`
5. Verify Process is linked to Reminder
6. Verify Process processes customer liabilities based on conditions

**Note:** Reminder Process is typically created when reminder is executed, not immediately on creation.

---

## 4. X-Energie Exception Report Process

### 4.1. X_ENERGIE_EXCEPTION_REPORT
**Source:** Scheduled job or manual trigger → `XEnergieSchedulerErrorHandler` or `XEnergieDealCreationService`
**Endpoint:** Internal (scheduled job)

**Test Case:**
```sql
-- Manual creation via database (for testing)
INSERT INTO process_management.process (
    type, 
    status, 
    file_url, 
    user_permissions, 
    date_field,
    create_date
) VALUES (
    'X_ENERGIE_EXCEPTION_REPORT',
    'NOT_STARTED',
    '',
    'system.admin',
    CURRENT_DATE,
    CURRENT_TIMESTAMP
);
```

**Steps:**
1. Trigger X-Energie scheduled job (if enabled in dev/test profile)
2. Or manually create Process via database
3. Verify Process created with type `X_ENERGIE_EXCEPTION_REPORT`
4. Verify Process processes X-Energie exceptions
5. Verify Excel report is generated on completion

**Note:** This Process type is typically created by scheduled jobs, not via REST API.

---

## Common Test Verification Steps

For all Process types, verify:

1. **Process Creation:**
   ```sql
   SELECT * FROM process_management.process 
   WHERE type = '{PROCESS_TYPE}' 
   ORDER BY id DESC 
   LIMIT 1;
   ```

2. **Process Status:**
   - Initial status should be `NOT_STARTED`
   - After start: `IN_PROGRESS`
   - After completion: `COMPLETED`
   - On error: `FAILED`

3. **Process Name:**
   - Format: `{PROCESS_TYPE}_{PROCESS_ID}`
   - Example: `PROCESS_CUSTOMER_MASS_IMPORT_12345`

4. **Process File URL:**
   - For mass imports: Should contain uploaded file path
   - For invoice cancellation: May be empty
   - For reminder: May be empty

5. **Process Date:**
   - Should match provided date or current date

6. **Process Permissions:**
   - Should contain user permissions from permission context

7. **RabbitMQ Event:**
   - Verify `ProcessCreatedEvent` is published to RabbitMQ
   - Event should contain Process ID and EventType

8. **Process Start:**
   ```http
   PUT /process/{processId}/start
   Authorization: Bearer {token}
   ```

9. **Process Status Check:**
   ```http
   GET /process/{processId}
   Authorization: Bearer {token}
   ```

10. **Process Report Download:**
    ```http
    GET /process/{processId}/report/download?multiSheetExcelType={type}
    Authorization: Bearer {token}
    ```

---

## Test Data Requirements

### Prerequisites for Testing:
1. **User Authentication:**
   - Valid bearer token with required permissions
   - User must have permissions for specific Process type context

2. **Templates:**
   - Templates must exist in `process_management.template` table
   - Template ID must match EventType name

3. **Required Entities:**
   - For Payment Mass Import: CollectionChannel must exist
   - For Invoice Cancellation: Invoices must exist and be valid
   - For Reminder: Customers, Templates, and Conditions must be valid

4. **File Format:**
   - Excel files (.xlsx) for most mass imports
   - Text files (.txt) for Payment Mass Import
   - Files must match template structure exactly

---

## Error Scenarios to Test

1. **Invalid File Format:**
   - Upload non-Excel file for mass imports
   - Upload Excel file for payment import
   - Expected: `400 Bad Request` with error message

2. **Invalid File Content:**
   - Upload file with wrong headers
   - Upload file with missing required columns
   - Expected: `400 Bad Request` with validation errors

3. **Missing Required Parameters:**
   - Upload payment file without date
   - Upload payment file without collectionChannelId
   - Expected: `400 Bad Request`

4. **Invalid Permissions:**
   - Use token without required permissions
   - Expected: `403 Forbidden`

5. **Invalid Template:**
   - Use non-existent template ID
   - Expected: `404 Not Found`

6. **Process Creation Failure:**
   - Database connection issues
   - Expected: `500 Internal Server Error`

---

## Summary

All Process types are created through:
1. **REST API endpoints** (Mass Import, Invoice Cancellation, Reminder)
2. **Scheduled jobs** (X-Energie Exception Report)
3. **Internal services** (Reminder execution)

The main entry point for Process creation is `ProcessService.createProcess()`, which:
- Creates Process entity in database
- Sets Process name as `{PROCESS_TYPE}_{PROCESS_ID}`
- Publishes RabbitMQ event for Process handling
- Returns created Process entity

Test cases should verify:
- Process creation in database
- Process status transitions
- File upload and storage
- RabbitMQ event publication
- Process execution and completion
- Error handling and reporting
