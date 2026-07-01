# სწრაფი დაწყება / Quick Start Guide

ეს გზამკვლევი დაგეხმარებათ სწრაფად დააყენოთ პროექტი ახალ კომპიუტერზე.

This guide will help you quickly set up the project on a new computer.

---

## ⚡ 3-ნაბიჯიანი Setup (მაქსიმალურად მარტივი)

### 1. Clone პროექტი GitHub-დან
```powershell
git clone https://github.com/Nika-Kevlishvili/cursor-project.git
cd cursor-project
```

### 2. გაუშვით Setup Script
```powershell
.\setup.ps1
```

ეს ავტომატურად:
- ✅ შექმნის .env ფაილს
- ✅ დააინსტალირებს Python dependencies
- ✅ შეამოწმებს Java

### 3. Credentials და Verification
```powershell
# შეავსეთ .env ფაილი credentials-ით
notepad .env

# Load environment variables
.\load_environment.ps1

# შემოწმება
.\verify_setup.ps1
```

**მზადაა!** ✅

---

## 📋 დეტალური Setup (თუ საჭიროა)

### Requirements

- **Python 3.8+** - [Download](https://www.python.org/downloads/)
- **Java 17+** - [Download](https://adoptium.net/)
- **PowerShell 5.1+** (Windows-ზე ჩვეულებრივ უკვე არის)

### Step-by-Step

#### 1. Python Environment
```powershell
# Create virtual environment
python -m venv venv

# Activate (Windows)
venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

#### 2. Environment Variables
```powershell
# .env ფაილი უკვე შექმნილია setup.ps1-ით
# შეავსეთ credentials-ით
notepad .env

# Load environment variables
.\load_environment.ps1
```

**საჭირო Environment Variables:**
- `GITLAB_URL`, `GITLAB_TOKEN`, `GITLAB_PROJECT_ID`
- `JIRA_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN`, `JIRA_PROJECT_KEY`
- `POSTMAN_API_KEY`, `POSTMAN_WORKSPACE_ID`
- `CONFLUENCE_URL` (optional)

#### 3. Java/Gradle
```powershell
cd phoenix-core-lib
.\gradlew.bat build
```

#### 4. Verification
```powershell
# Run verification script
.\verify_setup.ps1

# Optional: confirm Cursor rules folder exists (repo root = folder that contains .cursor/)
Test-Path .cursor\rules
```

---

## 🚨 Troubleshooting

### Python არ მოიძებნება
```powershell
# შეამოწმეთ Python ინსტალირებულია თუ არა
python --version

# თუ არა, დააინსტალირეთ: https://www.python.org/downloads/
```

### Java არ მოიძებნება
```powershell
# შეამოწმეთ Java
java -version

# თუ არა, დააინსტალირეთ Java 17+: https://adoptium.net/
```

### Environment Variables არ მუშაობს
```powershell
# შეამოწმეთ .env ფაილი
Test-Path .env

# თუ არ არსებობს
.\setup_environment.ps1

# Load environment variables
.\load_environment.ps1
```

### Cursor rules / workspace
```powershell
# This project uses .cursor/rules — not a Python agents package under Cursor-Project/agents/
Get-ChildItem .cursor\rules -Recurse -Filter *.mdc -ErrorAction SilentlyContinue | Select-Object -First 5 FullName
```
If you maintain **separate** Python automation, use that project’s own venv and imports — see **`docs/HISTORICAL_PYTHON_AGENTS_PACKAGE.md`** for legacy doc list.

---

## 📚 დამატებითი რესურსები

- [README.md](README.md) - პროექტის აღწერა
- [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md) - Environment variables დეტალები

---

## ✅ Setup Checklist

- [ ] Python 3.8+ ინსტალირებულია
- [ ] Java 17+ ინსტალირებულია
- [ ] პროექტი clone/copy-ია
- [ ] Virtual environment შექმნილია
- [ ] Dependencies ინსტალირებულია
- [ ] .env ფაილი შექმნილია
- [ ] Environment variables დაყენებულია
- [ ] `verify_setup.ps1` გაშვებულია და ყველაფერი OK-ია
- [ ] Python agents მუშაობს
- [ ] Gradle build მუშაობს

---

**ბოლო განახლება / Last Updated:** 2025-01-14

