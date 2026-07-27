# block-confluence-write.ps1 — beforeMCPExecution
$jsonInput = [Console]::In.ReadToEnd()
try {
    $input = $jsonInput | ConvertFrom-Json
    $toolName = $input.tool_name
    $forbiddenTools = @(
        'updateConfluencePage', 'createConfluencePage',
        'createConfluenceFooterComment', 'createConfluenceInlineComment',
        'deleteConfluencePage', 'editConfluencePage'
    )
    $isWrite = $false
    if ($toolName -match 'Confluence') {
        foreach ($kw in @('update', 'create', 'delete', 'edit', 'modify', 'remove')) {
            if ($toolName -match $kw) { $isWrite = $true; break }
        }
    }
    foreach ($f in $forbiddenTools) { if ($toolName -eq $f) { $isWrite = $true } }
    if ($isWrite) {
        @{
            continue = $true
            permission = 'deny'
            user_message = "[HOOK BLOCKED] Confluence write forbidden (Rule 1a). Tool: $toolName"
            agent_message = 'BLOCK: Confluence write tools are read-only only.'
        } | ConvertTo-Json -Compress
    } else {
        @{ continue = $true; permission = 'allow' } | ConvertTo-Json -Compress
    }
} catch {
    @{
        continue = $true
        permission = 'deny'
        user_message = '[HOOK BLOCKED] Confluence protection hook error — fail-secure.'
        agent_message = "BLOCK: block-confluence-write parse failed: $_"
    } | ConvertTo-Json -Compress
}
