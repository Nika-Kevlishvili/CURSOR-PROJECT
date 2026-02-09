"""
EnergoTS Test Agent Adapter - Adapter for EnergoTS Test Management Agent

This adapter implements the Agent interface for EnergoTSTestAgent,
allowing it to be registered in the AgentRegistry and consulted by other agents.
"""

from typing import Dict, List, Any, Optional
from agents.Core.agent_registry import Agent
from agents.Main.energo_ts_test_agent import get_energo_ts_test_agent


class EnergoTSTestAgentAdapter(Agent):
    """
    Adapter for EnergoTS Test Management Agent.
    
    Implements the Agent interface to allow registration in AgentRegistry.
    """
    
    def __init__(self, config: Dict[str, Any] = None):
        """
        Initialize adapter.
        
        Args:
            config: Optional configuration dictionary
        """
        self.config = config or {}
        self.agent = get_energo_ts_test_agent(config)
    
    def get_name(self) -> str:
        """Get agent name."""
        return "EnergoTSTestAgent"
    
    def get_capabilities(self) -> List[str]:
        """Get list of agent capabilities."""
        return [
            "study_energo_ts_tests",
            "copy_convert_tests",
            "create_new_tests",
            "analyze_test_patterns",
            "list_tests_by_domain",
            "energo_ts_test_management",
            "playwright_test_automation",
            "typescript_test_automation"
        ]
    
    def can_help_with(self, query: str) -> bool:
        """
        Check if agent can help with a query.
        
        Args:
            query: User query string
        
        Returns:
            True if agent can help, False otherwise
        """
        query_lower = query.lower()
        
        # Keywords that indicate EnergoTS test management needs
        energo_keywords = [
            'energo',
            'energots',
            'playwright test',
            'test automation',
            'test management',
            'study test',
            'copy test',
            'convert test',
            'create test',
            'test pattern',
            'test analysis',
            'reg-',
            'jira test',
            'test spec',
            '.spec.ts'
        ]
        
        return any(keyword in query_lower for keyword in energo_keywords)
    
    def consult(self, query: str, context: Dict[str, Any] = None) -> Dict[str, Any]:
        """
        Consult with agent about a query.
        
        Args:
            query: User query string
            context: Optional context dictionary
        
        Returns:
            Dictionary with consultation results:
            {
                'agent': str,
                'can_help': bool,
                'response': str,
                'capabilities': List[str],
                'suggested_actions': List[str]
            }
        """
        context = context or {}
        
        can_help = self.can_help_with(query)
        
        if not can_help:
            return {
                'agent': self.get_name(),
                'can_help': False,
                'response': f"{self.get_name()} cannot help with this query. This agent specializes in EnergoTS Playwright test automation.",
                'capabilities': self.get_capabilities(),
                'suggested_actions': []
            }
        
        # Provide helpful response based on query
        response_parts = [f"{self.get_name()} can help with EnergoTS test management."]
        suggested_actions = []
        
        query_lower = query.lower()
        
        if 'study' in query_lower or 'analyze' in query_lower:
            response_parts.append("I can study and analyze existing tests in the EnergoTS project.")
            suggested_actions.append("Use study_test() method to analyze a specific test file")
            suggested_actions.append("Use analyze_test_patterns() to analyze patterns across all tests")
        
        if 'copy' in query_lower or 'convert' in query_lower:
            response_parts.append("I can copy and convert tests to new test scenarios.")
            suggested_actions.append("Use copy_and_convert_test() method to copy and modify tests")
        
        if 'create' in query_lower or 'write' in query_lower or 'new test' in query_lower:
            response_parts.append("I can create new tests following EnergoTS patterns and conventions.")
            suggested_actions.append("Use create_new_test() method to generate new test files")
        
        if 'list' in query_lower or 'find' in query_lower:
            response_parts.append("I can list tests organized by domain.")
            suggested_actions.append("Use list_tests_by_domain() method to find tests")
        
        return {
            'agent': self.get_name(),
            'can_help': True,
            'response': ' '.join(response_parts),
            'capabilities': self.get_capabilities(),
            'suggested_actions': suggested_actions,
            'agent_instance': self.agent
        }
