"""
Agents Package - All agents for Phoenix Project

This package contains all agents:
- PhoenixExpert: Q&A agent for Phoenix project
- TestAgent: Automated testing agent
- AgentRegistry: Registry for managing agents
- PhoenixExpertAdapter: Adapter for PhoenixExpert
- IntegrationService: GitLab and Jira integration service (CRITICAL: All agents must use this)
- GlobalRules: Global rules enforcement system (CRITICAL: All agents must follow)
- AgentRouter: Intelligent agent routing and orchestration (CRITICAL: Automatic routing)
"""

from .phoenix_expert import get_phoenix_expert, PhoenixExpert
from .test_agent import get_test_agent, TestAgent, TestType, TestStatus
from .agent_registry import get_agent_registry, AgentRegistry, Agent
from .phoenix_expert_adapter import PhoenixExpertAdapter
from .test_agent_adapter import TestAgentAdapter
from .integration_service import get_integration_service, IntegrationService
from .postman_collection_generator import get_postman_collection_generator, PostmanCollectionGenerator
from .global_rules import get_global_rules, GlobalRules, PermissionStatus
from .agent_router import get_agent_router, AgentRouter

__all__ = [
    'get_phoenix_expert',
    'PhoenixExpert',
    'get_test_agent',
    'TestAgent',
    'TestType',
    'TestStatus',
    'get_agent_registry',
    'AgentRegistry',
    'Agent',
    'PhoenixExpertAdapter',
    'TestAgentAdapter',
    'get_integration_service',
    'IntegrationService',
    'get_postman_collection_generator',
    'PostmanCollectionGenerator',
    'get_global_rules',
    'GlobalRules',
    'PermissionStatus',
    'get_agent_router',
    'AgentRouter',
]

