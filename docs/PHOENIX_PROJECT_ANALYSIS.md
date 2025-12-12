# Phoenix Project Structure Analysis

## 📋 Overview

Phoenix არის მრავალმოდულური Java/Spring Boot პროექტი, რომელიც შედგება რამდენიმე დამოუკიდებელი მიკროსერვისისგან და საერთო ბიბლიოთეკისგან. პროექტი იყენებს Java 17, Spring Boot 3.x, Gradle build system-ს და PostgreSQL ბაზას.

Phoenix is a multi-modular Java/Spring Boot project consisting of several independent microservices and a shared library. The project uses Java 17, Spring Boot 3.x, Gradle build system, and PostgreSQL database.

---

## 🏗️ Project Modules

### 1. **phoenix-core-lib** (Core Library)
**ვერსია / Version:** `1.18.7-SNAPSHOT`  
**როლი / Role:** საერთო ბიბლიოთეკა, რომელიც შეიცავს ყველა საერთო ფუნქციონალს  
**Shared library containing all common functionality**

#### ძირითადი კომპონენტები / Main Components:
- **Models & Entities** (545+ entity files, 452+ enums, 673+ request models, 802+ response models)
- **Repositories** (506+ repository interfaces)
- **Services** (705+ service classes)
- **Controllers** - არ არის (library-ს არ აქვს controllers)
- **Billing Run Services** - billing run პროცესების სერვისები
- **APIS Integration** - APIS სერვისის ინტეგრაცია
- **Security** - JWT, permissions, ACL
- **Exceptions** - გლობალური exception handling
- **Config** - Spring configuration classes
- **Utils** - სხვადასხვა utility კლასები

#### ტექნოლოგიები / Technologies:
- Spring Boot 3.3.2
- Spring Data JPA
- Spring Security
- Spring Cloud Vault (secrets management)
- PostgreSQL, Oracle, SQL Server drivers
- Redis (caching)
- EhCache
- Jackson (JSON/XML processing)
- Aspose.Words (document generation)
- Templater (template engine)
- Apache POI (Excel processing)
- JWT (authentication)

#### დამოკიდებულებები / Dependencies:
- `bg.energo.common.*` - internal Energo common libraries
- `bg.energo.common.portal.api` - portal API integration
- `energo-common-acl` - access control
- `mass-comm-api` - mass communication API

---

### 2. **phoenix-core** (Main Application)
**ვერსია / Version:** `0.0.1-SNAPSHOT`  
**როლი / Role:** მთავარი Phoenix აპლიკაცია REST API endpoints-ებით  
**Main Phoenix application with REST API endpoints**

#### ძირითადი კომპონენტები / Main Components:

**Controllers (102+ files):**
- `billing/` - BillingRunController, InvoiceController, AccountingPeriodController
- `contract/` - Contract management (14 files)
- `customer/` - CustomerController, CustomerMassImportController, AccountManagerController
- `receivable/` - PaymentController, DepositController, ReschedulingController, LatePaymentFineController
- `pod/` - PointOfDeliveryController, MeterController, BillingByScalesController
- `product/` - Product management (14 files)
- `nomenclature/` - Nomenclature controllers (address, billing, contract, customer, pod, product, receivable)
- `template/` - TemplateController, QesDocumentController
- `signing/` - SocketController, QesSocketController (WebSocket)
- `signatus/` - SignatusAPIController, DocumentsController
- `xEnergie/` - XEnergieController
- `crm/` - EmailCommunicationController, SmsCommunicationController
- `task/` - TaskController
- `translation/` - TranslationController
- `testController/` - Test controllers for development

**Configuration:**
- `AsyncConfig` - async processing
- `CacheConfig` - caching configuration
- `JpaAuditingConfig` - JPA auditing
- `SchedulerConfig` - scheduled tasks
- `WebSocketConfig` - WebSocket configuration

#### ტექნოლოგიები / Technologies:
- Spring Boot 3.3.2
- Spring WebSocket
- Spring Cloud Vault
- Testcontainers (testing)
- WireMock (testing)

#### დამოკიდებულებები / Dependencies:
- `phoenix-core-lib:1.18.7-SNAPSHOT` - core library dependency

---

### 3. **phoenix-billing-run** (Billing Run Service)
**ვერსია / Version:** `0.0.1-SNAPSHOT`  
**როლი / Role:** დამოუკიდებელი სერვისი billing run პროცესების გასაშვებად  
**Independent service for running billing processes**

#### ძირითადი კომპონენტები / Main Components:

**Schedulers:**
- `BillingRunPeriodicityScheduler` - periodic billing runs
- `BillingRunStandardPreparationStateScheduler` - standard preparation
- `BillingRunStartAccountingScheduler` - accounting start
- `BillingRunStartGeneratingScheduler` - invoice generation start
- `OneTimeBillingRunScheduledService` - one-time billing runs

**Configuration:**
- `JpaConfig` - JPA configuration
- `SchedulerConfig` - scheduler configuration
- `DataSourceProxyConfiguration` - datasource proxy

#### ტექნოლოგიები / Technologies:
- Spring Boot 3.4.4
- Spring Cloud Vault
- Spring WebSocket
- Scheduled tasks

#### დამოკიდებულებები / Dependencies:
- `phoenix-core-lib:1.17.35-SNAPSHOT` - core library dependency

---

### 4. **phoenix-payment-api** (Payment API Service)
**ვერსია / Version:** `0.0.1-SNAPSHOT`  
**როლი / Role:** დამოუკიდებელი სერვისი გადახდების API-სთვის (EPay ინტეგრაცია)  
**Independent service for payment API (EPay integration)**

#### ძირითადი კომპონენტები / Main Components:

**Controllers:**
- `CustomerController` - customer management
- `EPayController` - EPay payment processing

**Services:**
- `CustomerService` - customer operations
- `EPayService` - EPay integration

**Models:**
- `Customer`, `CustomerDetails` - customer entities
- `EPayInvoice`, `LiabilityDetails` - payment entities
- `InitPayRequest`, `ConfirmPayRequest` - request models
- `InitPayResponse`, `ConfirmPayResponse` - response models
- Enums: `CustomerStatus`, `CustomerType`, `EPayStatusCode`, `RequestType`

**Utils:**
- `EPBFunctionUtils`, `EPBSignatureUtils`, `EPBStringUtils` - EPB integration utilities

#### ტექნოლოგიები / Technologies:
- Spring Boot 3.3.2
- Spring Security
- Spring Data JPA
- Spring Cloud Vault
- SpringDoc OpenAPI (Swagger)
- Testcontainers (testing)
- EhCache

#### დამოკიდებულებები / Dependencies:
- PostgreSQL
- Jackson (XML/JSON)
- Hibernate Validator

---

### 5. **phoenix-migration** (Migration Tool)
**ვერსია / Version:** `0.0.1-SNAPSHOT`  
**როლი / Role:** მიგრაციის ინსტრუმენტი მონაცემების გადატანისთვის  
**Migration tool for data migration**

#### ძირითადი კომპონენტები / Main Components:

**Packages:**
- `customer/` - customer migration (31 files)
- `integration/` - integration migration (29 files)
- `config/` - configuration (2 files)
- `utils/` - `CyrillicTransliteration` utility

**Main Class:**
- `MigrationApplication` - Spring Boot application

#### ტექნოლოგიები / Technologies:
- Spring Boot 3.4.5
- Spring Cloud Vault
- Spring Data JPA
- Spring WebFlux (reactive)
- Spring Security
- Liquibase (database migrations)
- SpringDoc OpenAPI

#### დამოკიდებულებები / Dependencies:
- PostgreSQL
- Jackson
- Hibernate Validator
- EhCache
- Redis

---

### 6. **phoenix-api-gateway** (API Gateway)
**სტატუსი / Status:** 🚧 In Development / განვითარების პროცესში  
**როლი / Role:** API Gateway Phoenix მიკროსერვისებისთვის  
**API Gateway for Phoenix microservices**

**შენიშვნა / Note:** ამჟამად მხოლოდ README.md არსებობს, კოდი ჯერ არ არის დამზადებული  
**Currently only README.md exists, code is not yet implemented**

---

### 7. **phoenix-mass-import** (Mass Import)
**სტატუსი / Status:** 🚧 In Development / განვითარების პროცესში  
**როლი / Role:** მასიური იმპორტის ინსტრუმენტი  
**Mass import tool**

**შენიშვნა / Note:** ამჟამად მხოლოდ README.md არსებობს  
**Currently only README.md exists**

---

## 🔗 Module Dependencies

```
phoenix-core-lib (1.18.7-SNAPSHOT)
    ↑
    ├── phoenix-core (uses 1.18.7-SNAPSHOT)
    ├── phoenix-billing-run (uses 1.17.35-SNAPSHOT)
    ├── phoenix-payment-api (no direct dependency, but likely uses it)
    └── phoenix-migration (no direct dependency, but likely uses it)
```

---

## 🗄️ Database & Infrastructure

### Databases:
- **PostgreSQL** - primary database
- **Oracle** - secondary database (via JDBC)
- **SQL Server** - secondary database (via JDBC)

### Caching:
- **Redis** - distributed caching
- **EhCache** - local caching

### Secrets Management:
- **HashiCorp Vault** - via Spring Cloud Vault

### Message Queue:
- **RabbitMQ** - via Spring AMQP (in core-lib)

### External Integrations:
- **APIS** - customer identification service
- **EPB/EPay** - payment processing
- **Signatus** - document signing
- **xEnergie** - external energy system
- **Mass Communication API** - email/SMS

---

## 📦 Build & Deployment

### Build System:
- **Gradle** - all modules use Gradle wrapper
- **Java 17** - required version
- **Maven Repository** - internal Nexus (`nexus.domain.internal:8081`)

### CI/CD:
- **GitLab CI** - `.gitlab-ci.yml` files in modules
- **Pipelines:**
  - `dev-pipeline.yml`
  - `feature-pipeline.yml`
  - `test-pipeline.yml`
  - `prod-pipeline.yml` (phoenix-core only)

### Docker:
- `Dockerfile` - in phoenix-core, phoenix-billing-run
- `docker-compose.yml` - for local development

---

## 🧪 Testing

### Test Frameworks:
- **JUnit 5** - unit and integration tests
- **Testcontainers** - for database testing
- **WireMock** - for external service mocking
- **Spring Security Test** - for security testing

### Test Structure:
- `src/test/java/` - test classes
- `src/test/resources/` - test resources, SQL scripts, mock data

---

## 🔐 Security

### Authentication & Authorization:
- **JWT** (JSON Web Tokens) - authentication
- **Spring Security** - security framework
- **Permission System** - custom permission annotations (`@PermissionValidator`, `@PermissionMapping`)
- **ACL** - Access Control List via `energo-common-acl`

### Security Features:
- Entity locking (`@WithEntityNotLocked`, `@WithLockValid`)
- Permission context validation
- Role-based access control

---

## 📊 Key Features

### 1. **Billing System**
- Billing run processing
- Invoice generation
- Accounting periods
- Government compensation
- Price components and scales

### 2. **Customer Management**
- Customer CRUD operations
- Customer mass import
- Account managers
- Connected groups
- Customer indicators

### 3. **Contract Management**
- Service contracts
- Product contracts
- Contract versions
- Contract terms

### 4. **Receivables**
- Payments
- Deposits
- Late payment fines
- Rescheduling
- Disconnection power supply
- Collection channels

### 5. **Point of Delivery (POD)**
- POD management
- Meter management
- Billing by profiles
- Billing by scales
- Discounts

### 6. **Document Management**
- Template generation
- PDF generation (Aspose.Words)
- Document signing (Signatus, QES)
- Document archivation

### 7. **Communication**
- Email communication
- SMS communication
- WebSocket for real-time updates

### 8. **Nomenclature**
- Addresses
- Products
- Billing components
- Customer types
- Contract types
- And many more...

---

## 🛠️ Development Setup

### Prerequisites:
- Java 17+
- Gradle (wrapper included)
- PostgreSQL
- Redis (optional, for caching)
- Vault (for secrets)

### Build Commands:
```bash
# Build core library
cd Phoenix/phoenix-core-lib
./gradlew build publishToMavenLocal

# Build main application
cd Phoenix/phoenix-core
./gradlew build

# Build billing run service
cd Phoenix/phoenix-billing-run
./gradlew build

# Build payment API
cd Phoenix/phoenix-payment-api
./gradlew build

# Build migration tool
cd Phoenix/phoenix-migration
./gradlew build
```

### Configuration:
- `application.properties` - main configuration
- `bootstrap.properties` - Vault configuration
- Environment-specific: `application-{profile}.properties`
- Profiles: `local`, `dev`, `test`

---

## 📈 Project Statistics

### Code Volume:
- **phoenix-core-lib**: 4514+ Java files
- **phoenix-core**: 185+ Java files (main), 236+ test files
- **phoenix-payment-api**: 27+ Java files
- **phoenix-billing-run**: 10 Java files
- **phoenix-migration**: 62+ Java files

### Key Numbers:
- 545+ Entity classes
- 506+ Repository interfaces
- 705+ Service classes
- 102+ Controller classes
- 452+ Enum types
- 673+ Request models
- 802+ Response models

---

## 🎯 Architecture Patterns

### 1. **Layered Architecture**
- Controllers → Services → Repositories → Entities

### 2. **Dependency Injection**
- Spring's `@Autowired` / constructor injection
- `@RequiredArgsConstructor` (Lombok)

### 3. **Aspect-Oriented Programming**
- `@WithEntityNotLocked` - entity locking
- `@WithLockValid` - lock validation

### 4. **Event-Driven Architecture**
- Event system (`Event`, `EventFactory`, `EventType`)
- RabbitMQ integration

### 5. **Microservices Architecture**
- Separate services for different domains
- Shared core library

---

## 🔍 Key Observations

1. **Large Codebase**: Phoenix-core-lib არის ძალიან დიდი ბიბლიოთეკა (4500+ ფაილი), რაც მიუთითებს კომპლექსურ დომეინზე.

2. **Version Mismatch**: phoenix-billing-run იყენებს ძველ ვერსიას phoenix-core-lib-ის (1.17.35 vs 1.18.7), რაც შეიძლება გამოიწვიოს პრობლემები.

3. **Incomplete Modules**: phoenix-api-gateway და phoenix-mass-import არის მხოლოდ README-ებით, კოდი არ არის დამზადებული.

4. **Strong Testing**: ყველა მოდულს აქვს test structure და იყენებს Testcontainers-ს.

5. **Security Focus**: ძლიერი security infrastructure - JWT, permissions, ACL, entity locking.

6. **Document Generation**: Aspose.Words და Templater გამოიყენება document generation-ისთვის.

7. **Multiple Database Support**: PostgreSQL, Oracle, SQL Server support.

---

## 📝 Recommendations

1. **Version Alignment**: გააერთიანეთ phoenix-core-lib-ის ვერსიები ყველა მოდულში.

2. **API Gateway**: დაასრულეთ phoenix-api-gateway-ის დეველოპმენტი.

3. **Mass Import**: დაასრულეთ phoenix-mass-import-ის დეველოპმენტი.

4. **Documentation**: გააუმჯობესეთ README-ები თითოეულ მოდულში.

5. **Code Organization**: განიხილეთ phoenix-core-lib-ის დაყოფა უფრო მცირე მოდულებად.

---

**ბოლო განახლება / Last Updated**: 2025-01-14  
**ანალიზი შესრულებულია / Analysis performed by**: PhoenixExpert
