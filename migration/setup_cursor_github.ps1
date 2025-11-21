# Cursor GitHub Integration Setup Script
# ეს სკრიპტი დაგეხმარებათ GitHub-ის ინტეგრაციის დაყენებაში Cursor-ში
# This script will help you set up GitHub integration in Cursor

param(
    [Parameter(Mandatory=$false)]
    [string]$GitHubUsername,
    
    [Parameter(Mandatory=$false)]
    [string]$RepositoryName = "cursor-project",
    
    [Parameter(Mandatory=$false)]
    [string]$UserEmail,
    
    [Parameter(Mandatory=$false)]
    [string]$UserName
)

Write-Host "🚀 Cursor GitHub Integration Setup" -ForegroundColor Cyan
Write-Host "===================================" -ForegroundColor Cyan
Write-Host ""

# Function to check if Git is installed
function Test-GitInstalled {
    try {
        $gitVersion = git --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            return $true
        }
    } catch {
        return $false
    }
    return $false
}

# Function to check if GitHub CLI is installed
function Test-GitHubCLIInstalled {
    try {
        $ghVersion = gh --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            return $true
        }
    } catch {
        return $false
    }
    return $false
}

# Check Git installation
Write-Host "📦 Checking Git installation..." -ForegroundColor Cyan
$gitInstalled = Test-GitInstalled

if (-not $gitInstalled) {
    Write-Host "❌ Git is not installed or not in PATH" -ForegroundColor Red
    Write-Host ""
    Write-Host "📥 Please install Git:" -ForegroundColor Yellow
    Write-Host "   1. Download from: https://git-scm.com/download/win" -ForegroundColor Gray
    Write-Host "   2. Or use winget: winget install Git.Git" -ForegroundColor Gray
    Write-Host "   3. Or use Chocolatey: choco install git" -ForegroundColor Gray
    Write-Host ""
    Write-Host "   After installation, restart PowerShell and run this script again." -ForegroundColor Yellow
    Write-Host ""
    
    $installNow = Read-Host "Do you want to try installing Git with winget? (y/n)"
    if ($installNow -eq 'y' -or $installNow -eq 'Y') {
        Write-Host "Installing Git with winget..." -ForegroundColor Cyan
        winget install Git.Git
        Write-Host ""
        Write-Host "⚠️  Please restart PowerShell after installation and run this script again." -ForegroundColor Yellow
        exit
    } else {
        Write-Host "Please install Git manually and run this script again." -ForegroundColor Yellow
        exit
    }
} else {
    $gitVersion = git --version
    Write-Host "✅ Git is installed: $gitVersion" -ForegroundColor Green
}

Write-Host ""

# Check GitHub CLI
Write-Host "📦 Checking GitHub CLI installation..." -ForegroundColor Cyan
$ghInstalled = Test-GitHubCLIInstalled

if ($ghInstalled) {
    $ghVersion = gh --version
    Write-Host "✅ GitHub CLI is installed: $ghVersion" -ForegroundColor Green
} else {
    Write-Host "ℹ️  GitHub CLI is not installed (optional, but recommended)" -ForegroundColor Yellow
    Write-Host "   Install with: winget install GitHub.cli" -ForegroundColor Gray
}
Write-Host ""

# Check if repository is already initialized
$gitInitialized = Test-Path .git

if (-not $gitInitialized) {
    Write-Host "📁 Initializing Git repository..." -ForegroundColor Cyan
    git init
    Write-Host "✅ Git repository initialized" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "✅ Git repository already initialized" -ForegroundColor Green
    Write-Host ""
}

# Configure Git user (if not already configured)
Write-Host "👤 Configuring Git user..." -ForegroundColor Cyan

$currentUserName = git config --get user.name 2>$null
$currentUserEmail = git config --get user.email 2>$null

if (-not $currentUserName) {
    if (-not $UserName) {
        $UserName = Read-Host "Enter your Git user name (or press Enter to skip)"
    }
    if ($UserName) {
        git config user.name $UserName
        Write-Host "✅ Git user name set to: $UserName" -ForegroundColor Green
    }
} else {
    Write-Host "✅ Git user name already configured: $currentUserName" -ForegroundColor Green
}

if (-not $currentUserEmail) {
    if (-not $UserEmail) {
        $UserEmail = Read-Host "Enter your Git email (or press Enter to skip)"
    }
    if ($UserEmail) {
        git config user.email $UserEmail
        Write-Host "✅ Git email set to: $UserEmail" -ForegroundColor Green
    }
} else {
    Write-Host "✅ Git email already configured: $currentUserEmail" -ForegroundColor Green
}
Write-Host ""

# Check remote
Write-Host "🔗 Checking remote configuration..." -ForegroundColor Cyan
$remoteUrl = git remote get-url origin 2>$null

if ($remoteUrl) {
    Write-Host "✅ Remote already configured: $remoteUrl" -ForegroundColor Green
    Write-Host ""
} else {
    Write-Host "ℹ️  No remote repository configured" -ForegroundColor Yellow
    Write-Host ""
    
    # Get GitHub username if not provided
    if (-not $GitHubUsername) {
        $GitHubUsername = Read-Host "Enter your GitHub username"
    }
    
    if (-not $RepositoryName) {
        $RepositoryName = Read-Host "Enter repository name (default: cursor-project)"
        if (-not $RepositoryName) {
            $RepositoryName = "cursor-project"
        }
    }
    
    Write-Host ""
    Write-Host "📋 Next steps:" -ForegroundColor Cyan
    Write-Host "=============" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "1. Create a new repository on GitHub:" -ForegroundColor White
    Write-Host "   - Go to: https://github.com/new" -ForegroundColor Gray
    Write-Host "   - Repository name: $RepositoryName" -ForegroundColor Gray
    Write-Host "   - Choose Public or Private" -ForegroundColor Gray
    Write-Host "   - DO NOT initialize with README, .gitignore, or license" -ForegroundColor Yellow
    Write-Host "   - Click 'Create repository'" -ForegroundColor Gray
    Write-Host ""
    
    $addRemote = Read-Host "Have you created the repository on GitHub? (y/n)"
    if ($addRemote -eq 'y' -or $addRemote -eq 'Y') {
        Write-Host ""
        Write-Host "🔗 Adding remote repository..." -ForegroundColor Cyan
        
        # Try to add remote
        $remoteUrl = "https://github.com/$GitHubUsername/$RepositoryName.git"
        git remote add origin $remoteUrl
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Remote added: $remoteUrl" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Could not add remote. It might already exist." -ForegroundColor Yellow
            Write-Host "   You can set it manually with:" -ForegroundColor Gray
            Write-Host "   git remote set-url origin $remoteUrl" -ForegroundColor Gray
        }
        Write-Host ""
    }
}

# Check if there are uncommitted changes
Write-Host "📊 Checking repository status..." -ForegroundColor Cyan
$status = git status --porcelain 2>$null

if ($status) {
    Write-Host "📝 Files to be committed:" -ForegroundColor Yellow
    git status --short
    Write-Host ""
    
    $commit = Read-Host "Do you want to commit these changes? (y/n)"
    if ($commit -eq 'y' -or $commit -eq 'Y') {
        Write-Host ""
        Write-Host "📦 Adding files..." -ForegroundColor Cyan
        git add .
        
        $commitMessage = Read-Host "Enter commit message (or press Enter for default)"
        if (-not $commitMessage) {
            $commitMessage = "Initial commit: Project setup"
        }
        
        Write-Host "💾 Creating commit..." -ForegroundColor Cyan
        git commit -m $commitMessage
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Commit created successfully" -ForegroundColor Green
        }
        Write-Host ""
    }
} else {
    Write-Host "✅ Working directory is clean" -ForegroundColor Green
    Write-Host ""
}

# Check if we can push
$remoteUrl = git remote get-url origin 2>$null
if ($remoteUrl) {
    Write-Host "🚀 Ready to push to GitHub!" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To push your code, run:" -ForegroundColor White
    Write-Host "   git branch -M main" -ForegroundColor Green
    Write-Host "   git push -u origin main" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  Note: You may need to authenticate with:" -ForegroundColor Yellow
    Write-Host "   - Personal Access Token (PAT) instead of password" -ForegroundColor Gray
    Write-Host "   - Or use GitHub CLI: gh auth login" -ForegroundColor Gray
    Write-Host ""
    
    $pushNow = Read-Host "Do you want to push now? (y/n)"
    if ($pushNow -eq 'y' -or $pushNow -eq 'Y') {
        Write-Host ""
        Write-Host "🌿 Setting branch to main..." -ForegroundColor Cyan
        git branch -M main
        
        Write-Host "📤 Pushing to GitHub..." -ForegroundColor Cyan
        Write-Host "   (You may be prompted for credentials)" -ForegroundColor Yellow
        git push -u origin main
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✅ Successfully pushed to GitHub!" -ForegroundColor Green
            Write-Host "   Repository: $remoteUrl" -ForegroundColor Gray
        } else {
            Write-Host ""
            Write-Host "⚠️  Push failed. You may need to:" -ForegroundColor Yellow
            Write-Host "   1. Authenticate with GitHub" -ForegroundColor Gray
            Write-Host "   2. Use Personal Access Token (PAT)" -ForegroundColor Gray
            Write-Host "   3. Or use GitHub CLI: gh auth login" -ForegroundColor Gray
        }
    }
}

Write-Host ""
Write-Host "✨ Setup complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📚 Cursor Git Integration:" -ForegroundColor Cyan
Write-Host "   - Cursor has built-in Git support" -ForegroundColor Gray
Write-Host "   - Use Source Control panel (Ctrl+Shift+G)" -ForegroundColor Gray
Write-Host "   - Commit and push directly from Cursor UI" -ForegroundColor Gray
Write-Host ""
Write-Host "🔐 GitHub Authentication Tips:" -ForegroundColor Cyan
Write-Host "   - Use Personal Access Token (PAT) for HTTPS" -ForegroundColor Gray
Write-Host "   - Or use SSH keys for better security" -ForegroundColor Gray
Write-Host "   - GitHub CLI: gh auth login (recommended)" -ForegroundColor Gray
Write-Host ""

