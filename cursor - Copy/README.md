# Cursor Configuration
# კურსორის კონფიგურაცია

ეს ფოლდერი შეიცავს Cursor IDE-ის კონფიგურაციას, რომელიც საჭიროა ამ პროექტის სრულფასოვანი მუშაობისთვის.

This folder contains Cursor IDE configuration required for full functionality of this project.

## 📋 ფაილები / Files

- **`mcp-config.json`** - MCP (Model Context Protocol) სერვერების კონფიგურაცია
- **`extensions.json`** - რეკომენდებული ექსთენშენების სია
- **`rules/phoenix.mdc`** - პროექტის rules და guidelines
- **`commands/phoenix.md`** - Custom commands

## 🚀 ახალ კომპიუტერზე დაყენება / Setup on New Computer

### Windows PowerShell

```powershell
# Navigate to project directory
cd C:\path\to\Cursor-Project

# Run setup script
.\setup-cursor-config.ps1
```

სკრიპტი ავტომატურად:
1. ✅ ამოწმებს Cursor-ის დაყენებას
2. ✅ აკეთებს backup-ს არსებული კონფიგურაციის
3. ✅ გადაიტანს MCP კონფიგურაციას Cursor-ის settings-ში
4. ✅ ითხოვს sensitive values (passwords, tokens)
5. ✅ აჩვენებს რეკომენდებული ექსთენშენების სიას

### Manual Setup / ხელით დაყენება

1. **MCP Configuration**:
   - Copy `.cursor\mcp-config.json` to `%APPDATA%\Cursor\mcp.json`
   - Update passwords and tokens in the file

2. **Extensions**:
   - Open Cursor
   - Press `Ctrl+Shift+X` to open Extensions
   - Install extensions from `.cursor\extensions.json`

3. **Restart Cursor** to apply changes

## ⚠️ მნიშვნელოვანი შენიშვნები / Important Notes

1. **Sensitive Data**: `mcp-config.json` შეიცავს placeholder-ებს (`PASSWORD`, `YOUR_GITLAB_TOKEN_HERE`). 
   ახალ კომპიუტერზე დაყენების შემდეგ განაახლეთ რეალური მნიშვნელობებით.

2. **Passwords**: პაროლები ინახება plain text-ში. დარწმუნდით რომ Cursor configuration directory დაცულია.

3. **Git**: არ დაკომიტოთ `mcp-config.json` Git-ში თუ შეიცავს რეალურ passwords-ს. 
   გამოიყენეთ `.gitignore` ან environment variables.

## 📝 MCP Servers

- **Confluence** - Atlassian Confluence ინტეგრაცია
- **GitLab** - GitLab ინტეგრაცია
- **PostgreSQLTest** - Test მონაცემთა ბაზა
- **PostgreSQLDev** - Development მონაცემთა ბაზა

## 📦 Recommended Extensions

- `ms-python.python` - Python language support
- `ms-python.debugpy` - Python debugging
- `anysphere.cursorpyright` - Python type checking
- `vscjava.vscode-gradle` - Gradle support
- `ms-vscode.powershell` - PowerShell support
- `ms-playwright.playwright` - Playwright testing

---

**Last Updated**: 2025-01-14

