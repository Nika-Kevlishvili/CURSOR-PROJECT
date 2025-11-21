# GitHub Repository Setup Script
# ეს სკრიპტი დაგეხმარებათ GitHub-ზე რეპოზიტორიის შექმნაში და კოდის ატვირთვაში

param(
    [Parameter(Mandatory=$true)]
    [string]$GitHubUsername,
    
    [Parameter(Mandatory=$true)]
    [string]$RepositoryName,
    
    [string]$Description = "Cursor Project - Multi-functional project with Python agents, Java/Gradle libraries, and Postman integration"
)

Write-Host "🚀 GitHub Repository Setup Script" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan
Write-Host ""

# Check if git is initialized
if (-not (Test-Path .git)) {
    Write-Host "⚠️  Git repository is not initialized. Initializing..." -ForegroundColor Yellow
    git init
    Write-Host "✅ Git initialized" -ForegroundColor Green
}

# Check if .gitignore exists
if (-not (Test-Path .gitignore)) {
    Write-Host "⚠️  .gitignore not found. Creating default one..." -ForegroundColor Yellow
    # .gitignore already exists, so this is just a check
}

# Check git status
Write-Host "📊 Checking git status..." -ForegroundColor Cyan
$status = git status --porcelain
if ($status) {
    Write-Host "📝 Files to be committed:" -ForegroundColor Yellow
    git status --short
} else {
    Write-Host "✅ Working directory is clean" -ForegroundColor Green
}

Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Cyan
Write-Host "=============" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Create a new repository on GitHub:" -ForegroundColor White
Write-Host "   - Go to: https://github.com/new" -ForegroundColor Gray
Write-Host "   - Repository name: $RepositoryName" -ForegroundColor Gray
Write-Host "   - Description: $Description" -ForegroundColor Gray
Write-Host "   - Choose Public or Private" -ForegroundColor Gray
Write-Host "   - DO NOT initialize with README, .gitignore, or license" -ForegroundColor Yellow
Write-Host "   - Click 'Create repository'" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Add all files and make initial commit:" -ForegroundColor White
Write-Host "   git add ." -ForegroundColor Green
Write-Host "   git commit -m 'Initial commit: Project migration to GitHub'" -ForegroundColor Green
Write-Host ""
Write-Host "3. Add remote and push:" -ForegroundColor White
Write-Host "   git remote add origin https://github.com/$GitHubUsername/$RepositoryName.git" -ForegroundColor Green
Write-Host "   git branch -M main" -ForegroundColor Green
Write-Host "   git push -u origin main" -ForegroundColor Green
Write-Host ""
Write-Host "4. If you need to authenticate:" -ForegroundColor White
Write-Host "   - Use Personal Access Token (PAT) instead of password" -ForegroundColor Yellow
Write-Host "   - Or use GitHub CLI: gh auth login" -ForegroundColor Yellow
Write-Host ""
Write-Host "💡 Alternative: Use GitHub CLI to create repository automatically" -ForegroundColor Cyan
Write-Host "   gh repo create $RepositoryName --public --source=. --remote=origin --push" -ForegroundColor Green
Write-Host ""

# Ask if user wants to proceed with automatic steps
$proceed = Read-Host "Do you want to proceed with adding files and preparing commit? (y/n)"
if ($proceed -eq 'y' -or $proceed -eq 'Y') {
    Write-Host ""
    Write-Host "📦 Adding files to git..." -ForegroundColor Cyan
    git add .
    
    Write-Host "💾 Creating initial commit..." -ForegroundColor Cyan
    git commit -m "Initial commit: Project migration to GitHub"
    
    Write-Host ""
    Write-Host "✅ Files added and committed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "📤 Now you can add the remote and push:" -ForegroundColor Cyan
    Write-Host "   git remote add origin https://github.com/$GitHubUsername/$RepositoryName.git" -ForegroundColor Green
    Write-Host "   git branch -M main" -ForegroundColor Green
    Write-Host "   git push -u origin main" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  Make sure you've created the repository on GitHub first!" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "✨ Setup complete! Good luck with your migration!" -ForegroundColor Green

