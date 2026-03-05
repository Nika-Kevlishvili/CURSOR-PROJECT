# block-jira-phoenix-delivery.ps1
# Hook: beforeSubmitPrompt
# Purpose: Block prompts that request creating/writing Jira bugs in Phoenix delivery
# Rule: jira_bug_agent.mdc (JIRA.0) - Jira bugs ONLY on Experiments board

$jsonInput = [Console]::In.ReadToEnd()

try {
    $input = $jsonInput | ConvertFrom-Json
    $prompt = $input.prompt

    if (-not $prompt) {
        @{ continue = $true } | ConvertTo-Json -Compress
        exit 0
    }

    $promptLower = $prompt.ToLower()

    # Jira bug creation intent
    $wantsJiraBug = ($promptLower -match "jira.*bug|bug.*jira|create.*bug|write.*bug|add.*bug|jira ticket|create ticket|!jira-bug|jira-bug")

    # Phoenix delivery – must not create bugs there
    $targetsPhoenixDelivery = ($promptLower -match "phoenix\s+delivery|phoenix delivery board|phoenix delivery project")

    # Block: user wants to create a Jira bug AND targets Phoenix delivery
    if ($wantsJiraBug -and $targetsPhoenixDelivery) {
        $response = @{
            continue    = $false
            block      = $true
            user_message  = "[HOOK BLOCKED] Creating Jira bugs in Phoenix delivery is not allowed (Rule JIRA.0). Use the Experiments board for bug creation. Command: !jira-bug (Experiments board only)."
            agent_message = "CRITICAL: Jira bug creation in Phoenix delivery is PROHIBITED. Use Experiments board only. See .cursor/rules/jira_bug_agent.mdc."
        }
    } else {
        $response = @{ continue = $true }
    }

    $response | ConvertTo-Json -Compress
} catch {
    @{ continue = $true } | ConvertTo-Json -Compress
}
