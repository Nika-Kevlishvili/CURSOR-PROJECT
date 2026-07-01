# Backend Architecture Knowledge Base

## API Overview
- **Title**: OpenAPI definition
- **Version**: v0
- **Base URL**: http://10.236.20.11:8091
- **Total Endpoints**: 1,149
- **Total Controllers**: 180
- **Total Schemas/Models**: 1,453
- **Total Domains**: 18

## Major Domains

### 1. Customer Domain (77 endpoints, 62 models)
**Controllers:**
- `customer-controller` (27 endpoints)
- `unwanted-customer-controller` (6 endpoints)
- `customer-liability-controller` (11 endpoints)
- `customer-assessment-controller` (12 endpoints)
- `customer-mass-import-controller` (1 endpoint)
- `related-customer-controller` (1 endpoint)
- `customer-owner-controller` (1 endpoint)
- `customer-indicators-controller` (1 endpoint)
- `customer-communications-controller` (2 endpoints)
- `customer-assessment-type-controller` (2 endpoints)
- `customer-assessment-criteria-controller` (4 endpoints)
- `unwanted-customer-reason-controller` (4 endpoints)
- `missing-customer-controller` (5 endpoints)

**Key Models:**
- `CreateCustomerRequest`, `EditCustomerRequest`
- `CustomerResponse`, `PageCustomerListingResponse`
- `CustomerLiabilityRequest`, `CustomerLiabilityResponse`
- `CustomerAssessmentBaseRequest`
- `UnwantedCustomerEditRequest`, `UnwantedCustomerResponse`

**Business Logic:**
- Customer CRUD operations
- Customer relationship management (related customers, owners)
- Customer liability and receivables tracking
- Customer assessment and risk evaluation
- Unwanted customer management
- Mass customer import functionality

### 2. Billing Domain (70 endpoints, 100+ models)
**Controllers:**
- `billing-run-controller` (39 endpoints) - **LARGEST CONTROLLER**
- `billing-group-controller` (6 endpoints)
- `billing-by-scales-controller` (5 endpoints)
- `billing-by-profile-controller` (8 endpoints)
- `invoice-controller` (9 endpoints)
- `invoice-cancellation-controller` (3 endpoints)

**Key Models:**
- Billing run creation, update, deletion
- Manual invoice import
- Invoice generation and cancellation
- Billing by scales and profiles
- Billing group management

**Business Logic:**
- Automated billing run processing
- Invoice generation and management
- Billing calculations (scales, profiles)
- Invoice cancellation workflows
- Manual invoice import

### 3. Contract Domain (60+ endpoints)
**Controllers:**
- `product-contract-controller` (24 endpoints)
- `service-contract-controller` (20 endpoints)
- `express-contract-controller` (7 endpoints)
- `contract-pod-controller` (1 endpoint)
- `contract-version-types-controller` (4 endpoints)
- `contract-and-order-number-sequence-test-controller` (2 endpoints)

**Key Models:**
- Product contract CRUD
- Service contract management
- Contract versioning
- POD (Point of Delivery) associations
- Express contract creation

**Business Logic:**
- Contract lifecycle management
- Contract versioning
- POD-contract relationships
- Express contract workflows

### 4. Pricing Domain (40+ endpoints)
**Controllers:**
- `price-component-controller` (13 endpoints)
- `price-parameter-controller` (9 endpoints)
- `price-component-group-controller` (6 endpoints)
- `price-component-value-type-controller` (4 endpoints)
- `price-component-price-type-controller` (4 endpoints)
- `electricity-price-type-controller` (4 endpoints)

**Key Models:**
- Price component definitions
- Price parameters
- Price component groups
- Value and price types

**Business Logic:**
- Price component configuration
- Pricing parameter management
- Price type definitions
- Price calculations

### 5. Product Domain (38 endpoints)
**Controllers:**
- `products-controller` (26 endpoints)
- `product-types-controller` (4 endpoints)
- `product-groups-controller` (4 endpoints)
- `test-product-contract-termination-controller` (9 endpoints)

**Key Models:**
- Product CRUD operations
- Product types and groups
- Product contract termination

**Business Logic:**
- Product catalog management
- Product type classification
- Product grouping
- Product contract termination workflows

### 6. Service Domain (80+ endpoints)
**Controllers:**
- `service-controller` (28 endpoints)
- `service-contract-controller` (20 endpoints)
- `service-order-controller` (20 endpoints)
- `service-type-controller` (4 endpoints)
- `service-unit-controller` (4 endpoints)
- `service-groups-controller` (4 endpoints)
- `test-service-contract-termination-controller` (5 endpoints)

**Key Models:**
- Service definitions
- Service contracts
- Service orders
- Service types, units, groups

**Business Logic:**
- Service catalog management
- Service contract lifecycle
- Service order processing
- Service termination workflows

### 7. Payment Domain (42 endpoints)
**Controllers:**
- `payment-controller` (11 endpoints)
- `payment-package-controller` (7 endpoints)
- `deposit-controller` (7 endpoints)
- `interim-advance-payment-controller` (8 endpoints)
- `advanced-payment-group-controller` (6 endpoints)
- `late-payment-fine-controller` (9 endpoints)

**Key Models:**
- Payment processing
- Payment packages
- Deposit management
- Interim advance payments
- Late payment fines

**Business Logic:**
- Payment processing and tracking
- Deposit management
- Advance payment handling
- Late payment fine calculation

### 8. Communication Domain (47 endpoints)
**Controllers:**
- `sms-communication-controller` (19 endpoints)
- `email-communication-controller` (23 endpoints)
- `email-mailboxes-controller` (4 endpoints)
- `email-sending-testing-controller` (1 endpoint)
- `customer-communications-controller` (2 endpoints)

**Key Models:**
- SMS communication templates and sending
- Email communication management
- Email mailbox configuration
- Mass communication capabilities

**Business Logic:**
- SMS and email communication workflows
- Mass communication campaigns
- Communication template management
- Email mailbox configuration

### 9. POD (Point of Delivery) Domain (17 endpoints)
**Controllers:**
- `point-of-delivery-controller` (8 endpoints)
- `pod-additional-params-controller` (5 endpoints)
- `test-controler-pod` (4 endpoints)

**Key Models:**
- POD CRUD operations
- POD additional parameters
- POD testing

**Business Logic:**
- Point of delivery management
- POD parameter configuration
- POD testing workflows

### 10. Termination Domain (30+ endpoints)
**Controllers:**
- `terminations-controller` (7 endpoints)
- `termination-group-controller` (6 endpoints)
- `test-product-contract-termination-controller` (9 endpoints)
- `test-service-contract-termination-controller` (5 endpoints)

**Key Models:**
- Termination requests
- Termination groups
- Contract termination workflows

**Business Logic:**
- Contract termination processing
- Termination group management
- Termination testing

### 11. Disconnection Domain (70+ endpoints)
**Controllers:**
- `disconnection-power-supply-controller` (15 endpoints)
- `disconnection-power-supply-requests-controller` (16 endpoints)
- `reconnection-of-the-power-supply-controller` (14 endpoints)
- `cancellation-of-disconnection-of-the-power-supply-controller` (13 endpoints)
- `power-supply-disconnection-reminder-controller` (12 endpoints)

**Key Models:**
- Disconnection requests
- Reconnection workflows
- Cancellation of disconnection
- Disconnection reminders

**Business Logic:**
- Power supply disconnection workflows
- Reconnection processing
- Disconnection cancellation
- Reminder management

### 12. Goods Domain (43 endpoints)
**Controllers:**
- `goods-order-controller` (25 endpoints)
- `goods-controller` (6 endpoints)
- `goods-units-controller` (4 endpoints)
- `goods-suppliers-controller` (4 endpoints)
- `goods-groups-controller` (4 endpoints)

**Key Models:**
- Goods catalog
- Goods orders
- Goods units, suppliers, groups

**Business Logic:**
- Goods catalog management
- Goods order processing
- Supplier and unit management

### 13. Task Domain (14 endpoints)
**Controllers:**
- `task-controller` (10 endpoints)
- `task-type-controller` (4 endpoints)

**Key Models:**
- Task management
- Task types

**Business Logic:**
- Task creation and assignment
- Task type configuration

### 14. Action Domain (28 endpoints)
**Controllers:**
- `action-controller` (16 endpoints)
- `action-type-controller` (4 endpoints)
- `action-penalty-calculation-test-controller` (8 endpoints)

**Key Models:**
- Action definitions
- Action types
- Penalty calculations

**Business Logic:**
- Action processing
- Action type management
- Penalty calculation testing

### 15. Reminder Domain (21 endpoints)
**Controllers:**
- `reminder-controller` (9 endpoints)
- `power-supply-disconnection-reminder-controller` (12 endpoints)

**Key Models:**
- Reminder creation and management
- Disconnection reminders

**Business Logic:**
- Reminder scheduling
- Disconnection reminder workflows

### 16. Collection Domain (17 endpoints)
**Controllers:**
- `collection-channel-controller` (13 endpoints)
- `collection-partner-controller` (4 endpoints)

**Key Models:**
- Collection channel configuration
- Collection partner management

**Business Logic:**
- Collection channel setup
- Partner management

### 17. Master Data Domain (423 endpoints, 331 models) - **LARGEST DOMAIN**
**Controllers:** 93 controllers covering:
- Geographic data (country, region, district, municipality, populated-place, residential-area, street, zip-code)
- Financial data (bank, currency, vat-rate, interest-rate, base-interest-rate)
- Reference data (title, user-type, legal-form, ownership-form, prefix)
- System configuration (platform, calendar, accounting-period, expiration-period, process-periodicity)
- Business entities (grid-operator, balancing-group-coordinators, economic-branch)
- Technical data (nomenclature, measurement-type, meter, representation-method, scales, profile)
- Business rules (segment, sales-channel, sales-area, risk-assessment, credit-rating)
- Status/reason codes (blocking-reason, deactivation-purpose, contact-purpose, topic-of-communication, unwanted-customer-reason, reason-for-disconnection, reason-for-cancellation)
- And many more...

**Business Logic:**
- Comprehensive master data management
- Reference data maintenance
- System configuration
- Business rule definitions

### 18. Other/Utility Domain
**Controllers:**
- `x-energie-controller` (19 endpoints)
- `test-controller` (7 endpoints)
- `manual-liability-offsetting-controller` (7 endpoints)
- `lock-controller` (3 endpoints)
- `notification-controller` (3 endpoints)
- `documents-controller` (3 endpoints)
- Various test and utility controllers

## Common Patterns

### CRUD Pattern
Most controllers follow standard CRUD operations:
- `GET /{resource}` - List/Filter
- `GET /{resource}/{id}` - View single
- `POST /{resource}` - Create
- `PUT /{resource}/{id}` - Update
- `DELETE /{resource}/{id}` - Delete (where applicable)

### Request/Response Naming Convention
- Request models: `{Entity}Request`, `Create{Entity}Request`, `Edit{Entity}Request`, `{Entity}FilterRequest`
- Response models: `{Entity}Response`, `Page{Entity}Response`, `{Entity}ListingResponse`
- DTOs follow clear naming patterns

### Validation Patterns
- Required fields defined in schema `required` arrays
- Format validation (email, date, etc.)
- Min/max length constraints
- Pattern matching for strings
- Min/max values for numbers

### Dependency Patterns
- Models reference other models via `$ref`
- Controllers use shared DTOs
- Master data referenced across domains
- Customer, Contract, POD relationships are central

## Key Business Flows

### Customer Onboarding Flow
1. Create Customer (`customer-controller` POST)
2. Create POD (`point-of-delivery-controller` POST)
3. Create Product Contract (`product-contract-controller` POST)
4. Create Service Contract (`service-contract-controller` POST)
5. Run Billing (`billing-run-controller` POST)

### Billing Flow
1. Create/Configure Billing Run (`billing-run-controller`)
2. Process billing calculations
3. Generate Invoices (`invoice-controller`)
4. Handle Payments (`payment-controller`)
5. Manage Receivables

### Contract Termination Flow
1. Create Termination Request (`terminations-controller`)
2. Process Termination (`test-product-contract-termination-controller` or `test-service-contract-termination-controller`)
3. Handle Disconnection if needed (`disconnection-power-supply-controller`)
4. Update Customer Status

### Disconnection Flow
1. Create Disconnection Request (`disconnection-power-supply-requests-controller`)
2. Send Reminders (`power-supply-disconnection-reminder-controller`)
3. Execute Disconnection (`disconnection-power-supply-controller`)
4. Handle Reconnection (`reconnection-of-the-power-supply-controller`)
5. Cancel if needed (`cancellation-of-disconnection-of-the-power-supply-controller`)

## Testing Considerations

### High-Priority Test Areas
1. **Billing Run Controller** (39 endpoints) - Critical business logic
2. **Customer Controller** (27 endpoints) - Core entity
3. **Product Contract Controller** (24 endpoints) - Key business process
4. **Service Contract Controller** (20 endpoints) - Key business process
5. **Email Communication Controller** (23 endpoints) - Communication workflows
6. **SMS Communication Controller** (19 endpoints) - Communication workflows

### Test Data Requirements
- Master data must be set up first (countries, regions, etc.)
- Customer creation requires valid master data references
- Contracts require customers and PODs
- Billing runs require contracts

### Endpoint Dependencies
- Customer → POD → Contract → Billing
- Master data referenced throughout
- Communication requires customer data
- Payments require invoices/billing

## Architecture Files Generated
- `backend-architecture.json` - Complete architectural model
- `domain-summary.json` - Domain-level summary
- `endpoints-by-tag.json` - Endpoints grouped by controller
- `all-endpoints.json` - All endpoints with metadata
- `schemas-list.json` - All schema/model names
- `architecture-summary.json` - High-level statistics

## Usage for Test Generation
This knowledge base enables:
1. **Test Case Generation**: Use domain/controller structure to generate comprehensive test suites
2. **API Testing Automation**: Map endpoints to test scenarios
3. **Flow Diagrams**: Visualize business flows based on controller relationships
4. **Dependency Mapping**: Understand data dependencies for test setup
5. **Playwright/Postman Flows**: Generate API test collections based on business flows

