# Cursor Project

Multi-functional project with Python agents, Java/Gradle libraries, Postman integration, and migration tools.

## 📁 Project Structure

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

## 🚀 Quick Start

### Cursor IDE Setup

**Important**: After transferring the project to a new computer, set up Cursor IDE configuration.

**Quick Setup**:
```powershell
# Windows PowerShell
.\setup-cursor-config.ps1
```

This script automatically:
- ✅ Transfers MCP configuration to Cursor settings
- ✅ Prompts for sensitive values (passwords, tokens)
- ✅ Shows recommended extensions list

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
# Or (if requirements.txt doesn't exist)
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

## 📚 Documentation

- [Architecture Knowledge Base](docs/ARCHITECTURE_KNOWLEDGE_BASE.md)
- [Postman Collection Generator](docs/POSTMAN_COLLECTION_GENERATOR.md)
- [Test Agent Documentation](docs/README_TEST_AGENT.md)
- [GitLab Update Agent](docs/GITLAB_UPDATE_AGENT.md)
- [Phoenix Project Analysis](docs/PHOENIX_PROJECT_ANALYSIS.md)

## 🔧 Technologies

- **Python** - Agents, automation scripts
- **Java/Gradle** - Phoenix Core Library
- **Postman** - API testing and collections

## ⚠️ Important Notes

1. **Secrets**: 
   - API keys, tokens, passwords should be in environment variables
   - Do NOT commit `.env` file to Git

## 📝 License

[Add your license here]

---

**Last Updated**: 2025-01-14

