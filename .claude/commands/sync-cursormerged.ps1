#!/usr/bin/env pwsh
# sync-cursormerged.ps1
# Brings new commits from origin/staging and origin/cursor onto the EnergoTS `cursorMerged`
# branch by cherry-pick. Procedure: .claude/skills/energo-ts-sync/SKILL.md
#
# Default mode is a PLAN: it fetches, works out what is pending and prints it. Nothing else changes.
# -Apply cherry-picks the pending commits (with -x) one at a time and stops at the first real conflict.
# This script never checks out a branch, never stashes, never resets and never pushes.
#
# Usage (from the workspace root):
#   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1
#   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Apply
#   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Source staging
#   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Apply -Exclude 53ff0f8
#   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Adapt
#   pwsh -NoProfile -File .claude/commands/sync-cursormerged.ps1 -Check
#
# -Adapt fits what the picks brought in to the cursorMerged layout: moves files out of pre-merge
# folders, removes run logs, rewrites imports that still point at the pre-merge layout. It runs by
# itself at the end of a finished -Apply. Changes are left uncommitted for review.
# -Check runs the TypeScript compiler and lists the errors in files this sync touched.
#
# Exit codes: 0 plan printed / up to date / applied, 2 precondition failed, 3 no watermark found,
#             4 working tree blocks the apply, 5 git error, 6 -Adapt or -Check left something to fix,
#             10 stopped on a cherry-pick conflict.

[CmdletBinding()]
param(
    # Cherry-pick the pending commits. Without it the script only prints the plan.
    [switch]$Apply,
    # Which source branch to process. Order is always staging first, then cursor.
    [ValidateSet('all', 'staging', 'cursor')][string]$Source = 'all',
    # Manual watermark: the last origin/staging commit that is already on cursorMerged.
    [string]$StagingBase,
    # Manual watermark: the last origin/cursor commit that is already on cursorMerged.
    [string]$CursorBase,
    # Commit hashes (prefixes are fine) to leave out of this run.
    [string[]]$Exclude = @(),
    # Allow -Apply while tracked files are modified, as long as none of them is touched by a pending commit.
    [switch]$AllowDirty,
    # Skip `git fetch` (use the remote-tracking refs as they are).
    [switch]$NoFetch,
    # Only fit the not-yet-pushed commits to the cursorMerged layout (no fetch, no cherry-pick).
    [switch]$Adapt,
    # Only type-check and list errors in files the not-yet-pushed commits touched.
    [switch]$Check
)

$ErrorActionPreference = 'Continue'
# `pwsh -File` passes "a,b" as one string, so split the list here.
$Exclude = @($Exclude | ForEach-Object { $_ -split '[,\s]+' } | Where-Object { $_ })
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$Remote = 'origin'
$Target = 'cursorMerged'
$US = [string][char]0x1f

# Files cursorMerged removed on purpose (0c7f33f, 6cd9645, 7d83bc5): CI files that were deleted
# there and run logs in the repository root. A source commit must not bring them back.
$RemovedOnPurpose = @('^\.github/', '^[^/]+\.(log|txt)$')
# Pre-merge layout roots that only exist on `cursor`. On cursorMerged they live under src/.
$OldLayout = @('^tests/(?!regression/)', '^(fixtures|jsons|utils|validations)/')

function Say {
    param([string]$Text = '')
    Write-Host $Text
}

function Invoke-Git {
    $out = & git @args 2>&1
    $script:GitExit = $LASTEXITCODE
    , @($out | ForEach-Object { [string]$_ })
}

function Invoke-GitLines {
    $lines = Invoke-Git @args
    return @($lines | Where-Object { $_ })
}

function Test-GitRef {
    param([string]$Ref)
    $null = Invoke-Git rev-parse -q --verify "$Ref^{commit}"
    return ($script:GitExit -eq 0)
}

function Get-Sha {
    param([string]$Ref)
    $o = Invoke-Git rev-parse -q --verify "$Ref^{commit}"
    if ($script:GitExit -ne 0 -or $o.Count -eq 0) { return $null }
    return $o[0].Trim()
}

function Get-Short {
    param([string]$Sha)
    if (-not $Sha) { return '' }
    return $Sha.Substring(0, [Math]::Min(7, $Sha.Length))
}

function Test-AnyPattern {
    param([string]$Path, [string[]]$Patterns)
    foreach ($p in $Patterns) { if ($Path -match $p) { return $true } }
    return $false
}

# Where a path from the pre-merge `cursor` layout lives on cursorMerged. $null when the path is not pre-merge layout.
function ConvertTo-MergedPath {
    param([string]$Path)
    if ($Path -match '^tests/regression/') { return $null }
    if ($Path -match '^tests/(.+)$') { return "src/tests/$($Matches[1])" }
    if ($Path -match '^jsons/payloads/create/(.+)$') { return "src/backend/jsons/payloads/$($Matches[1])" }
    if ($Path -match '^(fixtures|jsons|utils|validations)/(.+)$') { return "src/backend/$($Matches[1])/$($Matches[2])" }
    if ($Path -match '^mass-imports/(generators|massImportPayloads)/(.+)$') { return "src/backend/mass-imports/$($Matches[1])/$($Matches[2])" }
    return $null
}

function Get-MappedPaths {
    param([string]$Path)
    $r = @($Path)
    $m = ConvertTo-MergedPath $Path
    if ($m) { $r += $m }
    return $r
}

# The file an import path points at, or $null. $Base is an absolute path without a guaranteed extension.
function Resolve-ModuleFile {
    param([string]$Base)
    foreach ($suffix in @('', '.ts', '.tsx', '.d.ts', '.js', '.json')) {
        if (Test-Path -LiteralPath "$Base$suffix" -PathType Leaf) { return "$Base$suffix" }
    }
    $index = Join-Path $Base 'index.ts'
    if (Test-Path -LiteralPath $index -PathType Leaf) { return $index }
    return $null
}

function ConvertTo-RepoPath {
    param([string]$Full)
    return ([System.IO.Path]::GetRelativePath($script:RepoRoot, $Full)) -replace '\\', '/'
}

# Files the not-yet-pushed commits added or changed, plus files the adaptation itself moved (staged).
# The user's own uncommitted edits are deliberately outside this set.
function Get-SyncScope {
    param([string]$Filter = 'AMR')
    $base = "$Remote/$Target"
    $committed = Invoke-GitLines diff --name-only "--diff-filter=$Filter" $base HEAD
    $staged = Invoke-GitLines diff --cached --name-only --diff-filter=AR HEAD
    return @(@($committed) + @($staged) | Where-Object { $_ } | Sort-Object -Unique)
}

# Fits the commits that are on cursorMerged but not yet on origin/cursorMerged to the cursorMerged layout.
# Leaves every change uncommitted. Returns the number of things it could not fix.
function Invoke-Adaptation {
    $base = "$Remote/$Target"
    $leftover = 0
    Say ''
    Say "Adapting commits $base..HEAD to the $Target layout:"

    # 1. Files that arrived in a pre-merge folder.
    $moved = 0
    foreach ($f in (Invoke-GitLines diff --name-only --diff-filter=A $base HEAD)) {
        if (-not (Test-Path -LiteralPath (Join-Path $script:RepoRoot $f))) { continue }
        if ((Test-AnyPattern $f @('^[^/]+\.(log|txt)$'))) {
            $null = Invoke-Git rm -q -f '--' $f
            Say "  removed  $f  (run output in the repository root)"
            continue
        }
        $to = ConvertTo-MergedPath $f
        if (-not $to) {
            if ($f -match '^\.github/') { Say "  REVIEW   $f  (new CI file; $Target removed most of .github on purpose)"; $leftover++ }
            continue
        }
        if (Test-Path -LiteralPath (Join-Path $script:RepoRoot $to)) {
            Say "  REVIEW   $f  ($to already exists; merge the two by hand)"; $leftover++
            continue
        }
        $null = New-Item -ItemType Directory -Force -Path (Split-Path -Parent (Join-Path $script:RepoRoot $to))
        $o = Invoke-Git mv '--' $f $to
        if ($script:GitExit -ne 0) { Say "  REVIEW   $f  (git mv failed: $($o -join ' '))"; $leftover++ }
        else { Say "  moved    $f  ->  $to"; $moved++ }
    }

    # 2. Imports that still point at the pre-merge layout.
    $byName = @{}
    foreach ($f in (Invoke-GitLines ls-files '--' src)) {
        if ($f -notmatch '\.(ts|tsx|json|js)$') { continue }
        $key = [System.IO.Path]::GetFileNameWithoutExtension($f) -replace '\.d$', ''
        if (-not $byName.ContainsKey($key)) { $byName[$key] = @() }
        $byName[$key] += $f
    }
    $pattern = '(?<pre>\bfrom\s*|\bimport\s*\(\s*|\brequire\s*\(\s*|\bimport\s+)(?<q>[''"])(?<spec>\.{1,2}/[^''"\r\n]+)\k<q>'
    # No '*.ts' pathspec here: PowerShell would expand the wildcard against the current folder.
    $scope = @(Get-SyncScope | Where-Object { $_ -match '^src/.+\.tsx?$' })
    $rewritten = 0
    $unresolved = @()
    foreach ($f in $scope) {
        $full = Join-Path $script:RepoRoot $f
        if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { continue }
        $dir = Split-Path -Parent $full
        $text = [System.IO.File]::ReadAllText($full)
        $head = [byte[]]::new(3)
        $stream = [System.IO.File]::OpenRead($full)
        try { $null = $stream.Read($head, 0, 3) } finally { $stream.Dispose() }
        $hasBom = ($head[0] -eq 0xEF -and $head[1] -eq 0xBB -and $head[2] -eq 0xBF)
        $new = [regex]::Replace($text, $pattern, {
                param($m)
                $spec = $m.Groups['spec'].Value
                $abs = [System.IO.Path]::GetFullPath((Join-Path $dir $spec))
                if (Resolve-ModuleFile $abs) { return $m.Value }

                $rel = ConvertTo-RepoPath $abs
                $tries = @()
                if ($rel -match '^src/(.+)$') { $t = ConvertTo-MergedPath $Matches[1]; if ($t) { $tries += $t } }
                $t = ConvertTo-MergedPath $rel; if ($t) { $tries += $t }
                if ($rel -match '/jsons/payloads/create/') { $tries += ($rel -replace '/jsons/payloads/create/', '/jsons/payloads/') }
                $hit = $null
                foreach ($t in $tries) {
                    $hit = Resolve-ModuleFile (Join-Path $script:RepoRoot $t)
                    if ($hit) { $hit = Join-Path $script:RepoRoot $t; break }
                }
                if (-not $hit) {
                    # Same file name somewhere under src/, and only one candidate shares the longest path tail.
                    $leaf = (Split-Path -Leaf $spec) -replace '\.(ts|tsx|js|json)$', ''
                    if ($byName.ContainsKey($leaf)) {
                        $want = ($rel -replace '\.(ts|tsx|js|json)$', '').Split('/')
                        $best = @(); $bestScore = 0
                        foreach ($cand in $byName[$leaf]) {
                            $have = ($cand -replace '\.(d\.ts|ts|tsx|js|json)$', '').Split('/')
                            $score = 0
                            while ($score -lt $want.Count -and $score -lt $have.Count -and $want[$want.Count - 1 - $score] -eq $have[$have.Count - 1 - $score]) { $score++ }
                            if ($score -gt $bestScore) { $best = @($cand); $bestScore = $score }
                            elseif ($score -eq $bestScore) { $best += $cand }
                        }
                        if ($best.Count -eq 1) { $hit = Join-Path $script:RepoRoot ($best[0] -replace '\.(d\.ts|ts|tsx|js)$', '') }
                    }
                }
                if (-not $hit) { $script:AdaptUnresolved += "$f  ->  $spec"; return $m.Value }

                $out = ([System.IO.Path]::GetRelativePath($dir, $hit)) -replace '\\', '/'
                if ($out -notmatch '^\.') { $out = "./$out" }
                if ($spec -notmatch '\.(ts|tsx|js|json)$') { $out = $out -replace '\.(ts|tsx|js)$', '' }
                $script:AdaptChanges += "$spec  ->  $out"
                return $m.Groups['pre'].Value + $m.Groups['q'].Value + $out + $m.Groups['q'].Value
            })
        if ($new -ne $text) {
            [System.IO.File]::WriteAllText($full, $new, [System.Text.UTF8Encoding]::new($hasBom))
            $n = @($script:AdaptChanges).Count
            Say "  imports  $f  ($n rewritten)"
            $rewritten += $n
        }
        $script:AdaptChanges = @()
    }
    $unresolved = @($script:AdaptUnresolved | Sort-Object -Unique)
    $script:AdaptUnresolved = @()

    Say "  Result: $moved files moved, $rewritten imports rewritten, $($unresolved.Count) imports still unresolved."
    if ($unresolved.Count -gt 0) {
        Say "  Imports to fix by hand (no single matching file under src/):"
        $unresolved | ForEach-Object { Say "    $_" }
        $leftover += $unresolved.Count
    }
    if ($moved + $rewritten -gt 0) { Say "  The changes above are not committed. Review them with 'git diff' and 'git status'." }

    # 3. Things a reader has to settle. Listed, not changed.
    $review = @()
    $addedNow = @(Get-SyncScope -Filter 'A' | Where-Object { $_ -match '^src/(tests|frontend)/.+\.spec\.ts$' })
    $tracked = @(Invoke-GitLines ls-files '--' src/tests src/frontend | Where-Object { $_ -match '\.spec\.ts$' })
    foreach ($f in $addedNow) {
        $full = Join-Path $script:RepoRoot $f
        if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { continue }
        if ([System.IO.File]::ReadAllText($full) -match '\b(test\.)?beforeAll\s*\(') { $review += "$f  uses beforeAll for setup (Rule 40)" }
        if ((Split-Path -Leaf $f) -match '^([A-Za-z]+-\d+)') {
            $key = $Matches[1]
            $same = @($tracked | Where-Object { $_ -ne $f -and $addedNow -notcontains $_ -and (Split-Path -Leaf $_) -match "^(?i)$([regex]::Escape($key))(?!\d)" })
            if ($same.Count -gt 0) { $review += "$f  shares the key $key with a spec that was already on $($Target): $($same -join ', ')" }
        }
    }
    foreach ($f in $scope) {
        $full = Join-Path $script:RepoRoot $f
        if (-not (Test-Path -LiteralPath $full -PathType Leaf)) { continue }
        $n = 0
        foreach ($line in [System.IO.File]::ReadAllLines($full)) {
            $n++
            if ($line -match '^\s*(import|export)\b' -or $line -match '\bfrom\s*[''"]') { continue }
            if ($line -match '[''"`]((\.\./)+|\./)?(tests|fixtures|jsons|utils|validations)/' -and $line -match '(path\.|readFile|__dirname|cwd\(|require)') {
                $review += "$($f):$n  builds a file path with a pre-merge folder name"
            }
        }
    }
    if ($review.Count -gt 0) {
        Say "  Review by reading (nothing was changed for these):"
        $review | ForEach-Object { Say "    $_" }
    }
    return $leftover
}

# Runs the TypeScript compiler. Returns @{ ok; errors = @( @{ raw; file; key } ); output }.
# The key drops line and column, so an error that only moved is still the same error.
function Get-TypeErrors {
    $out = @(& npx tsc --noEmit --pretty false 2>&1 | ForEach-Object { [string]$_ })
    $errors = @()
    foreach ($line in $out) {
        if ($line -match '^(.+?)\(\d+,\d+\): error (TS\d+): (.*)$') {
            $file = $Matches[1] -replace '\\', '/'
            $errors += @{ raw = $line; file = $file; key = "$file|$($Matches[2])|$($Matches[3])" }
        }
    }
    $ok = ($errors.Count -gt 0) -or (($out -join ' ') -notmatch 'error|not found|ERR!')
    return @{ ok = $ok; errors = $errors; output = $out }
}

function Get-BaselinePath {
    return (Invoke-Git rev-parse --git-path sync-cursormerged-tsc-baseline.txt)[0]
}

# Remembers the type errors that exist before the first pick, so -Check can tell new errors from old ones.
function Save-TypeBaseline {
    $r = Get-TypeErrors
    if (-not $r.ok) { Say "WARNING: could not record the type-check baseline (is 'npm install' done?). -Check will fall back to per-file attribution."; return }
    $lines = @("# HEAD $(Get-Sha 'HEAD')") + @($r.errors | ForEach-Object { $_.key })
    [System.IO.File]::WriteAllLines((Join-Path $script:RepoRoot (Get-BaselinePath)), [string[]]$lines)
    Say "Type-check baseline recorded: $($r.errors.Count) errors exist before the sync."
}

# Lists the type errors the sync introduced. With a baseline: every error that was not there before the
# first pick, in any file. Without one: the errors in files the not-yet-pushed commits touched.
function Invoke-TypeCheck {
    Say ''
    $r = Get-TypeErrors
    if (-not $r.ok) {
        Say "Type check: tsc did not run cleanly:"
        $r.output | Select-Object -First 15 | ForEach-Object { Say "    $_" }
        return 1
    }
    $baseFile = Join-Path $script:RepoRoot (Get-BaselinePath)
    $ahead = [int](Invoke-Git rev-list --count "$Remote/$Target..HEAD")[0]
    if ((Test-Path -LiteralPath $baseFile) -and $ahead -gt 0) {
        $left = @{}
        foreach ($k in [System.IO.File]::ReadAllLines($baseFile)) {
            if ($k.StartsWith('#')) { continue }
            if ($left.ContainsKey($k)) { $left[$k]++ } else { $left[$k] = 1 }
        }
        $baseCount = 0; foreach ($v in $left.Values) { $baseCount += $v }
        $new = @()
        foreach ($e in $r.errors) {
            if ($left.ContainsKey($e.key) -and $left[$e.key] -gt 0) { $left[$e.key]-- } else { $new += $e }
        }
        Say "Type check (npx tsc --noEmit), compared with the errors that existed before the first pick:"
        $new | ForEach-Object { Say "  $($_.raw)" }
        Say "  Result: $($new.Count) errors introduced by this sync, $($r.errors.Count - $new.Count) of $baseCount older errors still present."
        return $new.Count
    }

    $scope = @(Get-SyncScope | Where-Object { $_ -match '\.tsx?$' })
    Say "Type check (npx tsc --noEmit), no baseline recorded, errors in the $($scope.Count) TypeScript files the commits $Remote/$Target..HEAD touched:"
    $mine = @($r.errors | Where-Object { $scope -contains $_.file })
    $mine | ForEach-Object { Say "  $($_.raw)" }
    Say "  Result: $($mine.Count) errors in files this sync touched, $($r.errors.Count - $mine.Count) in other files."
    return $mine.Count
}

function Get-CommitLine {
    param([string]$Sha)
    $o = Invoke-Git show -s "--format=%h$($US)%an$($US)%ad$($US)%s" '--date=format:%Y-%m-%d %H:%M' $Sha
    if ($script:GitExit -ne 0 -or $o.Count -eq 0) { return @{ short = (Get-Short $Sha); author = '?'; date = '?'; subject = '?' } }
    $p = $o[0].Split($US)
    return @{ short = $p[0]; author = $p[1]; date = $p[2]; subject = $p[3] }
}

# Everything on the target that can identify a commit that was already brought over.
function Get-TargetIndex {
    param([string]$Ref)
    $fingerprints = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($line in (Invoke-Git log --no-merges "--format=%ae$($US)%at$($US)%s" $Ref)) { [void]$fingerprints.Add($line) }

    $trailers = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($line in (Invoke-Git log '--format=%b' '--grep=cherry picked from commit' $Ref)) {
        foreach ($m in [regex]::Matches($line, '\(cherry picked from commit ([0-9a-f]{40})\)')) { [void]$trailers.Add($m.Groups[1].Value) }
    }

    $markers = @{}
    foreach ($line in (Invoke-Git log '--format=%B' '--grep=^Synced-' $Ref)) {
        if ($line -match '^Synced-(staging|cursor):\s*([0-9a-f]{7,40})\s*$') {
            if (-not $markers.ContainsKey($Matches[1])) { $markers[$Matches[1]] = $Matches[2] }
        }
    }
    return @{ fingerprints = $fingerprints; trailers = $trailers; markers = $markers }
}

function Get-SourcePlan {
    param([string]$Name, [string]$TargetRef, $Index, [string]$BaseOverride)

    $src = "$Remote/$Name"
    $plan = @{
        name = $Name; ref = $src; tip = (Get-Sha $src); status = ''; watermark = $null; signal = ''
        pending = @(); excluded = @(); merges = 0; candidates = 0; note = ''
    }

    # Source commits that are not reachable from the target, newest first.
    $raw = Invoke-Git rev-list --topo-order --no-merges "--format=%ae$($US)%at$($US)%s" "$TargetRef..$src"
    $list = [System.Collections.Generic.List[object]]::new()
    $cur = $null
    foreach ($l in $raw) {
        if ($l -match '^commit ([0-9a-f]{40})') { $cur = $Matches[1]; continue }
        if ($cur) { $list.Add(@{ sha = $cur; fp = $l }); $cur = $null }
    }
    $plan.candidates = $list.Count
    if ($list.Count -eq 0) { $plan.status = 'up-to-date'; return $plan }

    $patchSame = [System.Collections.Generic.HashSet[string]]::new()
    foreach ($l in (Invoke-Git cherry $TargetRef $src)) {
        if ($l -match '^- ([0-9a-f]{40})') { [void]$patchSame.Add($Matches[1]) }
    }

    $signalOf = {
        param($c)
        if ($Index.trailers.Contains($c.sha)) { return 'cherry-pick trailer' }
        if ($Index.fingerprints.Contains($c.fp)) { return 'author + time + subject' }
        if ($patchSame.Contains($c.sha)) { return 'identical patch' }
        return ''
    }

    $wm = $null; $signal = ''
    foreach ($c in $list) {
        $s = & $signalOf $c
        if ($s) { $wm = $c.sha; $signal = $s; break }
    }

    # A Synced-<source> marker written by an earlier sync wins when it is further ahead.
    if ($Index.markers.ContainsKey($Name)) {
        $m = Get-Sha $Index.markers[$Name]
        if ($m) {
            $null = Invoke-Git merge-base --is-ancestor $m $src
            if ($script:GitExit -eq 0) {
                $useMarker = $true
                if ($wm) {
                    $leftMarker = [int](Invoke-Git rev-list --count "$m..$src")[0]
                    $leftMatch = [int](Invoke-Git rev-list --count "$wm..$src")[0]
                    $useMarker = ($leftMarker -lt $leftMatch)
                }
                if ($useMarker) { $wm = $m; $signal = 'Synced- marker' }
            }
        }
    }

    if ($BaseOverride) {
        $b = Get-Sha $BaseOverride
        if (-not $b) { $plan.status = 'error'; $plan.note = "Base '$BaseOverride' is not a commit."; return $plan }
        $null = Invoke-Git merge-base --is-ancestor $b $src
        if ($script:GitExit -ne 0) { $plan.status = 'error'; $plan.note = "Base '$BaseOverride' is not on $src."; return $plan }
        $wm = $b; $signal = 'manual base'
    }

    if (-not $wm) {
        $plan.status = 'no-watermark'
        $plan.note = "$($list.Count) commits on $src are not on $Target and none of them could be matched to a commit on $Target."
        return $plan
    }
    $plan.watermark = $wm; $plan.signal = $signal

    $pendingShas = Invoke-Git rev-list --reverse --topo-order --no-merges $src "^$wm" "^$TargetRef"
    $bySha = @{}
    foreach ($c in $list) { $bySha[$c.sha] = $c }
    foreach ($sha in $pendingShas) {
        $sha = $sha.Trim()
        if (-not $sha) { continue }
        if ($bySha.ContainsKey($sha) -and (& $signalOf $bySha[$sha])) { continue }
        $files = Invoke-GitLines diff-tree --no-commit-id --name-only -r $sha
        $entry = @{ sha = $sha; info = (Get-CommitLine $sha); files = $files }
        $isExcluded = $false
        foreach ($x in $Exclude) { if ($x -and $sha.StartsWith($x.ToLower())) { $isExcluded = $true } }
        if ($isExcluded) { $plan.excluded += $entry } else { $plan.pending += $entry }
    }
    $plan.merges = [int](Invoke-Git rev-list --count --merges $src "^$wm" "^$TargetRef")[0]
    $plan.status = if ($plan.pending.Count -gt 0) { 'pending' } else { 'up-to-date' }
    return $plan
}

function Write-Plan {
    param($Plan)
    Say ''
    Say "[$($Plan.name)] $($Plan.ref) @ $(Get-Short $Plan.tip)"
    switch ($Plan.status) {
        'error' { Say "  ERROR: $($Plan.note)"; return }
        'no-watermark' {
            Say "  NO WATERMARK: $($Plan.note)"
            Say "  Refusing to guess. Name the last $($Plan.name) commit that is already on $Target with -$([char]::ToUpper($Plan.name[0]) + $Plan.name.Substring(1))Base <sha>."
            return
        }
    }
    if ($Plan.watermark) {
        $w = Get-CommitLine $Plan.watermark
        Say "  Last commit already on $($Target): $($w.short)  $($w.date)  $($w.author)  $($w.subject)"
        Say "  Detected by: $($Plan.signal)"
    }
    if ($Plan.status -eq 'up-to-date' -and $Plan.excluded.Count -eq 0) { Say "  Up to date. Nothing to cherry-pick."; return }
    Say "  Pending ($($Plan.pending.Count)), oldest first:"
    foreach ($c in $Plan.pending) {
        Say ("    {0}  {1}  {2}  {3}  ({4} files)" -f $c.info.short, $c.info.date, $c.info.author, $c.info.subject, $c.files.Count)
    }
    foreach ($c in $Plan.excluded) { Say ("    {0}  EXCLUDED by -Exclude  {1}" -f $c.info.short, $c.info.subject) }
    if ($Plan.merges -gt 0) { Say "  Merge commits in the range (not picked, their commits are picked one by one): $($Plan.merges)" }
}

# ---------------------------------------------------------------------------------------------
# Locate EnergoTS
# ---------------------------------------------------------------------------------------------
$workspaceRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$repo = Join-Path (Join-Path $workspaceRoot 'Cursor-Project') 'EnergoTS'
if (-not (Test-Path -LiteralPath $repo)) { Say "EnergoTS not found at $repo"; exit 2 }

$exitCode = 0
$script:RepoRoot = (Resolve-Path -LiteralPath $repo).Path
$script:AdaptChanges = @()
$script:AdaptUnresolved = @()
Push-Location -LiteralPath $repo
try {
    $mode = if ($Adapt) { 'ADAPT' } elseif ($Check) { 'CHECK' } elseif ($Apply) { 'APPLY' } else { 'PLAN (read-only, fetch only)' }
    Say "EnergoTS $Target sync - $mode"

    # --- Preconditions ---------------------------------------------------------------------
    $branch = (Invoke-Git rev-parse --abbrev-ref HEAD)[0]
    if ($branch -ne $Target) {
        Say "PRECONDITION: EnergoTS is on '$branch', not '$Target'. This script does not switch branches."
        Say "Ask the user before switching (Rule ENERGOTS.0), then run again."
        exit 2
    }
    foreach ($state in @('CHERRY_PICK_HEAD', 'MERGE_HEAD', 'REBASE_HEAD', 'REVERT_HEAD')) {
        if (Test-GitRef $state) {
            Say "PRECONDITION: an unfinished git operation is in progress ($state)."
            Say "Finish it (resolve, 'git add', 'git cherry-pick --continue') or abort it, then run again."
            exit 2
        }
    }

    if ($Adapt -or $Check) {
        $problems = 0
        if ($Adapt) { $problems += Invoke-Adaptation }
        if ($Check) { $problems += Invoke-TypeCheck }
        if ($problems -gt 0) { exit 6 }
        exit 0
    }

    if (-not $NoFetch) {
        $o = Invoke-Git fetch $Remote cursor staging $Target
        if ($script:GitExit -ne 0) { Say "git fetch failed:"; $o | ForEach-Object { Say "  $_" }; exit 5 }
    }
    foreach ($r in @("$Remote/cursor", "$Remote/staging", "$Remote/$Target")) {
        if (-not (Test-GitRef $r)) { Say "PRECONDITION: $r does not exist."; exit 2 }
    }
    if ((Invoke-Git rev-parse --is-shallow-repository)[0] -eq 'true') {
        Say "WARNING: this clone is shallow. Detection reads history, so the result may be wrong."
    }

    # --- Target against its own remote ------------------------------------------------------
    $ahead = [int](Invoke-Git rev-list --count "$Remote/$Target..HEAD")[0]
    $behind = [int](Invoke-Git rev-list --count "HEAD..$Remote/$Target")[0]
    Say "Target: $Target @ $(Get-Short (Get-Sha 'HEAD'))  ($Remote/$Target @ $(Get-Short (Get-Sha "$Remote/$Target")), ahead $ahead, behind $behind)"

    $targetRef = 'HEAD'
    if ($behind -gt 0 -and $ahead -gt 0) {
        Say "PRECONDITION: local $Target and $Remote/$Target have diverged. Reconcile them first (ask the user)."
        if ($Apply) { exit 2 }
        Say "Plan below is computed against local HEAD."
    }
    elseif ($behind -gt 0) {
        if ($Apply) {
            Say "Local $Target is behind its remote. Fast-forwarding."
            $o = Invoke-Git merge --ff-only "$Remote/$Target"
            if ($script:GitExit -ne 0) { Say "Fast-forward failed:"; $o | ForEach-Object { Say "  $_" }; exit 5 }
        }
        else {
            Say "Local $Target is behind its remote. -Apply fast-forwards first. Plan below is computed against $Remote/$Target."
            $targetRef = "$Remote/$Target"
        }
    }

    # --- Plan --------------------------------------------------------------------------------
    $names = if ($Source -eq 'all') { @('staging', 'cursor') } else { @($Source) }
    $index = Get-TargetIndex $targetRef
    $plans = @()
    foreach ($n in $names) {
        $base = if ($n -eq 'staging') { $StagingBase } else { $CursorBase }
        $plans += , (Get-SourcePlan -Name $n -TargetRef $targetRef -Index $index -BaseOverride $base)
    }
    foreach ($p in $plans) { Write-Plan $p }

    # --- Working tree ------------------------------------------------------------------------
    $dirty = Invoke-GitLines diff --name-only HEAD
    $untracked = Invoke-GitLines ls-files --others --exclude-standard
    $overlap = @()
    foreach ($p in $plans) {
        foreach ($c in $p.pending) {
            foreach ($f in $c.files) {
                $landing = if ($p.name -eq 'cursor') { Get-MappedPaths $f } else { @($f) }
                foreach ($l in $landing) {
                    if ($dirty -contains $l) { $overlap += "$l  (modified locally, touched by $($c.info.short) from $($p.name))" }
                    if ($untracked -contains $l) { $overlap += "$l  (untracked locally, touched by $($c.info.short) from $($p.name))" }
                }
            }
        }
    }
    Say ''
    Say "Working tree: $($dirty.Count) modified tracked files, $($untracked.Count) untracked files."
    if ($overlap.Count -gt 0) {
        Say "  Local changes that pending commits also touch:"
        $overlap | Sort-Object -Unique | ForEach-Object { Say "    $_" }
    }

    $totalPending = 0
    foreach ($p in $plans) { $totalPending += $p.pending.Count }
    $bad = @($plans | Where-Object { $_.status -in @('no-watermark', 'error') })

    if (-not $Apply) {
        Say ''
        if ($bad.Count -gt 0) { Say "Result: detection is incomplete for: $(($bad | ForEach-Object { $_.name }) -join ', ')."; exit 3 }
        if ($totalPending -eq 0) { Say "Result: $Target already has everything from $(($names) -join ' and ')." }
        else { Say "Result: $totalPending commits to cherry-pick. Run again with -Apply after the user agrees." }
        exit 0
    }

    # --- Apply -------------------------------------------------------------------------------
    if ($bad.Count -gt 0) { Say ''; Say "Not applying: detection is incomplete for $(($bad | ForEach-Object { $_.name }) -join ', ')."; exit 3 }
    if ($totalPending -eq 0) {
        $excludedOnly = 0
        foreach ($p in $plans) { $excludedOnly += $p.excluded.Count }
        if ($excludedOnly -eq 0) {
            Say ''; Say "Result: nothing to cherry-pick."
            if ([int](Invoke-Git rev-list --count "$Remote/$Target..HEAD")[0] -gt 0) { $null = Invoke-Adaptation }
            exit 0
        }
    }
    if ($overlap.Count -gt 0) {
        Say ''
        Say "Not applying: local uncommitted changes overlap pending commits (listed above)."
        Say "Ask the user to commit or put those changes aside. This script does not stash or reset."
        exit 4
    }
    if ($dirty.Count -gt 0 -and -not $AllowDirty) {
        Say ''
        Say "Not applying: $($dirty.Count) tracked files are modified. They do not overlap pending commits."
        Say "Ask the user, then run again with -AllowDirty, or have them commit first."
        exit 4
    }

    $startHead = Get-Sha 'HEAD'
    # A sync that starts from the pushed state records which type errors already exist.
    if ([int](Invoke-Git rev-list --count "$Remote/$Target..HEAD")[0] -eq 0 -or -not (Test-Path -LiteralPath (Join-Path $script:RepoRoot (Get-BaselinePath)))) {
        Save-TypeBaseline
    }
    $picked = @(); $skipped = @(); $autoResolved = @()
    $markerNeeded = $false
    $stopped = $false

    foreach ($p in $plans) {
        if ($stopped) { break }
        $lastWasPicked = $true
        if ($p.pending.Count -gt 0) { Say ''; Say "Cherry-picking from $($p.ref):" }
        for ($i = 0; $i -lt $p.pending.Count; $i++) {
            $c = $p.pending[$i]
            $o = Invoke-Git -c merge.directoryRenames=true cherry-pick -x $c.sha
            if ($script:GitExit -eq 0) {
                Say "  picked   $($c.info.short)  $($c.info.subject)"
                $picked += @{ source = $p.name; commit = $c }; $lastWasPicked = $true
                continue
            }
            if (-not (Test-GitRef 'CHERRY_PICK_HEAD')) {
                Say "  FAILED   $($c.info.short)  $($c.info.subject)"
                $o | ForEach-Object { Say "    $_" }
                $exitCode = 5; $stopped = $true; break
            }

            # Paths the target removed on purpose: keep them removed when the source only modified them.
            $stages = @{}
            foreach ($l in (Invoke-Git ls-files -u)) {
                if ($l -match '^\d+ [0-9a-f]+ (\d)\t(.+)$') {
                    if (-not $stages.ContainsKey($Matches[2])) { $stages[$Matches[2]] = @() }
                    $stages[$Matches[2]] += $Matches[1]
                }
            }
            foreach ($path in @($stages.Keys)) {
                $deletedOnTarget = ($stages[$path] -notcontains '2')
                if ($deletedOnTarget -and (Test-AnyPattern $path $RemovedOnPurpose)) {
                    $null = Invoke-Git rm -q -f '--' $path
                    $autoResolved += "$path  (stays deleted, $($c.info.short))"
                    $stages.Remove($path)
                }
            }

            if ($stages.Count -gt 0) {
                Say "  CONFLICT $($c.info.short)  $($c.info.subject)"
                foreach ($path in ($stages.Keys | Sort-Object)) { Say "    unmerged: $path" }
                Say ''
                Say "Stopped with the cherry-pick in progress. Resolve the files above (SKILL: conflict policy),"
                Say "'git add' them, run 'git -c core.editor=true cherry-pick --continue', then run this script with -Apply again."
                $left = @()
                for ($j = $i + 1; $j -lt $p.pending.Count; $j++) { $left += $p.pending[$j].info.short }
                if ($left.Count -gt 0) { Say "Still pending from $($p.name): $($left -join ' ')" }
                $exitCode = 10; $stopped = $true; break
            }

            $null = Invoke-Git diff --cached --quiet
            if ($script:GitExit -eq 0) {
                $null = Invoke-Git cherry-pick --skip
                Say "  skipped  $($c.info.short)  $($c.info.subject)  (no change left to apply on $Target)"
                $skipped += @{ source = $p.name; commit = $c }; $lastWasPicked = $false
            }
            else {
                $o = Invoke-Git -c core.editor=true cherry-pick --continue
                if ($script:GitExit -ne 0) {
                    Say "  FAILED   $($c.info.short) while continuing:"; $o | ForEach-Object { Say "    $_" }
                    $exitCode = 5; $stopped = $true; break
                }
                Say "  picked   $($c.info.short)  $($c.info.subject)  (files removed on $Target were kept removed)"
                $picked += @{ source = $p.name; commit = $c }; $lastWasPicked = $true
            }
        }
        if (-not $stopped) {
            # The newest commit of this source left no trace on the target, so the next run would offer it again.
            if (($p.pending.Count -gt 0 -and -not $lastWasPicked) -or $p.excluded.Count -gt 0) { $markerNeeded = $true }
        }
    }

    # --- Report ------------------------------------------------------------------------------
    $endHead = Get-Sha 'HEAD'
    Say ''
    Say "Summary: $($picked.Count) picked, $($skipped.Count) skipped as already applied. $Target $(Get-Short $startHead) -> $(Get-Short $endHead)."
    if ($autoResolved.Count -gt 0) {
        Say "Kept deleted (removed on $Target on purpose):"
        $autoResolved | ForEach-Object { Say "  $_" }
    }

    if (-not $stopped) {
        Say ''
        Say "Closing commit trailers (put them in the adapt commit when you make one):"
        foreach ($p in $plans) { Say "  Synced-$($p.name): $($p.tip)" }
        if ($markerNeeded) {
            Say "Marker needed: YES. The newest commit of a source was skipped or excluded, so it left no trace."
            Say "Record the trailers above in a commit (git commit --allow-empty if there is nothing to adapt)."
        }
        else { Say "Marker needed: no." }

        $null = Invoke-Adaptation
        Say ''
        Say "Nothing was pushed. Next: run this script with -Check, finish the adaptation (SKILL step 5), commit it, then ask the user before 'git push'."
    }
    exit $exitCode
}
finally {
    Pop-Location
}
