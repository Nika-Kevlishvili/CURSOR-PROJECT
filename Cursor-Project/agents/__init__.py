"""
Agents Package - All agents for Phoenix Project

This package contains all agents organized by category:
- Main: Primary agents (PhoenixExpert, TestAgent)
- Support: Supporting agents (GitLabUpdateAgent, EnvironmentAccessAgent)
- Core: System components (AgentRegistry, AgentRouter, IntegrationService, GlobalRules)
- Adapters: Adapter classes for agents
- Services: Supporting services (ReportingService, PostmanCollectionGenerator)
- Utils: Utility modules

Structure:
- Main agents: Q&A and testing agents
- Support agents: GitLab and environment access agents
- Core components: Agent management and routing
- Adapters: Agent interface adapters
- Services: Reporting and collection generation
- Utils: Initialization, logging, and helper utilities
"""

# Main Agents
from .Main import (
    get_phoenix_expert, PhoenixExpert,
    get_test_agent, TestAgent, TestType, TestStatus,
    get_bug_finder_agent, BugFinderAgent,
    get_test_case_generator_agent, TestCaseGeneratorAgent,
    get_production_data_reader_agent, ProductionDataReaderAgent,
    get_energo_ts_test_agent, EnergoTSTestAgent
)

# Support Agents
from .Support import (
    get_gitlab_update_agent, GitLabUpdateAgent,
    get_environment_access_agent, EnvironmentAccessAgent, Environment
)

# Core Components
from .Core import (
    get_agent_registry, AgentRegistry, Agent,
    get_agent_router, AgentRouter,
    get_integration_service, IntegrationService,
    get_global_rules, GlobalRules, PermissionStatus
)

# Adapters
from .Adapters import (
    PhoenixExpertAdapter,
    TestAgentAdapter,
    EnvironmentAccessAdapter,
    TestCaseGeneratorAdapter,
    ProductionDataReaderAdapter,
    EnergoTSTestAgentAdapter
)

# Services
from .Services import (
    get_reporting_service, ReportingService, AgentActivity,
    get_postman_collection_generator, PostmanCollectionGenerator
)

# Utils (import separately to avoid circular imports)
# To initialize all agents, use:
#   from agents.Utils import initialize_all_agents
#   initialize_all_agents()

__all__ = [
    'get_phoenix_expert',
    'PhoenixExpert',
    'get_test_agent',
    'TestAgent',
    'TestType',
    'TestStatus',
    'get_bug_finder_agent',
    'BugFinderAgent',
    'get_test_case_generator_agent',
    'TestCaseGeneratorAgent',
    'get_production_data_reader_agent',
    'ProductionDataReaderAgent',
    'get_energo_ts_test_agent',
    'EnergoTSTestAgent',
    'get_gitlab_update_agent',
    'GitLabUpdateAgent',
    'get_agent_registry',
    'AgentRegistry',
    'Agent',
    'PhoenixExpertAdapter',
    'TestAgentAdapter',
    'TestCaseGeneratorAdapter',
    'ProductionDataReaderAdapter',
    'EnergoTSTestAgentAdapter',
    'get_integration_service',
    'IntegrationService',
    'get_postman_collection_generator',
    'PostmanCollectionGenerator',
    'get_global_rules',
    'GlobalRules',
    'PermissionStatus',
    'get_agent_router',
    'AgentRouter',
    'get_reporting_service',
    'ReportingService',
    'AgentActivity',
]

