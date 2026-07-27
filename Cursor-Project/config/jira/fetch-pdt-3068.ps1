$ErrorActionPreference = 'Stop'
$base = $env:JIRA_BASE_URL
$email = $env:JIRA_EMAIL
$token = $env:JIRA_API_TOKEN
$envCandidates = @(
  'c:\Users\N.kevlishvili\Cursor\Cursor-Project\EnergoTS\.env',
  'c:\Users\N.kevlishvili\Cursor\.env',
  'c:\Users\N.kevlishvili\Cursor\Cursor-Project\.env'
)
foreach ($envFile in $envCandidates) {
  if (-not (Test-Path $envFile)) { continue }
  Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*JIRA_BASE_URL\s*=\s*(.+)$') { if (-not $base) { $base = $Matches[1].Trim().Trim('"').Trim("'") } }
    if ($_ -match '^\s*JIRA_EMAIL\s*=\s*(.+)$') { if (-not $email) { $email = $Matches[1].Trim().Trim('"').Trim("'") } }
    if ($_ -match '^\s*JIRA_API_TOKEN\s*=\s*(.+)$') { if (-not $token) { $token = $Matches[1].Trim().Trim('"').Trim("'") } }
  }
}
$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{ Authorization = "Basic $pair"; Accept = 'application/json' }

function Get-AdfText($node) {
  if ($null -eq $node) { return '' }
  $sb = New-Object System.Text.StringBuilder
  function Walk($n) {
    if ($null -eq $n) { return }
    if ($n -is [string]) { [void]$sb.Append($n); return }
    if ($n.type -eq 'text' -and $n.text) { [void]$sb.Append($n.text) }
    if ($n.type -eq 'hardBreak') { [void]$sb.AppendLine() }
    if ($n.type -eq 'mention' -and $n.attrs.text) { [void]$sb.Append($n.attrs.text) }
    if ($n.type -eq 'inlineCard' -and $n.attrs.url) { [void]$sb.Append($n.attrs.url) }
    if ($n.marks) {
      foreach ($m in $n.marks) {
        if ($m.type -eq 'link' -and $m.attrs.href) { [void]$sb.Append(' [' + $m.attrs.href + '] ') }
      }
    }
    if ($n.content) {
      foreach ($c in $n.content) { Walk $c }
      if ($n.type -in @('paragraph', 'heading', 'listItem', 'blockquote')) { [void]$sb.AppendLine() }
    }
  }
  Walk $node
  return $sb.ToString()
}

$issue = Invoke-RestMethod -Uri "$base/rest/api/3/issue/PDT-3068?expand=names" -Headers $headers -Method Get
$f = $issue.fields
$out = [PSCustomObject]@{
  key = $issue.key
  summary = $f.summary
  status = $f.status.name
  descriptionText = (Get-AdfText $f.description).Trim()
  custom10103Text = (Get-AdfText $f.customfield_10103).Trim()
  acceptanceText = (Get-AdfText $f.customfield_10048).Trim()
  attachmentCount = @($f.attachment).Count
  attachments = @($f.attachment | ForEach-Object { $_.filename })
}
$outDir = 'c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\jira\attachments\PDT-3068'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$out | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $outDir 'issue-summary.json') -Encoding UTF8
Write-Output ($out | ConvertTo-Json -Depth 8)
