# block-phoenix-code-requests.ps1
# Hook: beforeSubmitPrompt
# Purpose: Block prompts that request code modifications in Phoenix project
# Rule: 0.8 - Code Modification is STRICTLY FORBIDDEN

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $prompt = $input.prompt
    
    if (-not $prompt) {
        $response = @{ continue = $true }
        $response | ConvertTo-Json -Compress
        exit 0
    }
    
    $promptLower = $prompt.ToLower()
    
    # Keywords that indicate code modification requests - ENHANCED
    $modificationKeywords = @(
        "modify", "change", "edit", "update", "fix", "refactor", 
        "optimize", "improve", "rewrite", "alter", "replace",
        "delete code", "remove code", "add code", "implement",
        "create file", "write file", "edit file", "correct",
        "repair", "debug", "patch", "adjust", "tweak",
        "make changes", "do changes", "apply changes"
    )
    
    # Phoenix-related keywords - ENHANCED
    $phoenixKeywords = @(
        "phoenix", 
        "billingruncommonservice", 
        "cursor-project/phoenix",
        "phoenix-core",
        "phoenix-core-lib",
        "phoenix-billing-run",
        "phoenix-api-gateway",
        "phoenix-migration",
        "phoenix-payment-api",
        "phoenix-ui"
    )
    
    $hasModificationKeyword = $false
    $hasPhoenixKeyword = $false
    
    foreach ($keyword in $modificationKeywords) {
        if ($promptLower -match "\b$keyword\b") {
            $hasModificationKeyword = $true
            break
        }
    }
    
    foreach ($keyword in $phoenixKeywords) {
        if ($promptLower -match [regex]::Escape($keyword)) {
            $hasPhoenixKeyword = $true
            break
        }
    }
    
    # Block if prompt requests modification AND mentions Phoenix
    if ($hasModificationKeyword -and $hasPhoenixKeyword) {
        $response = @{
            continue = $false
            block = $true
            user_message = "[HOOK BLOCKED] Code modification requests in Phoenix project are FORBIDDEN (Rule 0.8). Please note that Phoenix code cannot be modified."
            agent_message = "CRITICAL: Prompt blocked. Rule 0.8 states that code modification is STRICTLY FORBIDDEN in Phoenix project."
        }
    } else {
        $response = @{ continue = $true }
    }
    
    $response | ConvertTo-Json -Compress
} catch {
    # On error, allow prompt but log warning
    $response = @{ continue = $true }
    $response | ConvertTo-Json -Compress
}
