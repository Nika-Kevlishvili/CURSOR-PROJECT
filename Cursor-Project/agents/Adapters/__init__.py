"""
Adapters - Adapter classes for agents

This package contains adapter classes that implement the Agent interface:
- PhoenixExpertAdapter: Adapter for PhoenixExpert
- TestAgentAdapter: Adapter for TestAgent
- EnvironmentAccessAdapter: Adapter for EnvironmentAccessAgent
- TestCaseGeneratorAdapter: Adapter for TestCaseGeneratorAgent
"""

from .phoenix_expert_adapter import PhoenixExpertAdapter
from .test_agent_adapter import TestAgentAdapter
from .environment_access_adapter import EnvironmentAccessAdapter
from .test_case_generator_adapter import TestCaseGeneratorAdapter
from .production_data_reader_adapter import ProductionDataReaderAdapter

__all__ = [
    'PhoenixExpertAdapter',
    'TestAgentAdapter',
    'EnvironmentAccessAdapter',
    'TestCaseGeneratorAdapter',
    'ProductionDataReaderAdapter',
]
