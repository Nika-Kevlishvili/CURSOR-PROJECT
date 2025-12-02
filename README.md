# Cursor Project

მრავალფუნქციური პროექტი Python agents, Java/Gradle libraries, Postman ინტეგრაცია და migration tools-ით.

Multi-functional project with Python agents, Java/Gradle libraries, Postman integration, and migration tools.

## 📁 პროექტის სტრუქტურა / Project Structure

```
├── agents/              # Python agents (Phoenix Expert, Test Agent, etc.)
├── config/              # Configuration files (backend architecture, swagger specs)
├── docs/                # Documentation (architecture, integration guides)
├── examples/            # Example scripts
├── migration/             # Migration scripts and guides
├── phoenix-core-lib/     # Java/Gradle library project
└── postman/              # Postman collections and integration
```

## 🚀 სწრაფი დაწყება / Quick Start

**სწრაფი გზა (5 წუთი):** იხილეთ [QUICK_START.md](QUICK_START.md)

**Quick way (5 minutes):** See [QUICK_START.md](QUICK_START.md)

### ⚙️ Environment Variables Setup (მნიშვნელოვანი!)

**ახალ კომპიუტერზე ან GitHub-დან clone-ის შემდეგ:**

```powershell
# 1. Create .env file from template
.\setup_environment.ps1 -Interactive

# 2. Load environment variables
.\load_environment.ps1
```

დეტალური ინფორმაციისთვის იხილეთ [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md)

For detailed information see [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md)

### მიგრაცია / Migration

დეტალური ინფორმაციისთვის იხილეთ [migration/README.md](migration/README.md)

For detailed information see [migration/README.md](migration/README.md)

### Python Agents

**Requirements:**
- Python 3.8+

```bash
# Setup virtual environment
python -m venv venv
venv\Scripts\activate  # Windows
source venv/bin/activate  # Linux/Mac

# Install dependencies
pip install -r requirements.txt
# ან (თუ requirements.txt არ არსებობს)
pip install -r config/requirements_test_agent.txt
```

### Java/Gradle Project

**Requirements:**
- Java 17+ (required - see `phoenix-core-lib/build.gradle`)
- Gradle wrapper included (no installation needed)

```bash
cd phoenix-core-lib
./gradlew build
```

## 📚 დოკუმენტაცია / Documentation

- [Quick Start Guide](QUICK_START.md) - ⚡ სწრაფი დაწყება (5 წუთი)
- [Portability Assessment](PORTABILITY_ASSESSMENT.md) - პორტატულობის შეფასება
- [Migration Guide](migration/MIGRATION_GUIDE.md) - სრული მიგრაციის გზამკვლევი
- [Environment Setup](ENVIRONMENT_SETUP.md) - Environment variables დეტალები
- [Architecture Knowledge Base](docs/ARCHITECTURE_KNOWLEDGE_BASE.md)
- [Postman Collection Generator](docs/POSTMAN_COLLECTION_GENERATOR.md)
- [Test Agent Documentation](docs/README_TEST_AGENT.md)

## 🔧 ტექნოლოგიები / Technologies

- **Python** - Agents, automation scripts
- **Java/Gradle** - Phoenix Core Library
- **Postman** - API testing and collections
- **PowerShell** - Migration and setup scripts

## ⚠️ მნიშვნელოვანი შენიშვნები / Important Notes

1. **Environment Variables** (კრიტიკული!):
   - ახალ კომპიუტერზე **უნდა** დააყენოთ environment variables
   - On new computer you **MUST** set up environment variables
   - გამოიყენეთ `.\setup_environment.ps1` და `.\load_environment.ps1`
   - Use `.\setup_environment.ps1` and `.\load_environment.ps1`
   - იხილეთ [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md) დეტალებისთვის
   - See [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md) for details

2. **Secrets**: 
   - API keys, tokens, passwords უნდა იყოს environment variables-ში
   - API keys, tokens, passwords should be in environment variables
   - არ დაკომიტოთ `.env` ფაილი Git-ში
   - Do NOT commit `.env` file to Git

3. **Migration**: 
   - გამოიყენეთ `migration/` დირექტორია setup-ისთვის
   - Use `migration/` directory for setup

## 📝 License

[Add your license here]

---

**ბოლო განახლება / Last Updated**: 2025-01-14

