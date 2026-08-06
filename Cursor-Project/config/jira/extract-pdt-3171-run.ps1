$ErrorActionPreference = 'Stop'
$path = 'c:\Users\N.kevlishvili\Cursor\Cursor-Project\EnergoTS\playwright-report.json'
$j = Get-Content $path -Raw | ConvertFrom-Json
$outDir = 'c:\Users\N.kevlishvili\Cursor\Cursor-Project\config\jira\PDT-3171-run-extract'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function WalkAtt($suites) {
  foreach ($s in $suites) {
    if ($s.specs) {
      foreach ($sp in $s.specs) {
        if ($sp.title -notmatch 'PDT-3171|TC-BE') { continue }
        foreach ($t in $sp.tests) {
          $r = $t.results | Select-Object -Last 1
          Write-Output ("TITLE: " + $sp.title)
          Write-Output ("STATUS: " + $r.status + " DURATION: " + $r.duration)
          $safe = ($sp.title -replace '[^\w\-]+', '_')
          if ($safe.Length -gt 90) { $safe = $safe.Substring(0, 90) }
          $i = 0
          foreach ($a in @($r.attachments)) {
            $i++
            $body = $a.body
            if (-not $body -and $a.path -and (Test-Path -LiteralPath $a.path)) {
              $body = Get-Content -LiteralPath $a.path -Raw
            }
            if (-not $body) { continue }
            $dest = Join-Path $outDir ("{0}-{1}-{2}.txt" -f $safe, $i, ($a.name -replace '[^\w\-]+', '_'))
            try {
              $decoded = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($body))
              Set-Content -LiteralPath $dest -Value $decoded -Encoding UTF8
            } catch {
              Set-Content -LiteralPath $dest -Value $body -Encoding UTF8
            }
            Write-Output ("  ATT: " + $a.name + " -> " + $dest)
          }
          # stdout
          foreach ($err in @($r.stdout, $r.stderr, $r.error)) {
            if ($err) { Write-Output ("  OUT_SNIP: " + ([string]$err).Substring(0, [Math]::Min(500, ([string]$err).Length))) }
          }
          Write-Output ''
        }
      }
    }
    if ($s.suites) { WalkAtt $s.suites }
  }
}
WalkAtt $j.suites
Write-Output '=== FILES ==='
Get-ChildItem $outDir | ForEach-Object { $_.Name + ' ' + $_.Length }
