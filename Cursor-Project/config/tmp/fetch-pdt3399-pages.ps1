$ErrorActionPreference = 'Stop'
$pageScript = Join-Path $PSScriptRoot '..\confluence\get-confluence-page-rest.ps1'
$cqlScript = Join-Path $PSScriptRoot '..\confluence\search-confluence-cql-rest.ps1'
$outDir = Join-Path $PSScriptRoot 'pdt3399-pages'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

Write-Host '===== extra CQL ====='
& $cqlScript -Cql 'text ~ "1- 10 characters" AND text ~ "Less than" AND type = page' -Limit 25 -OutFile (Join-Path $outDir 'cql-length.json')
& $cqlScript -Cql 'text ~ "Exclude liabilities by Prefix" AND type = page' -Limit 25 -OutFile (Join-Path $outDir 'cql-prefix.json')
& $cqlScript -Cql 'title ~ "Collection channel" AND type = page' -Limit 40 -OutFile (Join-Path $outDir 'cql-title-cc.json')

$ids = @(
  '49381450','49446980','49381470','49446990','49381460','98009168','219217932','49348664',
  '585697551','585697584','585697623','585697597','585697540',
  '80904210','156041566','585697313','585699003',
  '49807485','81133985'
)

function Get-Plain($html) {
  if (-not $html) { return '' }
  $t = $html -replace '<br\s*/?>', "`n" -replace '</(p|li|h\d|tr|div)>', "`n" -replace '<li>', '- ' -replace '<[^>]+>', ' '
  $t = [System.Net.WebUtility]::HtmlDecode($t)
  return ($t -replace '[ \t]+', ' ')
}

$report = New-Object System.Text.StringBuilder
foreach ($id in $ids) {
  $jf = Join-Path $outDir "$id.json"
  Write-Host "FETCH $id"
  & $pageScript -PageId $id -OutFile $jf
  $j = Get-Content -Raw -LiteralPath $jf | ConvertFrom-Json
  $title = $j.title
  $html = $null
  if ($j.body.storage.value) { $html = $j.body.storage.value }
  $plain = Get-Plain $html
  $hits = @()
  foreach ($kw in @('Exclude liabilities by amount','Less than','Greater than','min value','minimum','0-9','1- 10','1 – 10','numeric')) {
    if ($plain -match [regex]::Escape($kw) -or $plain.ToLower().Contains($kw.ToLower())) {
      $hits += $kw
    }
  }
  [void]$report.AppendLine("==== $id | $title | hits: $($hits -join ', ') ====")
  $idx = $plain.ToLower().IndexOf('exclude liabilities by amount')
  if ($idx -lt 0) { $idx = $plain.ToLower().IndexOf('less than') }
  if ($idx -ge 0) {
    $start = [Math]::Max(0, $idx - 40)
    $len = [Math]::Min(1800, $plain.Length - $start)
    [void]$report.AppendLine($plain.Substring($start, $len))
  } else {
    [void]$report.AppendLine('(no Exclude-by-amount / Less than window)')
    # still show if "collection channel" amount filter described
    $idx2 = $plain.ToLower().IndexOf('less than')
    if ($idx2 -ge 0) {
      $start = [Math]::Max(0, $idx2 - 40)
      $len = [Math]::Min(800, $plain.Length - $start)
      [void]$report.AppendLine('ALT: ' + $plain.Substring($start, $len))
    }
  }
  [void]$report.AppendLine('')
}

$report.ToString() | Set-Content -LiteralPath (Join-Path $outDir 'extract.txt') -Encoding UTF8
Write-Host 'WROTE extract.txt'
