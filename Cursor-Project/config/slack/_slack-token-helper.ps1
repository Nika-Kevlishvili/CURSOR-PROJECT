param([switch]$ReturnToken)
$ErrorActionPreference = 'Stop'
$ws = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$envPath = Join-Path $ws 'Cursor-Project\EnergoTS\.env'
if (-not (Test-Path -LiteralPath $envPath)) { throw "Not found: $envPath" }
function Unquote-EnvValue([string]$raw) {
    $v = $raw.Trim()
    if ($v.StartsWith('"') -and $v.EndsWith('"') -and $v.Length -ge 2) { $v = $v.Substring(1, $v.Length - 2) }
    elseif ($v.StartsWith("'") -and $v.EndsWith("'") -and $v.Length -ge 2) { $v = $v.Substring(1, $v.Length - 2) }
    return $v.Trim()
}
$token = $null
Get-Content -LiteralPath $envPath | ForEach-Object {
    if ($_ -match '^\s*SLACK_BOT_TOKEN\s*=\s*(.+)$') { $token = Unquote-EnvValue $matches[1] }
    elseif (-not $token -and $_ -match '^\s*SLACK_API_TOKEN\s*=\s*(.+)$') { $token = Unquote-EnvValue $matches[1] }
}
if (-not $token) { throw 'No Slack token in EnergoTS\.env' }
if ($ReturnToken) { return $token }
$headers = @{ Authorization = "Bearer $token" }
$auth = Invoke-RestMethod -Uri 'https://slack.com/api/auth.test' -Headers $headers -Method Get
[PSCustomObject]@{ ok = $auth.ok; team = $auth.team; user = $auth.user; user_id = $auth.user_id; token_prefix = $token.Substring(0, [Math]::Min(8, $token.Length)) }
