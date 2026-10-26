param(
    [switch]$SkipJira,
    [string]$Repo = $env:GH_PAGES_REPO
)

$ErrorActionPreference = 'Stop'
$siteRoot = Split-Path -Parent $PSScriptRoot

if (-not $Repo) {
    $Repo = 'Nika-Kevlishvili/asterbit-qa-ai-plan'
}

if (-not $SkipJira) {
    & (Join-Path $PSScriptRoot 'sync-jira.ps1')
}

$gh = Get-Command gh -ErrorAction SilentlyContinue
if (-not $gh) {
    Write-Error 'GitHub CLI (gh) is required. Install it, then run: gh auth login'
}

$auth = & gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host $auth
    Write-Error 'Not logged in to GitHub. Run: gh auth login'
}

$exists = $false
& gh repo view $Repo 1>$null 2>$null
if ($LASTEXITCODE -eq 0) { $exists = $true }

if (-not $exists) {
    Write-Host "Creating public repo $Repo for GitHub Pages"
    & gh repo create $Repo --public --description 'Asterbit QA AI one-year plan site'
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Could not create $Repo"
    }
}

$stage = Join-Path $env:TEMP ('qa-ai-plan-site-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage | Out-Null
Copy-Item (Join-Path $siteRoot 'index.html') $stage
Copy-Item (Join-Path $siteRoot 'README.md') $stage -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path (Join-Path $stage 'data') | Out-Null
Copy-Item (Join-Path $siteRoot 'data\*.json') (Join-Path $stage 'data')
New-Item -ItemType Directory -Path (Join-Path $stage 'scripts') | Out-Null
Copy-Item (Join-Path $siteRoot 'scripts\sync-jira.ps1') (Join-Path $stage 'scripts')
Copy-Item (Join-Path $siteRoot 'scripts\serve.ps1') (Join-Path $stage 'scripts')
New-Item -ItemType Directory -Path (Join-Path $stage '.github\workflows') -Force | Out-Null
Copy-Item (Join-Path $siteRoot '.github\workflows\sync-jira.yml') (Join-Path $stage '.github\workflows') -ErrorAction SilentlyContinue
Set-Content -LiteralPath (Join-Path $stage '.nojekyll') -Value '' -Encoding ascii

Push-Location $stage
try {
    git init -b main | Out-Null
    git add .
    git -c user.email='qa-ai-plan@local' -c user.name='QA AI plan publish' commit -m "Publish QA AI plan site" | Out-Null
    git remote add origin ("https://github.com/$Repo.git")
    git push -u origin main --force
    if ($LASTEXITCODE -ne 0) {
        Write-Error "git push to $Repo failed"
    }
}
finally {
    Pop-Location
    Remove-Item -LiteralPath $stage -Recurse -Force -ErrorAction SilentlyContinue
}

& gh api --method POST "repos/$Repo/pages" -f 'source[branch]=main' -f 'source[path]=/' 1>$null 2>$null

$owner, $name = $Repo.Split('/')
$url = "https://$($owner.ToLower()).github.io/$name/"
Write-Host "GitHub Pages URL (allow 1-2 minutes): $url"
Write-Host "Add Actions secrets JIRA_EMAIL and JIRA_API_TOKEN on $Repo for scheduled sync."
