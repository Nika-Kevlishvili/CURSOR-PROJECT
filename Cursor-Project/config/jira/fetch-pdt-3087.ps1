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
    if ($_ -match '^\s*JIRA_BASE_URL\s*=\s*(.+)$') {
      if (-not $base) { $base = $Matches[1].Trim().Trim('"').Trim("'") }
    }
    if ($_ -match '^\s*JIRA_EMAIL\s*=\s*(.+)$') {
      if (-not $email) { $email = $Matches[1].Trim().Trim('"').Trim("'") }
    }
    if ($_ -match '^\s*JIRA_API_TOKEN\s*=\s*(.+)$') {
      if (-not $token) { $token = $Matches[1].Trim().Trim('"').Trim("'") }
    }
  }
}

if (-not $base -or -not $email -or -not $token) {
  Write-Output 'MISSING_CREDS'
  exit 1
}

$pair = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("${email}:${token}"))
$headers = @{
  Authorization = "Basic $pair"
  Accept        = 'application/json'
}

$url = "$base/rest/api/3/issue/PDT-3087?expand=names"
$issue = Invoke-RestMethod -Uri $url -Headers $headers -Method Get
$f = $issue.fields

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
    if ($n.type -eq 'link' -and $n.attrs.href) { [void]$sb.Append(' ' + $n.attrs.href + ' ') }
    if ($n.content) {
      foreach ($c in $n.content) { Walk $c }
      if ($n.type -in @('paragraph', 'heading', 'listItem', 'blockquote')) { [void]$sb.AppendLine() }
    }
  }
  Walk $node
  return $sb.ToString()
}

$envText = $null
if ($f.environment -is [string]) {
  $envText = $f.environment
} elseif ($f.environment) {
  $envText = (Get-AdfText $f.environment).Trim()
}

$links = @()
foreach ($l in @($f.issuelinks)) {
  $other = if ($l.outwardIssue) { $l.outwardIssue } else { $l.inwardIssue }
  if ($other) {
    $links += [PSCustomObject]@{
      type   = $l.type.name
      key    = $other.key
      summary = $other.fields.summary
      status = $other.fields.status.name
    }
  }
}

$attachments = @($f.attachment | ForEach-Object {
  [PSCustomObject]@{ filename = $_.filename; mimeType = $_.mimeType; size = $_.size }
})

$summary = [PSCustomObject]@{
  key             = $issue.key
  summary         = $f.summary
  issuetype       = $f.issuetype.name
  status          = $f.status.name
  environment     = $envText
  labels          = $f.labels
  components      = @($f.components | ForEach-Object { $_.name })
  descriptionText = (Get-AdfText $f.description).Trim()
  custom10103Text = (Get-AdfText $f.customfield_10103).Trim()
  acceptanceText  = (Get-AdfText $f.customfield_10048).Trim()
  attachmentCount = $attachments.Count
  attachments     = $attachments
  links           = $links
}

$outDir = 'c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\jira\attachments\PDT-3087'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$issue | ConvertTo-Json -Depth 40 | Set-Content -Path (Join-Path $outDir 'issue-full.json') -Encoding UTF8
$summary | ConvertTo-Json -Depth 10 | Set-Content -Path (Join-Path $outDir 'issue-summary.json') -Encoding UTF8
Write-Output ($summary | ConvertTo-Json -Depth 10)
