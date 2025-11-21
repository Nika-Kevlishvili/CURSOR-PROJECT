# MCP Servers Configuration / MCP Servers კონფიგურაცია

ეს დირექტორია შეიცავს MCP (Model Context Protocol) servers-ის კონფიგურაციას.

This directory contains MCP (Model Context Protocol) servers configuration.

## 📋 MCP Servers სია / MCP Servers List

### 🔵 Atlassian Services (URL-based)

1. **Jira** - Issue tracking და project management
   - URL: `https://mcp.atlassian.com/v1/sse`
   - Type: URL-based MCP server

2. **Confluence** - Documentation access
   - URL: `https://mcp.atlassian.com/v1/sse`
   - Type: URL-based MCP server

### 🗄️ PostgreSQL Databases (Command-based)

სხვადასხვა გარემოსთვის PostgreSQL database connections:

PostgreSQL database connections for different environments:

1. **PostgreSQLDev** - DEV გარემო
   - Host: `10.236.20.21`
   - Port: `5432`
   - Database: `phoenix`
   - User: `postgres`

2. **PostgreSQLDev2** - DEV2 გარემო
   - Host: `10.236.20.22`
   - Port: `5432`
   - Database: `phoenix`
   - User: `postgres`

3. **PostgreSQLTest** - TEST გარემო
   - Host: `10.236.20.24`
   - Port: `5432`
   - Database: `phoenix`
   - User: `postgres`

4. **PostgreSQLPreProd** - PRE-PROD გარემო
   - Host: `10.236.20.76`
   - Port: `5432`
   - Database: `phoenix`
   - User: `postgres`

## 🔐 Security / უსაფრთხოება

⚠️ **მნიშვნელოვანი**: 
- `mcp_servers_template.json` შეიცავს **placeholder values** (YOUR_PASSWORD)
- **არ** დააკომიტოთ რეალური პაროლები Git-ში
- გამოიყენეთ `scripts/setup_mcp_servers.ps1` რეალური credentials-ის დასამატებლად

⚠️ **Important**: 
- `mcp_servers_template.json` contains **placeholder values** (YOUR_PASSWORD)
- **Do NOT** commit real passwords to Git
- Use `scripts/setup_mcp_servers.ps1` to add real credentials

## 🚀 Setup / დაყენება

### ვარიანტი 1: ავტომატური Script / Option 1: Automatic Script

```powershell
.\scripts\setup_mcp_servers.ps1
```

ეს script:
- წაიკითხავს `mcp_servers_template.json`
- მოგთხოვთ passwords-ს თითოეული გარემოსთვის
- შექმნის MCP კონფიგურაციას Cursor settings-ში

This script will:
- Read `mcp_servers_template.json`
- Ask for passwords for each environment
- Create MCP configuration in Cursor settings

### ვარიანტი 2: ხელით / Option 2: Manual

1. გახსენით Cursor settings:
   ```powershell
   code "$env:APPDATA\Cursor\User\settings.json"
   ```

2. დაამატეთ MCP კონფიგურაცია `mcpServers` section-ში:
   ```json
   {
     "mcpServers": {
       "Jira": {
         "url": "https://mcp.atlassian.com/v1/sse"
       },
       ...
     }
   }
   ```

3. შეცვალეთ `YOUR_PASSWORD` რეალური passwords-ით

## 📝 Environment Variables / გარემოს ცვლადები

თუ გსურთ environment variables-ის გამოყენება passwords-ისთვის:

If you want to use environment variables for passwords:

```powershell
# Set environment variables
$env:POSTGRES_DEV_PASSWORD = "your-dev-password"
$env:POSTGRES_DEV2_PASSWORD = "your-dev2-password"
$env:POSTGRES_TEST_PASSWORD = "your-test-password"
$env:POSTGRES_PREPROD_PASSWORD = "your-preprod-password"
```

შემდეგ გამოიყენეთ `$env:POSTGRES_DEV_PASSWORD` კონფიგურაციაში.

Then use `$env:POSTGRES_DEV_PASSWORD` in configuration.

## 🔍 Verification / შემოწმება

MCP servers-ის შემოწმებისთვის:

To verify MCP servers:

1. გადატვირთეთ Cursor
2. `Ctrl+Shift+P` → "MCP" ან "Model Context Protocol"
3. შეამოწმეთ რომ ყველა server ჩანს

## 🆘 Troubleshooting / პრობლემების გადაჭრა

### პრობლემა: PostgreSQL MCP server არ მუშაობს
**გადაწყვეტა**:
1. შეამოწმეთ რომ `npx` დაყენებულია
2. შეამოწმეთ network connection database-თან
3. შეამოწმეთ credentials

### პრობლემა: Jira/Confluence MCP server არ მუშაობს
**გადაწყვეტა**:
1. შეამოწმეთ URL: `https://mcp.atlassian.com/v1/sse`
2. შეამოწმეთ authentication
3. გადატვირთეთ Cursor

---

**ბოლო განახლება / Last Updated**: 2025-01-14

