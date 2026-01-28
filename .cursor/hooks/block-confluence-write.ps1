# block-confluence-write.ps1
# Hook: beforeMCPExecution
# Purpose: Block all Confluence write operations (Rule 1a)

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $toolName = $input.tool_name
    
    $forbiddenTools = @("updateConfluencePage", "createConfluencePage", "createConfluenceFooterComment", "createConfluenceInlineComment", "deleteConfluencePage", "editConfluencePage")
    
    $isConfluenceWrite = $false
    
    if ($toolName -match "Confluence") {
        $writeKeywords = @("update", "create", "delete", "edit", "modify", "remove")
        foreach ($keyword in $writeKeywords) {
            if ($toolName -match $keyword) {
                $isConfluenceWrite = $true
                break
            }
        }
    }
    
    foreach ($forbidden in $forbiddenTools) {
        if ($toolName -eq $forbidden) {
            $isConfluenceWrite = $true
            break
        }
    }
    
    if ($isConfluenceWrite) {
        $response = @{
            continue = $true
            permission = "deny"
            user_message = "[HOOK BLOCKED] Confluence write operations are FORBIDDEN (Rule 1a). Tool: $toolName"
            agent_message = "CRITICAL: Confluence write operation blocked. Rule 1a - Confluence Editing Tools are FORBIDDEN - READ-ONLY ONLY."
        }
    } else {
        $response = @{ continue = $true; permission = "allow" }
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}
