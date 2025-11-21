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

### მიგრაცია / Migration

დეტალური ინფორმაციისთვის იხილეთ [migration/README.md](migration/README.md)

For detailed information see [migration/README.md](migration/README.md)

### Python Agents

```bash
# Setup virtual environment
python -m venv venv
venv\Scripts\activate  # Windows
source venv/bin/activate  # Linux/Mac

# Install dependencies
pip install -r config/requirements_test_agent.txt
```

### Java/Gradle Project

```bash
cd phoenix-core-lib
./gradlew build
```

## 📚 დოკუმენტაცია / Documentation

- [Migration Guide](migration/MIGRATION_GUIDE.md) - სრული მიგრაციის გზამკვლევი
- [Architecture Knowledge Base](docs/ARCHITECTURE_KNOWLEDGE_BASE.md)
- [Postman Collection Generator](docs/POSTMAN_COLLECTION_GENERATOR.md)
- [Test Agent Documentation](docs/README_TEST_AGENT.md)

## 🔧 ტექნოლოგიები / Technologies

- **Python** - Agents, automation scripts
- **Java/Gradle** - Phoenix Core Library
- **Postman** - API testing and collections
- **PowerShell** - Migration and setup scripts

## ⚠️ მნიშვნელოვანი შენიშვნები / Important Notes

1. **Environment Variables**: არ დაკომიტოთ sensitive data Git-ში
   - Do not commit sensitive data to Git
   - გამოიყენეთ `.gitignore` / Use `.gitignore`

2. **Secrets**: 
   - API keys, tokens, passwords უნდა იყოს environment variables-ში
   - API keys, tokens, passwords should be in environment variables

3. **Migration**: 
   - გამოიყენეთ `migration/` დირექტორია setup-ისთვის
   - Use `migration/` directory for setup

## 📝 License

[Add your license here]

---

**ბოლო განახლება / Last Updated**: 2025-01-14

