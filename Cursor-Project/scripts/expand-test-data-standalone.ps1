<#
.SYNOPSIS
  Expand legacy "Apply Test data steps …" references to STANDALONE numbered preconditions.

.NOTES
  **LIMITATION (do not run on production TCs yet):** Only single-line numbered steps under
  ## Test data are supported. Multi-line steps (e.g. step 3 with sub-bullets) are truncated
  and WILL corrupt files. Reverted 2026-05-31 after audit found broken PDT-2846/2815/2881 TCs.

  Usage (when fixed):
    powershell -ExecutionPolicy Bypass -File "Cursor-Project/scripts/expand-test-data-standalone.ps1" -Path "..."
#>
param(
    [string]$Path = '',
    [switch]$WhatIf
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$TestCasesRoot = Join-Path $RepoRoot 'Cursor-Project\test_cases'

if ([string]::IsNullOrWhiteSpace($Path)) {
    Write-Error @'
Bulk migration DISABLED: this script does not support multi-line Test data steps (sub-bullets under a numbered step).
Running without -Path corrupts TC files (see PDT-2846 revert 2026-05-31).
Pass -Path only after manual review, or migrate STANDALONE preconditions by hand.
'@
    exit 1
}

function Get-TestDataSteps([string]$Content) {
    if ($Content -notmatch '(?ms)^## Test data \(preconditions\)\s*\r?\n(.*?)(?=^## |\z)') {
        return @{}
    }
    $block = $Matches[1]
    $steps = @{}
    foreach ($line in ($block -split '\r?\n')) {
        if ($line -match '^\s*(\d+)\.\s+(.+)') {
            $steps[[int]$Matches[1]] = $Matches[2].Trim()
        }
    }
    return $steps
}

function Expand-StepSpec([string]$Spec, [hashtable]$Steps) {
    $indices = [System.Collections.Generic.List[int]]::new()
    $parts = $Spec -split ','
    foreach ($part in $parts) {
        $p = $part.Trim()
        if ($p -match '^(\d+)\s*[–-]\s*(\d+)$') {
            for ($i = [int]$Matches[1]; $i -le [int]$Matches[2]; $i++) { $indices.Add($i) }
        } elseif ($p -match '^(\d+)$') {
            $indices.Add([int]$Matches[1])
        }
    }
    $out = [System.Collections.Generic.List[string]]::new()
    $n = 1
    foreach ($idx in ($indices | Select-Object -Unique | Sort-Object)) {
        if (-not $Steps.ContainsKey($idx)) {
            throw "Test data step $idx not found in ## Test data section"
        }
        $out.Add("$n. $($Steps[$idx])")
        $n++
    }
    return ($out -join "`n")
}

function Expand-File([string]$FilePath) {
    $raw = Get-Content -LiteralPath $FilePath -Raw -Encoding UTF8
    if ($raw -notmatch 'Apply Test data step') { return $false }

    $steps = Get-TestDataSteps $raw
    if ($steps.Count -eq 0) {
        Write-Warning "No numbered steps in Test data: $FilePath"
        return $false
    }

    $newContent = [regex]::Replace($raw, '(?m)^(\s*\d+\.\s*)Apply Test data steps?\s+([\d,\s–-]+)(.*)$', {
        param($m)
        $prefix = $m.Groups[1].Value
        $spec = $m.Groups[2].Value.Trim()
        $suffix = $m.Groups[3].Value
        $expanded = Expand-StepSpec $spec $steps
        # Re-number expanded block; drop duplicate leading "1." from prefix if preconditions already numbered
        $lines = $expanded -split "`n"
        $result = ($lines | ForEach-Object { $_ }) -join "`n"
        if ($suffix -match '^\s*[.:]?\s*$') {
            return $result
        }
        # Preserve trailing context on same logical block (e.g. "with product contract...")
        $lastLine = $lines[-1]
        if ($suffix.Trim().Length -gt 0 -and $suffix -notmatch '^\s*[.:]\s*$') {
            $lines[-1] = "$lastLine$suffix"
            return ($lines -join "`n")
        }
        return $result
    })

    if ($newContent -eq $raw) { return $false }

    if ($WhatIf) {
        Write-Host "Would update: $FilePath" -ForegroundColor Cyan
    } else {
        Set-Content -LiteralPath $FilePath -Value $newContent -Encoding UTF8 -NoNewline
        Write-Host "Updated: $FilePath" -ForegroundColor Green
    }
    return $true
}

$files = if ($Path) {
    @(Resolve-Path (Join-Path $RepoRoot $Path))
} else {
    Get-ChildItem -Path $TestCasesRoot -Recurse -Filter '*.md' -File |
        Where-Object { $_.Name -ne 'README.md' -and (Select-String -LiteralPath $_.FullName -Pattern 'Apply Test data step' -Quiet) } |
        ForEach-Object { $_.FullName }
}

$count = 0
foreach ($f in $files) {
    if (Expand-File $f) { $count++ }
}
Write-Host "Done. Files updated: $count"
