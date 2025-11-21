# Migration Directory

ეს დირექტორია შეიცავს ყველა migration-ისთვის საჭირო ფაილებს, გზამკვლევებს და სკრიპტებს.

This directory contains all files, guides, and scripts needed for environment migration.

## 📁 შინაარსი / Contents

### 📚 დოკუმენტაცია / Documentation

- **MIGRATION_GUIDE.md** - სრული მიგრაციის გზამკვლევი (13 სექცია)
  - Git/GitLab/GitHub კონფიგურაცია
  - Postman ინტეგრაცია
  - Database connections
  - Confluence ინტეგრაცია
  - Agents და logic
  - Python/Java setup
  - Environment variables
  - Troubleshooting

- **MIGRATION_SUMMARY.md** - მიგრაციის შეჯამება და overview
  - შექმნილი ფაილების სია
  - მიგრაციის პროცესი
  - კრიტიკული ინფორმაცია
  - Checklist

- **QUICK_REFERENCE.md** - სწრაფი მითითება
  - კრიტიკული credentials
  - სწრაფი commands
  - Environment variables

### 🔧 სკრიპტები / Scripts

- **migration_helper.ps1** - მიმდინარე კომპიუტერზე გამოსაყენებელი
  - System requirements შემოწმება
  - Environment variables ექსპორტი
  - Connections ტესტირება
  - Migration checklist შექმნა

- **setup_new_computer.ps1** - ახალ კომპიუტერზე setup-ისთვის
  - Python environment setup
  - Environment variables setup
  - Java/Gradle setup
  - Agents ტესტირება

- **export_postman_collections.ps1** - Postman collections ექსპორტი
  - Collections API-ით ექსპორტი
  - Environments ექსპორტი
  - ლოკალურად შენახვა

### 🔵 GitHub ინტეგრაცია / GitHub Integration

- **GITHUB_SETUP_GUIDE.md** - GitHub-ის setup გზამკვლევი
  - Repository შექმნა
  - პროექტის ატვირთვა
  - Authentication setup
  - სხვა კომპიუტერზე გადმოწერა

- **CURSOR_GITHUB_INTEGRATION.md** - Cursor-ში GitHub ინტეგრაცია
  - Cursor-ის ჩაშენებული Git
  - Source Control Panel
  - GitHub Authentication
  - Git commands

- **setup_github.ps1** - GitHub repository setup script
  - Repository initialization
  - Remote configuration
  - Initial commit

- **setup_cursor_github.ps1** - Cursor GitHub integration setup
  - Git installation check
  - Git configuration
  - Cursor integration

- **clone_and_setup.ps1** - Repository clone და setup
  - GitHub-დან clone
  - პროექტის setup
  - Dependencies installation

## 🚀 სწრაფი დაწყება / Quick Start

### მიმდინარე კომპიუტერზე / Current Computer:

```powershell
# 1. Run migration helper
.\migration\migration_helper.ps1

# 2. Export environment variables
# (Select Option 3 in the menu)

# 3. Export Postman collections
.\migration\export_postman_collections.ps1
```

### ახალ კომპიუტერზე / New Computer:

```powershell
# 1. Copy all files

# 2. Run setup script
.\migration\setup_new_computer.ps1

# 3. Set environment variables
.\migration\environment_variables_export.ps1  # if exists

# 4. Test everything
.\migration\migration_helper.ps1  # Option 6
```

## 📋 რა იქნება გადატანილი / What Gets Migrated

- ✅ Git/GitHub/GitLab - credentials, tokens, project IDs
- ✅ GitHub Integration - setup guides, scripts, Cursor integration
- ✅ Postman - API key, workspace ID, 29 collections, 5 environments
- ✅ Database - connection strings, credentials
- ✅ Confluence - read-only access configuration
- ✅ Agents - ყველა agent და მათი logic
- ✅ Python environment - dependencies, virtual environment
- ✅ Java/Gradle - project setup
- ✅ Environment variables - სრული სია და setup

## 📖 დეტალური ინფორმაცია

დეტალური ინფორმაციისთვის იხილეთ:
- **MIGRATION_GUIDE.md** - სრული გზამკვლევი
- **QUICK_REFERENCE.md** - სწრაფი მითითება
- **MIGRATION_SUMMARY.md** - შეჯამება

## ⚠️ მნიშვნელოვანი შენიშვნები

1. **Security**: 
   - `environment_variables_export.ps1` შეიცავს sensitive data-ს
   - არ დაკომიტოთ Git-ში
   - გამოიყენეთ `.gitignore`

2. **Backup**:
   - რეგულარულად backup-ი გააკეთეთ
   - განსაკუთრებით `postman/` და `config/` დირექტორიები

3. **Testing**:
   - ყოველთვის ტესტირება გააკეთეთ migration-ის შემდეგ
   - გამოიყენეთ `migration_helper.ps1` Option 6

---

**ბოლო განახლება**: 2025-01-14  
**ვერსია**: 1.0

