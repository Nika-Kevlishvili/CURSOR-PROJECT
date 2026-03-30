# Fetch all git repos under Cursor-Project/Phoenix (read-only). See .cursor/rules/integrations/git_sync_workflow.mdc
$ErrorActionPreference = 'Continue'
$CursorProjectRoot = Split-Path -Parent $PSScriptRoot
$phoenix = Join-Path $CursorProjectRoot 'Phoenix'
if (-not (Test-Path $phoenix)) {
    Write-Output "ERROR: Missing $phoenix"
    exit 1
}
$repos = Get-ChildItem $phoenix -Directory | Where-Object { Test-Path (Join-Path $_.FullName '.git') }
$results = @()
foreach ($r in $repos) {
    $name = $r.Name
    Push-Location $r.FullName
    try {
        $dirty = git status --porcelain 2>$null
        $stashed = $false
        if ($dirty) {
            git stash push -m 'Stashed before fetch - preserving local changes' 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) { $stashed = $true }
        }
        git fetch origin 2>&1 | Out-Null
        $fa = $LASTEXITCODE
        git fetch origin --prune 2>&1 | Out-Null
        $fb = $LASTEXITCODE
        if ($stashed) {
            git stash pop 2>&1 | Out-Null
        }
        if ($fa -eq 0 -and $fb -eq 0) {
            $results += [PSCustomObject]@{ Repo = $name; Status = 'success' }
        } else {
            $results += [PSCustomObject]@{ Repo = $name; Status = "failed (exit $fa / $fb)" }
        }
    } catch {
        $results += [PSCustomObject]@{ Repo = $name; Status = "error: $($_.Exception.Message)" }
    } finally {
        Pop-Location
    }
}
$results | Format-Table -AutoSize
