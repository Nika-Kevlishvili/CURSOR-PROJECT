# სწრაფი დაწყება / Quick Start Guide

ეს გზამკვლევი დაგეხმარებათ სწრაფად დააყენოთ პროექტი ახალ კომპიუტერზე.

This guide will help you quickly set up the project on a new computer.

---

## ⚡ 5-წუთიანი Setup (სწრაფი გზა)

### 1. Clone ან Copy პროექტი
```powershell
# Git-დან
git clone <repository-url>
cd Cursor

# ან უბრალოდ copy ფაილები
```

### 2. გაუშვით Setup Script
```powershell
.\migration\setup_new_computer.ps1
```

აირჩიეთ "6. Run All Setup" - ეს ავტომატურად გააკეთებს ყველაფერს!

### 3. Environment Variables
```powershell
.\setup_environment.ps1 -Interactive
.\load_environment.ps1
```

### 4. შემოწმება
```powershell
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
# Create .env from template
.\setup_environment.ps1 -Interactive

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

# Test Python agents
python -c "from agents import get_integration_service; print('OK')"
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

### Agents არ იმპორტირდება
```powershell
# Activate virtual environment
venv\Scripts\activate

# Reinstall dependencies
pip install -r requirements.txt

# Test import
python -c "from agents import get_integration_service"
```

---

## 📚 დამატებითი რესურსები

- [README.md](README.md) - პროექტის აღწერა
- [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md) - Environment variables დეტალები
- [migration/MIGRATION_GUIDE.md](migration/MIGRATION_GUIDE.md) - სრული მიგრაციის გზამკვლევი
- [PORTABILITY_ASSESSMENT.md](PORTABILITY_ASSESSMENT.md) - პორტატულობის შეფასება

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

