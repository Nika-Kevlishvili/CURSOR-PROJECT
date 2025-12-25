"""
Initialize All Agents - Auto-register all agents in the system

This module automatically registers all available agents in the AgentRegistry
so they can be discovered and used by AgentRouter.
"""

from typing import Dict, Any, Optional
from agents.Core import get_agent_registry
from agents.Adapters import PhoenixExpertAdapter, TestAgentAdapter, EnvironmentAccessAdapter, TestCaseGeneratorAdapter
from agents.Support import get_gitlab_update_agent


def initialize_all_agents(config: Dict[str, Any] = None) -> None:
    """
    Initialize and register all agents in the AgentRegistry.
    
    This function should be called once at startup to ensure all agents
    are available for routing and consultation.
    
    Args:
        config: Configuration dictionary for agents
    """
    registry = get_agent_registry()
    
    print("\n" + "="*70)
    print("Initializing All Agents")
    print("="*70)
    
    # Register PhoenixExpert
    try:
        phoenix_adapter = PhoenixExpertAdapter()
        registry.register_agent(phoenix_adapter)
        print("✓ Registered PhoenixExpert")
    except Exception as e:
        print(f"✗ Failed to register PhoenixExpert: {e}")
    
    # Register TestAgent
    try:
        test_adapter = TestAgentAdapter()
        registry.register_agent(test_adapter)
        print("✓ Registered TestAgent")
    except Exception as e:
        print(f"✗ Failed to register TestAgent: {e}")
    
    # Register GitLabUpdateAgent (implements Agent interface directly)
    try:
        gitlab_agent = get_gitlab_update_agent(config)
        registry.register_agent(gitlab_agent)
        print("✓ Registered GitLabUpdateAgent")
    except Exception as e:
        print(f"✗ Failed to register GitLabUpdateAgent: {e}")
    
    # Register EnvironmentAccessAgent
    try:
        env_access_adapter = EnvironmentAccessAdapter()
        registry.register_agent(env_access_adapter)
        print("✓ Registered EnvironmentAccessAgent")
    except Exception as e:
        print(f"✗ Failed to register EnvironmentAccessAgent: {e}")
    
    # Register TestCaseGeneratorAgent
    try:
        test_case_generator_adapter = TestCaseGeneratorAdapter()
        registry.register_agent(test_case_generator_adapter)
        print("✓ Registered TestCaseGeneratorAgent")
    except Exception as e:
        print(f"✗ Failed to register TestCaseGeneratorAgent: {e}")
    
    print("-"*70)
    print(f"Total agents registered: {len(registry.list_agents())}")
    print(f"Available agents: {', '.join(registry.list_agents())}")
    print("="*70 + "\n")


# Auto-initialize when imported (optional)
# Disabled by default to avoid circular imports
# Call initialize_all_agents() explicitly when needed
_AUTO_INIT_ENABLED = False

if _AUTO_INIT_ENABLED:
    try:
        initialize_all_agents()
    except Exception as e:
        print(f"Warning: Failed to auto-initialize agents: {e}")

