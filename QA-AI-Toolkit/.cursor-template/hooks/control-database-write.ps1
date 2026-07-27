# control-database-write.ps1 — beforeMCPExecution
# Asks permission for SQL writes on any PostgreSQL* MCP tool.
$jsonInput = [Console]::In.ReadToEnd()
try {
    $input = $jsonInput | ConvertFrom-Json
    $toolName = [string]$input.tool_name
    $toolInput = $input.tool_input
    $isDb = ($toolName -match 'PostgreSQL' -or $toolName -match 'postgres')
    if (-not $isDb) {
        @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
        exit 0
    }
    $sqlQuery = ''
    try {
        if ($toolInput -is [string]) {
            $obj = $toolInput | ConvertFrom-Json
            $sqlQuery = [string]$obj.sql
        } else {
            $sqlQuery = [string]$toolInput.sql
        }
    } catch {
        $sqlQuery = [string]$toolInput
    }
    $sqlUpper = $sqlQuery.ToUpperInvariant()
    $isWrite = $false
    foreach ($kw in @('INSERT', 'UPDATE', 'DELETE', 'DROP', 'ALTER', 'CREATE', 'TRUNCATE', 'REPLACE', 'MERGE')) {
        if ($sqlUpper -match "\b$kw\b") { $isWrite = $true; break }
    }
    if ($isWrite) {
        @{
            continue = $true
            permission = 'ask'
            user_message = "[PERMISSION REQUIRED] Database write detected. Tool: $toolName"
            agent_message = 'Database write requires explicit user approval.'
        } | ConvertTo-Json -Compress
    } else {
        @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
    }
} catch {
    @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
}
