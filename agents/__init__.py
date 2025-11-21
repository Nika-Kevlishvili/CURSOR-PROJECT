"""
Agents Package - All agents for Phoenix Project

This package contains all agents:
- PhoenixExpert: Q&A agent for Phoenix project
- TestAgent: Automated testing agent
- AgentRegistry: Registry for managing agents
- PhoenixExpertAdapter: Adapter for PhoenixExpert
- IntegrationService: GitLab and Jira integration service (CRITICAL: All agents must use this)
"""

from .phoenix_expert import get_phoenix_expert, PhoenixExpert
from .test_agent import get_test_agent, TestAgent, TestType, TestStatus
from .agent_registry import get_agent_registry, AgentRegistry, Agent
from .phoenix_expert_adapter import PhoenixExpertAdapter
from .integration_service import get_integration_service, IntegrationService
from .postman_collection_generator import get_postman_collection_generator, PostmanCollectionGenerator

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
    'get_integration_service',
    'IntegrationService',
    'get_postman_collection_generator',
    'PostmanCollectionGenerator',
]

