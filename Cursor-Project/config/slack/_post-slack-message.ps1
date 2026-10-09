param(
    [Parameter(Mandatory = $true)][string]$Channel,
    [Parameter(Mandatory = $true)][string]$Text
)
$ErrorActionPreference = 'Stop'
$token = & (Join-Path $PSScriptRoot '_slack-token-helper.ps1') -ReturnToken
$payload = @{ channel = $Channel; text = $Text; mrkdwn = $false } | ConvertTo-Json -Compress
$bytes = [System.Text.Encoding]::UTF8.GetBytes($payload)
$response = Invoke-RestMethod -Uri 'https://slack.com/api/chat.postMessage' -Headers @{ Authorization = "Bearer $token" } -Method Post -Body $bytes -ContentType 'application/json; charset=utf-8'
[PSCustomObject]@{ ok = $response.ok; error = $response.error; channel = $response.channel; ts = $response.ts }
