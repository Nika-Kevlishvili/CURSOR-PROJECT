# Cursor Configuration

This folder contains Cursor IDE configuration required for full functionality of this project.

## 📋 Files

- **`mcp-config.json`** - MCP (Model Context Protocol) server configuration
- **`extensions.json`** - Recommended extensions list
- **`hooks.json`** - Cursor hooks configuration (protection and control)
- **`hooks/`** - Hook scripts (PowerShell)
- **`rules/phoenix.mdc`** - Project rules and guidelines
- **`rules/git_sync_workflow.mdc`** - Git sync workflow rule for GitLab projects (`!sync`, `!update`, `!checkout`)
- **`commands/phoenix.md`** - Custom commands

## 🚀 Setup on New Computer

### Windows PowerShell

```powershell
# Navigate to project directory
cd C:\path\to\Cursor-Project

# Run setup script
.\setup-cursor-config.ps1
```

The script automatically:
1. ✅ Checks Cursor installation
2. ✅ Creates backup of existing configuration
3. ✅ Transfers MCP configuration to Cursor settings
4. ✅ Prompts for sensitive values (passwords, tokens)
5. ✅ Shows recommended extensions list

### Manual Setup

1. **MCP Configuration**:
   - Copy `.cursor\mcp-config.json` to `%APPDATA%\Cursor\mcp.json`
   - Update passwords and tokens in the file

2. **Extensions**:
   - Open Cursor
   - Press `Ctrl+Shift+X` to open Extensions
   - Install extensions from `.cursor\extensions.json`

3. **Restart Cursor** to apply changes

## ⚠️ Important Notes

1. **Sensitive Data**: `mcp-config.json` contains placeholders (`PASSWORD`, `YOUR_GITLAB_TOKEN_HERE`). 
   After setup on a new computer, update with real values.

2. **Passwords**: Passwords are stored in plain text. Ensure that Cursor configuration directory is secured.

3. **Git**: Do NOT commit `mcp-config.json` to Git if it contains real passwords. 
   Use `.gitignore` or environment variables.

## 🛡️ Hooks (Protection and Control)

Hooks automatically protect the project from unwanted operations:

### **`beforeSubmitPrompt`** - Prompt Validation
- **`block-phoenix-code-requests.ps1`** - Blocks prompts that request Phoenix code modifications
- Rule 0.8 - Code Modification is STRICTLY FORBIDDEN

### **`beforeFileEdit`** - File Protection
- **`protect-phoenix-code.ps1`** - Blocks code file edits in Phoenix/Cursor-Project folders
- Protected: `.java`, `.ts`, `.js`, `.py`, `.xml`, `.sql` and other code files
- Rule 0.8 - Code Modification is STRICTLY FORBIDDEN

### **`afterFileEdit`** - Backup Safety Check
- **`warn-phoenix-code-edit.ps1`** - Warns if Phoenix code file was edited (backup safety check)
- ⚠️ **Note**: This hook is for warning only - it does not block changes
- Used as backup if `beforeFileEdit` hook did not work or is not supported in Cursor version

### **`beforeMCPExecution`** - MCP Tool Control
- **`block-confluence-write.ps1`** - Blocks Confluence write operations (read-only)
- **`control-database-write.ps1`** - Requires permission for database write operations

### **`beforeShellExecution`** - Shell Command Control
- **`control-git-push.ps1`** - Requires permission for Git commit/push/merge operations

## 📝 MCP Servers

- **Confluence** - Atlassian Confluence integration
- **GitLab** - GitLab integration
- **PostgreSQLTest** - Test database
- **PostgreSQLDev** - Development database

## 📦 Recommended Extensions

- `ms-python.python` - Python language support
- `ms-python.debugpy` - Python debugging
- `anysphere.cursorpyright` - Python type checking
- `vscjava.vscode-gradle` - Gradle support
- `ms-vscode.powershell` - PowerShell support
- `ms-playwright.playwright` - Playwright testing

---

**Last Updated**: 2025-01-14
