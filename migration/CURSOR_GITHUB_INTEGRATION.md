# Cursor GitHub Integration Guide / Cursor GitHub-ის ინტეგრაციის გზამკვლევი

ეს გზამკვლევი დაგეხმარებათ GitHub-ის ინტეგრაციის დაყენებაში Cursor-ში.

This guide will help you set up GitHub integration in Cursor.

## 🚀 სწრაფი დაწყება / Quick Start

### ვარიანტი 1: ავტომატური სკრიპტი / Option 1: Automatic Script

```powershell
# Run the setup script
.\setup_cursor_github.ps1 -GitHubUsername "YOUR_USERNAME" -RepositoryName "cursor-project"
```

### ვარიანტი 2: Cursor-ის ჩაშენებული Git / Option 2: Cursor Built-in Git

Cursor-ს აქვს ჩაშენებული Git მხარდაჭერა:

1. **Source Control Panel** (Ctrl+Shift+G ან Cmd+Shift+G)
   - ნახეთ ყველა ცვლილება / View all changes
   - Commit-ის შექმნა / Create commits
   - Push/Pull ოპერაციები / Push/Pull operations

2. **GitHub Authentication in Cursor:**
   - Cursor ავტომატურად იყენებს Git credentials-ს
   - Personal Access Token (PAT) საჭიროა HTTPS-ისთვის
   - SSH keys-ის გამოყენება შესაძლებელია

## 📋 დეტალური ნაბიჯები / Detailed Steps

### 1️⃣ Git-ის დაყენება / Install Git

თუ Git არ არის დაყენებული:

**Windows:**
```powershell
# Option 1: winget
winget install Git.Git

# Option 2: Chocolatey
choco install git

# Option 3: Download from https://git-scm.com/download/win
```

**გადატვირთეთ PowerShell** დაყენების შემდეგ.

### 2️⃣ Git კონფიგურაცია / Configure Git

```powershell
# Set your name and email
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Enable credential storage (Windows)
git config --global credential.helper wincred
```

### 3️⃣ GitHub Authentication / GitHub-ის აუთენტიფიკაცია

#### ვარიანტი A: Personal Access Token (PAT) / Option A: Personal Access Token

1. **PAT-ის შექმნა:**
   - GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Generate new token (classic)
   - Select scopes: `repo` (full control of private repositories)
   - Copy the token

2. **გამოყენება:**
   - Push-ის დროს გამოიყენეთ token პაროლის ნაცვლად
   - ან შეინახეთ Windows Credential Manager-ში

#### ვარიანტი B: GitHub CLI / Option B: GitHub CLI

```powershell
# Install GitHub CLI
winget install GitHub.cli

# Authenticate
gh auth login

# This will automatically configure Git credentials
```

#### ვარიანტი C: SSH Keys / Option C: SSH Keys

```powershell
# Generate SSH key
ssh-keygen -t ed25519 -C "your.email@example.com"

# Add to SSH agent
ssh-add ~/.ssh/id_ed25519

# Copy public key
cat ~/.ssh/id_ed25519.pub

# Add to GitHub: Settings → SSH and GPG keys → New SSH key
```

### 4️⃣ Repository Setup / რეპოზიტორიის Setup

#### ახალი Repository-ის შექმნა / Create New Repository

1. **GitHub-ზე:**
   - გადადით https://github.com/new
   - შეიყვანეთ repository name
   - აირჩიეთ Public ან Private
   - ⚠️ **არ** დაამატოთ README, .gitignore, ან license
   - დააჭირეთ "Create repository"

2. **ლოკალურად:**
```powershell
# Initialize repository (if not already done)
git init

# Add remote
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git

# Or with SSH
git remote add origin git@github.com:YOUR_USERNAME/YOUR_REPO_NAME.git

# Add files and commit
git add .
git commit -m "Initial commit"

# Push to GitHub
git branch -M main
git push -u origin main
```

### 5️⃣ Cursor-ში Git-ის გამოყენება / Using Git in Cursor

#### Source Control Panel

1. **გახსენით Source Control:**
   - `Ctrl+Shift+G` (Windows/Linux)
   - `Cmd+Shift+G` (Mac)
   - ან მენიუდან: View → Source Control

2. **Commit-ის შექმნა:**
   - აირჩიეთ ფაილები staging-ისთვის (+ ღილაკი)
   - შეიყვანეთ commit message
   - დააჭირეთ Commit (✓)

3. **Push/Pull:**
   - Sync Changes (↑↓) - push და pull ერთად
   - Push (↑) - მხოლოდ push
   - Pull (↓) - მხოლოდ pull

#### Git Commands in Terminal

Cursor-ის ჩაშენებულ terminal-ში შეგიძლიათ გამოიყენოთ ყველა Git command:

```powershell
# Status
git status

# Add files
git add .
git add specific-file.py

# Commit
git commit -m "Your commit message"

# Push
git push

# Pull
git pull

# Branch operations
git branch
git checkout -b new-branch
git merge branch-name
```

## 🔐 Security Best Practices / უსაფრთხოების საუკეთესო პრაქტიკები

### ✅ რა უნდა იყოს GitHub-ზე / What Should be on GitHub

- ✅ Source code
- ✅ Configuration templates
- ✅ Documentation
- ✅ Scripts (without sensitive data)
- ✅ README files

### ❌ რა არ უნდა იყოს GitHub-ზე / What Should NOT be on GitHub

- ❌ `.env` files
- ❌ API keys და tokens
- ❌ Passwords
- ❌ Personal information
- ❌ `venv/` directories
- ❌ `__pycache__/` directories
- ❌ IDE settings (`.vscode/`, `.idea/`)

### .gitignore

დარწმუნდით, რომ `.gitignore` ფაილი შეიცავს:

```
.env
*.env
venv/
__pycache__/
*.pyc
.vscode/
.idea/
*.log
```

## 🆘 Troubleshooting / პრობლემების გადაჭრა

### პრობლემა: "Git is not recognized"

**გადაწყვეტა:**
1. დარწმუნდით, რომ Git დაყენებულია
2. გადატვირთეთ PowerShell/Terminal
3. შეამოწმეთ PATH: `$env:PATH`

### პრობლემა: "Authentication failed"

**გადაწყვეტა:**
1. გამოიყენეთ Personal Access Token (PAT) პაროლის ნაცვლად
2. ან გამოიყენეთ GitHub CLI: `gh auth login`
3. ან დააყენეთ SSH keys

### პრობლემა: "remote origin already exists"

**გადაწყვეტა:**
```powershell
# Remove existing remote
git remote remove origin

# Add new remote
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
```

### პრობლემა: "Large files" error

**გადაწყვეტა:**
1. შეამოწმეთ `.gitignore` მუშაობს
2. წაშალეთ დიდი ფაილები Git history-დან (თუ საჭიროა)

## 📚 დამატებითი რესურსები / Additional Resources

- [Git Documentation](https://git-scm.com/doc)
- [GitHub Docs](https://docs.github.com)
- [Cursor Git Integration](https://cursor.sh/docs)
- [GitHub CLI Documentation](https://cli.github.com/manual/)

## 💡 Tips / რჩევები

1. **გამოიყენეთ GitHub CLI** - უფრო მარტივი authentication-ისთვის
2. **SSH Keys** - უფრო უსაფრთხოა ვიდრე HTTPS
3. **Branch Strategy** - გამოიყენეთ branches feature development-ისთვის
4. **Commit Messages** - დაწერეთ meaningful commit messages
5. **Regular Pushes** - push-ეთ ხშირად, რომ არ დაკარგოთ ცვლილებები

---

**ბოლო განახლება / Last Updated**: 2025-01-14

