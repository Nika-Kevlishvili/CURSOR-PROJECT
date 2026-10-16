# block-atlassian-mcp.ps1
# Hook: beforeMCPExecution
# Purpose: Deny Jira and Confluence MCP tools so Atlassian Rovo credits stay at zero.
# PostgreSQL, Elasticsearch, Slack, Playwright, and other MCP servers stay allowed.
# Fail-open on parse errors so a hook bug does not freeze unrelated MCP.

$jsonInput = [Console]::In.ReadToEnd()

function Write-HookAllow {
    @{ continue = $true; permission = "allow" } | ConvertTo-Json -Compress
}

$deniedExact = @(
    "getJiraIssue", "createJiraIssue", "editJiraIssue", "searchJiraIssuesUsingJql",
    "addCommentToJiraIssue", "transitionJiraIssue", "getTransitionsForJiraIssue",
    "getJiraIssueRemoteIssueLinks", "getVisibleJiraProjects", "getJiraProjectIssueTypesMetadata",
    "getJiraIssueTypeMetaWithFields", "lookupJiraAccountId", "addWorklogToJiraIssue",
    "getIssueLinkTypes", "createIssueLink", "atlassianUserInfo", "getAccessibleAtlassianResources",
    "getContentFormatGuide",
    "getConfluencePage", "searchConfluenceUsingCql", "getConfluenceSpaces", "getPagesInConfluenceSpace",
    "getConfluencePageFooterComments", "getConfluencePageInlineComments", "getConfluenceCommentChildren",
    "getConfluencePageDescendants", "createConfluencePage", "updateConfluencePage",
    "createConfluenceFooterComment", "createConfluenceInlineComment", "deleteConfluencePage", "editConfluencePage"
)

try {
    if (-not $jsonInput) {
        Write-HookAllow
        exit 0
    }

    $payload = $jsonInput | ConvertFrom-Json
    $toolName = $payload.tool_name
    if (-not $toolName) { $toolName = $payload.toolName }

    $innerName = $null
    $toolInput = $payload.tool_input
    if (-not $toolInput) { $toolInput = $payload.toolInput }
    if (-not $toolInput) { $toolInput = $payload.arguments }
    if ($toolName -eq "CallDynamicTool" -and $toolInput) {
        $innerName = $toolInput.toolName
        if (-not $innerName) { $innerName = $toolInput.tool_name }
        if (-not $innerName -and $toolInput.arguments) {
            $innerName = $toolInput.arguments.toolName
            if (-not $innerName) { $innerName = $toolInput.arguments.tool_name }
        }
    }

    $names = @($toolName, $innerName) | Where-Object { $_ }
    $deny = $false
    foreach ($name in $names) {
        if ($deniedExact -contains $name) { $deny = $true }
    }

    $sample = $jsonInput
    if ($sample.Length -gt 6000) { $sample = $sample.Substring(0, 6000) }

    if (-not $deny -and ($toolName -eq "search" -or $toolName -eq "fetch" -or $innerName -eq "search" -or $innerName -eq "fetch")) {
        if ($sample -match "Confluence|mcp\.atlassian\.com|getConfluencePage|searchConfluence") {
            $deny = $true
        }
    }

    if (-not $deny -and $sample -match '"namespace"\s*:\s*"[^"]*(Confluence|Jira)[^"]*"') {
        $deny = $true
    }

    if ($deny) {
        @{
            continue      = $true
            permission    = "deny"
            user_message  = "[HOOK BLOCKED] Jira and Confluence MCP are disabled so Rovo credits stay at zero. Jira uses official acli. Confluence uses the REST GET scripts."
            agent_message = "BLOCK (ATLASSIAN.0): Do not call Jira or Confluence MCP. Jira read: acli jira workitem view KEY --json --fields <list>, or acli jira workitem search --jql <jql> --json --limit 25. Jira create/edit (bug reporter only, after approval): acli jira workitem create/edit. Confluence: Cursor-Project/config/confluence/get-confluence-page-rest.ps1 and search-confluence-cql-rest.ps1. Never acli rovodev, acli rovo, or twg."
        } | ConvertTo-Json -Compress
        exit 0
    }

    Write-HookAllow
} catch {
    Write-HookAllow
}
