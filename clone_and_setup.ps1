# Clone and Setup Script for New Computer
# ეს სკრიპტი დაგეხმარებათ სხვა კომპიუტერზე პროექტის გადმოწერაში და setup-ში

param(
    [Parameter(Mandatory=$true)]
    [string]$GitHubUsername,
    
    [Parameter(Mandatory=$true)]
    [string]$RepositoryName,
    
    [string]$ClonePath = "."
)

Write-Host "📥 GitHub Repository Clone and Setup Script" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# Check if git is installed
try {
    $gitVersion = git --version
    Write-Host "✅ Git is installed: $gitVersion" -ForegroundColor Green
} catch {
    Write-Host "❌ Git is not installed. Please install Git first:" -ForegroundColor Red
    Write-Host "   https://git-scm.com/download/win" -ForegroundColor Yellow
    exit 1
}

# Determine clone URL
$cloneUrl = "https://github.com/$GitHubUsername/$RepositoryName.git"
Write-Host "🔗 Repository URL: $cloneUrl" -ForegroundColor Cyan
Write-Host ""

# Ask for confirmation
Write-Host "This will clone the repository to: $(Resolve-Path $ClonePath)" -ForegroundColor Yellow
$confirm = Read-Host "Continue? (y/n)"
if ($confirm -ne 'y' -and $confirm -ne 'Y') {
    Write-Host "Cancelled." -ForegroundColor Yellow
    exit 0
}

# Clone repository
Write-Host ""
Write-Host "📥 Cloning repository..." -ForegroundColor Cyan
try {
    if ($ClonePath -eq ".") {
        # Clone into current directory
        git clone $cloneUrl temp_clone
        Move-Item -Path "temp_clone\*" -Destination "." -Force
        Move-Item -Path "temp_clone\.git" -Destination "." -Force
        Remove-Item -Path "temp_clone" -Force
    } else {
        git clone $cloneUrl $ClonePath
        Set-Location $ClonePath
    }
    Write-Host "✅ Repository cloned successfully!" -ForegroundColor Green
} catch {
    Write-Host "❌ Error cloning repository: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "📋 Next Steps:" -ForegroundColor Cyan
Write-Host "=============" -ForegroundColor Cyan
Write-Host ""

# Check if migration setup script exists
if (Test-Path "migration\setup_new_computer.ps1") {
    Write-Host "✅ Found migration setup script!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🚀 Run the migration setup script:" -ForegroundColor Cyan
    Write-Host "   .\migration\setup_new_computer.ps1" -ForegroundColor Green
    Write-Host ""
    
    $runSetup = Read-Host "Do you want to run the setup script now? (y/n)"
    if ($runSetup -eq 'y' -or $runSetup -eq 'Y') {
        Write-Host ""
        Write-Host "🔧 Running setup script..." -ForegroundColor Cyan
        & ".\migration\setup_new_computer.ps1"
    }
} else {
    Write-Host "⚠️  Migration setup script not found." -ForegroundColor Yellow
    Write-Host "   Manual setup may be required." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "📚 Additional Setup:" -ForegroundColor Cyan
Write-Host "===================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. Python Environment:" -ForegroundColor White
Write-Host "   python -m venv venv" -ForegroundColor Green
Write-Host "   .\venv\Scripts\activate" -ForegroundColor Green
Write-Host "   pip install -r config\requirements_test_agent.txt" -ForegroundColor Green
Write-Host ""
Write-Host "2. Java/Gradle (if needed):" -ForegroundColor White
Write-Host "   cd phoenix-core-lib" -ForegroundColor Green
Write-Host "   .\gradlew build" -ForegroundColor Green
Write-Host ""
Write-Host "3. Environment Variables:" -ForegroundColor White
Write-Host "   - Check migration/QUICK_REFERENCE.md for required variables" -ForegroundColor Gray
Write-Host "   - Set up API keys, tokens, and credentials" -ForegroundColor Gray
Write-Host ""
Write-Host "4. Postman Setup:" -ForegroundColor White
Write-Host "   - Import collections from postman/ directory" -ForegroundColor Gray
Write-Host "   - Configure environments" -ForegroundColor Gray
Write-Host ""
Write-Host "✅ Clone complete! Check the README.md for more information." -ForegroundColor Green
Write-Host ""

