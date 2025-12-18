# Cursor Project

მრავალფუნქციური პროექტი Python agents, Java/Gradle libraries, Postman ინტეგრაცია და migration tools-ით.

Multi-functional project with Python agents, Java/Gradle libraries, Postman integration, and migration tools.

## 📁 პროექტის სტრუქტურა / Project Structure

```
├── .cursor/             # Cursor IDE configuration (MCP config, extensions, rules)
├── agents/              # Python agents (Phoenix Expert, Test Agent, etc.)
├── config/              # Configuration files (backend architecture, swagger specs, env.example)
├── docs/                # Documentation (architecture, integration guides, setup docs)
├── examples/            # Example scripts (download projects, generate collections, etc.)
├── Phoenix/             # Phoenix Java projects (phoenix-core, phoenix-core-lib, etc.)
├── postman/             # Postman collections and integration
├── setup-cursor-config.ps1  # Script to setup Cursor config on new computer
├── README.md            # Main project documentation
└── requirements.txt     # Python dependencies
```

## 🚀 სწრაფი დაწყება / Quick Start

### Cursor IDE Setup / კურსორის IDE დაყენება

**Important**: ახალ კომპიუტერზე პროექტის გადატანის შემდეგ, დააყენეთ Cursor IDE კონფიგურაცია.

**Important**: After transferring the project to a new computer, set up Cursor IDE configuration.

**Quick Setup**:
```powershell
# Windows PowerShell
.\setup-cursor-config.ps1
```

ეს სკრიპტი ავტომატურად:
- ✅ გადაიტანს MCP კონფიგურაციას Cursor-ის settings-ში
- ✅ ითხოვს sensitive values (passwords, tokens)
- ✅ აჩვენებს რეკომენდებული ექსთენშენების სიას

**Manual Setup**: See [`.cursor/README.md`](.cursor/README.md) for detailed instructions.

---

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

- [Architecture Knowledge Base](docs/ARCHITECTURE_KNOWLEDGE_BASE.md)
- [Postman Collection Generator](docs/POSTMAN_COLLECTION_GENERATOR.md)
- [Test Agent Documentation](docs/README_TEST_AGENT.md)
- [GitLab Update Agent](docs/GITLAB_UPDATE_AGENT.md)
- [Phoenix Project Analysis](docs/PHOENIX_PROJECT_ANALYSIS.md)

## 🔧 ტექნოლოგიები / Technologies

- **Python** - Agents, automation scripts
- **Java/Gradle** - Phoenix Core Library
- **Postman** - API testing and collections

## ⚠️ მნიშვნელოვანი შენიშვნები / Important Notes

1. **Secrets**: 
   - API keys, tokens, passwords უნდა იყოს environment variables-ში
   - API keys, tokens, passwords should be in environment variables
   - არ დაკომიტოთ `.env` ფაილი Git-ში
   - Do NOT commit `.env` file to Git

## 📝 License

[Add your license here]

---

**ბოლო განახლება / Last Updated**: 2025-01-14

