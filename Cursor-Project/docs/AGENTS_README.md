# Agents Package - Organized Structure

Agents are organized by topic in separate folders.

## 📁 Structure

```
agents/
├── Main/              # Main agents
│   ├── phoenix_expert.py
│   ├── test_agent.py
│   ├── bug_finder_agent.py
│   └── test_case_generator_agent.py
├── Support/           # Support agents
│   ├── gitlab_update_agent.py
│   └── environment_access_agent.py
├── Core/              # Core components
│   ├── agent_registry.py
│   ├── agent_router.py
│   ├── integration_service.py
│   └── global_rules.py
├── Adapters/          # Adapters
│   ├── phoenix_expert_adapter.py
│   ├── test_agent_adapter.py
│   ├── environment_access_adapter.py
│   └── test_case_generator_adapter.py
├── Services/          # Services
│   ├── reporting_service.py
│   └── postman_collection_generator.py
├── Utils/             # Utilities
│   ├── initialize_agents.py
│   ├── rules_loader.py
│   ├── logger_utils.py
│   ├── reporting_helper.py
│   └── ai_response_logger.py
├── __init__.py        # Main exports
└── README.md          # This file
```

## 📝 Categories

### Main Agents
- **PhoenixExpert**: Q&A agent for Phoenix project
- **TestAgent**: Automated testing agent
- **BugFinderAgent**: Bug validation agent
- **TestCaseGeneratorAgent**: Test case generation agent (based on bug or task descriptions)

### Support Agents
- **GitLabUpdateAgent**: Agent for updating projects from GitLab
- **EnvironmentAccessAgent**: Agent for accessing DEV and DEV-2 environments

### Core Components
- **AgentRegistry**: Agent registry
- **AgentRouter**: Intelligent agent routing
- **IntegrationService**: GitLab and Jira integration service
- **GlobalRules**: Global rules system

### Adapters
- **PhoenixExpertAdapter**: PhoenixExpert adapter
- **TestAgentAdapter**: TestAgent adapter
- **EnvironmentAccessAdapter**: EnvironmentAccessAgent adapter
- **TestCaseGeneratorAdapter**: TestCaseGeneratorAgent adapter

### Services
- **ReportingService**: Agent activity reporting service
- **PostmanCollectionGenerator**: Postman collection generation service

### Utils
- **initialize_agents**: Initialize all agents
- **rules_loader**: Load rules from .cursor/rules/ directory
- **logger_utils**: Logging utilities
- **reporting_helper**: Reporting helper functions
- **ai_response_logger**: AI response logging

## 🔧 Usage

### Imports

All agents and components can be imported from the main `agents` package:

```python
# Main agents
from agents import PhoenixExpert, TestAgent, TestCaseGeneratorAgent, get_phoenix_expert, get_test_agent, get_test_case_generator_agent

# Support agents
from agents import GitLabUpdateAgent, EnvironmentAccessAgent

# Core components
from agents import AgentRegistry, AgentRouter, IntegrationService, GlobalRules

# Adapters
from agents import PhoenixExpertAdapter, TestAgentAdapter, TestCaseGeneratorAdapter

# Services
from agents import ReportingService, PostmanCollectionGenerator

# Utils
from agents.Utils import initialize_all_agents
```

Or directly from the category:

```python
from agents.Main import PhoenixExpert, TestAgent, TestCaseGeneratorAgent
from agents.Support import GitLabUpdateAgent
from agents.Core import AgentRegistry
from agents.Adapters import PhoenixExpertAdapter
from agents.Services import ReportingService
from agents.Utils import initialize_all_agents
```

## 📌 Notes

- All imports work with both absolute (`from agents...`) and relative imports
- `__init__.py` files in each folder provide convenient imports
- When adding new agents, please place them in the appropriate folder
