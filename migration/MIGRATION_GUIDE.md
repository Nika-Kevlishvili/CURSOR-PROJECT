# გარემოს მიგრაციის გზამკვლევი / Environment Migration Guide

ეს გზამკვლევი დაგეხმარებათ გადაიტანოთ მთელი გარემო სხვა კომპიუტერზე ყველა კონფიგურაციით, ბმულებით და ინტეგრაციებით.

This guide will help you migrate your entire environment to another computer with all configurations, links, and integrations.

---

## 📋 შინაარსი / Table of Contents

1. [ზოგადი მიგრაცია / General Migration](#1-ზოგადი-მიგრაცია--general-migration)
2. [Git/GitHub/GitLab კონფიგურაცია](#2-gitgithubgitlab-კონფიგურაცია)
3. [Postman ინტეგრაცია](#3-postman-ინტეგრაცია)
4. [ბაზის კონფიგურაცია](#4-ბაზის-კონფიგურაცია)
5. [Confluence ინტეგრაცია](#5-confluence-ინტეგრაცია)
6. [Agents და Logic](#6-agents-და-logic)
7. [Python გარემო](#7-python-გარემო)
8. [Java/Gradle პროექტი](#8-javagradle-პროექტი)
9. [Environment Variables](#9-environment-variables)
10. [შემოწმების ჩამონათვალი](#10-შემოწმების-ჩამონათვალი)

---

## 1. ზოგადი მიგრაცია / General Migration

### 1.1 ფაილების კოპირება

```bash
# მთელი workspace-ის კოპირება
# Copy entire workspace

# Windows-ზე:
xcopy /E /I /H "%USERPROFILE%\Cursor" "D:\Backup\Cursor"

# ან PowerShell-ში:
Copy-Item -Path "$env:USERPROFILE\Cursor" -Destination "D:\Backup\Cursor" -Recurse -Force
```

### 1.2 სტრუქტურა

გადაიტანეთ შემდეგი დირექტორიები:
- `agents/` - ყველა agent-ი
- `config/` - კონფიგურაციის ფაილები
- `docs/` - დოკუმენტაცია
- `postman/` - Postman კოლექციები
- `phoenix-core-lib/` - Java პროექტი
- `examples/` - მაგალითები

---

## 2. Git/GitHub/GitLab კონფიგურაცია

### 2.1 Git კონფიგურაცია

**მიმდინარე კომპიუტერზე:**

```bash
# Git კონფიგურაციის ნახვა
git config --list --show-origin

# Git credentials-ის შენახვა
git config --global credential.helper store
```

**ახალ კომპიუტერზე:**

```bash
# Git კონფიგურაცია
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# GitLab/GitHub credentials
git config --global credential.helper store
```

### 2.2 GitLab ინტეგრაცია

**Environment Variables (ახალ კომპიუტერზე):**

```bash
# Windows PowerShell:
$env:GITLAB_URL="https://gitlab.com"
$env:GITLAB_TOKEN="your-gitlab-token"
$env:GITLAB_PROJECT_ID="12345678"

# ან Windows Environment Variables-ში:
# System Properties → Environment Variables → New
```

**GitLab Token-ის მიღება:**
1. გადადით GitLab → Settings → Access Tokens
2. შექმენით token `api` scope-ით
3. დააკოპირეთ token და გამოიყენეთ `GITLAB_TOKEN`-ად

### 2.3 GitHub ინტეგრაცია

```bash
# GitHub Personal Access Token
$env:GITHUB_TOKEN="your-github-token"

# GitHub Token-ის მიღება:
# GitHub → Settings → Developer settings → Personal access tokens → Generate new token
```

---

## 3. Postman ინტეგრაცია

### 3.1 Postman API Key

**მიმდინარე კომპიუტერზე - Key-ის პოვნა:**

```bash
# Environment variable-ში:
echo $env:POSTMAN_API_KEY

# ან Postman-ში:
# Settings → API Keys → Generate API Key
```

**ახალ კომპიუტერზე:**

```bash
# Windows PowerShell:
$env:POSTMAN_API_KEY="your-postman-api-key"
$env:POSTMAN_WORKSPACE_ID="your-workspace-id"

# ან Windows Environment Variables-ში დამატება
```

**Postman API Key-ის მიღება:**
1. გადადით: https://go.postman.co/settings/me/api-keys
2. დააჭირეთ "Generate API Key"
3. დააკოპირეთ key

**Workspace ID-ის პოვნა:**
1. Postman-ში გადადით თქვენს workspace-ზე
2. URL-ში: `https://app.getpostman.com/workspace/{workspace-id}/...`
3. ან API-ით:
```bash
curl -X GET https://api.getpostman.com/workspaces -H "X-Api-Key: your-api-key"
```

### 3.2 Postman Collections

**მიმდინარე კომპიუტერზე:**

```bash
# Collections-ის ექსპორტი Postman-იდან:
# Postman → Collections → Export → Export Collection
```

**ახალ კომპიუტერზე:**

1. Postman-ის ინსტალაცია
2. Collections-ის იმპორტი:
   - Postman → Import → File/URL
   - ან `postman/postman_collections/` დირექტორიიდან

**Collections-ის სია (29 კოლექცია):**
- 0----when environments are empty
- 0---Billing runs
- 0---Compensations
- 0---Disconnection
- 0---Download and Upload
- 0---Export liability in sheard folder
- 0---Log-In
- 0---Reminder
- 0---rescheduling
- 1---Customer
- 2---Point of Delivery
- 3---Customer Communication
- 4---Contracts and Orders
- 5---Product and Services
- 6---Energy Data
- 7---Billing
- 8---Receivables Management
- 9---Operations Management
- 9.1---Master Data
- 9.2---when envaronments is empty
- Collection for performance testing
- Collection for service order
- Deposit ooffsetting
- New Collection
- Online payment
- Signatus
- Topic of communication
- document generation
- for my

### 3.3 Postman Environments

**5 Environment:**
- DEV 2
- DEV
- Prod
- TEST
- Pre Prod

**მიგრაცია:**
1. Postman → Environments → Export
2. ახალ კომპიუტერზე → Import

---

## 4. ბაზის კონფიგურაცია

### 4.1 Database Connections

ბაზის კონფიგურაცია ინახება `phoenix-core-lib/src/main/resources/application.properties`-ში.

**მიმდინარე კომპიუტერზე:**

```bash
# application.properties-ის ნახვა
cat phoenix-core-lib/src/main/resources/application.properties
```

**ახალ კომპიუტერზე:**

1. დააკოპირეთ `application.properties`
2. განაახლეთ connection strings:
   - PostgreSQL connections
   - Oracle connections (xEnergie)
   - EnergoPro database
   - Redis connections

**Database Connection Strings (მაგალითები):**

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://host:port/database
spring.datasource.username=username
spring.datasource.password=password

# Oracle (xEnergie)
xEnergie.database.connection-string=jdbc:oracle:thin:@host:port:service
xEnergie.database.username=username
xEnergie.database.password=password

# EnergoPro
energopro.database.connection-string=jdbc:sqlserver://host:port;database=database
energopro.database.username=username
energopro.database.password=password
```

### 4.2 Vault Configuration

თუ იყენებთ HashiCorp Vault-ს:

```properties
# Vault configuration
spring.cloud.vault.uri=https://vault-server:8200
spring.cloud.vault.authentication=TOKEN
spring.cloud.vault.token=your-vault-token
```

---

## 5. Confluence ინტეგრაცია

### 5.1 Confluence Access

**Confluence URL:**
- Base URL: `https://asterbit.atlassian.net/wiki/home`

**კონფიგურაცია:**
- Read-only access (არ საჭიროებს API key-ს)
- Cache location: `confluence_cache/`

**ახალ კომპიუტერზე:**

1. Confluence cache-ის კოპირება (თუ არსებობს):
```bash
Copy-Item -Path "confluence_cache" -Destination "new-computer\confluence_cache" -Recurse
```

2. Confluence access ავტომატურად იმუშავებს PhoenixExpert agent-ის მეშვეობით

---

## 6. Agents და Logic

### 6.1 Agents სტრუქტურა

**Agents დირექტორია:**
```
agents/
├── __init__.py
├── agent_registry.py
├── integration_service.py
├── phoenix_expert_adapter.py
├── phoenix_expert.py
├── postman_collection_generator.py
└── test_agent.py
```

**ახალ კომპიუტერზე:**

1. დააკოპირეთ `agents/` დირექტორია
2. დააინსტალირეთ Python dependencies (იხ. სექცია 7)

### 6.2 Agent Configurations

**Integration Service (GitLab + Jira):**

Environment variables:
```bash
# GitLab
$env:GITLAB_URL="https://gitlab.com"
$env:GITLAB_TOKEN="your-token"
$env:GITLAB_PROJECT_ID="12345678"
$env:GITLAB_PIPELINE_ID="123456"  # Optional

# Jira
$env:JIRA_URL="https://your-company.atlassian.net"
$env:JIRA_EMAIL="your-email@example.com"
$env:JIRA_API_TOKEN="your-jira-api-token"
$env:JIRA_PROJECT_KEY="PROJ"
```

**PhoenixExpert Agent:**
- არ საჭიროებს დამატებით კონფიგურაციას
- იყენებს `config/backend-architecture.json`
- იყენებს `phoenix-core-lib/` კოდს

**Postman Collection Generator:**
```bash
$env:POSTMAN_API_KEY="your-api-key"
$env:POSTMAN_WORKSPACE_ID="your-workspace-id"
```

**Test Agent:**
- იყენებს Integration Service-ს
- იყენებს Postman Collection Generator-ს

---

## 7. Python გარემო

### 7.1 Python Version

```bash
# Python version-ის შემოწმება
python --version
# Python 3.8+ recommended
```

### 7.2 Dependencies

**მიმდინარე კომპიუტერზე - dependencies-ის ექსპორტი:**

```bash
# pip freeze-ის გაშვება
pip freeze > requirements.txt
```

**ახალ კომპიუტერზე:**

```bash
# Virtual environment-ის შექმნა
python -m venv venv

# Activation (Windows)
venv\Scripts\activate

# Dependencies-ის ინსტალაცია
pip install -r requirements.txt

# ან ძირითადი dependencies:
pip install requests>=2.31.0
```

**ძირითადი Dependencies:**
- `requests>=2.31.0` - HTTP requests
- (სხვა dependencies agent-ების მიხედვით)

### 7.3 Test Agent Dependencies

```bash
# Test Agent dependencies
pip install -r config/requirements_test_agent.txt

# Optional:
# npm install -g newman  # Postman collection execution
# npm install -g @playwright/test  # Playwright UI tests
```

---

## 8. Java/Gradle პროექტი

### 8.1 Java Version

```bash
# Java version-ის შემოწმება
java -version
# Java 17+ recommended
```

### 8.2 Gradle

**Gradle Wrapper:**
- `phoenix-core-lib/gradlew` (Windows: `gradlew.bat`)
- `phoenix-core-lib/gradle/wrapper/` - wrapper files

**ახალ კომპიუტერზე:**

```bash
cd phoenix-core-lib

# Gradle wrapper-ის გაშვება
.\gradlew.bat build

# ან თუ Gradle ინსტალირებულია:
gradle build
```

### 8.3 Application Properties

**კონფიგურაციის ფაილები:**
- `phoenix-core-lib/src/main/resources/application.properties`
- Environment-specific properties (თუ არსებობს)

**მიგრაცია:**
1. დააკოპირეთ `application.properties`
2. განაახლეთ database connections
3. განაახლეთ API endpoints
4. განაახლეთ credentials

---

## 9. Environment Variables

### 9.1 სრული სია

**Windows PowerShell (Session-level):**

```powershell
# GitLab
$env:GITLAB_URL="https://gitlab.com"
$env:GITLAB_TOKEN="your-gitlab-token"
$env:GITLAB_PROJECT_ID="12345678"
$env:GITLAB_PIPELINE_ID="123456"

# Jira
$env:JIRA_URL="https://your-company.atlassian.net"
$env:JIRA_EMAIL="your-email@example.com"
$env:JIRA_API_TOKEN="your-jira-api-token"
$env:JIRA_PROJECT_KEY="PROJ"

# Postman
$env:POSTMAN_API_KEY="your-postman-api-key"
$env:POSTMAN_WORKSPACE_ID="your-workspace-id"

# GitHub (თუ საჭიროა)
$env:GITHUB_TOKEN="your-github-token"
```

**Windows System Environment Variables (Permanent):**

1. System Properties → Environment Variables
2. User variables ან System variables
3. New → დაამატეთ თითოეული variable

### 9.2 .env ფაილი (ალტერნატივა)

შექმენით `.env` ფაილი (არ დაკომიტოთ Git-ში):

```env
GITLAB_URL=https://gitlab.com
GITLAB_TOKEN=your-gitlab-token
GITLAB_PROJECT_ID=12345678
JIRA_URL=https://your-company.atlassian.net
JIRA_EMAIL=your-email@example.com
JIRA_API_TOKEN=your-jira-api-token
JIRA_PROJECT_KEY=PROJ
POSTMAN_API_KEY=your-postman-api-key
POSTMAN_WORKSPACE_ID=your-workspace-id
```

**Python-ში .env-ის გამოყენება:**

```bash
pip install python-dotenv
```

```python
from dotenv import load_dotenv
load_dotenv()
```

---

## 10. შემოწმების ჩამონათვალი

### 10.1 Pre-Migration Checklist

- [ ] Git credentials შენახულია
- [ ] Postman API key ცნობილია
- [ ] Postman workspace ID ცნობილია
- [ ] Database connection strings ცნობილია
- [ ] Jira credentials ცნობილია
- [ ] GitLab token ცნობილია
- [ ] Confluence access შემოწმებულია
- [ ] Python dependencies დოკუმენტირებულია
- [ ] Java/Gradle კონფიგურაცია ცნობილია

### 10.2 Post-Migration Checklist

- [ ] ყველა ფაილი კოპირებულია
- [ ] Git კონფიგურირებულია
- [ ] Python environment setup-ია
- [ ] Java/Gradle setup-ია
- [ ] Environment variables დაყენებულია
- [ ] Postman collections იმპორტირებულია
- [ ] Postman environments იმპორტირებულია
- [ ] Database connections მუშაობს
- [ ] Agents მუშაობს
- [ ] Integration Service მუშაობს
- [ ] PhoenixExpert agent მუშაობს
- [ ] Test Agent მუშაობს

### 10.3 Testing

**Agents-ის ტესტირება:**

```python
# Test Integration Service
from agents import get_integration_service
service = get_integration_service()
result = service.update_before_task("Test task", "test")

# Test PhoenixExpert
from agents import get_phoenix_expert
expert = get_phoenix_expert()
response = expert.answer_question("What is Phoenix?")

# Test Postman Collection Generator
from agents.postman_collection_generator import PostmanCollectionGenerator
generator = PostmanCollectionGenerator()
collections = generator.get_all_collections()
```

**Postman API-ის ტესტირება:**

```bash
curl -X GET https://api.getpostman.com/collections \
  -H "X-Api-Key: $env:POSTMAN_API_KEY"
```

**GitLab API-ის ტესტირება:**

```bash
curl -X GET "https://gitlab.com/api/v4/projects/$env:GITLAB_PROJECT_ID" \
  -H "PRIVATE-TOKEN: $env:GITLAB_TOKEN"
```

---

## 11. დამატებითი რესურსები

### 11.1 დოკუმენტაცია

- `docs/INTEGRATION_SERVICE_CONFIG.md` - Integration Service კონფიგურაცია
- `docs/POSTMAN_COLLECTION_GENERATOR.md` - Postman Collection Generator
- `docs/confluence_integration_status.md` - Confluence ინტეგრაცია
- `docs/ARCHITECTURE_KNOWLEDGE_BASE.md` - Architecture documentation

### 11.2 კონფიგურაციის ფაილები

- `config/backend-architecture.json` - Backend architecture data
- `config/swagger-spec.json` - Swagger/OpenAPI specification
- `config/cursorrules/autonomous_rules.md` - Agent rules

### 11.3 მაგალითები

- `examples/generate_pod_collection.py` - POD collection generation example

---

## 12. Troubleshooting

### 12.1 Common Issues

**Postman API Key არ მუშაობს:**
- შეამოწმეთ API key სწორია თუ არა
- შეამოწმეთ workspace ID
- შეამოწმეთ network connectivity

**GitLab Integration არ მუშაობს:**
- შეამოწმეთ GITLAB_TOKEN სწორია თუ არა
- შეამოწმეთ GITLAB_PROJECT_ID
- შეამოწმეთ token-ს აქვს `api` scope

**Database Connection არ მუშაობს:**
- შეამოწმეთ connection string
- შეამოწმეთ credentials
- შეამოწმეთ network/firewall

**Python Agents არ მუშაობს:**
- შეამოწმეთ Python version (3.8+)
- შეამოწმეთ dependencies ინსტალირებულია
- შეამოწმეთ environment variables

---

## 13. Backup Recommendations

### 13.1 რეგულარული Backup

```bash
# Weekly backup script
# backup.ps1

$backupPath = "D:\Backups\Cursor\$(Get-Date -Format 'yyyy-MM-dd')"
Copy-Item -Path "$env:USERPROFILE\Cursor" -Destination $backupPath -Recurse -Force
```

### 13.2 Critical Files Backup

- `postman/postman_collections/` - Postman collections
- `config/` - Configuration files
- `agents/` - Agent code
- Environment variables (secure storage)

---

## დასკვნა

ეს გზამკვლევი მოიცავს ყველა საჭირო ინფორმაციას გარემოს მიგრაციისთვის. თუ რაიმე საკითხი გაქვთ, მიმართეთ დოკუმენტაციას ან agent-ებს.

---

**ბოლო განახლება:** 2025-01-14
**ვერსია:** 1.0

