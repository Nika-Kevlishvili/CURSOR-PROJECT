# control-database-write.ps1
# Hook: beforeMCPExecution
# Purpose: Ask permission for database write operations

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $toolName = $input.tool_name
    $toolInput = $input.tool_input
    
    $databaseTools = @("mcp_PostgreSQLTest_execute", "mcp_PostgreSQLDev_execute", "mcp_PostgreSQLDev2_execute", "mcp_PostgreSQLPreProd_execute", "mcp_PostgreSQLProd_execute")
    
    $isDatabaseTool = $false
    foreach ($dbTool in $databaseTools) {
        if ($toolName -eq $dbTool) {
            $isDatabaseTool = $true
            break
        }
    }
    
    if ($isDatabaseTool) {
        $sqlQuery = ""
        try {
            if ($toolInput -is [string]) {
                $toolInputObj = $toolInput | ConvertFrom-Json
                $sqlQuery = $toolInputObj.sql
            } else {
                $sqlQuery = $toolInput.sql
            }
        } catch {
            if ($toolInput -match "^\s*(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE)") {
                $sqlQuery = $toolInput
            }
        }
        
        $writeKeywords = @("INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE", "REPLACE", "MERGE")
        $isWriteOperation = $false
        $sqlUpper = $sqlQuery.ToUpper()
        
        foreach ($keyword in $writeKeywords) {
            if ($sqlUpper -match "\b$keyword\b") {
                $isWriteOperation = $true
                break
            }
        }
        
        if ($isWriteOperation) {
            $response = @{
                continue = $true
                permission = "ask"
                user_message = "[PERMISSION REQUIRED] Database write operation detected. Tool: $toolName, SQL: $sqlQuery"
                agent_message = "Database write operation requires explicit user approval. Please wait for user approval."
            }
        } else {
            $response = @{ continue = $true; permission = "allow" }
        }
    } else {
        $response = @{ continue = $true; permission = "allow" }
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    @{ continue = $true; permission = "ask"; user_message = "[PERMISSION REQUIRED] Error parsing database operation." } | ConvertTo-Json -Compress
}
