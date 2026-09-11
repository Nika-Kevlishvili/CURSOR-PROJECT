$j = Get-Content -Raw 'Cursor-Project/config/jira/PDT-3409-full.json' | ConvertFrom-Json
$f = $j.fields
Write-Host '=== KEY FIELDS ==='
Write-Host ("key: " + $j.key)
Write-Host ("summary: " + $f.summary)
Write-Host ("issuetype: " + $f.issuetype.name)
Write-Host ("status: " + $f.status.name)
Write-Host ("priority: " + $f.priority.name)
if ($null -eq $f.environment) {
    Write-Host 'ENVIRONMENT IS NULL'
} else {
    Write-Host '--- environment dump ---'
    $f.environment | ConvertTo-Json -Depth 8
}
Write-Host '--- labels ---'
$f.labels
Write-Host '--- components ---'
$f.components | ForEach-Object { $_.name }
Write-Host ("reporter: " + $f.reporter.displayName)
Write-Host ("assignee: " + $(if ($f.assignee) { $f.assignee.displayName } else { 'UNASSIGNED' }))
Write-Host '--- attachments ---'
if ($f.attachment) { $f.attachment | ForEach-Object { $_.filename + ' | ' + $_.mimeType + ' | ' + $_.size } } else { Write-Host 'NONE' }
Write-Host '--- issuelinks ---'
if ($f.issuelinks) { $f.issuelinks | ConvertTo-Json -Depth 6 } else { Write-Host 'NONE' }
Write-Host ("cf_10103 present: " + [bool]$f.customfield_10103)
Write-Host ("cf_10217 present: " + [bool]$f.customfield_10217)
Write-Host ("cf_10048 present: " + [bool]$f.customfield_10048)
Write-Host ("cf_10745 present: " + [bool]$f.customfield_10745)
Write-Host ("description present: " + [bool]$f.description)
Write-Host '--- description json ---'
if ($f.description) { $f.description | ConvertTo-Json -Depth 30 }
Write-Host '--- cf_10103 json ---'
if ($f.customfield_10103) { $f.customfield_10103 | ConvertTo-Json -Depth 30 }
Write-Host '--- cf_10217 json ---'
if ($f.customfield_10217) { $f.customfield_10217 | ConvertTo-Json -Depth 30 }
Write-Host '--- comments ---'
if ($f.comment -and $f.comment.comments) { $f.comment.comments | ConvertTo-Json -Depth 20 } else { Write-Host 'NO COMMENTS' }
