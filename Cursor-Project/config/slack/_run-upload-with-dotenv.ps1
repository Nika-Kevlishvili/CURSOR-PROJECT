# Loads SLACK_API_TOKEN and/or SLACK_BOT_TOKEN from Cursor-Project\EnergoTS\.env, then calls upload-file-to-slack.ps1.
param(
    [Parameter(Mandatory = $true)][string]$FilePath,
    [Parameter(Mandatory = $true)][string]$ChannelOrUserId,
    [string]$InitialComment = ''
)
$ErrorActionPreference = 'Stop'
$cursorProjectRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$envPath = Join-Path $cursorProjectRoot 'EnergoTS\.env'
if (-not (Test-Path -LiteralPath $envPath)) {
    Write-Error "Not found: $envPath"
    exit 1
}
function Unquote-EnvValue([string]$raw) {
    $v = $raw.Trim()
    if ($v.StartsWith('"') -and $v.EndsWith('"') -and $v.Length -ge 2) { $v = $v.Substring(1, $v.Length - 2) }
    elseif ($v.StartsWith("'") -and $v.EndsWith("'") -and $v.Length -ge 2) { $v = $v.Substring(1, $v.Length - 2) }
    $hash = $v.IndexOf('#')
    if ($hash -ge 0) { $v = $v.Substring(0, $hash).TrimEnd() }
    return $v.Trim()
}
Get-Content -LiteralPath $envPath | ForEach-Object {
    $line = $_.TrimStart([char]0xFEFF)
    if ($line -match '^\s*SLACK_API_TOKEN\s*=\s*(.+)$') {
        $env:SLACK_API_TOKEN = (Unquote-EnvValue $matches[1])
    }
    elseif ($line -match '^\s*SLACK_BOT_TOKEN\s*=\s*(.+)$') {
        $env:SLACK_BOT_TOKEN = (Unquote-EnvValue $matches[1])
    }
}
if (-not $env:SLACK_API_TOKEN -and -not $env:SLACK_BOT_TOKEN) {
    Write-Error 'Set SLACK_API_TOKEN or SLACK_BOT_TOKEN in EnergoTS\.env'
    exit 1
}
& (Join-Path $PSScriptRoot 'upload-file-to-slack.ps1') -FilePath $FilePath -ChannelOrUserId $ChannelOrUserId -InitialComment $InitialComment
