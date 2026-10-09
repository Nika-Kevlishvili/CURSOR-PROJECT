$ErrorActionPreference = 'Stop'
$token = & (Join-Path $PSScriptRoot '_slack-token-helper.ps1') -ReturnToken
$headers = @{ Authorization = "Bearer $token" }
$cursor = $null
$all = @()
do {
    $uri = 'https://slack.com/api/conversations.list?types=public_channel,private_channel&limit=200'
    if ($cursor) { $uri += "&cursor=$cursor" }
    $r = Invoke-RestMethod -Uri $uri -Headers $headers -Method Get
    if (-not $r.ok) { throw ($r | ConvertTo-Json -Compress) }
    $all += $r.channels
    $cursor = $r.response_metadata.next_cursor
} while ($cursor)
$all | Where-Object { $_.name -match 'bug|valid|ai-report|report' -or $_.id -eq 'C0AUEEDVCEL' -or $_.id -eq 'C0AK96S1D7X' } |
    Select-Object id, name, is_member, is_private | Format-Table -AutoSize
