# Cursor Project

Multi-functional project with **Cursor rules/subagents/skills**, optional Python tooling, Java/Gradle libraries, Postman integration, and migration tools. The **`agents/`** Python package under `Cursor-Project/` is **not** shipped in this workspace; see **`docs/AGENTS_COMPARISON_AND_ALIGNMENT.md`**.

## 📁 Project Structure

```
├── .cursor/             # Cursor IDE configuration (MCP config, extensions, rules)
├── (no agents/ here)    # Cursor: ../.cursor/agents/*.md + ../.cursor/rules/
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

**Manual Setup**: See [`../.cursor/README.md`](../.cursor/README.md) for workspace Cursor config (rules live under `../.cursor/rules/`).

---

### Python (optional scripts / legacy deps)

**Requirements:** Python 3.8+ if you run project scripts or `requirements.txt`.

```bash
python -m venv venv
venv\Scripts\activate   # Windows
pip install -r requirements.txt
```

There is **no** `from agents import ...` package in this tree. For Cursor workflows, use **`.cursor/`** and **`docs/CURSOR_SUBAGENTS.md`**.

### Java/Gradle Project

**Requirements:**
- Java 17+ (required - see `phoenix-core-lib/build.gradle`)
- Gradle wrapper included (no installation needed)

```bash
cd phoenix-core-lib
./gradlew build
```

## 📚 Documentation

- **[Agent / subagent map (canonical paths)](docs/AGENT_SUBAGENT_MAP.md)** · [Cursor subagents](docs/CURSOR_SUBAGENTS.md) · [Commands reference](docs/COMMANDS_REFERENCE.md) · [Rules loading](docs/RULES_LOADING_SYSTEM.md) · [Agents model (current)](docs/AGENTS_COMPARISON_AND_ALIGNMENT.md) · [Historical Python agents docs](docs/HISTORICAL_PYTHON_AGENTS_PACKAGE.md)
- [Architecture Knowledge Base](docs/ARCHITECTURE_KNOWLEDGE_BASE.md)
- [Postman Collection Generator](docs/POSTMAN_COLLECTION_GENERATOR.md)
- [Test Agent Documentation](docs/README_TEST_AGENT.md)
- [GitLab Update Agent](docs/GITLAB_UPDATE_AGENT.md)
- [Phoenix Project Analysis](docs/PHOENIX_PROJECT_ANALYSIS.md)

## 🔧 Technologies

- **Python** - Optional automation scripts / dependencies (no bundled `agents` package)
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

