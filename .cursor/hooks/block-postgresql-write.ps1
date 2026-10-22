# block-postgresql-write.ps1
# Hook: beforeShellExecution
# Purpose: Block direct psql, mcp-postgres-server, and unapproved writes.
#          Read-only psql-phoenix.py queries stay allowed.
#          execute --allow-write asks for approval, except on read-only environments.
# Fail-open on parse errors.

$jsonInput = [Console]::In.ReadToEnd()

function Write-Allow {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}

function Write-Deny {
    param([string]$AgentMessage)
    @{
        continue      = $false
        permission    = "deny"
        user_message  = "[HOOK BLOCKED] Direct psql and unapproved PostgreSQL writes are disabled."
        agent_message = $AgentMessage
    } | ConvertTo-Json -Compress
}

try {
    if (-not $jsonInput) { Write-Allow; exit 0 }
    $payload = $jsonInput | ConvertFrom-Json
    $command = [string]$payload.command
    if (-not $command) { Write-Allow; exit 0 }
    $lower = $command.ToLowerInvariant()

    if ($lower -match "mcp-postgres-server") {
        Write-Deny "BLOCK (DB.0): mcp-postgres-server is removed. Use python3 Cursor-Project/scripts/psql-phoenix.py. Read .cursor/skills/phoenix-database/SKILL.md."
        exit 0
    }

    $usesWrapper = $lower -match "psql-phoenix\.py"
    $usesPsqlBin = $lower -match '(^|[;&|`]\s*|[/\s])psql(\.exe)?(\s|$)'
    if ($usesPsqlBin -and -not $usesWrapper) {
        Write-Deny "BLOCK (DB.0): Do not call psql directly. Use python3 Cursor-Project/scripts/psql-phoenix.py query --environment <env> --sql `"SELECT ...`". Passwords must not appear on the command line."
        exit 0
    }

    if ($usesWrapper -and $lower -match "--allow-write") {
        if ($lower -match "--environment(?:=|\s+)(prod|es|slr)\b") {
            Write-Deny "BLOCK (DB.5): prod, es, and slr are read-only. Do not pass --allow-write for those environments."
            exit 0
        }
        @{
            continue      = $true
            permission    = "ask"
            user_message  = "[PERMISSION REQUIRED] PostgreSQL write via psql-phoenix.py --allow-write."
            agent_message = "Database write requires explicit user approval. Wait for approval before retrying execute."
        } | ConvertTo-Json -Compress
        exit 0
    }

    Write-Allow
} catch {
    Write-Allow
    exit 0
}
