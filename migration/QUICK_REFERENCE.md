# სწრაფი მითითება / Quick Reference

## 🔑 კრიტიკული Credentials

### GitLab
- **URL**: `https://gitlab.com` (ან თქვენი GitLab instance)
- **Token**: GitLab → Settings → Access Tokens → Create token with `api` scope
- **Project ID**: Project → Settings → General → Project ID

### Postman
- **API Key**: https://go.postman.co/settings/me/api-keys
- **Workspace ID**: Postman URL-ში `workspace/{id}` ან API-ით

### Jira
- **URL**: `https://your-company.atlassian.net`
- **Email**: თქვენი Jira account email
- **API Token**: Jira → Account Settings → Security → API Tokens

### Confluence
- **URL**: `https://asterbit.atlassian.net/wiki/home`
- **Access**: Read-only (არ საჭიროებს API key)

---

## 📝 Environment Variables

```powershell
# GitLab
$env:GITLAB_URL="https://gitlab.com"
$env:GITLAB_TOKEN="your-token"
$env:GITLAB_PROJECT_ID="12345678"

# Jira
$env:JIRA_URL="https://company.atlassian.net"
$env:JIRA_EMAIL="email@example.com"
$env:JIRA_API_TOKEN="your-token"
$env:JIRA_PROJECT_KEY="PROJ"

# Postman
$env:POSTMAN_API_KEY="your-api-key"
$env:POSTMAN_WORKSPACE_ID="your-workspace-id"
```

---

## 📁 მნიშვნელოვანი ფაილები

- `agents/` - ყველა agent
- `config/backend-architecture.json` - Architecture data
- `postman/postman_collections/` - Postman collections
- `phoenix-core-lib/` - Java project
- `MIGRATION_GUIDE.md` - სრული გზამკვლევი

---

## 🚀 სწრაფი Setup

### ახალ კომპიუტერზე:

1. **ფაილების კოპირება**
   ```powershell
   Copy-Item -Path "source\Cursor" -Destination "C:\Users\...\Cursor" -Recurse
   ```

2. **Python Setup**
   ```powershell
   python -m venv venv
   .\venv\Scripts\Activate.ps1
   pip install -r requirements.txt
   ```

3. **Environment Variables**
   ```powershell
   .\environment_variables_export.ps1  # თუ არსებობს
   # ან manually
   ```

4. **ტესტირება**
   ```powershell
   .\migration_helper.ps1  # Option 6: Run All Checks
   ```

---

## 🔍 სწრაფი ტესტირება

### GitLab
```powershell
curl -X GET "https://gitlab.com/api/v4/user" -H "PRIVATE-TOKEN: $env:GITLAB_TOKEN"
```

### Postman
```powershell
curl -X GET "https://api.getpostman.com/workspaces" -H "X-Api-Key: $env:POSTMAN_API_KEY"
```

### Python Agents
```python
from agents import get_integration_service, get_phoenix_expert
service = get_integration_service()
expert = get_phoenix_expert()
```

---

## 📊 Collections & Environments

**Postman Collections: 29**
- 0---* collections (9)
- 1---Customer
- 2---Point of Delivery
- 3---Customer Communication
- 4---Contracts and Orders
- 5---Product and Services
- 6---Energy Data
- 7---Billing
- 8---Receivables Management
- 9---Operations Management
- ... და სხვა

**Postman Environments: 5**
- DEV
- DEV 2
- TEST
- Pre Prod
- Prod

---

## 🛠️ სკრიპტები

- `migration_helper.ps1` - მიგრაციის დახმარება (მიმდინარე კომპიუტერზე)
- `setup_new_computer.ps1` - Setup ახალ კომპიუტერზე

---

## 📞 დახმარება

- `MIGRATION_GUIDE.md` - სრული გზამკვლევი
- `docs/` - დეტალური დოკუმენტაცია

